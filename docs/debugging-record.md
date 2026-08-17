# E004: `10.0` で登録した価格を `10.00` で検索できない

## 目的

価格カタログが数値としての価格でSKUを識別するなら、`10.0` で登録したSKUは `10.00` でも検索できるべきである。本ラボは、BigDecimalをMapキーに用いるとき、数値比較とオブジェクトの等価性を混同して生じる検索不全を扱う。

## 最初に観測した事実

バグ状態はコミット `639965a`（`BigDecimalのスケール差で価格検索に失敗する状態を再現する`）に保存した。`PriceCatalog` は `HashMap<BigDecimal, String>` を使い、`10.0` をキーに `NOTEBOOK` を登録する。その後、数値として等しい `10.00` で検索する。

```bash
git switch --detach 639965a
mvn --batch-mode test
```

テストは、直接結果、登録件数、登録済み金額、数値比較をそれぞれ観測する。失敗出力は以下のとおりである。

```text
[evidence] registered=10.0 scale=1 requested=10.00 scale=2 compareTo=0 equals=false

数値として同じ10.0と10.00なら同じSKUを返すこと
expected: <Optional[NOTEBOOK]> but was: <Optional.empty>
```

| 観測項目 | 期待 | 実際 | 根拠 |
| --- | --- | --- | --- |
| 直接結果 | `Optional[NOTEBOOK]` | `Optional.empty` | 失敗テスト |
| `compareTo` | `0` | `0` | 観測ログ |
| `equals` | 数値の等価性と同じと予想した | `false` | 観測ログ |
| 登録件数 | `1` | `1` | テスト |
| 登録済み金額 | `10.0` | `10.0` | テスト |

したがって、SKUが未登録なのではない。登録値と要求値の数値は同じだが、Mapのキー照合で使われる等価性が異なることが観測できる。

## テストの境界

純粋な標準ライブラリ挙動であるため、JUnit 5の単体テストを使う。`PriceCatalog#findSku` を公開境界として実行し、戻り値だけでなく、登録件数と元のキーを独立して確認する。

| 要素 | 決定 |
| --- | --- |
| 公開境界 | `PriceCatalog#findSku` |
| 初期状態 | 空の価格カタログ |
| 入力 | `10.0` を登録し、`10.00` を検索する |
| 直接観測 | `findSku` の `Optional` |
| 最終観測 | `entryCount` と `registeredPriceFor` |
| 対照観測 | `compareTo` が `0` であること |

## 仮説と切り分け

| 仮説 | 予測 | 最小実験 | 結果 | 判定 |
| --- | --- | --- | --- | --- |
| SKU登録が失敗した | カタログの登録件数が0、または元のキーでもSKUが得られない | 件数と登録済み金額を確認する | 件数は1、`10.0` は登録済み | 棄却 |
| `10.0` と `10.00` は数値として異なる | `compareTo` が0以外になる | 二値を直接比較する | `compareTo == 0` | 棄却 |
| Mapのキー照合がスケールを含む等価性を使う | 数値比較が等しくても検索が空になる | `equals` と検索結果を同時に観測する | `equals == false`、検索は空 | 採用 |

## 原因

BigDecimalは非スケール化値とスケールから構成され、同じ数値を異なるスケールで表せる。Java SE 21のBigDecimal APIは、自然順序は同一cohortの値を等しいとみなす一方、`equals` は数値と表現の両方が同じでなければならないと説明している。[1]

`HashMap` はキーの `hashCode` と `equals` により照合する。バグ状態で `10.0` と `10.00` はスケールが異なるため `equals` が偽となり、同じ数値であっても別のキーとして扱われた。したがって、`HashMap#get(new BigDecimal("10.00"))` は `Optional.empty` になった。

## 修正

`PriceCatalog` の内部Mapを `HashMap` から `TreeMap` へ置き換えた。

```java
private final Map<BigDecimal, String> skuByPrice = new TreeMap<>();
```

TreeMapはキーの自然順序を使う。BigDecimalの自然順序は `compareTo` に基づき、同じ数値で表現だけが異なる `10.0` と `10.00` を等しいとみなす。[1] [2] そのため、`10.00` による取得は `10.0` で登録したエントリを返す。

修正後のコミットは `c1e8350`（`数値比較で価格キーを検索できるようにする`）である。

## 再現手順

```bash
# バグ状態
git switch --detach 639965a
mvn --batch-mode test

# 修正状態
git switch main
mvn --batch-mode test
```

## 再発防止テスト

修正前から `PriceCatalogTest#数値として同じ金額ならスケールが違ってもSKUを検索できる` を保持している。このテストは、別スケールでの検索結果、登録件数、登録済み金額、数値比較を検証する。修正後の出力でも `compareTo=0` と `equals=false` は変わらないが、SKUの検索は成功する。

```text
[evidence] registered=10.0 scale=1 requested=10.00 scale=2 compareTo=0 equals=false
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 適用範囲と注意点

この修正は、カタログのキー契約が「BigDecimalの表現」ではなく「数値としての価格」である場合に限る。`TreeMap` の自然順序により、`10.0` と `10.00` を別のキーとして保持することはできない。

JavaのComparable APIは、BigDecimalの自然順序が `equals` と一貫しない例外であること、SortedMapやSortedSetで使うときに注意が必要なことを説明している。[2] 金額の小数桁そのものが業務上の意味を持つ場合、または通貨ごとに固定スケールを持つ場合は、入力境界で `setScale` などにより正規化してHashMapのキーに使う設計を検討する。本ラボは通貨、丸め、負のスケール、`stripTrailingZeros`、金額計算の一般的な規約を扱わない。

## References

[1] [Java SE 21 API — BigDecimal](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigDecimal.html)

[2] [Java SE 21 API — Comparable](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Comparable.html)
