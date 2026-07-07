# GoShopping Flutter → Kotlin 移植マスタープラン

**作成日**: 2026-06-30
**対象**: Flutter版 GoShopping (v1.1.0 / Build 16) → Kotlin (Android Native) 移植
**ステータス**: Phase 3 完了（Phase 4 リスト + アイテム 着手前）

このプランは以下のドキュメントを根拠に作成しています。

- [ARCHITECTURE.md](../../ARCHITECTURE.md) — Clean Architecture レイヤー設計
- [README.md](../../README.md) — 技術スタック・フェーズ概要
- [docs/flutter_vs_kotlin.md](../flutter_vs_kotlin.md) — Flutter/Kotlin 実装比較
- [docs/migration_notes.md](../migration_notes.md) — 移植時の気づき・ハマりどころ
- [docs/Flutter_Version_docs/specifications/porting_spec_kotlin_swift.md](../Flutter_Version_docs/specifications/porting_spec_kotlin_swift.md) — 移植仕様の正本（データモデル・同期・セキュリティ）
- [docs/Flutter_Version_docs/specifications/data_classes_reference.md](../Flutter_Version_docs/specifications/data_classes_reference.md) — データモデル定義
- [docs/Flutter_Version_docs/specifications/invitation_system.md](../Flutter_Version_docs/specifications/invitation_system.md) — QR招待仕様
- [docs/Flutter_Version_docs/specifications/notification_system.md](../Flutter_Version_docs/specifications/notification_system.md) — 通知仕様

---

## 1. ゴールとスコープ

### 目的

就活ポートフォリオとして、同一アプリを Flutter と Kotlin の両方で実装し、技術スタックの違いによる設計の変化を示す。**機能等価**かつ **Firestore データ互換**（同じバックエンドを共有できる）を満たすことを最優先とする。

### 移植スコープ（機能等価対象）

| 機能 | 優先度 | 根拠ドキュメント |
|------|--------|------------------|
| メール/パスワード認証（サインアップ・サインイン・サインアウト・リセット） | P0 | porting_spec §4 |
| グループ管理（作成・一覧・選択・削除・離脱） | P0 | porting_spec §3-4, §6 |
| 共有リスト管理（CRUD） | P0 | porting_spec §3-3, §6 |
| アイテム管理（追加・編集・購入・論理削除・差分更新） | P0 | porting_spec §5-3, §12-1 |
| Firestore リアルタイム同期 + Room キャッシュ + オフライン対応 | P0 | porting_spec §7, §12-3 |
| QR招待（作成・スキャン・受諾・手動入力） | P1 | porting_spec §8, invitation_system |
| 通知（作成・受信・既読管理） | P1 | porting_spec §10, notification_system |
| ホワイトボード（手書き・差分保存・グループ/個人） | P2 | porting_spec §9 |
| 設定（UIモード切替・モード切替・言語・アカウント削除） | P2 | porting_spec §3-5 |

### 非スコープ（移植しない／後回し）

- 伝言メッセージ機能（Flutter版でも棚上げ: member_message_feature_shelved.md）
- 組織構造一括セットアップ（GroupStructureConfig 系）— ポートフォリオでは不要
- iOS/Swift 実装（本プランは Android Kotlin に限定）

---

## 2. ターゲット技術スタック

README / ARCHITECTURE に準拠。

```
言語         : Kotlin 1.9+
UI           : Jetpack Compose + Material 3
アーキテクチャ : Clean Architecture (Presentation / Domain / Data)
状態管理      : ViewModel + StateFlow + collectAsStateWithLifecycle
DI           : Hilt
非同期        : Coroutines + Flow（callbackFlow で Firestore 連携）
ローカルDB    : Room（Hive の置き換え。キャッシュ専用）
バックエンド   : Firebase Auth + Cloud Firestore（オフライン永続化 ON）
ナビゲーション : Navigation Compose
QR           : CameraX + ML Kit Barcode Scanning
テスト        : JUnit5 + MockK + Turbine（Flow）+ Compose UI Test
最小SDK      : minSdk 26 / targetSdk 34 / compileSdk 34
```

### データ戦略（porting_spec §2 準拠）

- **Firestore ファースト**: Firestore が唯一の真実 (Source of Truth)
- **Room はキャッシュのみ**: 読み込み高速化・オフライン表示専用
- **Hybrid 戦略**: Firestore 失敗時もローカル書き込みを続行する

---

## 3. ターゲットパッケージ構成

`net.sumomo_planning.goshopping`

```
app/src/main/kotlin/net/sumomo_planning/goshopping/
├── GoShopApplication.kt              # @HiltAndroidApp + Firestore 設定
├── MainActivity.kt                   # @AndroidEntryPoint + NavHost
├── di/                               # Hilt Modules
│   ├── AppModule.kt
│   ├── FirebaseModule.kt
│   ├── DatabaseModule.kt
│   ├── RepositoryModule.kt
│   └── DispatcherModule.kt           # @IoDispatcher 等
├── core/
│   ├── common/                       # Result, 拡張関数, ID生成 (DeviceIdService)
│   ├── network/                      # NetworkMonitor
│   └── ui/                           # 共通 Composable, テーマ
├── domain/
│   ├── model/                        # SharedGroup, SharedList, SharedItem, Whiteboard 等
│   ├── repository/                   # Repository インターフェース
│   └── usecase/                      # UseCase（機能単位）
├── data/
│   ├── remote/
│   │   ├── firestore/                # FirestoreXxxDataSource
│   │   └── dto/                      # Firestore DTO（@DocumentId 等）
│   ├── local/
│   │   ├── room/                     # Database, DAO, Entity
│   │   └── prefs/                    # UserPreferences (DataStore)
│   ├── mapper/                       # DTO/Entity <-> Domain 変換
│   └── repository/                   # HybridXxxRepositoryImpl
└── presentation/
    ├── navigation/                   # NavGraph, Route 定義
    ├── auth/                         # Home (SignIn/SignUp)
    ├── shoppinglist/                 # SharedListScreen + ViewModel
    ├── group/                        # SharedGroupScreen + ViewModel
    ├── settings/                     # SettingsScreen + ViewModel
    ├── whiteboard/                   # WhiteboardEditorScreen + ViewModel
    └── qr/                           # QrScanScreen + ViewModel
```

---

## 4. フェーズ別計画

各フェーズは「成果物」と「完了条件（DoD）」を持つ。P0 を最優先で縦に貫通（認証→グループ→リスト→アイテム→同期）させ、早期に動くアプリを作る。

### Phase 0: 基盤セットアップ

**成果物**
- Android Studio プロジェクト（Empty Compose Activity）
- `build.gradle.kts`（Hilt / Compose BOM / Room / Firebase BOM / Coroutines）
- `google-services.json`（dev: `gotoshop-572b7` / prod: `go-shopping-61515`）配置
- `productFlavors`（dev / prod）と `BuildConfig`（SENTRY_DSN は `.env` 経由, §12-10）
- `GoShopApplication`（`@HiltAndroidApp` + Firestore オフライン永続化 ON, §12-3）

**DoD**: 空アプリが dev/prod 両フレーバーでビルド・起動し、Firebase 初期化が成功する。

> ⚠️ migration_notes の既知の罠: Hilt は `kapt`/`ksp` プラグイン有効化と `@HiltAndroidApp` 付与が必須。

### Phase 1: ドメインモデル + ローカル層

**成果物**
- `domain/model`: `SharedGroup`, `SharedGroupMember`, `SharedList`, `SharedItem`, `Whiteboard`, `DrawingStroke`, `DrawingPoint`, `Notification`, `Invitation` と enum 群（porting_spec Appendix のKotlin定義をそのまま採用）
  - `SharedList.activeItems`（`isDeleted == false` のみ・`registeredDate` 昇順）を計算プロパティで実装（§12-2）
- `data/local/room`: Entity / DAO / Database（キャッシュ用スキーマ）
- `data/local/prefs`: `UserPreferences`（DataStore。ユーザー名等）
- `core/common/DeviceIdService`: グループID `prefix_timestamp` / リストID `prefix_uuid8`（§6-3）

**DoD**: Room の Insert/Query が単体テストで通る。`activeItems` のフィルタ/ソートをテストで検証。

### Phase 2: 認証（P0）

**成果物**
- `FirebaseAuthDataSource` + `AuthRepository` + UseCase（SignUp / SignIn / SignOut / ResetPassword / observeAuthState）
- `presentation/auth`: HomeScreen（サインイン/サインアップ切替・バリデーション・パスワード表示トグル）
- エラーコード → 日本語メッセージ変換（porting_spec §4-7 の表）

**重要な実装順序（porting_spec §4 厳守）**
- サインアップ: ①ローカルキャッシュ全クリア → ②Auth登録 → ③ユーザー名保存 → ④displayName更新 → ⑤`/users/{uid}` 保存
- サインアウト: ①ローカルキャッシュクリア → ②状態リセット → ③`signOut()`（**必ず最後**, §12-8）
- パスワードリセット: `sendPasswordResetEmail`（標準, §4-6。`mail` コレクション方式は廃止）

**DoD**: 実機/エミュで dev フレーバーにサインアップ→サインアウト→サインインが通り、`/users/{uid}` が作成される。

### Phase 3: グループ管理（P0）

**成果物**
- `FirestoreSharedGroupDataSource`（`whereArrayContains("allowedUid", uid)` でクエリ, §12-5）
- `RoomSharedGroupDataSource`
- `HybridSharedGroupRepositoryImpl`（Firestore優先・失敗時ローカル続行, §6-2）
- UseCase: Get / Create / Delete / LeaveGroup
- `presentation/group`: SharedGroupScreen（カード一覧・権限制御・FAB）
  - 削除はオーナーのみ、離脱はメンバーのみ（§3-4）
  - 離脱時はローカルからグループ＋配下リストを即削除しUI即時反映（§12-9）

**DoD**: グループ作成→一覧表示→削除/離脱が動作。オフライン時もローカル反映される。

### Phase 4: リスト + アイテム（P0）

**成果物**
- `FirestoreSharedListDataSource` / `RoomSharedListDataSource` / `HybridSharedListRepositoryImpl`
- UseCase: CreateList / GetListsByGroup / DeleteList / WatchSharedList / AddItem / UpdateItem / RemoveItem（論理削除）
- `presentation/shoppinglist`: SharedListScreen（グループ選択・リスト選択・アイテム一覧・FAB・編集モーダル）

**CRITICAL ルール（porting_spec §12-1, §12-2）**
- アイテムは `Map<String, SharedItem>`。追加/更新/削除は **`items.{itemId}` 単位の差分更新**（全件置換は禁止）
- 削除は `isDeleted = true` の論理削除。UI表示は `activeItems` のみ
- `runTransaction()` はオフラインでハングするため使わず `update()` を使う（§12-3）

**DoD**: アイテム追加/編集/購入トグル/削除が Firestore に差分反映され、論理削除アイテムが非表示。

### Phase 5: リアルタイム同期 + オフライン（P0 仕上げ）

**成果物**
- `callbackFlow` ベースの `watchSharedList` / `watchGroups`（porting_spec §7-1）
- `NetworkMonitor`（オンライン/オフライン判定）
- Hybrid 読み込み: オンライン=Firestore→Roomキャッシュ更新→emit、オフライン=Room、エラー=Roomフォールバック（ARCHITECTURE / flutter_vs_kotlin §4）

**DoD**: 2端末（または2エミュ）で片方の変更が他方へリアルタイム反映。機内モードでも閲覧・編集でき、復帰後に自動同期。

> ⚠️ migration_notes: UI が更新されない時は `StateFlow` + `collectAsStateWithLifecycle()` を使っているか確認。

### Phase 6: QR招待（P1）

**成果物**
- `InvitationRepository` + UseCase（Create / Validate / Accept）
- 招待作成: トークン `INV_{uuid}`、`expiresAt = +24h`、`maxUses = 5`（§8-1）
- QrScanScreen（CameraX + ML Kit）+ 手動入力ダイアログ
- 受諾フロー（§8-2）: バリデーション（期限・回数・重複・既存メンバー）→ `currentUses`++ / `usedBy`追加 / `allowedUid`追加 / `members`追加 / `acceptedInvitations` 記録
- メール招待は `mailto:` URLスキーム（§8-4。`mail` コレクション方式は廃止）

**DoD**: ホストでQR生成→ゲストがスキャン/手動入力で参加し、両者のグループ一覧に反映。

### Phase 7: 通知（P1）

**成果物**
- `NotificationRepository`（自分宛 `userId` + `isRead=false` を監視, §10-2）
- リスト/メンバー/グループ操作時の通知書き込み（§10-1 の type 表）
- 通知一覧UI + 既読管理

**DoD**: リスト作成/メンバー参加等で宛先ユーザーに通知が届き、既読化できる。

### Phase 8: ホワイトボード（P2）

**成果物**
- `WhiteboardRepository`（strokes サブコレクション）
- WhiteboardEditorScreen（1280×720 固定キャンバス・2レイヤー Canvas・ツールバー・Undo/Redo・ズーム/スクロール）
- 差分保存（未保存ストロークのみ・バッチ書き込み・fire-and-forget, §9-1, §9-2, §12-7）
- グループ共有(`ownerId=null,isPrivate=false`) / 個人用(`ownerId=uid,isPrivate=true`)（§9-3）

**CRITICAL ルール**
- ストローク監視で `orderBy` 禁止 → クライアントソート（§12-6）
- オフライン時も保存を中止しない（§12-11）

**DoD**: 手書き→保存→他メンバーに反映。オフラインでも描画・保存が継続。

### Phase 9: 設定 + 仕上げ（P2）

**成果物**
- SettingsScreen（認証状態・同期状態・SingleUI/MultiUI 切替・Shopping/ToDo 切替・言語・アカウント削除）
- アカウント削除フロー（data_deletion 準拠）
- ProGuard/R8 設定、リリース署名、AAB ビルド

**DoD**: 設定変更が反映され、リリース AAB がビルドできる。

---

## 5. 横断的な必須ルール（チェックリスト）

porting_spec §12 を実装全体で遵守する。

- [ ] アイテムは `items.{itemId}` の差分更新（全件置換禁止）— §12-1
- [ ] アイテム削除は論理削除（`isDeleted`）、UIは `activeItems` のみ — §12-2
- [ ] Firestore オフライン永続化 ON、`runTransaction()` を避け `set/update` を使う — §12-3
- [ ] ドロップダウンは `groupId` で重複除去 — §12-4
- [ ] グループ取得は `whereArrayContains("allowedUid", uid)` — §12-5
- [ ] ストローク監視は `orderBy` 禁止・クライアントソート — §12-6
- [ ] ストローク保存は fire-and-forget（UIブロックしない）— §12-7
- [ ] サインアウトは キャッシュクリア→状態リセット→signOut の順 — §12-8
- [ ] グループ離脱時はローカルを即時クリーンアップ — §12-9
- [ ] Sentry DSN は `.env`/BuildConfig 経由（ハードコード禁止）— §12-10
- [ ] ホワイトボード保存はオフラインでも中止しない — §12-11
- [ ] クライアント側でも `canAccessGroup` でアクセス判定 — §11-2

---

## 6. テスト戦略

| 層 | 対象 | ツール |
|----|------|--------|
| Domain | UseCase のロジック、`activeItems` フィルタ | JUnit5 + MockK |
| Data | Repository の Hybrid 切替/フォールバック、Mapper | MockK + Turbine |
| Local | Room DAO | Room in-memory + JUnit |
| Presentation | ViewModel の StateFlow 遷移 | Turbine + MockK |
| UI | 主要画面のレンダリング・操作 | Compose UI Test |
| 結合 | Firestore 同期（dev フレーバー） | 実機/エミュ手動 + Firebase Emulator Suite（任意） |

---

## 7. リスクと対策

| リスク | 影響 | 対策 |
|--------|------|------|
| Hilt/kapt ビルドエラー | 着手直後の停滞 | Phase 0 で最小構成を先に通す。ksp 採用も検討 |
| Firestore データ非互換 | Flutter版とバックエンド共有不可 | DTO のフィールド名・型を porting_spec のJSONに厳密一致させる |
| オフライン時のハング | UX 劣化 | `runTransaction` 不使用、fire-and-forget 徹底（§12-3, §12-7） |
| Timestamp 変換ズレ | 日時表示不正 | Firestore `Timestamp` ⇔ `Date`/`Instant` の Mapper を一元化しテスト |
| アイテム全件置換の混入 | 並行編集でデータ消失 | コードレビュー必須項目化（§5 チェックリスト） |
| QR/カメラ権限 | 招待機能が動かない | CameraX 権限フローを Phase 6 冒頭で実装 |

---

## 8. マイルストーン

| マイルストーン | 含むフェーズ | 達成状態 |
|----------------|--------------|----------|
| M1: 動く認証アプリ | Phase 0–2 | サインアップ/サインインが通る |
| M2: コア機能完成（縦貫通） | Phase 3–5 | グループ/リスト/アイテムが同期する MVP |
| M3: 共有体験完成 | Phase 6–7 | 招待・通知でマルチユーザー協働が可能 |
| M4: 機能等価リリース候補 | Phase 8–9 | ホワイトボード・設定込みで AAB ビルド可能 |

---

## 9. 次のアクション（着手手順）

1. Android Studio で Empty Compose Activity を作成（package: `net.sumomo_planning.goshopping`）
2. `build.gradle.kts` に依存追加（Compose BOM / Hilt / Room / Firebase BOM / Coroutines / Navigation / CameraX / ML Kit）
3. `google-services.json`（dev/prod）配置、`productFlavors` 定義
4. `GoShopApplication` で Firestore オフライン永続化を有効化し、`@HiltAndroidApp` を付与
5. Phase 1 のドメインモデルを porting_spec Appendix からコード化
6. 以降、Phase 2 → 5 を縦に貫通して MVP を最優先で完成させる

> 移植中の気づき・ハマりどころは [docs/migration_notes.md](../migration_notes.md) に追記していく。
