# QR招待受諾フローの Flutter 版互換修正計画

ユーザーの指摘に基づき、Kotlin版の招待受諾フローを Flutter 版の「ルート Notifications コレクションを利用するフラットな構造」および「受諾者による直接参加」の流れに合わせるように修正します。

## 変更のポイント

1.  **直接参加の実行**:
    - 招待を受諾する側（非メンバー）が、Firestore の `allowedJoinUpdate` ルールを利用して `SharedGroups` ドキュメントを直接更新し、自分自身をメンバーとして追加します。
    - これにより、その後の通知作成時に「グループメンバーであること」というセキュリティルールをパスできるようになります。
2.  **通知構造のフラット化**:
    - Flutter 版に合わせて、`Notifications` コレクションに作成するドキュメントを、`metadata` マップを使用しないフラットな構造に変更します。
3.  **モデルとマッパーの更新**:
    - `AcceptedInvitation` および `Notification` 関連の DTO / Mapper を更新し、フラットなフィールド（`acceptorUid`, `invitationId` など）をトップレベルで扱えるようにします。

## Proposed Changes

### [Domain Model]

#### [MODIFY] [AcceptedInvitation.kt](file:///I:/KotlinProject/TaskRingK/app/src/main/kotlin/net/sumomo_planning/taskringk/domain/model/AcceptedInvitation.kt)
- `invitationId` フィールドを追加します。

### [Data Layer]

#### [MODIFY] [NotificationDto.kt](file:///I:/KotlinProject/TaskRingK/app/src/main/kotlin/net/sumomo_planning/taskringk/data/remote/dto/NotificationDto.kt)
- `invitationId`, `acceptorUid`, `acceptorName`, `acceptorEmail` などのフラットなフィールドをオプションで追加します。

#### [MODIFY] [NotificationMappers.kt](file:///I:/KotlinProject/TaskRingK/app/src/main/kotlin/net/sumomo_planning/taskringk/data/mapper/NotificationMappers.kt)
- フラットなフィールドが存在する場合に、それを `Notification.metadata` にマッピングするロジックを追加します（既存の `metadata` マップとの互換性維持のため）。

#### [MODIFY] [InvitationMappers.kt](file:///I:/KotlinProject/TaskRingK/app/src/main/kotlin/net/sumomo_planning/taskringk/data/mapper/InvitationMappers.kt)
- `AcceptedInvitation.toFirestoreMap()` に `invitationId` を追加し、通知として必要な基本フィールド（`userId`, `type`, `message`, `isRead`）を含めるように拡張します。

#### [MODIFY] [HybridInvitationRepositoryImpl.kt](file:///I:/KotlinProject/TaskRingK/app/src/main/kotlin/net/sumomo_planning/taskringk/data/repository/HybridInvitationRepositoryImpl.kt)
- `acceptInvitation` 内で、通知作成の前に `processAcceptedInvitation` を呼び出し、直接グループに参加するように変更します。
- 通知作成時に、フラットな構造のマッピングを使用するように修正します。

## Verification Plan

### Automated Tests
- `SharedGroupViewModelTest` を実行し、受諾フローが正常に動作することを確認します。

### Manual Verification
1. 実機で QR スキャンを実行します。
2. `SharedGroups` ドキュメントに `allowedUid` と `members` が即座に追加されることを確認します。
3. `Notifications` コレクションにフラットな構造の通知が作成されることを確認します。
4. 招待側（エミュレータ）がその通知を受け取り、UI に反映されることを確認します。
