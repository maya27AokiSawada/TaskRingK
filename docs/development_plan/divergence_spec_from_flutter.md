# Flutter版からの差分仕様書

**作成日**: 2026-07-07
**適用対象**: Kotlin (Android) / Swift (iOS)
**ステータス**: Kotlin 版で実装済み・Swift 版未実装

本文書は `porting_spec_kotlin_swift.md`（Flutter→ネイティブ移植仕様の正本）に対して、
**Kotlin 版の実装段階で追加・変更した仕様**を列挙する。
Swift 版はこの差分を `porting_spec` と同等の強度で実装すること。

参照元: `docs/Flutter_Version_docs/specifications/porting_spec_kotlin_swift.md`（gitignore 対象・ローカル参照用）

---

## 変更履歴

| 日付 | 差分ID | 内容 |
|---|---|---|
| 2026-07-07 | D-01 | `SharedList.listKind` フィールド追加 |
| 2026-07-07 | D-02 | ShoppingList ビューと ToDo ビューの分岐 |
| 2026-07-07 | D-03 | ToDo ビュー 3 モード（Day / Week / Month） |
| 2026-07-07 | D-04 | `SharedItem.deadline` の時刻対応と表示フォーマット |

---

## D-01 — `SharedList.listKind` フィールド追加

### 背景

Flutter 版には存在しない。リストの UI 表示形式を「買い物チェックリスト」と「ToDo カレンダー」に分けるために新設。

### Firestore ドキュメント（`/SharedGroups/{groupId}/sharedLists/{listId}`）

```diff
 {
   "listId": "...",
   "listName": "...",
+  "listKind": "shoppingList",   // 新規フィールド
   "listType": "shopping",       // 既存フィールド（そのまま残す）
   ...
 }
```

| 値 | 意味 |
|---|---|
| `"shoppingList"` | 買い物チェックリスト（デフォルト） |
| `"toDoList"` | ToDo カレンダービュー |

- **デフォルト値**: `"shoppingList"`
- **既存ドキュメント**: `listKind` フィールドが存在しない場合は `"shoppingList"` にフォールバック
- **ローカルキャッシュ**: `shared_lists` テーブルに `listKind TEXT NOT NULL DEFAULT 'shoppingList'` カラムを追加

### ドメインモデル

```kotlin
// Kotlin（実装済み）
enum class ListKind { SHOPPING_LIST, TO_DO_LIST }

data class SharedList(
    ...
    val listKind: ListKind = ListKind.SHOPPING_LIST,  // 追加
)
```

```swift
// Swift 実装ガイド
enum ListKind: String, Codable {
    case shoppingList = "shoppingList"
    case toDoList     = "toDoList"
}

struct SharedList {
    ...
    var listKind: ListKind = .shoppingList
}
```

---

## D-02 — SharedList 画面の分岐（listKind による）

### 背景

Flutter 版は単一の買い物リスト画面（`SharedListPage`）のみ。
Kotlin 版では `listKind` により表示ビューを切り替える。

### 画面構成

```
SharedListScreen
  ├── TabRow: [ ショッピング ] [ ToDo ]   ← listKind 切替
  ├── listKind == SHOPPING_LIST → ShoppingListView（D-02-A）
  └── listKind == TO_DO_LIST   → TodoScreen（D-03）
```

### D-02-A: ShoppingListView（SHOPPING_LIST 用チェックリスト）

Flutter 版の `SharedListPage` と同等の UI。

| 要素 | 仕様 |
|---|---|
| アイテム行 | Checkbox + アイテム名（購入済み=取り消し線）+ 数量バッジ + 期限ラベル |
| ソート | `activeItems`（`isDeleted=false`）を `registeredDate` 昇順 |
| 期限ラベル | D-04 参照 |
| 空状態 | ショッピングカートアイコン + "アイテムがありません" |

---

## D-03 — ToDo ビュー 3 モード（Day / Week / Month）

`listKind == TO_DO_LIST` のときに表示する新規ビュー群。Flutter 版には存在しない。

### ビューモード

```
TodoScreen
  ├── TabRow: [ 日 ] [ 週 ] [ 月 ]
  ├── DateNavigationHeader: ← {日付ラベル} →
  ├── mode == DAY   → TodoDayView
  ├── mode == WEEK  → TodoWeekView
  └── mode == MONTH → TodoMonthView
```

### D-03-A: Day View（日ビュー）

日付ナビゲーション: 前日 / 翌日ボタン

| セクション | 表示条件 | ソート |
|---|---|---|
| 🔴 期限切れ | `deadline.localDate < today`（今日表示時のみ） | deadline 昇順 |
| 📋 今日 | `deadline.localDate == selectedDate` | deadline 時刻昇順 |
| 📝 期限なし | `deadline == null` | registeredDate 昇順 |

```swift
// Swift 実装ガイド
func categorize(items: [SharedItem], selectedDate: Date)
    -> (overdue: [SharedItem], today: [SharedItem], noDeadline: [SharedItem])
{
    let cal = Calendar.current
    let today = cal.startOfDay(for: Date())
    let selected = cal.startOfDay(for: selectedDate)
    let isToday = selected == today

    let overdue = isToday
        ? items.filter { $0.deadline.map { cal.startOfDay(for: $0) < today } ?? false }
               .sorted { $0.deadline! < $1.deadline! }
        : []
    let todayItems = items
        .filter { $0.deadline.map { cal.startOfDay(for: $0) == selected } ?? false }
        .sorted { ($0.deadline ?? .distantFuture) < ($1.deadline ?? .distantFuture) }
    let noDl = items.filter { $0.deadline == nil }
                    .sorted { $0.registeredDate < $1.registeredDate }
    return (overdue, todayItems, noDl)
}
```

### D-03-B: Week View（週ビュー）

日付ナビゲーション: 前週 / 翌週ボタン

- **週の起点**: 月曜日（ISO 8601: `Calendar(identifier: .iso8601)`）
- 7 曜日セクション（月〜日）を常に表示
- 各セクション: deadline がその日のアイテムを時刻昇順で表示
- 過去の曜日はエラーカラー（赤系）で強調
- 期限なしアイテムは最下部の「期限なし」セクション

```swift
// Swift 実装ガイド: 週の月曜日を取得
func weekStart(from date: Date) -> Date {
    var cal = Calendar(identifier: .iso8601)
    cal.locale = Locale(identifier: "ja_JP")
    return cal.date(from: cal.dateComponents(
        [.yearForWeekOfYear, .weekOfYear], from: date))!
}
```

### D-03-C: Month View（月ビュー・カレンダーグリッド）

日付ナビゲーション: 前月 / 翌月ボタン

| 要素 | 仕様 |
|---|---|
| グリッド | 7 列（月〜日）× 最大 6 行 = 42 セル |
| グリッド起点 | 月初日を含む週の月曜日 |
| 当月以外の日 | グレーアウト |
| 当日セル | アクセントカラーの塗りつぶし円背景 |
| アイテム数バッジ | deadline がその日のアイテム数（0 件は非表示） |
| セル選択 | 下部に選択日のアイテム一覧を表示 |

```swift
// Swift 実装ガイド: グリッド日付生成
func calendarDays(year: Int, month: Int) -> [Date] {
    var cal = Calendar(identifier: .iso8601)
    cal.locale = Locale(identifier: "ja_JP")
    let firstDay = cal.date(from: DateComponents(year: year, month: month, day: 1))!
    let gridStart = cal.date(from: cal.dateComponents(
        [.yearForWeekOfYear, .weekOfYear], from: firstDay))!
    return (0..<42).compactMap {
        cal.date(byAdding: .day, value: $0, to: gridStart)
    }
}
```

### 状態保持

| 状態 | 型 | 保持方法 |
|---|---|---|
| `listKind` | `ListKind` | AppStorage / UserDefaults |
| `todoViewMode` | `TodoViewMode` (DAY/WEEK/MONTH) | AppStorage |
| `currentDate` | `Date`（epochDay として Long 保存） | AppStorage |
| 月ビュー `selectedDate` | `Date` | ローカル（ビュー内のみ） |

---

## D-04 — `SharedItem.deadline` の時刻対応

### 背景

Flutter 版では deadline が日付のみとして扱われることが多かった。
Kotlin / Swift 版では **日付 + 時刻** の両方を格納・表示する。

### データ型

`Instant?` / `Date?` — ともに時刻精度を持つ。Firestore Timestamp も秒+ナノ秒精度。  
**backward compatibility**: 既存ドキュメントは `00:00:00` 相当 → 「時刻未設定」として扱う。

### 表示フォーマット

| 条件 | フォーマット | 例 |
|---|---|---|
| 時刻が `00:00:00`（日付のみ）| `yyyy/MM/dd` | `2026/07/15` |
| 時刻が `00:00:00` 以外 | `yyyy/MM/dd HH:mm` | `2026/07/15 14:30` |

```swift
// Swift 実装ガイド
extension Date {
    func deadlineLabel(timeZone: TimeZone = .current) -> String {
        var cal = Calendar.current
        cal.timeZone = timeZone
        let c = cal.dateComponents([.hour, .minute, .second], from: self)
        let hasTime = (c.hour != 0 || c.minute != 0 || c.second != 0)
        let fmt = DateFormatter()
        fmt.locale = Locale(identifier: "ja_JP")
        fmt.timeZone = timeZone
        fmt.dateFormat = hasTime ? "yyyy/MM/dd HH:mm" : "yyyy/MM/dd"
        return fmt.string(from: self)
    }
}
```

### タイムゾーン注意事項

`deadline` を日付比較する際は**必ずローカルタイムゾーンへ変換**してから `startOfDay` を取得すること。UTC のまま比較すると日付がずれる。

### Phase 4 入力 UI 要件

```
┌──────────────────────────────────┐
│  期限日  [2026/07/15] [カレンダー]  │
│  期限時刻 [14:30]    [時計]         │
│  ☐ 時刻なし（日付のみ）            │
└──────────────────────────────────┘
```

「時刻なし」チェック ON → time を `00:00:00 (local)` として Firestore に保存。

---

## まとめ：`porting_spec` との対応表

| 項目 | porting_spec 参照 | 本差分仕様 |
|---|---|---|
| SharedList.listType | §5-3 | 変更なし |
| **SharedList.listKind** | 記載なし | **D-01** |
| SharedList 画面 UI | §3-3（単一ビュー） | **D-02 に差し替え** |
| **ToDo Day / Week / Month** | 記載なし | **D-03** |
| SharedItem.deadline 型 | §5-3 (Timestamp) | 型変更なし |
| **deadline 時刻表示** | 記載なし | **D-04** |
| パスワードリセット | §4-6（変更済み） | porting_spec 参照 |
| メール招待 | §8-4（変更済み） | porting_spec 参照 |
| Sentry DSN | §12-10（変更済み） | porting_spec 参照 |

---

## Kotlin 実装ファイル（Swift 実装の参考）

| 実装対象 | Kotlin ファイル |
|---|---|
| `ListKind` enum + Firestore mapper | `data/mapper/EnumMappers.kt`, `data/mapper/SharedGroupFirestoreMappers.kt` |
| `SharedListScreen`（listKind 切替）| `presentation/shoppinglist/SharedListScreen.kt` |
| `ShoppingListView` | `presentation/shoppinglist/ShoppingListView.kt` |
| `TodoViewMode` enum | `presentation/todo/TodoViewMode.kt` |
| `TodoScreen`（コンテナ + ナビ）| `presentation/todo/TodoScreen.kt` |
| `TodoDayView` | `presentation/todo/TodoDayView.kt` |
| `TodoWeekView` + `weekStart()` | `presentation/todo/TodoWeekView.kt` |
| `TodoMonthView`（カレンダーグリッド）| `presentation/todo/TodoMonthView.kt` |
| `deadline.toDeadlineLabel()` | `presentation/shoppinglist/ShoppingListView.kt`（末尾 internal 拡張） |
