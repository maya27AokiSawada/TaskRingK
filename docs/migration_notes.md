# Flutter → Kotlin 移植時の気づき・メモ

## 移植作業ログ

このファイルは実際の移植作業中に気づいた技術的な違いや、ハマった問題・解決策を記録します。

---

## 📝 作業開始前の準備（2026-01-13）

### プロジェクト構成決定

- Clean Architecture採用（Flutter版のRepository PatternをClean Architectureに昇華）
- Jetpack Compose（宣言的UI、Flutter Widgetと類似）
- Hilt（DI自動生成、Riverpod手動登録からの脱却）
- Room（Hive NoSQLからSQLiteへ）
- Coroutine Flow（StreamBuilderからStateFlowへ）

### Flutter版の主な技術的負債

1. **状態管理の分散**
   - Riverpod Provider + StreamBuilder + Hive
   - → Kotlin版: ViewModel + StateFlow一元化

2. **Repository層の複雑性**
   - HybridRepository（Firestore/Hive切り替え）
   - → Kotlin版: DataSourceレイヤー分離

3. **差分同期の手動実装**
   - `addSingleItem()`, `removeSingleItem()` 個別実装
   - → Kotlin版: UseCase層で抽象化

---

## Week 1: プロジェクトセットアップ（予定）

### Day 1: Android Studio初期設定

- [ ] Android Studioインストール
- [ ] Empty Compose Activityプロジェクト作成
- [ ] パッケージ名: `net.sumomo_planning.goshopping_kotlin`
- [ ] Hilt依存関係追加
- [ ] Firebase SDK追加

### Day 2: プロジェクト構造作成

- [ ] パッケージ構成:
  ```
  net.sumomo_planning.goshopping_kotlin/
  ├── presentation/
  │   ├── shopping_list/
  │   ├── group/
  │   └── navigation/
  ├── domain/
  │   ├── model/
  │   ├── repository/
  │   └── usecase/
  └── data/
      ├── repository/
      ├── datasource/
      │   ├── firestore/
      │   └── room/
      └── entity/
  ```

---

## 技術的な気づき（随時更新）

### [未記入] Flutter Widget → Compose変換

**Flutter**:
```dart
Scaffold(
  appBar: AppBar(title: Text('タイトル')),
  body: ListView.builder(
    itemCount: items.length,
    itemBuilder: (context, index) => ListTile(
      title: Text(items[index].name),
    ),
  ),
)
```

**Kotlin**:
```kotlin
Scaffold(
  topBar = { TopAppBar(title = { Text("タイトル") }) }
) { padding ->
  LazyColumn(modifier = Modifier.padding(padding)) {
    items(items, key = { it.id }) { item ->
      ListItem(headlineContent = { Text(item.name) })
    }
  }
}
```

**気づき**:
- [ ] Composeの`key`は必須（Flutter `Key`相当）
- [ ] `padding`をLazyColumnに渡す必要あり

---

### [未記入] StreamBuilder → collectAsStateWithLifecycle

**Flutter**:
```dart
StreamBuilder<List<Item>>(
  stream: repository.watchItems(),
  builder: (context, snapshot) {
    if (snapshot.connectionState == ConnectionState.waiting) {
      return CircularProgressIndicator();
    }
    return ListView(children: snapshot.data);
  },
)
```

**Kotlin**:
```kotlin
val items by viewModel.items.collectAsStateWithLifecycle()

when (val state = items) {
  is UiState.Loading -> CircularProgressIndicator()
  is UiState.Success -> LazyColumn { items(state.data) { /* ... */ } }
  is UiState.Error -> ErrorMessage(state.message)
}
```

**気づき**:
- [ ] UIStateをsealed interfaceで定義すると型安全
- [ ] `collectAsStateWithLifecycle()`でライフサイクル自動管理

---

### [未記入] Riverpod ref.invalidate → ViewModel loadData()

**Flutter**:
```dart
Future<void> createGroup(String name) async {
  await repository.createGroup(name);
  ref.invalidateSelf();  // 自動再取得
}
```

**Kotlin**:
```kotlin
fun createGroup(name: String) {
  viewModelScope.launch {
    createGroupUseCase(name)
    loadGroups()  // 手動再読込
  }
}
```

**気づき**:
- [ ] Kotlin版は明示的な再読込が必要
- [ ] または`Flow.stateIn()`で自動監視

---

### [未記入] Hive → Room マイグレーション

**Flutter (Hive)**:
```dart
@HiveType(typeId: 3)
class SharedItem {
  @HiveField(0) String name;
  @HiveField(1) bool isPurchased;
}

Box<SharedItem> box = Hive.box<SharedItem>('items');
box.put(item.itemId, item);
```

**Kotlin (Room)**:
```kotlin
@Entity(tableName = "shopping_items")
data class ShoppingItemEntity(
  @PrimaryKey val itemId: String,
  @ColumnInfo(name = "name") val name: String,
  @ColumnInfo(name = "is_purchased") val isPurchased: Boolean
)

@Dao
interface ShoppingItemDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(item: ShoppingItemEntity)
}
```

**気づき**:
- [ ] RoomはSQLiteベース、スキーマ定義必要
- [ ] マイグレーション戦略（`fallbackToDestructiveMigration`）

---

### [未記入] Firebase差分更新

**Flutter**:
```dart
await firestore.collection('sharedLists').doc(listId).update({
  'items.${item.itemId}': _itemToFirestore(item),
});
```

**Kotlin**:
```kotlin
firestore.collection("sharedLists")
  .document(listId)
  .update(mapOf("items.${item.itemId}" to item))
  .await()
```

**気づき**:
- [ ] Map差分更新はほぼ同じ構文
- [ ] Kotlinは`await()`でsuspend関数化

---

## ハマった問題リスト

### [未記入] 問題1: Hiltのビルドエラー

**症状**:
```
Hilt processor error: ...
```

**原因**:
- [ ] kapt pluginの有効化忘れ
- [ ] @HiltAndroidApp付与忘れ

**解決策**:
```kotlin
// build.gradle.kts
plugins {
  id("com.google.dagger.hilt.android")
  kotlin("kapt")
}

// Application.kt
@HiltAndroidApp
class GoShopApplication : Application()
```

---

### [未記入] 問題2: Flow.collect()でUIが更新されない

**症状**:
- ViewModelでcollectしてもComposeが再描画されない

**原因**:
- [ ] `collectAsState()`ではなく`collect {}`を使った
- [ ] ViewModelがStateFlowではなくFlowを公開

**解決策**:
```kotlin
// ViewModel
private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
val uiState: StateFlow<UiState> = _uiState.asStateFlow()

// Composable
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
```

---

## パフォーマンス比較（実測）

### ビルド時間

| フェーズ | Flutter | Kotlin (予測) |
|----------|---------|---------------|
| 初回ビルド | 10分 | 予測7分 |
| 増分ビルド | 30秒 | 予測1分 |
| Hot Reload | 1秒 | N/A |
| Instant Run | N/A | 予測3秒 |

### APKサイズ

| ビルドタイプ | Flutter | Kotlin (予測) |
|------------|---------|---------------|
| Debug APK | 未測定 | 予測40MB |
| Release APK | 未測定 | 予測25MB |
| AAB | 57.6MB | 予測30MB |

---

## 参考にしたコード

### Flutter版で特に参考になったファイル

1. `lib/datastore/hybrid_shared_list_repository.dart`
   - Firestore優先・Hiveフォールバックパターン
   - → Kotlin版DataSource切り替えロジック

2. `lib/services/sync_service.dart`
   - バックグラウンド同期
   - → Kotlin版WorkManager実装参考

3. `lib/pages/shopping_list_page_v2.dart`
   - StreamBuilder + UI更新パターン
   - → Kotlin版ViewModel + StateFlow設計

4. `lib/providers/purchase_group_provider.dart`
   - Provider依存関係管理
   - → Kotlin版Hilt Module構成参考

---

## 次回作業予定メモ

- [ ] Android Studio最新版（Ladybug）インストール
- [ ] Jetpack Compose BOM最新版確認
- [ ] Firebase Android SDK初期設定
- [ ] Hilt依存関係バージョン固定
- [ ] Room Database初期スキーマ設計

---

**Last Updated**: 2026-01-13
