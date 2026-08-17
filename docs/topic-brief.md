# 題材企画: `10.0` で登録した価格を `10.00` で検索できない

## 対象

| 項目 | 内容 |
| --- | --- |
| 対象言語 | Java 21 |
| 対象読者 | 金額をBigDecimalで扱い、MapやSetのキー・要素に用いるJava開発者 |
| 難易度プロファイル | 基礎から実践 |
| 選定理由 | 数値比較とオブジェクト等価性の差が、単純に見える価格検索契約を壊す。 |
| 実行基盤 | Maven 3.8.7、JUnit 5.11.4、Java標準ライブラリ |
| フレームワーク非依存性 | BigDecimal、HashMap、TreeMapのみで再現する。 |

## 学習する契約

> `10.0` でSKUを登録した価格カタログは、数値として同じ `10.00` で検索したときに同じSKUを返すべきである。しかしHashMapを使うバグ状態では `Optional.empty` になる。

### 対象の直接原因

BigDecimalは数値として等しい値でもスケールが異なれば `equals` が偽となる。HashMapはキー照合に `equals` と `hashCode` を使うため、`10.0` と `10.00` が異なるキーとして扱われる。[1]

### 対象外

通貨別の固定小数桁、丸め、負のスケール、`stripTrailingZeros`、金額計算、JPA永続化、JSONシリアライズ、Springの入力変換は扱わない。

## 再現設計

| 要素 | 決定 |
| --- | --- |
| 公開境界 | `PriceCatalog#findSku` |
| 入力・初期状態 | 空のカタログへ `10.0` と `NOTEBOOK` を登録し、`10.00` を検索する。 |
| Redの観測 | SKU検索の結果が `Optional.empty` となる。 |
| 最終観測 | 登録件数と登録済みキーを独立して確認する。 |
| 対照観測 | `compareTo` が `0` であること。 |
| 固定状態の検証コマンド | `mvn --batch-mode test` |
| バグ状態の確認コマンド | `mvn --batch-mode test` |

## 仮説

| 仮説 | どう検証または除外するか |
| --- | --- |
| 登録処理そのものが失敗している | カタログの件数と登録済み金額を確認する。 |
| 二つの金額は数値として異なる | `compareTo` を直接観測する。 |
| Mapのキー照合がスケールを含む等価性を使う | `equals` と検索結果を同じテストで観測する。 |

## 予定した履歴

| 順序 | コミットの目的 | 期待する状態 |
| --- | --- | --- |
| 1 | `639965a`：BigDecimalのスケール差で価格検索に失敗する状態を再現する | `Optional[NOTEBOOK]` に対して `Optional.empty` となり失敗する。 |
| 2 | `c1e8350`：数値比較で価格キーを検索できるようにする | 同じテストが成功する。 |

## References

[1] [Java SE 21 API — BigDecimal](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigDecimal.html)
