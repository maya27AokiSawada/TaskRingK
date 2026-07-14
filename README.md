# Go Shop Kotlin - 買い物リスト共有アプリ（Android Native版）

## プロジェクト概要

このプロジェクトは、Flutter版Go Shopの**Kotlin（Android Native）移植版**です。
就活ポートフォリオとして、同じアプリをFlutterとKotlinの両方で実装し、技術スタックの違いによる設計の変化を示すことを目的としています。

## Flutter版との技術比較

| 項目 | Flutter版 | Kotlin版 |
|------|-----------|----------|
| **UI** | Flutter Widgets | Jetpack Compose |
| **State Management** | Riverpod | ViewModel + StateFlow |
| **DI** | Provider | Hilt |
| **非同期処理** | Future/Stream | Coroutines/Flow |
| **ローカルDB** | Hive | Room |
| **アーキテクチャ** | Repository Pattern | Clean Architecture (MVVM) |
| **ビルドシステム** | Flutter CLI | Gradle (Kotlin DSL) |
| **言語** | Dart | Kotlin |

## 技術スタック

```
UI: Jetpack Compose + Material Design 3
Architecture: Clean Architecture (MVVM)
DI: Hilt
Async: Kotlin Coroutines + Flow
Local DB: Room
Backend: Firebase (Auth + Firestore)
Testing: JUnit5 + MockK + Turbine
```

## 実装予定機能（優先順位順）

### Phase 1: 基本機能（Week 1-4）
- [x] プロジェクトセットアップ
- [ ] Firebase認証（Email/Password）
- [ ] グループ管理（作成・表示・選択）
- [ ] リスト管理（CRUD）
- [ ] アイテム管理（追加・削除・購入状態）

### Phase 2: リアルタイム同期（Week 5-6）
- [ ] Firestore Snapshotリスナー
- [ ] Roomキャッシュ連携
- [ ] オフライン対応

### Phase 3: 高度な機能（Week 7-8）
- [ ] QRコード招待
- [ ] 定期購入設定
- [ ] プッシュ通知

## プロジェクト構造（予定）

```
app/src/main/kotlin/net/sumomo_planning/goshopping/
├── data/
│   ├── local/          # Room Database
│   ├── remote/         # Firebase Firestore
│   └── repository/     # Repository実装
├── domain/
│   ├── model/          # Domain Model
│   ├── repository/     # Repository Interface
│   └── usecase/        # UseCase
├── presentation/
│   ├── ui/             # Compose UI
│   └── viewmodel/      # ViewModel
└── di/                 # Hilt Modules
```

## セットアップ手順（準備中）

```bash
# 1. リポジトリクローン
git clone https://github.com/maya27AokiSawada/go_shop_kotlin.git

# 2. Firebase設定ファイル配置
# google-services.json を app/ に配置

# 3. ビルド
./gradlew assembleDebug

# 4. インストール
./gradlew installDebug
```

## 公開前の機密情報チェック

リポジトリを Public にする前に、以下の自動チェックを実行してください。

```powershell
./scripts/security-preflight.ps1
```

このスクリプトは次を検査します。
- 追跡中ファイルに `.env` や `app/google-services.json` などが含まれていないこと
- 追跡中ソースに API キーや秘密鍵パターンがないこと
- Git 履歴に高リスクなキー痕跡が残っていないこと

加えて GitHub Actions の `Security Checks` ワークフローで、PR / main push 時に
同じ preflight と gitleaks を自動実行します。

main ブランチの必須チェック化は、GitHub CLI ログイン後に次で適用できます。

```powershell
gh auth login
./scripts/apply-branch-protection.ps1
```

デフォルトは `maya27AokiSawada/TaskRingK` の `main` に適用します。
Private リポジトリの場合、GitHub プランによっては Branch protection が 403 で拒否されます。
その場合は Public 化後に同コマンドを再実行してください。

## Firestore ルールと Phase 8 検証

Firestore のセキュリティルールは `firebase/firestore.rules` に定義しています。

Phase 8（Whiteboard同期）の検証は次で実行できます。

```powershell
./scripts/phase8-validation.ps1
```

手動チェック項目は `docs/testing/phase8_validation.md` を参照してください。

Firestore ルール反映:

```powershell
./scripts/deploy-firestore-rules.ps1 -ProjectId <your-firebase-project-id>
```

## Flutter版との設計比較

詳細は [docs/flutter_vs_kotlin.md](docs/flutter_vs_kotlin.md) を参照してください。

### 主な設計の違い

**1. State Management**
- Flutter: Riverpodの宣言的状態管理
- Kotlin: ViewModelのライフサイクル連動型

**2. リアクティブプログラミング**
- Flutter: StreamBuilder + FutureBuilder
- Kotlin: Flow + collectAsStateWithLifecycle

**3. 依存性注入**
- Flutter: Providerパターン
- Kotlin: Hiltの自動生成

## 移植時の気づき

移植作業中に得られた知見は [docs/migration_notes.md](docs/migration_notes.md) に記録していきます。

## 開発環境

- Android Studio Iguana | 2023.2.1
- Kotlin 1.9.0+
- Gradle 8.2+
- minSdk 26 (Android 8.0)
- targetSdk 34 (Android 14)
- compileSdk 34

## Flutter版リポジトリ

- [Go Shop (Flutter版)](https://github.com/maya27AokiSawada/go_shop)

## 開発者

maya27AokiSawada

## ライセンス

Private Project - All Rights Reserved

---

**Note**: このプロジェクトは就活ポートフォリオとして、Flutter版の技術的な移植を目的としています。
