# リストビュー設計仕様（Kotlin Android）

**作成日**: 2026-07-07
**対象フェーズ**: Phase 3.5（UI構造）/ Phase 4（データ接続）

---

## 1. 概要

`SharedList.listKind` によって表示ビューを分岐する。

| listKind | 表示ビュー |
|---|---|
| `SHOPPING_LIST` | ShoppingList View — 従来のチェックリスト |
| `TO_DO_LIST` | ToDo View — Day / Week / Month の 3 モード |

画面最上部の `TabRow` で両モードを切り替える。
ToDo View 内では `TabRow` でさらに 日 / 週 / 月 を切り替える。

---

## 2. ShoppingList View

### UI 要素

| 要素 | 詳細 |
|---|---|
| アイテム行 | チェックボックス + アイテム名（購入済みは取り消し線）+ 期限ラベル + 数量バッジ |
| ソート順 | `activeItems`（`isDeleted=false`）を `registeredDate` 昇順 |
| 空状態 | ショッピングカートアイコン + "アイテムがありません" |

### 期限フォーマット（§5 参照）

- 時刻が `00:00` → `yyyy/MM/dd`
- 時刻が `00:00` 以外 → `yyyy/MM/dd HH:mm`

---

## 3. ToDo View

### 3-1. 表示モード

| モード | 説明 |
|---|---|
| `DAY`（日） | 選択日のアイテムを「期限切れ / 今日 / 期限なし」にセクション分けして表示 |
| `WEEK`（週） | 選択週（月〜日）をセクション分けし、各日のアイテムを表示 |
| `MONTH`（月） | 月曜日起点カレンダーグリッド + 選択日アイテム一覧 |

### 3-2. Day View 詳細

ヘッダー: `←` 選択日 `→` のナビゲーションボタン付き日付ラベル

| セクション | 表示条件 | ソート |
|---|---|---|
| 🔴 期限切れ | `deadline.toLocalDate() < today` | deadline 昇順 |
| 📋 今日 | `deadline.toLocalDate() == selectedDate` | deadline 時刻昇順 |
| 📝 期限なし | `deadline == null` | registeredDate 昇順 |

> 選択日が今日以外のとき「期限切れ」セクションは非表示。

### 3-3. Week View 詳細

ヘッダー: `←` `yyyy年M月W週目` `→` のナビゲーション

- 月曜日起点で 7 日表示（`previousOrSame(MONDAY)`）
- 各曜日セクション（月〜日）に deadline がその日のアイテムを表示
- アイテムのない曜日はヘッダーのみ（折りたたみなし）

### 3-4. Month View 詳細

ヘッダー: `←` `yyyy年M月` `→` のナビゲーション

- 7 列 × 最大 6 行のカレンダーグリッド（月曜日起点）
- 当月以外の日付はグレーアウト
- 当日セルをアクセントカラーで強調
- 各セルにアイテム数バッジ（0 件は非表示）
- セル選択 → 下部に選択日のアイテム一覧をリスト表示

---

## 4. アイテムの期限（deadline）仕様

### 4-1. データモデル

```kotlin
// domain/model/SharedItem.kt
val deadline: Instant? = null
```

`Instant` は UTC エポックミリ秒であり、**日付・時刻の両方を保持**する。  
Phase 4 の期限入力 UI では `DatePickerDialog` + `TimePickerDialog` を組み合わせ、時刻まで設定可能とする。

### 4-2. 表示フォーマット

| 条件 | フォーマット | 例 |
|---|---|---|
| 時刻が 00:00:00（日付のみ） | `yyyy/MM/dd` | `2026/07/15` |
| 時刻が 00:00:00 以外（時刻あり）| `yyyy/MM/dd HH:mm` | `2026/07/15 14:30` |

タイムゾーン変換は `ZoneId.systemDefault()` を使用。

### 4-3. Firestore 保存仕様

- Firestore Timestamp として保存（秒 + ナノ秒）
- エポック日ではなくエポックミリ秒のまま Round-trip する
- 日付のみ設定時も時刻は 00:00:00 として保存（切り捨て禁止）

### 4-4. Phase 4 入力ダイアログ要件

```
期限設定 UI:
  ┌────────────────────────────────┐
  │  期限日  [2026/07/15] [カレンダー]  │
  │  期限時刻 [14:30]    [時計]        │
  │  ☐ 時刻なし（日付のみ）           │
  └────────────────────────────────┘
```

「時刻なし」チェック ON → time を 00:00:00 に固定して保存。

---

## 5. ファイル構成

```
presentation/
├── shoppinglist/
│   ├── SharedListScreen.kt      ← listKind 切替 + 各ビュー委譲
│   └── ShoppingListView.kt      ← SHOPPING_LIST 用チェックリスト
└── todo/
    ├── TodoViewMode.kt           ← enum: DAY / WEEK / MONTH
    ├── TodoScreen.kt             ← モード切替 + 日付ナビ + ビュー委譲
    ├── TodoDayView.kt            ← 日ビュー
    ├── TodoWeekView.kt           ← 週ビュー
    └── TodoMonthView.kt          ← 月ビュー（カレンダーグリッド）
```

---

## 6. 状態管理

| 画面 | 状態 | 保持方法 |
|---|---|---|
| `SharedListScreen` | `listKind: ListKind` | `rememberSaveable` |
| `TodoScreen` | `viewMode: TodoViewMode` | `rememberSaveable` |
| `TodoScreen` | `currentDate: LocalDate`（epochDay as Long） | `rememberSaveable` |
| `TodoMonthView` | `selectedDate: LocalDate` | `remember`（月ビュー内ローカル） |

Phase 4 でリスト選択・アイテム一覧は ViewModel + StateFlow に移行する。
