# E004: `10.0` で登録した価格を `10.00` で検索できない

このリポジトリは、Javaの `BigDecimal` を `HashMap` のキーに使ったとき、数値として同じ金額を別スケールで検索できなくなる不具合を学ぶ実行可能なデバッグラボです。バグ状態を再現する失敗テスト、観測ログ、最小修正、回帰テスト、分離したGit履歴を含みます。既定ブランチは修正済みであり、バグ状態は履歴に保存しています。

> 学習する契約：価格カタログが数値としての金額でSKUを識別するなら、`10.0` で登録したSKUを `10.00` でも検索できなければならない。

## 学習の進め方

| 段階 | 実施内容 | 観測すること |
| --- | --- | --- |
| 再現 | バグコミットでテストを実行する | `Optional.empty` となりSKUを検索できない |
| 観測 | 値、スケール、`compareTo`、`equals` を出力する | 数値比較は等しいが、`equals` は偽である |
| 修正 | `HashMap` を `TreeMap` に置き換える | キー照合にBigDecimalの自然順序を使う |
| 回帰防止 | 同じテストを再実行する | 別スケールの数値でもSKUを検索できる |

## 収録済み教材

| ID | テーマ | バグ状態の観測 | 修正後に守る契約 |
| --- | --- | --- | --- |
| E004 | BigDecimalのスケールとMapキー | `10.00` の検索が空になる | 同じ数値は別スケールでも同じ価格キーとして検索できる |

## 必要な環境

| 項目 | 本ラボで検証したバージョン |
| --- | --- |
| JDK | 21.0.11 |
| Maven | 3.8.7 |
| JUnit | 5.11.4 |

## 修正後のテストを実行する

```bash
mvn --batch-mode test
```

テストは、`10.0` をキーとしてSKUを登録した後に、数値として等しい `10.00` で同じSKUを取得できることを確認します。登録件数と登録済み金額も別途確認します。

## バグを自分で再現する

```bash
git switch --detach 639965a
mvn --batch-mode test
# expected: Optional[NOTEBOOK], but was: Optional.empty

git switch main
mvn --batch-mode test
# BUILD SUCCESS
```

## プロジェクト構成

```text
src/main/java/com/example/bigdecimalkey/
└── PriceCatalog.java                    # 価格キーとSKUを対応付ける公開境界
src/test/java/com/example/bigdecimalkey/
└── PriceCatalogTest.java                # スケール差の検索契約を表す回帰テスト

docs/
├── topic-brief.md
├── novelty-report.md
├── debugging-record.md
├── bug-state-test-output.log
└── fixed-state-test-output.log
```

Javaの公式APIは、BigDecimalの自然順序が同じ数値で表現の異なる値を等しいとみなす一方、`equals` は数値と表現の両方が同じであることを要求すると説明しています。[1] また、`BigDecimal` の自然順序は `equals` と一貫しないため、`SortedMap` や `SortedSet` で使うときには注意が必要です。[1] 本ラボでは、カタログが「数値としての価格」でSKUを一意にするという契約を明示しているため、`TreeMap` を選択します。

詳細な仮説比較と証拠は、[デバッグ記録](docs/debugging-record.md)を参照してください。

## References

[1] [Java SE 21 API — BigDecimal](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigDecimal.html)

[2] [Java SE 21 API — Comparable](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Comparable.html)
