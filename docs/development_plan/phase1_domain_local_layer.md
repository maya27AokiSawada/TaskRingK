# Phase 1 実施内容: ドメインモデル + ローカル層

**対象フェーズ**: Phase 1（[kotlin_migration_master_plan.md](kotlin_migration_master_plan.md) §4 参照）
**前提**: Phase 0 完了（dev/prod ビルド成功、Hilt/Compose/Room/Firebase 依存導入済み）
**作成日**: 2026-06-30
**ステータス**: 設計確定（実装着手前）

このドキュメントは Phase 1 の実装仕様書を兼ねます。コード例は **そのまま実装に流用できる正本** として記述しています。
根拠ドキュメント:

- [porting_spec_kotlin_swift.md](../Flutter_Version_docs/specifications/porting_spec_kotlin_swift.md) §5（データモデル）, §6-3（ID生成）, §12（注意事項）, Appendix（Kotlin型定義）
- [data_classes_reference.md](../Flutter_Version_docs/specifications/data_classes_reference.md)

---

## 1. このフェーズのゴール

Firestore とローカル（Room / DataStore）の **両方に依存しない純粋なドメインモデル** を定義し、ローカルキャッシュ層（Room）と端末設定（DataStore）、ID 生成ユーティリティを実装する。Repository / Firestore 連携は **Phase 3 以降** で扱うため、本フェーズでは扱わない。

### スコープ

| 含む | 含まない（後フェーズ） |
|------|------------------------|
| `domain/model` の全データクラス・enum | Repository 実装（Phase 3–4） |
| `SharedList.activeItems` 等の計算プロパティ | Firestore DataSource（Phase 3–5） |
| Room（Entity / DAO / Database / TypeConverter） | UI / ViewModel（Phase 2 以降） |
| DataStore（`UserPreferences`） | 同期ロジック（Phase 5） |
| `DeviceIdService`（ID 生成） | 認証（Phase 2） |
| 上記の単体テスト | — |

---

## 2. 成果物（作成ファイル一覧）

```
app/src/main/kotlin/net/sumomo_planning/goshopping/
├── core/common/
│   ├── DeviceIdService.kt          # グループID/リストID/アイテムID 生成
│   └── Clock.kt                    # 現在時刻の抽象（テスト容易性）
├── domain/model/
│   ├── SharedGroup.kt              # SharedGroup + GroupType + SyncStatus
│   ├── SharedGroupMember.kt        # SharedGroupMember + SharedGroupRole + InvitationStatus
│   ├── SharedList.kt               # SharedList + ListType + activeItems
│   ├── SharedItem.kt               # SharedItem
│   ├── Whiteboard.kt               # Whiteboard + DrawingStroke + DrawingPoint
│   ├── Notification.kt             # Notification + NotificationType
│   ├── Invitation.kt               # Invitation
│   └── AcceptedInvitation.kt       # AcceptedInvitation
├── data/local/room/
│   ├── GoShopDatabase.kt           # @Database
│   ├── RoomConverters.kt           # TypeConverter（Instant, List<String>, Map<String,SharedItem> 等）
│   ├── entity/
│   │   ├── SharedGroupEntity.kt
│   │   ├── SharedListEntity.kt
│   │   └── WhiteboardEntity.kt
│   └── dao/
│       ├── SharedGroupDao.kt
│       ├── SharedListDao.kt
│       └── WhiteboardDao.kt
├── data/local/prefs/
│   └── UserPreferences.kt          # DataStore（ユーザー名・UIモード・アプリモード等）
├── data/mapper/
│   ├── SharedGroupMappers.kt       # Entity <-> Domain
│   └── SharedListMappers.kt        # Entity <-> Domain
└── di/
    ├── DatabaseModule.kt           # Room / DAO の提供
    └── DispatcherModule.kt         # @IoDispatcher 等

app/src/test/kotlin/net/sumomo_planning/goshopping/
├── domain/model/SharedListActiveItemsTest.kt
├── core/common/DeviceIdServiceTest.kt
└── data/mapper/SharedListMappersTest.kt

app/src/androidTest/kotlin/net/sumomo_planning/goshopping/
└── data/local/room/SharedListDaoTest.kt
```

---

## 3. ドメインモデル設計

### 3-1. 設計方針

- **不変（immutable）**: すべて `data class` + `val`。更新は `copy()` で行う。
- **プラットフォーム非依存**: Firestore / Room / Android 型を持ち込まない。日時は `java.time.Instant` を採用（`Timestamp`/`Date` は Mapper で変換）。
- **null 安全**: 未購入・未削除などの「無い状態」は nullable で表現（`purchaseDate: Instant?` 等）。
- **enum はシリアライズ名を固定**: Firestore の文字列値（`"owner"`, `"shopping"` 等）と対応させるため、変換は Mapper に集約し、enum 自体には Firestore 文字列を持たせない（後述の `fromFirestore`/`toFirestore` 拡張で対応）。

> 日時型に関する注記: porting_spec Appendix は `Date` を使用しているが、本実装では **`java.time.Instant`** に統一する（minSdk 26 で利用可能、テスト容易・不変・スレッドセーフ）。Firestore `Timestamp` ⇔ `Instant` の変換は Phase 3 の Mapper で一元化する。

### 3-2. SharedGroup（porting_spec §5-2, Appendix）

```kotlin
// domain/model/SharedGroup.kt
package net.sumomo_planning.goshopping.domain.model

import java.time.Instant

data class SharedGroup(
    val groupId: String,
    val groupName: String,
    val ownerUid: String,
    val allowedUid: List<String>,
    val members: List<SharedGroupMember>,
    val groupType: GroupType = GroupType.SHOPPING,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val createdAt: Instant,
    val updatedAt: Instant? = null,
) {
    /** クライアント側アクセス判定（porting_spec §11-2）。 */
    fun canAccess(uid: String): Boolean =
        ownerUid == uid || allowedUid.contains(uid)
}

enum class GroupType { SHOPPING, TODO }
enum class SyncStatus { SYNCED, PENDING, LOCAL }
```

### 3-3. SharedGroupMember（porting_spec Appendix）

```kotlin
// domain/model/SharedGroupMember.kt
package net.sumomo_planning.goshopping.domain.model

import java.time.Instant

data class SharedGroupMember(
    val memberId: String,        // UUID v4
    val name: String,
    val contact: String,
    val role: SharedGroupRole,
    val isSignedIn: Boolean = false,
    val invitationStatus: InvitationStatus = InvitationStatus.PENDING,
    val securityKey: String? = null,
    val invitedAt: Instant? = null,
    val acceptedAt: Instant? = null,
)

enum class SharedGroupRole { OWNER, MEMBER, MANAGER, PARTNER }
enum class InvitationStatus { SELF, PENDING, ACCEPTED, DELETED }
```

### 3-4. SharedList + activeItems（porting_spec §5-3, §12-2, Appendix）

```kotlin
// domain/model/SharedList.kt
package net.sumomo_planning.goshopping.domain.model

import java.time.Instant

data class SharedList(
    val listId: String,
    val listName: String,
    val ownerUid: String,
    val groupId: String,
    val groupName: String,
    val description: String = "",
    val listType: ListType = ListType.SHOPPING,
    val items: Map<String, SharedItem> = emptyMap(),  // キーは itemId（配列ではない）
    val createdAt: Instant,
    val updatedAt: Instant? = null,
) {
    /**
     * UI 表示対象。論理削除されていないアイテムのみを登録日時昇順で返す。
     * porting_spec §12-2: UI 表示は必ず isDeleted == false のみ。
     */
    val activeItems: List<SharedItem>
        get() = items.values
            .filter { !it.isDeleted }
            .sortedBy { it.registeredDate }
}

enum class ListType { SHOPPING, TODO }
```

### 3-5. SharedItem（porting_spec §5-4, Appendix）

```kotlin
// domain/model/SharedItem.kt
package net.sumomo_planning.goshopping.domain.model

import java.time.Instant

data class SharedItem(
    val itemId: String,                 // UUID v4
    val name: String,
    val quantity: Int = 1,
    val memberId: String,               // 登録者の memberId
    val registeredDate: Instant,
    val purchaseDate: Instant? = null,  // null = 未購入
    val isPurchased: Boolean = false,
    val isDeleted: Boolean = false,     // 論理削除フラグ（UI 非表示）
    val deletedAt: Instant? = null,
    val shoppingInterval: Int = 0,      // 繰り返し間隔(日)。0 = 繰り返しなし
    val deadline: Instant? = null,      // 購入期限
)
```

### 3-6. Whiteboard / DrawingStroke / DrawingPoint（porting_spec §5-5, Appendix）

```kotlin
// domain/model/Whiteboard.kt
package net.sumomo_planning.goshopping.domain.model

import java.time.Instant

data class Whiteboard(
    val whiteboardId: String,
    val groupId: String,
    val ownerId: String? = null,     // null = グループ共有 / uid = 個人用
    val isPrivate: Boolean = false,  // false = グループ共有 / true = 個人用
    val createdAt: Instant,
    val updatedAt: Instant? = null,
)

data class DrawingStroke(
    val strokeId: String,
    val points: List<DrawingPoint>,
    val colorValue: Int,             // Color ARGB int（例: -16777216 = 黒）
    val strokeWidth: Float,
    val createdAt: Instant,
    val authorId: String,
    val authorName: String,
)

data class DrawingPoint(val x: Float, val y: Float)
```

### 3-7. Notification（porting_spec §5-7, §10-1）

```kotlin
// domain/model/Notification.kt
package net.sumomo_planning.goshopping.domain.model

import java.time.Instant

data class Notification(
    val notificationId: String,
    val userId: String,             // 宛先ユーザーUID
    val type: NotificationType,
    val groupId: String,
    val listId: String? = null,
    val message: String,
    val isRead: Boolean = false,
    val createdAt: Instant,
)

enum class NotificationType {
    LIST_CREATED, LIST_DELETED, LIST_RENAMED,
    MEMBER_JOINED, MEMBER_LEFT, GROUP_DELETED,
}
```

### 3-8. Invitation / AcceptedInvitation（porting_spec §5-6, §8, data_classes_reference）

```kotlin
// domain/model/Invitation.kt
package net.sumomo_planning.goshopping.domain.model

import java.time.Instant

data class Invitation(
    val token: String,              // "INV_" + UUID v4
    val groupId: String,
    val groupName: String,
    val invitedBy: String,          // inviter UID
    val inviterName: String,
    val createdAt: Instant,
    val expiresAt: Instant,
    val maxUses: Int = 5,
    val currentUses: Int = 0,
    val usedBy: List<String> = emptyList(),
    val securityKey: String? = null,
) {
    fun isValidAt(now: Instant): Boolean =
        now.isBefore(expiresAt) && currentUses < maxUses
}
```

```kotlin
// domain/model/AcceptedInvitation.kt
package net.sumomo_planning.goshopping.domain.model

import java.time.Instant

data class AcceptedInvitation(
    val acceptorUid: String,
    val acceptorEmail: String,
    val acceptorName: String,
    val groupId: String,
    val listId: String? = null,
    val role: SharedGroupRole,
    val acceptedAt: Instant,
    val processedAt: Instant? = null,
)
```

---

## 4. ID 生成（DeviceIdService — porting_spec §6-3）

```kotlin
// core/common/DeviceIdService.kt
package net.sumomo_planning.goshopping.core.common

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ID 生成。タイムスタンプ単体の ID は衝突リスクがあるため禁止（porting_spec §6-3）。
 * devicePrefix は端末固有の 8 桁プレフィックス。
 */
@Singleton
class DeviceIdService @Inject constructor() {

    /** グループID: {devicePrefix}_{epochMillis} */
    fun generateGroupId(devicePrefix: String, nowMillis: Long = System.currentTimeMillis()): String =
        "${devicePrefix}_$nowMillis"

    /** リストID: {devicePrefix}_{uuid8} */
    fun generateListId(devicePrefix: String): String =
        "${devicePrefix}_${uuid8()}"

    /** アイテムID: UUID v4 */
    fun generateItemId(): String = UUID.randomUUID().toString()

    /** メンバーID: UUID v4 */
    fun generateMemberId(): String = UUID.randomUUID().toString()

    /** 招待トークン: INV_ + UUID v4 */
    fun generateInvitationToken(): String = "INV_${UUID.randomUUID()}"

    private fun uuid8(): String =
        UUID.randomUUID().toString().replace("-", "").substring(0, 8)
}
```

> `devicePrefix` の取得（`Settings.Secure.ANDROID_ID` のハッシュ等）は Phase 3 で `DeviceProvider` として実装する。本フェーズでは引数として受け取る形にしてテスト可能にする。

---

## 5. Room ローカル層

### 5-1. 設計方針（porting_spec §2, §4-14 ハマりどころ）

- **Room はキャッシュ専用**（Source of Truth は Firestore）。スキーマは「読み込み高速化・オフライン表示」のために最小限に保つ。
- アイテムの `Map<String, SharedItem>` は **JSON 文字列としてカラムに格納**する（Room はネストした Map を直接扱えないため）。差分更新ロジックは Firestore 側の責務（§12-1）であり、Room は全体を上書きキャッシュする。
- マイグレーション戦略は `fallbackToDestructiveMigration()`（キャッシュなので破棄して再同期可能）。
- DAO の参照系は **`Flow` を返す**（UI のリアクティブ更新に直結）。

### 5-2. Entity

```kotlin
// data/local/room/entity/SharedGroupEntity.kt
@Entity(tableName = "shared_groups")
data class SharedGroupEntity(
    @PrimaryKey val groupId: String,
    val groupName: String,
    val ownerUid: String,
    val allowedUid: List<String>,        // RoomConverters で JSON 化
    val membersJson: String,             // List<SharedGroupMember> を JSON 文字列で保持
    val groupType: String,               // "shopping" / "todo"
    val syncStatus: String,              // "synced" / "pending" / "local"
    val createdAt: Long,                 // epochMillis
    val updatedAt: Long?,
)
```

```kotlin
// data/local/room/entity/SharedListEntity.kt
@Entity(
    tableName = "shared_lists",
    indices = [Index("groupId")],
)
data class SharedListEntity(
    @PrimaryKey val listId: String,
    val listName: String,
    val ownerUid: String,
    val groupId: String,
    val groupName: String,
    val description: String,
    val listType: String,
    val itemsJson: String,               // Map<String, SharedItem> を JSON 文字列で保持
    val createdAt: Long,
    val updatedAt: Long?,
)
```

```kotlin
// data/local/room/entity/WhiteboardEntity.kt
@Entity(
    tableName = "whiteboards",
    indices = [Index("groupId")],
)
data class WhiteboardEntity(
    @PrimaryKey val whiteboardId: String,
    val groupId: String,
    val ownerId: String?,
    val isPrivate: Boolean,
    val createdAt: Long,
    val updatedAt: Long?,
)
```

> ストローク（`DrawingStroke`）は数が多く Firestore サブコレクション管理のため、Phase 1 の Room ではキャッシュ対象外（Phase 8 で必要に応じてテーブル追加を検討）。

### 5-3. TypeConverter

```kotlin
// data/local/room/RoomConverters.kt
class RoomConverters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter fun fromStringList(value: List<String>): String = json.encodeToString(value)
    @TypeConverter fun toStringList(value: String): List<String> = json.decodeFromString(value)
}
```

> `membersJson` / `itemsJson` は Entity 側で String として保持し、ドメイン変換は Mapper（§6）で行う。`kotlinx.serialization` を採用するため、Phase 1 で `org.jetbrains.kotlin.plugin.serialization` プラグインと `kotlinx-serialization-json` 依存を追加する（version catalog 更新）。

### 5-4. DAO

```kotlin
// data/local/room/dao/SharedListDao.kt
@Dao
interface SharedListDao {
    @Query("SELECT * FROM shared_lists WHERE groupId = :groupId")
    fun observeByGroup(groupId: String): Flow<List<SharedListEntity>>

    @Query("SELECT * FROM shared_lists WHERE listId = :listId")
    fun observeById(listId: String): Flow<SharedListEntity?>

    @Upsert
    suspend fun upsert(entity: SharedListEntity)

    @Upsert
    suspend fun upsertAll(entities: List<SharedListEntity>)

    @Query("DELETE FROM shared_lists WHERE listId = :listId")
    suspend fun deleteById(listId: String)

    @Query("DELETE FROM shared_lists WHERE groupId = :groupId")
    suspend fun deleteByGroup(groupId: String)   // グループ離脱時の一括削除（§12-9）
}
```

`SharedGroupDao` / `WhiteboardDao` も同様の `observe* / upsert / deleteById` 構成。

### 5-5. Database

```kotlin
// data/local/room/GoShopDatabase.kt
@Database(
    entities = [SharedGroupEntity::class, SharedListEntity::class, WhiteboardEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(RoomConverters::class)
abstract class GoShopDatabase : RoomDatabase() {
    abstract fun sharedGroupDao(): SharedGroupDao
    abstract fun sharedListDao(): SharedListDao
    abstract fun whiteboardDao(): WhiteboardDao
}
```

---

## 6. Mapper（Entity ⇔ Domain）

変換ロジックは Mapper に一元化する（テスト対象を集約）。例:

```kotlin
// data/mapper/SharedListMappers.kt
fun SharedListEntity.toDomain(json: Json): SharedList = SharedList(
    listId = listId,
    listName = listName,
    ownerUid = ownerUid,
    groupId = groupId,
    groupName = groupName,
    description = description,
    listType = listType.toListType(),
    items = json.decodeFromString<Map<String, SharedItemDto>>(itemsJson)
        .mapValues { it.value.toDomain() },
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = updatedAt?.let(Instant::ofEpochMilli),
)

fun SharedList.toEntity(json: Json): SharedListEntity = SharedListEntity(
    listId = listId,
    listName = listName,
    ownerUid = ownerUid,
    groupId = groupId,
    groupName = groupName,
    description = description,
    listType = listType.firestoreValue,   // "shopping" / "todo"
    itemsJson = json.encodeToString(items.mapValues { it.value.toDto() }),
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt?.toEpochMilli(),
)
```

> `SharedItemDto` は JSON シリアライズ専用の `@Serializable` 中間型。ドメインの `SharedItem`（`Instant` を使用）を直接シリアライズしないことで、保存形式（epochMillis）とドメイン表現を分離する。Firestore 用 DTO は Phase 3 で別途定義する。

---

## 7. DataStore（UserPreferences）

SharedPreferences 相当（porting_spec §3-5 設定、§4-2 ユーザー名保存）を DataStore で実装。

```kotlin
// data/local/prefs/UserPreferences.kt
@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val Context.dataStore by preferencesDataStore("user_prefs")

    val userName: Flow<String?> = context.dataStore.data.map { it[KEY_USER_NAME] }
    val uiMode: Flow<UiMode> = context.dataStore.data.map {
        UiMode.from(it[KEY_UI_MODE]) // SINGLE / MULTI
    }
    val appMode: Flow<AppMode> = context.dataStore.data.map {
        AppMode.from(it[KEY_APP_MODE]) // SHOPPING / TODO
    }

    suspend fun setUserName(name: String) { /* edit */ }
    suspend fun setUiMode(mode: UiMode) { /* edit */ }
    suspend fun setAppMode(mode: AppMode) { /* edit */ }

    /** サインアウト/サインアップ時の全クリア（porting_spec §4-2, §4-4, §12-8）。 */
    suspend fun clearAll() { context.dataStore.edit { it.clear() } }

    private companion object {
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_UI_MODE = stringPreferencesKey("ui_mode")
        val KEY_APP_MODE = stringPreferencesKey("app_mode")
    }
}

enum class UiMode { SINGLE, MULTI; companion object { fun from(v: String?) = /* ... */ MULTI } }
enum class AppMode { SHOPPING, TODO; companion object { fun from(v: String?) = /* ... */ SHOPPING } }
```

---

## 8. Hilt モジュール

```kotlin
// di/DispatcherModule.kt
@Qualifier annotation class IoDispatcher
@Qualifier annotation class DefaultDispatcher

@Module @InstallIn(SingletonComponent::class)
object DispatcherModule {
    @Provides @IoDispatcher fun io(): CoroutineDispatcher = Dispatchers.IO
    @Provides @DefaultDispatcher fun default(): CoroutineDispatcher = Dispatchers.Default
}
```

```kotlin
// di/DatabaseModule.kt
@Module @InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GoShopDatabase =
        Room.databaseBuilder(context, GoShopDatabase::class.java, "goshop.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun groupDao(db: GoShopDatabase) = db.sharedGroupDao()
    @Provides fun listDao(db: GoShopDatabase) = db.sharedListDao()
    @Provides fun whiteboardDao(db: GoShopDatabase) = db.whiteboardDao()

    @Provides @Singleton fun json(): Json = Json { ignoreUnknownKeys = true }
}
```

---

## 9. テスト計画

| テスト | 種別 | 検証内容 |
|--------|------|----------|
| `SharedListActiveItemsTest` | JUnit | `activeItems` が `isDeleted=true` を除外し `registeredDate` 昇順で返す |
| `DeviceIdServiceTest` | JUnit | groupId=`prefix_millis`、listId=`prefix_uuid8`（8桁）、token=`INV_`接頭、ID の一意性 |
| `SharedListMappersTest` | JUnit | Entity⇔Domain 往復で値が保存される（Instant⇔epochMillis、items JSON 往復） |
| `SharedGroupCanAccessTest` | JUnit | `canAccess` が owner/allowedUid を正しく判定（§11-2） |
| `SharedListDaoTest` | androidTest（in-memory Room） | upsert/observeByGroup/deleteByGroup が機能する |

実行コマンド（Phase 0 のメモ参照）:

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:testDevDebugUnitTest --console=plain
```

---

## 10. 完了条件（DoD）

- [ ] `domain/model` の全モデル・enum が定義され、コンパイルが通る
- [ ] `SharedList.activeItems` の単体テストが green（フィルタ + ソート）
- [ ] Room（Entity/DAO/Database/Converter）が定義され、in-memory DAO テストが green
- [ ] `UserPreferences`（DataStore）と `clearAll()` が実装される
- [ ] `DeviceIdService` の単体テストが green
- [ ] `DatabaseModule` / `DispatcherModule` を含めて `:app:assembleDevDebug` が成功する
- [ ] `kotlinx.serialization` プラグイン/依存追加に伴い version catalog と app/build.gradle.kts が更新される

---

## 11. このフェーズで遵守する横断ルール（再掲）

- 日時はドメインで `Instant`、保存で epochMillis、Firestore で `Timestamp`（Phase 3 で変換一元化）
- アイテムは `Map<String, SharedItem>`（配列にしない）— §5-3
- UI 表示は `activeItems` のみ（論理削除を除外）— §12-2
- グループ離脱時のローカル一括削除に備え `deleteByGroup` を用意 — §12-9
- ID は `prefix_*` 形式（タイムスタンプ単体禁止）— §6-3

---

## 12. 次フェーズへの引き継ぎ

- Phase 2（認証）は `UserPreferences.clearAll()` と `domain/model` を利用する。
- Phase 3（グループ）は本フェーズの DAO / Mapper / `DeviceIdService` を土台に、Firestore DTO と Hybrid Repository を追加する。Firestore `Timestamp` ⇔ `Instant` 変換 Mapper はそこで新設する。
