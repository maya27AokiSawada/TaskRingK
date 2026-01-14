# Flutter版 vs Kotlin版 技術比較

## 目的

この文書は、FlutterとKotlinで同じアプリを実装する際の技術的な違いを比較し、それぞれの長所・短所を明確化することを目的としています。

## 1. 開発環境

| 項目 | Flutter | Kotlin (Android) |
|------|---------|------------------|
| **IDE** | VS Code / Android Studio | Android Studio |
| **言語** | Dart | Kotlin |
| **ビルド時間** | 初回遅い（Windows: 5-10分）<br>Hot Reload: 1秒以下 | 初回遅い（5-7分）<br>Instant Run: 3-5秒 |
| **エミュレータ** | Android/iOS/Web/Desktop | Android のみ |
| **開発効率** | ⭐⭐⭐⭐⭐ マルチプラットフォーム | ⭐⭐⭐⭐ Android専用 |

## 2. UI実装

### Flutter (Compose-like Declarative UI)

```dart
// lib/pages/shopping_list_page_v2.dart
Widget build(BuildContext context) {
  return Scaffold(
    appBar: AppBar(title: Text('買い物リスト')),
    body: StreamBuilder<SharedList?>(
      stream: repository.watchSharedList(groupId, listId),
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return CircularProgressIndicator();
        }
        final list = snapshot.data;
        return ListView.builder(
          itemCount: list?.activeItems.length ?? 0,
          itemBuilder: (context, index) {
            final item = list!.activeItems[index];
            return ListTile(
              title: Text(item.name),
              trailing: Checkbox(
                value: item.isPurchased,
                onChanged: (value) => _updateItem(item),
              ),
            );
          },
        );
      },
    ),
  );
}
```

### Kotlin (Jetpack Compose)

```kotlin
@Composable
fun ShoppingListScreen(viewModel: ShoppingListViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("買い物リスト") }) }
    ) { padding ->
        when (val state = uiState) {
            is ShoppingListUiState.Loading ->
                CircularProgressIndicator(modifier = Modifier.padding(padding))

            is ShoppingListUiState.Success ->
                LazyColumn(modifier = Modifier.padding(padding)) {
                    items(state.list.activeItems, key = { it.itemId }) { item ->
                        ListItem(
                            headlineContent = { Text(item.name) },
                            trailingContent = {
                                Checkbox(
                                    checked = item.isPurchased,
                                    onCheckedChange = { viewModel.updateItem(item) }
                                )
                            }
                        )
                    }
                }

            is ShoppingListUiState.Error ->
                ErrorMessage(state.message, modifier = Modifier.padding(padding))
        }
    }
}
```

**比較ポイント**:
- Flutter: `StreamBuilder`で自動UI更新（Firestore snapshots直接）
- Kotlin: `ViewModel` + `StateFlow`の2段構成（レイヤー分離）
- Flutter: 状態管理が分散しがち（Riverpod + StreamBuilder）
- Kotlin: 状態管理が一元化（ViewModelに集約）

## 3. 状態管理

### Flutter (Riverpod)

```dart
// lib/providers/purchase_group_provider.dart
@riverpod
class AllGroupsNotifier extends _$AllGroupsNotifier {
  @override
  Future<List<SharedGroup>> build() async {
    final repository = ref.read(SharedGroupRepositoryProvider);
    return await repository.getAllGroups();
  }

  Future<void> createGroup(String groupName) async {
    state = const AsyncValue.loading();
    try {
      await repository.createGroup(groupId, groupName, ownerMember);
      ref.invalidateSelf();
    } catch (e) {
      state = AsyncValue.error(e, StackTrace.current);
    }
  }
}
```

### Kotlin (ViewModel + StateFlow)

```kotlin
@HiltViewModel
class GroupViewModel @Inject constructor(
    private val getGroupsUseCase: GetGroupsUseCase,
    private val createGroupUseCase: CreateGroupUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<GroupUiState>(GroupUiState.Loading)
    val uiState: StateFlow<GroupUiState> = _uiState.asStateFlow()

    init {
        loadGroups()
    }

    private fun loadGroups() {
        viewModelScope.launch {
            getGroupsUseCase()
                .catch { e -> _uiState.value = GroupUiState.Error(e.message) }
                .collect { groups -> _uiState.value = GroupUiState.Success(groups) }
        }
    }

    fun createGroup(groupName: String) {
        viewModelScope.launch {
            try {
                createGroupUseCase(groupName)
                loadGroups()  // 再読込
            } catch (e: Exception) {
                _uiState.value = GroupUiState.Error(e.message)
            }
        }
    }
}
```

**比較ポイント**:
- Flutter: `ref.invalidateSelf()`で自動再取得
- Kotlin: 手動で`loadGroups()`呼び出し
- Flutter: Provider間の依存関係が複雑になりやすい
- Kotlin: ViewModelが独立しており依存関係がシンプル

## 4. データ層（Repository Pattern）

### Flutter (Hybrid Repository)

```dart
// lib/datastore/hybrid_shared_list_repository.dart
class HybridSharedListRepository implements SharedListRepository {
  final FirestoreSharedListRepository? _firestoreRepo;
  final HiveSharedListRepository _hiveRepo;

  @override
  Future<SharedList?> getSharedListById(String listId) async {
    if (F.appFlavor == Flavor.prod && _firestoreRepo != null) {
      try {
        // Firestore優先
        final firestoreList = await _firestoreRepo!.getSharedListById(listId);
        if (firestoreList != null) {
          // Hiveにキャッシュ
          await _hiveRepo.updateSharedList(firestoreList);
        }
        return firestoreList;
      } catch (e) {
        // Firestoreエラー時はHiveフォールバック
        return await _hiveRepo.getSharedListById(listId);
      }
    }
    return await _hiveRepo.getSharedListById(listId);
  }

  @override
  Future<void> addSingleItem(String listId, SharedItem item) async {
    if (F.appFlavor == Flavor.prod && _firestoreRepo != null) {
      await _firestoreRepo!.addSingleItem(listId, item);
    }
    await _hiveRepo.addSingleItem(listId, item);
  }
}
```

### Kotlin (Clean Architecture Repository)

```kotlin
class ShoppingListRepositoryImpl @Inject constructor(
    private val firestoreDataSource: FirestoreShoppingListDataSource,
    private val roomDataSource: RoomShoppingListDataSource,
    private val networkMonitor: NetworkMonitor
) : ShoppingListRepository {

    override fun getListById(listId: String): Flow<ShoppingList?> = flow {
        if (networkMonitor.isOnline) {
            // Firestore優先
            firestoreDataSource.getListById(listId)
                .collect { firestoreList ->
                    if (firestoreList != null) {
                        // Roomにキャッシュ
                        roomDataSource.insertList(firestoreList)
                    }
                    emit(firestoreList?.toDomainModel())
                }
        } else {
            // オフライン時はRoomから
            roomDataSource.getListById(listId)
                .collect { cachedList ->
                    emit(cachedList?.toDomainModel())
                }
        }
    }.catch { e ->
        // エラー時はRoomフォールバック
        roomDataSource.getListById(listId)
            .collect { cachedList ->
                emit(cachedList?.toDomainModel())
            }
    }

    override suspend fun addItem(listId: String, item: ShoppingItem) {
        coroutineScope {
            // 並列実行
            launch { firestoreDataSource.addItem(listId, item.toDataModel()) }
            launch { roomDataSource.addItem(listId, item.toDataModel()) }
        }
    }
}
```

**比較ポイント**:
- Flutter: Future/async-awaitベース（逐次処理）
- Kotlin: Flow/Coroutineベース（リアクティブ＋並列処理）
- Flutter: Hive（NoSQL、型安全）
- Kotlin: Room（SQLite、型安全＋クエリ最適化）

## 5. Firebase連携

### Flutter (直接Firestoreアクセス)

```dart
// lib/datastore/firestore_shared_list_repository.dart
Stream<SharedList?> watchSharedList(String groupId, String listId) {
  return _collection(groupId).doc(listId).snapshots().map((snapshot) {
    if (!snapshot.exists) return null;
    final data = snapshot.data();
    return SharedList.fromFirestore(data!, snapshot.id);
  });
}

Future<void> addSingleItem(String listId, SharedItem item) async {
  final list = await getSharedListById(listId);
  await _collection(list.groupId).doc(listId).update({
    'items.${item.itemId}': _itemToFirestore(item),
    'updatedAt': FieldValue.serverTimestamp(),
  });
}
```

### Kotlin (DataSourceレイヤー)

```kotlin
class FirestoreShoppingListDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    fun watchList(groupId: String, listId: String): Flow<ShoppingListEntity?> =
        callbackFlow {
            val listener = firestore.collection("SharedGroups")
                .document(groupId)
                .collection("sharedLists")
                .document(listId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    val entity = snapshot?.toObject(ShoppingListEntity::class.java)
                    trySend(entity)
                }
            awaitClose { listener.remove() }
        }.flowOn(ioDispatcher)

    suspend fun addItem(listId: String, item: ShoppingItemEntity) =
        withContext(ioDispatcher) {
            val listSnapshot = firestore.collectionGroup("sharedLists")
                .whereEqualTo("listId", listId)
                .get()
                .await()

            val doc = listSnapshot.documents.firstOrNull()
                ?: throw Exception("List not found")

            doc.reference.update(
                mapOf("items.${item.itemId}" to item)
            ).await()
        }
}
```

**比較ポイント**:
- Flutter: `Stream<T>`で直接Firestore連携
- Kotlin: `Flow<T>`でコルーチン連携（cancellation対応）
- Flutter: `snapshots()`がシンプル
- Kotlin: `callbackFlow`で細かい制御が可能

## 6. 依存性注入

### Flutter (Provider手動登録)

```dart
// lib/providers/repositories.dart
final SharedGroupRepositoryProvider = Provider<SharedGroupRepository>((ref) {
  if (F.appFlavor == Flavor.prod) {
    return FirestoreSharedGroupRepository(ref);
  } else {
    return HiveSharedGroupRepository(ref);
  }
});

// main.dart
void main() {
  runApp(
    ProviderScope(
      child: MyApp(),
    ),
  );
}
```

### Kotlin (Hilt自動生成)

```kotlin
// di/DataModule.kt
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideShoppingListRepository(
        firestoreDataSource: FirestoreShoppingListDataSource,
        roomDataSource: RoomShoppingListDataSource,
        networkMonitor: NetworkMonitor
    ): ShoppingListRepository =
        ShoppingListRepositoryImpl(firestoreDataSource, roomDataSource, networkMonitor)
}

// Application.kt
@HiltAndroidApp
class GoShopApplication : Application()
```

**比較ポイント**:
- Flutter: 手動でProviderを定義（柔軟だが冗長）
- Kotlin: アノテーションベース自動生成（簡潔だが学習コスト）

## 7. ビルド・デプロイ

| 項目 | Flutter | Kotlin |
|------|---------|--------|
| **APKサイズ** | 57.6MB (release) | 推定20-30MB |
| **ビルド時間** | 5-10分（初回）<br>30秒-1分（増分） | 5-7分（初回）<br>1-2分（増分） |
| **難読化** | `--obfuscate`フラグ | ProGuard自動適用 |
| **署名** | flutter build設定 | Gradleで設定 |
| **フレーバー** | `--flavor prod/dev` | `productFlavors` |

## 8. パフォーマンス

### 起動時間

| 測定項目 | Flutter | Kotlin (予測) |
|----------|---------|---------------|
| **Cold Start** | 2-3秒 | 1-2秒 |
| **Hot Reload** | < 1秒 | 3-5秒 (Instant Run) |

### メモリ使用量

| 測定項目 | Flutter | Kotlin (予測) |
|----------|---------|---------------|
| **アイドル時** | 80-100MB | 50-70MB |
| **リスト表示** | 120-150MB | 80-100MB |

## 9. 開発体験 (DX)

### Flutter

**長所**:
- ✅ Hot Reload（1秒以下）
- ✅ マルチプラットフォーム（iOS/Android/Web/Desktop）
- ✅ 豊富なWidget（Material/Cupertino）
- ✅ VS Codeで軽快

**短所**:
- ❌ APKサイズ大（Flutter Engine込み）
- ❌ ネイティブAPIアクセスにプラグイン必要
- ❌ Dartエコシステムが小さい
- ❌ 複雑な状態管理（Riverpod学習コスト）

### Kotlin

**長所**:
- ✅ APKサイズ小（Android標準API）
- ✅ ネイティブAPI直接アクセス
- ✅ Android公式言語（最新機能即対応）
- ✅ Jetpack Composeの成熟度

**短所**:
- ❌ Android専用（iOS開発にはSwift必要）
- ❌ Instant Runが遅い（3-5秒）
- ❌ Clean Architectureの学習コスト
- ❌ XML Layoutからの移行期

## 10. 採用判断基準

### Flutterを選ぶべき場合

- ✅ iOS/Android同時リリースが必須
- ✅ 小規模チーム（1-3人）
- ✅ プロトタイピング重視
- ✅ Web/Desktopも視野に入れている

### Kotlinを選ぶべき場合

- ✅ Android専用アプリ
- ✅ パフォーマンス重視
- ✅ ネイティブAPI活用が多い
- ✅ Android開発者がチームにいる

## まとめ

| 観点 | Flutter | Kotlin |
|------|---------|--------|
| **開発速度** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| **パフォーマンス** | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **APKサイズ** | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **学習コスト** | ⭐⭐⭐⭐ | ⭐⭐⭐ |
| **プラットフォーム** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ (Android only) |
| **コミュニティ** | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |

**結論**:
- **Flutter版**: マルチプラットフォーム対応力を示すポートフォリオ
- **Kotlin版**: Android専用技術の深い理解を示すポートフォリオ

両方を実装することで、**クロスプラットフォーム開発とネイティブ開発の両方のスキルを証明**できます。

---

**Last Updated**: 2026-01-13
