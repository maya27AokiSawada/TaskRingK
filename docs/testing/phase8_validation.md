# Phase 8 Validation Checklist

## Purpose

Validate Whiteboard realtime sync and offline recovery on two devices, and confirm Firestore rules alignment for whiteboards and strokes.

## Preconditions

1. Two devices or emulators are signed in with different users.
2. Both users belong to the same SharedGroup.
3. Firebase project uses rules from firebase/firestore.rules.

## Automated Checks

Run:

```powershell
./scripts/phase8-validation.ps1
```

Expected:

1. WhiteboardViewModel unit tests pass.
2. HybridWhiteboardRepositoryImpl unit tests pass.
3. Device count warning is not shown when two devices are connected.

## Manual Two-Device Sync Checks

1. On Device A, open the same group whiteboard as Device B.
2. Draw 3 strokes on Device A.
3. Confirm Device B receives all 3 strokes within a few seconds.
4. Delete one stroke on Device B.
5. Confirm Device A removes the same stroke.

Pass criteria:

1. Both devices converge to the same stroke set.
2. No duplicate strokes after multiple edits.

## Offline Recovery Checks

1. Put Device A in airplane mode.
2. Draw 2 new strokes on Device A.
3. Keep Device B online and confirm it does not receive them yet.
4. Disable airplane mode on Device A.
5. Confirm Device B receives the queued strokes after reconnection.

Pass criteria:

1. Offline-created strokes are not lost.
2. Reconnect sync happens without app restart.

## Rules Alignment Checks

1. Group member can create and update their own strokes.
2. Group member cannot modify another member's stroke.
3. Group owner can delete any stroke in the group.
4. Non-member cannot read whiteboards or strokes.

Pass criteria:

1. Allowed actions succeed.
2. Disallowed actions return Firestore permission denied.
