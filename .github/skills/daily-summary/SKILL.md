---
name: daily-summary
description: >
  その日の作業内容を開発日報として整理し、関連ドキュメントを最新化する。
  日報、daily update、進捗報告、status update、ふりかえり、EOD summary、今日のまとめ等の依頼が来たときに使う。
  出力先: docs/daily_reports/YYYY-MM/ ディレクトリ
license: Proprietary
metadata:
  owner: team
  version: "1.0"
---

# Daily Summary Skill

## When to Use

- ユーザーが「今日のまとめ」「日報」「EOD」「進捗共有」「status update」などを求めたとき
- 当日のセッション中の作業を開発日報としてまとめたいとき

## 作業定義（3ステップ）

「今日のまとめ」は以下の **3つのステップ** をすべて実行することを意味する。

### Step 1: 日報を書く

ファイル名: `daily_report_YYYYMMDD.md`
保存先: `docs/daily_reports/YYYY-MM/`

当日のセッションコンテキスト（会話履歴・ツール実行結果・ファイル変更・コミット）を情報源として日報を作成する。テンプレートは後述。

### Step 2: ドキュメントの更新

当日の作業内容を確認し、以下のドキュメントに **反映すべき変更がないか** 判断する。

| 対象ドキュメント               | パス                                          | 更新が必要なケース                               |
| ------------------------------ | --------------------------------------------- | ------------------------------------------------ |
| アーキテクチャ設計書           | `ARCHITECTURE.md`                             | レイヤー構成変更、設計方針の追加・変更           |
| Flutter vs Kotlin 比較         | `docs/flutter_vs_kotlin.md`                   | 実装方針の差異が明確になったとき                 |
| 移行ノート                     | `docs/migration_notes.md`                     | Flutter版からの移行手順・方針に変更があるとき    |
| 移植仕様書                     | `docs/specifications/porting_spec_kotlin_swift.md` | 機能仕様の追加・変更・確定があるとき        |
| README                         | `README.md`                                   | 機能追加、フェーズ進捗、セットアップ手順変更     |

**判断基準**:

- 新しいレイヤー・クラス・モジュールを追加した → 該当するドキュメントに記載を追加
- 既存の設計・仕様を変更した → 該当するドキュメントの記載を更新
- バグ修正で動作仕様が明確になった → 仕様書に正しい仕様を明記
- アンチパターンを発見・修正した → ARCHITECTURE.md の禁止事項に追加
- 変更が軽微（リファクタリングのみ、コメント追加等）→ **更新不要**

更新不要と判断した場合は、日報に「ドキュメント更新: なし（理由: 〇〇）」と記載する。

### Step 3: コミット＆プッシュ

日報と更新したドキュメントをすべてコミットし、`master` ブランチにプッシュする。

```bash
git add docs/daily_reports/ ARCHITECTURE.md docs/ README.md
git commit -m "docs: 日報 YYYY-MM-DD + ドキュメント更新"
git push origin master
```

---

## 日報テンプレート

```markdown
# 開発日報 - YYYY年MM月DD日

## 📅 本日の目標

- [x] 完了した目標をチェックボックスで示す
- [ ] 未完了の目標もチェックボックスで示す

---

## ✅ 完了した作業

### 1. 作業タイトル ✅

**Purpose**: この作業の目的を1行で

**Background**: 前提情報・経緯（必要に応じて）

**Problem / Root Cause**:

（問題の原因をコードブロック付きで説明）

```kotlin
// ❌ 問題のあったコード
```

**Solution**:

（修正内容をコードブロック付きで説明）

```kotlin
// ✅ 修正後のコード
```

**検証結果**: テスト結果やログ出力

**Modified Files**:

- `app/src/main/java/com/goshopping/android/path/to/File.kt` （変更内容の簡潔な説明）

**Commit**: `ハッシュ` （コミット済みの場合）
**Status**: ✅ 完了・検証済み

---

### 2. 次の作業タイトル ✅

（同じ構成で記述）

---

## 🐛 発見された問題

### 問題タイトル（修正済みなら ✅、未修正なら ⚠️）

- **症状**: 何が起きたか
- **原因**: 原因の概要
- **対処**: 修正内容
- **状態**: 修正完了 / 未着手 / 調査中

（問題がなければ「（なし）」）

---

## 📊 バグ対応進捗

### 完了 ✅

1. ✅ バグ名（完了日: YYYY-MM-DD）

### 対応中 🔄

1. 🔄 バグ名（Priority: High/Medium/Low）

### 未着手 ⏳

1. ⏳ バグ名（Priority: High/Medium/Low）

### 翌日継続 ⏳

- ⏳ 継続タスク名

---

## 💡 技術的学習事項

### 学習トピック名

**問題パターン**:

```kotlin
// 問題のあるコード例
```

**正しいパターン**:

```kotlin
// 正しいコード例
```

**教訓**: 1〜2文で要約

---

## 🗓 翌日（YYYY-MM-DD）の予定

1. タスク名（優先度順）
2. タスク名

---

## 📝 ドキュメント更新

| ドキュメント | 更新内容 |
|---|---|
| `ARCHITECTURE.md` | 変更の要約 |
| （更新なし） | 理由: 〇〇 |
```

---

## Guidance

### Step 1: 日報作成ルール

- 言語は**日本語**で統一する
- 完了作業は**詳細に**記述する（Background、Root Cause、Solution、Modified Files、Status を含める）
- コードの問題と修正は `❌ Before` / `✅ After` パターンのコードブロック（Kotlin）で示す
- テスト結果がある場合はテーブル形式（`| テスト | 結果 |`）で記載する
- **Modified Files** セクションには変更したファイルパスを列挙する
- **Commit** ハッシュがあれば記録する（なければ省略可）
- バグ対応進捗は**累積管理**する（前日から引き継いだ項目も含める）
- 技術的学習事項にはコードブロック付きの具体例を入れる（言語は Kotlin）
- 事実と推測を混ぜない。不明点は「不明」と明示する
- 当日のセッション中のコンテキスト（会話内容、ツール実行結果、ファイル変更履歴）を情報源として活用する

### Step 2: ドキュメント更新ルール

- 更新前に必ず該当ファイルを読んで現在の内容を確認する
- 既存の構成・フォーマットを維持したまま差分だけ追記・修正する
- 大幅な構成変更が必要な場合はユーザーに確認してから実行する
- 更新した場合は日報の「📝 ドキュメント更新」セクションに変更内容を記載する
- 更新不要の場合もその旨と理由を日報に明記する

### Step 3: コミット＆プッシュルール

- 日報ファイル＋更新ドキュメントをまとめて1コミットにする
- コミットメッセージ形式: `docs: 日報 YYYY-MM-DD + ドキュメント更新`（ドキュメント更新がない場合は `docs: 日報 YYYY-MM-DD`）
- `master` ブランチにプッシュする

## Examples

### Input

「今日の日報作って」

### Output

```markdown
# 開発日報 - 2026年06月04日

## 📅 本日の目標

- [x] Room Databaseのエンティティ定義
- [x] ShoppingListRepositoryインターフェースの実装
- [ ] HiltによるDI設定完了

---

## ✅ 完了した作業

### 1. Room Databaseエンティティ定義 ✅

**Purpose**: ローカルDBのスキーマをKotlinエンティティとして定義する

**Implementation**:

- `ShoppingListEntity` / `ShoppingItemEntity` を Room `@Entity` で定義
- `ShoppingListDao` / `ShoppingItemDao` にCRUDクエリを実装
- `AppDatabase` に `@Database` アノテーションを追加

**Modified Files**:

- `app/src/main/java/com/goshopping/android/data/local/entity/ShoppingListEntity.kt`（新規）
- `app/src/main/java/com/goshopping/android/data/local/dao/ShoppingListDao.kt`（新規）
- `app/src/main/java/com/goshopping/android/data/local/AppDatabase.kt`（新規）

**Status**: ✅ 完了・ビルド成功

---

## 🐛 発見された問題

（なし）

---

## 📊 バグ対応進捗

### 翌日継続 ⏳

- ⏳ HiltによるDI設定

---

## 💡 技術的学習事項

### Room の Flow 返却で collectLatest を使うべき場面

**問題パターン**:

```kotlin
// ❌ collect は新しい値が来ても前の処理が終わるまで待つ
dao.getAll().collect { items -> updateUi(items) }
```

**正しいパターン**:

```kotlin
// ✅ collectLatest は新しい値が来たら前の処理をキャンセルして最新値を処理する
dao.getAll().collectLatest { items -> updateUi(items) }
```

**教訓**: DBの変更を即時反映するUIでは `collectLatest` を使い、古い値の処理を自動キャンセルさせる。

---

## 🗓 翌日（2026-06-05）の予定

1. HiltによるDI設定完了
2. Firebase Auth 連携実装開始

---

## 📝 ドキュメント更新

| ドキュメント | 更新内容 |
|---|---|
| `ARCHITECTURE.md` | Data Layer の Room エンティティ構成を追記 |
```
