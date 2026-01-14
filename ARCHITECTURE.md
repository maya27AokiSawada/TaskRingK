# Go Shop Kotlin - アーキテクチャ設計

## Clean Architecture 採用理由

Flutter版で学んだRepository Patternの知見を活かしつつ、Kotlinでのモダンな設計パターンとして**Clean Architecture**を採用します。

### レイヤー構成

```
┌─────────────────────────────────────────┐
│         Presentation Layer              │
│  (Compose UI + ViewModel + StateFlow)   │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│          Domain Layer                   │
│   (UseCase + Model + Repository I/F)    │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│           Data Layer                    │
│  (Repository Impl + Room + Firestore)   │
└─────────────────────────────────────────┘
```

## レイヤー詳細

### 1. Presentation Layer

**責務**: UI表示とユーザー操作の処理

```kotlin
// ViewModel例
@HiltViewModel
class ShoppingListViewModel @Inject constructor(
    private val getShoppingListsUseCase: GetShoppingListsUseCase,
    private val addItemUseCase: AddItemUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ShoppingListUiState>(ShoppingListUiState.Loading)
    val uiState: StateFlow<ShoppingListUiState> = _uiState.asStateFlow()

    init {
        loadShoppingLists()
    }

    private fun loadShoppingLists() {
        viewModelScope.launch {
            getShoppingListsUseCase()
                .catch { e -> _uiState.value = ShoppingListUiState.Error(e.message) }
                .collect { lists -> _uiState.value = ShoppingListUiState.Success(lists) }
        }
    }
}

// UI State
sealed interface ShoppingListUiState {
    object Loading : ShoppingListUiState
    data class Success(val lists: List<ShoppingList>) : ShoppingListUiState
    data class Error(val message: String?) : ShoppingListUiState
}

// Compose UI
@Composable
fun ShoppingListScreen(viewModel: ShoppingListViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is ShoppingListUiState.Loading -> LoadingIndicator()
        is ShoppingListUiState.Success -> ShoppingListContent(state.lists)
        is ShoppingListUiState.Error -> ErrorMessage(state.message)
    }
}
```

### 2. Domain Layer

**責務**: ビジネスロジックの定義（プラットフォーム非依存）

```kotlin
// UseCase例
class GetShoppingListsUseCase @Inject constructor(
    private val shoppingListRepository: ShoppingListRepository
) {
    operator fun invoke(): Flow<List<ShoppingList>> {
        return shoppingListRepository.getAllLists()
    }
}

class AddItemUseCase @Inject constructor(
    private val shoppingListRepository: ShoppingListRepository
) {
    suspend operator fun invoke(listId: String, item: ShoppingItem) {
        shoppingListRepository.addItem(listId, item)
    }
}

// Repository Interface（Domain層で定義）
interface ShoppingListRepository {
    fun getAllLists(): Flow<List<ShoppingList>>
    suspend fun addItem(listId: String, item: ShoppingItem)
    suspend fun deleteItem(listId: String, itemId: String)
}

// Domain Model
data class ShoppingList(
    val listId: String,
    val listName: String,
    val items: List<ShoppingItem>,
    val updatedAt: LocalDateTime
)

data class ShoppingItem(
    val itemId: String,
    val name: String,
    val isPurchased: Boolean,
    val quantity: Int,
    val shoppingInterval: Int? = null,
    val purchaseDate: LocalDateTime? = null
)
```

### 3. Data Layer

**責務**: データの取得・保存（Firestore + Room）

```kotlin
// Repository実装
class ShoppingListRepositoryImpl @Inject constructor(
    private val firestoreDataSource: FirestoreShoppingListDataSource,
    private val roomDataSource: RoomShoppingListDataSource,
    private val networkMonitor: NetworkMonitor
) : ShoppingListRepository {

    override fun getAllLists(): Flow<List<ShoppingList>> = flow {
        if (networkMonitor.isOnline) {
            // Firestore優先
            firestoreDataSource.getAllLists()
                .collect { firestoreLists ->
                    // Roomにキャッシュ
                    roomDataSource.cacheLists(firestoreLists)
                    emit(firestoreLists.map { it.toDomainModel() })
                }
        } else {
            // オフライン時はRoomから
            roomDataSource.getAllLists()
                .collect { cachedLists ->
                    emit(cachedLists.map { it.toDomainModel() })
                }
        }
    }.catch { e ->
        // エラー時はRoomフォールバック
        roomDataSource.getAllLists()
            .collect { cachedLists ->
                emit(cachedLists.map { it.toDomainModel() })
            }
    }

    override suspend fun addItem(listId: String, item: ShoppingItem) {
        // Firestore + Room両方に保存
        firestoreDataSource.addItem(listId, item.toDataModel())
        roomDataSource.addItem(listId, item.toDataModel())
    }
}

// Firestore DataSource
class FirestoreShoppingListDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    fun getAllLists(): Flow<List<ShoppingListEntity>> = callbackFlow {
        val listener = firestore.collection("sharedLists")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val lists = snapshot?.documents?.mapNotNull {
                    it.toObject(ShoppingListEntity::class.java)
                } ?: emptyList()
                trySend(lists)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addItem(listId: String, item: ShoppingItemEntity) {
        firestore.collection("sharedLists")
            .document(listId)
            .update("items.${item.itemId}", item)
            .await()
    }
}

// Room DataSource
@Dao
interface ShoppingListDao {
    @Query("SELECT * FROM shopping_lists")
    fun getAllLists(): Flow<List<ShoppingListEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLists(lists: List<ShoppingListEntity>)
}
```

## 依存性注入（Hilt）

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore =
        Firebase.firestore

    @Provides
    @Singleton
    fun provideRoomDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "go_shop_db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideShoppingListRepository(
        firestoreDataSource: FirestoreShoppingListDataSource,
        roomDataSource: RoomShoppingListDataSource,
        networkMonitor: NetworkMonitor
    ): ShoppingListRepository =
        ShoppingListRepositoryImpl(firestoreDataSource, roomDataSource, networkMonitor)
}
```

## Flutter版との設計比較

| 設計要素 | Flutter版 | Kotlin版 |
|----------|-----------|----------|
| **アーキテクチャ** | Repository Pattern | Clean Architecture |
| **レイヤー数** | 2層（UI + Repository） | 3層（Presentation + Domain + Data） |
| **状態管理** | Riverpod AsyncNotifier | ViewModel + StateFlow |
| **DI** | Provider（手動） | Hilt（自動生成） |
| **リアクティブ** | Stream/Future | Flow/suspend |
| **キャッシュ戦略** | Hive Box | Room Database |

## テスト戦略

```kotlin
// ViewModel Test
@Test
fun `getAllLists should emit success state`() = runTest {
    // Given
    val expectedLists = listOf(/* test data */)
    coEvery { getShoppingListsUseCase() } returns flowOf(expectedLists)

    // When
    val viewModel = ShoppingListViewModel(getShoppingListsUseCase, addItemUseCase)

    // Then
    viewModel.uiState.test {
        assertEquals(ShoppingListUiState.Loading, awaitItem())
        assertEquals(ShoppingListUiState.Success(expectedLists), awaitItem())
    }
}

// Repository Test
@Test
fun `addItem should save to both Firestore and Room`() = runTest {
    // Given
    val listId = "list123"
    val item = ShoppingItem(/* test data */)

    // When
    repository.addItem(listId, item)

    // Then
    coVerify { firestoreDataSource.addItem(listId, any()) }
    coVerify { roomDataSource.addItem(listId, any()) }
}
```

## パフォーマンス最適化

### 1. Flow最適化
```kotlin
fun getAllLists(): Flow<List<ShoppingList>> = flow {
    // ...
}.distinctUntilChanged()  // 重複データスキップ
 .flowOn(Dispatchers.IO)  // バックグラウンドスレッド
```

### 2. Room キャッシュ戦略
```kotlin
@Query("SELECT * FROM shopping_lists WHERE updatedAt > :since")
fun getRecentLists(since: Long): Flow<List<ShoppingListEntity>>
```

### 3. Compose 再描画最適化
```kotlin
@Composable
fun ShoppingListItem(item: ShoppingItem) {
    // Key指定で不要な再描画を防ぐ
    key(item.itemId) {
        // UI content
    }
}
```

## 参考資料

- [Android Architecture Samples](https://github.com/android/architecture-samples)
- [Now in Android App](https://github.com/android/nowinandroid)
- [Jetpack Compose Samples](https://github.com/android/compose-samples)

---

**Last Updated**: 2026-01-13
