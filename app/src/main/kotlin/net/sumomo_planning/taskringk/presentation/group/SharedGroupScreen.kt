package net.sumomo_planning.taskringk.presentation.group

import android.Manifest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.AnnotatedString
import android.graphics.Bitmap
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.util.concurrent.Executors
import net.sumomo_planning.taskringk.core.ui.OfflineBanner
import net.sumomo_planning.taskringk.core.common.InvitationPayload
import net.sumomo_planning.taskringk.core.common.InvitationPayloadParser
import net.sumomo_planning.taskringk.core.common.toInvitationPayloadJson
import net.sumomo_planning.taskringk.domain.model.Invitation
import net.sumomo_planning.taskringk.domain.model.GroupType
import net.sumomo_planning.taskringk.domain.model.SharedGroup
import net.sumomo_planning.taskringk.domain.model.SharedGroupRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedGroupScreen(
    modifier: Modifier = Modifier,
    onOpenGroupDetail: (SharedGroup) -> Unit = {},
    viewModel: SharedGroupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var groupToInvite by remember { mutableStateOf<SharedGroup?>(null) }
    var groupToDelete by remember { mutableStateOf<SharedGroup?>(null) }
    var groupToLeave by remember { mutableStateOf<SharedGroup?>(null) }
    var showScanDialog by rememberSaveable { mutableStateOf(false) }
    var scannedPayload by remember { mutableStateOf<InvitationPayload?>(null) }
    var scanErrorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.errorMessage) {
        val msg = uiState.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearError()
    }

    LaunchedEffect(uiState.activeInvitation) {
        if (uiState.activeInvitation == null) {
            groupToInvite = null
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("グループ管理") },
                actions = {
                    IconButton(onClick = { showScanDialog = true }) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "QRスキャン")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "グループを作成")
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            OfflineBanner(isOnline = uiState.isOnline)

            Box(modifier = Modifier.weight(1f)) {
                when {
                    uiState.isLoading && uiState.groups.isEmpty() -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }

                    uiState.groups.isEmpty() -> {
                        EmptyGroupsPlaceholder(modifier = Modifier.align(Alignment.Center))
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 16.dp,
                                vertical = 8.dp,
                            ),
                        ) {
                            items(
                                items = uiState.groups,
                                key = { it.groupId },
                            ) { group ->
                                GroupCard(
                                    group = group,
                                    currentUid = uiState.currentUser?.uid ?: "",
                                    currentUserEmail = uiState.currentUser?.email ?: "",
                                    onOpenDetail = { onOpenGroupDetail(group) },
                                    onInviteRequested = { groupToInvite = group },
                                    onDeleteRequested = { groupToDelete = group },
                                    onLeaveRequested = { groupToLeave = group },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ---- Dialogs ----

    if (showCreateDialog) {
        CreateGroupDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                viewModel.createGroup(name)
                showCreateDialog = false
            },
        )
    }

    groupToInvite?.let { group ->
        ConfirmInviteDialog(
            groupName = group.groupName,
            onDismiss = { groupToInvite = null },
            onConfirm = {
                viewModel.createInvitation(group)
                groupToInvite = null
            },
        )
    }

    uiState.activeInvitation?.let { invitation ->
        InvitationShareDialog(
            invitation = invitation,
            onCopy = {
                clipboardManager.setText(AnnotatedString(invitation.toInvitationPayloadJson()))
            },
            onDismiss = { viewModel.clearActiveInvitation() },
        )
    }

    if (showScanDialog) {
        QrScanDialog(
            onDismiss = { showScanDialog = false },
            onScanned = { raw ->
                showScanDialog = false
                runCatching { InvitationPayloadParser.parse(raw) }
                    .onSuccess {
                        scannedPayload = it
                        scanErrorMessage = null
                    }
                    .onFailure {
                        scannedPayload = null
                        scanErrorMessage = "QRの内容を解析できませんでした"
                    }
            },
        )
    }

    scannedPayload?.let { payload ->
        ScannedInvitationDialog(
            payload = payload,
            onDismiss = { scannedPayload = null },
        )
    }

    scanErrorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { scanErrorMessage = null },
            title = { Text("QRスキャン") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { scanErrorMessage = null }) { Text("閉じる") }
            },
        )
    }

    groupToDelete?.let { group ->
        ConfirmDeleteDialog(
            groupName = group.groupName,
            onDismiss = { groupToDelete = null },
            onConfirm = {
                viewModel.deleteGroup(group.groupId)
                groupToDelete = null
            },
        )
    }

    groupToLeave?.let { group ->
        val currentEmail = uiState.currentUser?.email ?: ""
        val memberId = group.members.find { it.contact == currentEmail }?.memberId
        ConfirmLeaveDialog(
            groupName = group.groupName,
            onDismiss = { groupToLeave = null },
            onConfirm = {
                if (memberId != null) {
                    viewModel.leaveGroup(group.groupId, memberId)
                }
                groupToLeave = null
            },
        )
    }
}

// ---- Private composables ----

@Composable
private fun EmptyGroupsPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Group,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "グループがありません",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "+ ボタンからグループを作成してください",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun GroupCard(
    group: SharedGroup,
    currentUid: String,
    currentUserEmail: String,
    onOpenDetail: () -> Unit,
    onInviteRequested: () -> Unit,
    onDeleteRequested: () -> Unit,
    onLeaveRequested: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isOwner = group.ownerUid == currentUid
    var showMenu by remember { mutableStateOf(false) }

    Card(
        onClick = onOpenDetail,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (group.groupType == GroupType.SHOPPING)
                        Icons.Default.ShoppingCart else Icons.Default.Group,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.groupName,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${group.members.size} 人のメンバー",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onOpenDetail) {
                Icon(Icons.Default.Group, contentDescription = "詳細を開く")
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "メニュー")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("詳細を開く") },
                        leadingIcon = {
                            Icon(Icons.Default.Group, contentDescription = null)
                        },
                        onClick = {
                            showMenu = false
                            onOpenDetail()
                        },
                    )
                    if (isOwner) {
                        DropdownMenuItem(
                            text = { Text("招待") },
                            leadingIcon = {
                                Icon(Icons.Default.Add, contentDescription = null)
                            },
                            onClick = {
                                showMenu = false
                                onInviteRequested()
                            },
                        )
                        // Owner can delete the group (§3-4)
                        DropdownMenuItem(
                            text = { Text("削除", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            },
                            onClick = {
                                showMenu = false
                                onDeleteRequested()
                            },
                        )
                    } else {
                        // Non-owner member can leave the group (§3-4)
                        DropdownMenuItem(
                            text = { Text("離脱") },
                            leadingIcon = {
                                Icon(Icons.Default.ExitToApp, contentDescription = null)
                            },
                            onClick = {
                                showMenu = false
                                onLeaveRequested()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var groupName by rememberSaveable { mutableStateOf("") }
    val isNameValid = groupName.isNotBlank()

    fun submit() {
        if (isNameValid) onConfirm(groupName.trim())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("グループを作成") },
        text = {
            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                label = { Text("グループ名") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                isError = groupName.isNotEmpty() && !isNameValid,
            )
        },
        confirmButton = {
            TextButton(onClick = ::submit, enabled = isNameValid) {
                Text("作成")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        },
    )
}

@Composable
private fun ConfirmInviteDialog(
    groupName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("グループを招待") },
        text = { Text("「$groupName」への招待コードを作成しますか？") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("作成")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        },
    )
}

@Composable
private fun InvitationShareDialog(
    invitation: Invitation,
    onCopy: () -> Unit,
    onDismiss: () -> Unit,
) {
    val payload = invitation.toInvitationPayloadJson()
    val qrBitmap by rememberUpdatedState(newValue = createQrBitmap(payload, 720))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("招待コードを作成しました") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("この QR または JSON を Flutter 側に渡せます。")
                qrBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "招待QRコード",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                    )
                }
                SelectionContainer {
                    Text(payload)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCopy) {
                Text("コピー")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("閉じる") }
        },
    )
}

private fun createQrBitmap(content: String, sizePx: Int): Bitmap? {
    return runCatching {
        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
        Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888).apply {
            for (x in 0 until sizePx) {
                for (y in 0 until sizePx) {
                    setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
        }
    }.getOrNull()
}

@Composable
private fun QrScanDialog(
    onDismiss: () -> Unit,
    onScanned: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val scanner = remember { BarcodeScanning.getClient() }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var hasCameraPermission by rememberSaveable { mutableStateOf(false) }
    var detected by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { scanner.close() }
            cameraExecutor.shutdown()
            runCatching { cameraProviderFuture.get().unbindAll() }
        }
    }

    LaunchedEffect(hasCameraPermission, detected) {
        if (!hasCameraPermission || detected) return@LaunchedEffect

        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }

        val analyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        analyzer.setAnalyzer(cameraExecutor) { imageProxy ->
            val mediaImage = imageProxy.image
            if (mediaImage == null || detected) {
                imageProxy.close()
                return@setAnalyzer
            }

            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val raw = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue
                    if (!raw.isNullOrBlank() && !detected) {
                        detected = true
                        onScanned(raw)
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            analyzer,
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("QRスキャン") },
        text = {
            if (hasCameraPermission) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                )
            } else {
                Text("カメラ権限が必要です")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("閉じる") }
        },
    )
}

@Composable
private fun ScannedInvitationDialog(
    payload: InvitationPayload,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("招待QRを読み取りました") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("groupId: ${payload.groupId}")
                Text("invitationId: ${payload.invitationId}")
                Text("type: ${payload.type} / v${payload.version}")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("閉じる") }
        },
    )
}

@Composable
private fun ConfirmDeleteDialog(
    groupName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("グループを削除") },
        text = { Text("「$groupName」を削除してもよろしいですか？\nメンバー全員がアクセスできなくなります。") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("削除", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        },
    )
}

@Composable
private fun ConfirmLeaveDialog(
    groupName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("グループを離脱") },
        text = { Text("「$groupName」から離脱してもよろしいですか？") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("離脱") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        },
    )
}
