package net.sumomo_planning.taskringk.presentation.whiteboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Locale
import net.sumomo_planning.taskringk.domain.model.StrokeCapStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhiteboardScreen(
    modifier: Modifier = Modifier,
    viewModel: WhiteboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.errorMessage) {
        val msg = uiState.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearError()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("ホワイトボード") },
                actions = {
                    IconButton(onClick = { viewModel.undo() }, enabled = uiState.committedStrokes.isNotEmpty()) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "元に戻す")
                    }
                    IconButton(onClick = { viewModel.redo() }, enabled = uiState.redoStack.isNotEmpty()) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "やり直し")
                    }
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        enabled = uiState.selectedWhiteboardId != null,
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "ボード削除")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.createWhiteboard() }) {
                Icon(Icons.Default.Add, contentDescription = "ボード作成")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        if (uiState.isLoading && uiState.groups.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GroupSelectorRow(
                    groups = uiState.groups,
                    selectedGroupId = uiState.selectedGroupId,
                    onSelectGroup = viewModel::selectGroup,
                )
                BoardSelectorRow(
                    selectedWhiteboardId = uiState.selectedWhiteboardId,
                    boardIds = uiState.whiteboards.map { it.whiteboardId },
                    onSelectBoard = viewModel::selectWhiteboard,
                    onClearBoard = viewModel::clearBoard,
                )
                BrushToolRow(
                    selectedColorValue = uiState.selectedColorValue,
                    selectedStrokeWidth = uiState.selectedStrokeWidth,
                    selectedCapStyle = uiState.selectedStrokeCapStyle,
                    isEraserEnabled = uiState.isEraserEnabled,
                    onSelectColor = viewModel::selectBrushColor,
                    onStrokeWidthChange = viewModel::updateStrokeWidth,
                    onSelectCapStyle = viewModel::selectStrokeCapStyle,
                    onToggleEraser = viewModel::toggleEraser,
                )
                WhiteboardCanvas(
                    committedStrokes = uiState.committedStrokes,
                    activeStroke = uiState.activeStroke,
                    enabled = uiState.selectedWhiteboardId != null,
                    onStrokeStart = viewModel::startStroke,
                    onStrokePoint = viewModel::appendStrokePoint,
                    onStrokeEnd = viewModel::endStroke,
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("ボードを削除") },
            text = { Text("選択中のホワイトボードを削除しますか？") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteSelectedWhiteboard()
                    },
                ) { Text("削除") }
            },
            dismissButton = {
                Button(onClick = { showDeleteDialog = false }) { Text("キャンセル") }
            },
        )
    }
}

@Composable
private fun GroupSelectorRow(
    groups: List<net.sumomo_planning.taskringk.domain.model.SharedGroup>,
    selectedGroupId: String?,
    onSelectGroup: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = groups.firstOrNull { it.groupId == selectedGroupId }?.groupName ?: "グループ未選択"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("グループ:", style = MaterialTheme.typography.titleSmall)
        Button(onClick = { expanded = true }) {
            Text(selectedName)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            groups.forEach { group ->
                DropdownMenuItem(
                    text = { Text(group.groupName) },
                    onClick = {
                        expanded = false
                        onSelectGroup(group.groupId)
                    },
                )
            }
        }
    }
}

@Composable
private fun BoardSelectorRow(
    selectedWhiteboardId: String?,
    boardIds: List<String>,
    onSelectBoard: (String) -> Unit,
    onClearBoard: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = selectedWhiteboardId?.takeLast(8)?.let { "Board-$it" } ?: "ボード未選択"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("ボード:", style = MaterialTheme.typography.titleSmall)
        Button(onClick = { expanded = true }, enabled = boardIds.isNotEmpty()) {
            Text(selectedLabel)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            boardIds.forEach { boardId ->
                DropdownMenuItem(
                    text = { Text("Board-${boardId.takeLast(8)}") },
                    onClick = {
                        expanded = false
                        onSelectBoard(boardId)
                    },
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Button(onClick = onClearBoard, enabled = selectedWhiteboardId != null) {
            Text("クリア")
        }
    }
}

@Composable
private fun WhiteboardCanvas(
    committedStrokes: List<net.sumomo_planning.taskringk.domain.model.DrawingStroke>,
    activeStroke: net.sumomo_planning.taskringk.domain.model.DrawingStroke?,
    enabled: Boolean,
    onStrokeStart: (Offset) -> Unit,
    onStrokePoint: (Offset) -> Unit,
    onStrokeEnd: () -> Unit,
) {
    val hScroll = rememberScrollState()
    val vScroll = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(8.dp)
            .horizontalScroll(hScroll)
            .verticalScroll(vScroll),
    ) {
        Canvas(
            modifier = Modifier
                .size(width = 1280.dp, height = 720.dp)
                .background(Color.White, RoundedCornerShape(8.dp))
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectDragGestures(
                        onDragStart = { offset -> onStrokeStart(offset) },
                        onDrag = { change, _ ->
                            onStrokePoint(change.position)
                            change.consume()
                        },
                        onDragEnd = onStrokeEnd,
                        onDragCancel = onStrokeEnd,
                    )
                },
        ) {
            committedStrokes.forEach { stroke ->
                drawStroke(stroke)
            }
            activeStroke?.let { drawStroke(it) }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStroke(
    stroke: net.sumomo_planning.taskringk.domain.model.DrawingStroke,
) {
    if (stroke.points.size < 2) return
    val color = Color(stroke.colorValue)
    val cap = when (stroke.strokeCapStyle) {
        StrokeCapStyle.ROUND -> StrokeCap.Round
        StrokeCapStyle.BUTT -> StrokeCap.Butt
    }
    stroke.points.zipWithNext().forEach { (from, to) ->
        drawLine(
            color = color,
            start = Offset(from.x, from.y),
            end = Offset(to.x, to.y),
            strokeWidth = stroke.strokeWidth,
            cap = cap,
        )
    }
}

@Composable
private fun BrushToolRow(
    selectedColorValue: Int,
    selectedStrokeWidth: Float,
    selectedCapStyle: StrokeCapStyle,
    isEraserEnabled: Boolean,
    onSelectColor: (Int) -> Unit,
    onStrokeWidthChange: (Float) -> Unit,
    onSelectCapStyle: (StrokeCapStyle) -> Unit,
    onToggleEraser: () -> Unit,
) {
    val palette = listOf(
        0xFF111111.toInt(),
        0xFFD32F2F.toInt(),
        0xFF1976D2.toInt(),
        0xFF2E7D32.toInt(),
        0xFFF9A825.toInt(),
        0xFF6A1B9A.toInt(),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("色:", style = MaterialTheme.typography.titleSmall)
            palette.forEach { colorValue ->
                val isSelected = colorValue == selectedColorValue
                val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .border(width = 2.dp, color = borderColor, shape = RoundedCornerShape(14.dp))
                        .background(color = Color(colorValue), shape = RoundedCornerShape(14.dp))
                        .padding(2.dp)
                        .clickable { onSelectColor(colorValue) },
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("太さ:", style = MaterialTheme.typography.titleSmall)
            Slider(
                modifier = Modifier.weight(1f),
                value = selectedStrokeWidth,
                onValueChange = onStrokeWidthChange,
                valueRange = 2f..24f,
            )
            Text(String.format(Locale.ROOT, "%.1f", selectedStrokeWidth))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("線端:", style = MaterialTheme.typography.titleSmall)
            Button(
                onClick = { onSelectCapStyle(StrokeCapStyle.ROUND) },
                enabled = selectedCapStyle != StrokeCapStyle.ROUND,
            ) {
                Text("Round")
            }
            Button(
                onClick = { onSelectCapStyle(StrokeCapStyle.BUTT) },
                enabled = selectedCapStyle != StrokeCapStyle.BUTT,
            ) {
                Text("Butt")
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = onToggleEraser) {
                Text(if (isEraserEnabled) "消しゴムON" else "消しゴムOFF")
            }
        }
    }
}