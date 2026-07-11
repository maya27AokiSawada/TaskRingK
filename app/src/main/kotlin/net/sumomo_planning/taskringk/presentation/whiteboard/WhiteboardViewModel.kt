package net.sumomo_planning.taskringk.presentation.whiteboard

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.sumomo_planning.taskringk.domain.model.AuthUser
import net.sumomo_planning.taskringk.domain.model.DrawingPoint
import net.sumomo_planning.taskringk.domain.model.DrawingStroke
import net.sumomo_planning.taskringk.domain.model.SharedGroup
import net.sumomo_planning.taskringk.domain.model.StrokeCapStyle
import net.sumomo_planning.taskringk.domain.model.Whiteboard
import net.sumomo_planning.taskringk.domain.usecase.auth.ObserveAuthStateUseCase
import net.sumomo_planning.taskringk.domain.usecase.whiteboard.ClearWhiteboardStrokesUseCase
import net.sumomo_planning.taskringk.domain.usecase.group.ObserveGroupsUseCase
import net.sumomo_planning.taskringk.domain.usecase.whiteboard.CreateWhiteboardUseCase
import net.sumomo_planning.taskringk.domain.usecase.whiteboard.DeleteWhiteboardStrokeUseCase
import net.sumomo_planning.taskringk.domain.usecase.whiteboard.DeleteWhiteboardUseCase
import net.sumomo_planning.taskringk.domain.usecase.whiteboard.ObserveWhiteboardStrokesUseCase
import net.sumomo_planning.taskringk.domain.usecase.whiteboard.ObserveWhiteboardsByGroupUseCase
import net.sumomo_planning.taskringk.domain.usecase.whiteboard.UpsertWhiteboardStrokeUseCase

data class WhiteboardUiState(
    val currentUser: AuthUser? = null,
    val groups: List<SharedGroup> = emptyList(),
    val selectedGroupId: String? = null,
    val whiteboards: List<Whiteboard> = emptyList(),
    val selectedWhiteboardId: String? = null,
    val committedStrokes: List<DrawingStroke> = emptyList(),
    val activeStroke: DrawingStroke? = null,
    val redoStack: List<DrawingStroke> = emptyList(),
    val selectedColorValue: Int = 0xFF111111.toInt(),
    val selectedStrokeWidth: Float = 6f,
    val selectedStrokeCapStyle: StrokeCapStyle = StrokeCapStyle.ROUND,
    val isEraserEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class WhiteboardViewModel @Inject constructor(
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val observeGroupsUseCase: ObserveGroupsUseCase,
    private val observeWhiteboardsByGroupUseCase: ObserveWhiteboardsByGroupUseCase,
    private val createWhiteboardUseCase: CreateWhiteboardUseCase,
    private val deleteWhiteboardUseCase: DeleteWhiteboardUseCase,
    private val observeWhiteboardStrokesUseCase: ObserveWhiteboardStrokesUseCase,
    private val upsertWhiteboardStrokeUseCase: UpsertWhiteboardStrokeUseCase,
    private val deleteWhiteboardStrokeUseCase: DeleteWhiteboardStrokeUseCase,
    private val clearWhiteboardStrokesUseCase: ClearWhiteboardStrokesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WhiteboardUiState())
    val uiState: StateFlow<WhiteboardUiState> = _uiState.asStateFlow()

    private var groupsJob: Job? = null
    private var whiteboardsJob: Job? = null
    private var strokesJob: Job? = null
    private val redoByBoard = mutableMapOf<String, List<DrawingStroke>>()

    init {
        observeAuthStateUseCase()
            .onEach { user ->
                _uiState.update { it.copy(currentUser = user) }
                if (user == null) {
                    groupsJob?.cancel()
                    whiteboardsJob?.cancel()
                    strokesJob?.cancel()
                    _uiState.value = WhiteboardUiState()
                } else {
                    observeGroups(user.uid)
                }
            }
            .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
            .launchIn(viewModelScope)
    }

    private fun observeGroups(uid: String) {
        groupsJob?.cancel()
        _uiState.update { it.copy(isLoading = true) }
        groupsJob = observeGroupsUseCase(uid)
            .onEach { groups ->
                val selectedGroupId = _uiState.value.selectedGroupId ?: groups.firstOrNull()?.groupId
                _uiState.update {
                    it.copy(
                        groups = groups,
                        selectedGroupId = selectedGroupId,
                        isLoading = false,
                        errorMessage = null,
                    )
                }
                if (selectedGroupId != null) observeWhiteboards(selectedGroupId)
            }
            .catch {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "グループの取得に失敗しました")
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeWhiteboards(groupId: String) {
        whiteboardsJob?.cancel()
        whiteboardsJob = observeWhiteboardsByGroupUseCase(groupId)
            .onEach { whiteboards ->
                val selected = _uiState.value.selectedWhiteboardId
                    ?.takeIf { id -> whiteboards.any { it.whiteboardId == id } }
                    ?: whiteboards.firstOrNull()?.whiteboardId
                _uiState.update {
                    it.copy(
                        whiteboards = whiteboards,
                        selectedWhiteboardId = selected,
                    )
                }
                selected?.let { observeStrokes(it) } ?: clearBoardStrokes()
            }
            .catch {
                _uiState.update { it.copy(errorMessage = "ホワイトボード取得に失敗しました") }
            }
            .launchIn(viewModelScope)
    }

    fun selectGroup(groupId: String) {
        _uiState.update {
            it.copy(
                selectedGroupId = groupId,
                selectedWhiteboardId = null,
                whiteboards = emptyList(),
            )
        }
        observeWhiteboards(groupId)
    }

    fun selectWhiteboard(whiteboardId: String) {
        _uiState.update { it.copy(selectedWhiteboardId = whiteboardId) }
        observeStrokes(whiteboardId)
    }

    fun createWhiteboard() {
        val groupId = _uiState.value.selectedGroupId ?: return
        val ownerId = _uiState.value.currentUser?.uid
        viewModelScope.launch {
            createWhiteboardUseCase(groupId, ownerId, isPrivate = false)
                .onFailure {
                    _uiState.update { state ->
                        state.copy(errorMessage = it.message ?: "ホワイトボード作成に失敗しました")
                    }
                }
        }
    }

    fun deleteSelectedWhiteboard() {
        val whiteboardId = _uiState.value.selectedWhiteboardId ?: return
        viewModelScope.launch {
            deleteWhiteboardUseCase(whiteboardId)
                .onFailure {
                    _uiState.update { state ->
                        state.copy(errorMessage = it.message ?: "ホワイトボード削除に失敗しました")
                    }
                }
        }
    }

    fun startStroke(offset: Offset) {
        val boardId = _uiState.value.selectedWhiteboardId ?: return
        val user = _uiState.value.currentUser ?: return
        val color = if (_uiState.value.isEraserEnabled) {
            ERASER_COLOR_VALUE
        } else {
            _uiState.value.selectedColorValue
        }
        val width = _uiState.value.selectedStrokeWidth
        val capStyle = _uiState.value.selectedStrokeCapStyle
        val stroke = DrawingStroke(
            strokeId = UUID.randomUUID().toString(),
            points = listOf(DrawingPoint(offset.x, offset.y)),
            colorValue = color,
            strokeWidth = width,
            strokeCapStyle = capStyle,
            createdAt = java.time.Instant.now(),
            authorId = user.uid,
            authorName = user.displayName ?: user.uid,
        )
        _uiState.update { it.copy(activeStroke = stroke) }
        redoByBoard[boardId] = emptyList()
    }

    fun appendStrokePoint(offset: Offset) {
        val active = _uiState.value.activeStroke ?: return
        val updated = active.copy(points = active.points + DrawingPoint(offset.x, offset.y))
        _uiState.update { it.copy(activeStroke = updated) }
    }

    fun endStroke() {
        val selection = currentSelection() ?: return
        val active = _uiState.value.activeStroke ?: return
        redoByBoard[selection.whiteboardId] = emptyList()
        _uiState.update { it.copy(activeStroke = null, redoStack = emptyList()) }

        viewModelScope.launch {
            upsertWhiteboardStrokeUseCase(selection.groupId, selection.whiteboardId, active)
                .onFailure {
                    _uiState.update { state ->
                        state.copy(errorMessage = it.message ?: "ストローク保存に失敗しました")
                    }
                }
        }
    }

    fun undo() {
        val selection = currentSelection() ?: return
        val current = _uiState.value.committedStrokes
        if (current.isEmpty()) return
        val removed = current.last()
        val redo = listOf(removed) + (_uiState.value.redoStack)
        redoByBoard[selection.whiteboardId] = redo
        _uiState.update { it.copy(redoStack = redo) }

        viewModelScope.launch {
            deleteWhiteboardStrokeUseCase(selection.groupId, selection.whiteboardId, removed.strokeId)
                .onFailure {
                    _uiState.update { state ->
                        state.copy(errorMessage = it.message ?: "ストローク削除に失敗しました")
                    }
                }
        }
    }

    fun redo() {
        val selection = currentSelection() ?: return
        val redo = _uiState.value.redoStack
        if (redo.isEmpty()) return
        val stroke = redo.first()
        val newRedo = redo.drop(1)
        redoByBoard[selection.whiteboardId] = newRedo
        _uiState.update { it.copy(redoStack = newRedo) }

        viewModelScope.launch {
            upsertWhiteboardStrokeUseCase(selection.groupId, selection.whiteboardId, stroke)
                .onFailure {
                    _uiState.update { state ->
                        state.copy(errorMessage = it.message ?: "ストローク再追加に失敗しました")
                    }
                }
        }
    }

    fun clearBoard() {
        val selection = currentSelection() ?: return
        redoByBoard[selection.whiteboardId] = emptyList()
        _uiState.update { it.copy(committedStrokes = emptyList(), activeStroke = null, redoStack = emptyList()) }

        viewModelScope.launch {
            clearWhiteboardStrokesUseCase(selection.groupId, selection.whiteboardId)
                .onFailure {
                    _uiState.update { state ->
                        state.copy(errorMessage = it.message ?: "ボードクリアに失敗しました")
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun selectBrushColor(colorValue: Int) {
        _uiState.update { it.copy(selectedColorValue = colorValue, isEraserEnabled = false) }
    }

    fun updateStrokeWidth(strokeWidth: Float) {
        val clamped = strokeWidth.coerceIn(MIN_STROKE_WIDTH, MAX_STROKE_WIDTH)
        _uiState.update { it.copy(selectedStrokeWidth = clamped) }
    }

    fun selectStrokeCapStyle(style: StrokeCapStyle) {
        _uiState.update { it.copy(selectedStrokeCapStyle = style) }
    }

    fun toggleEraser() {
        _uiState.update { it.copy(isEraserEnabled = !it.isEraserEnabled) }
    }

    private fun observeStrokes(boardId: String) {
        val groupId = _uiState.value.selectedGroupId ?: return
        strokesJob?.cancel()
        _uiState.update {
            it.copy(
                committedStrokes = emptyList(),
                activeStroke = null,
                redoStack = redoByBoard[boardId].orEmpty(),
            )
        }
        strokesJob = observeWhiteboardStrokesUseCase(groupId, boardId)
            .onEach { strokes ->
                _uiState.update {
                    it.copy(
                        committedStrokes = strokes,
                        activeStroke = null,
                        redoStack = redoByBoard[boardId].orEmpty(),
                    )
                }
            }
            .catch {
                _uiState.update { it.copy(errorMessage = "ストローク取得に失敗しました") }
            }
            .launchIn(viewModelScope)
    }

    private data class BoardSelection(
        val groupId: String,
        val whiteboardId: String,
    )

    private fun currentSelection(): BoardSelection? {
        val groupId = _uiState.value.selectedGroupId ?: return null
        val whiteboardId = _uiState.value.selectedWhiteboardId ?: return null
        return BoardSelection(groupId = groupId, whiteboardId = whiteboardId)
    }

    private fun clearBoardStrokes() {
        strokesJob?.cancel()
        _uiState.update {
            it.copy(
                committedStrokes = emptyList(),
                activeStroke = null,
                redoStack = emptyList(),
            )
        }
    }

    private companion object {
        const val MIN_STROKE_WIDTH = 2f
        const val MAX_STROKE_WIDTH = 24f
        const val ERASER_COLOR_VALUE = 0xFFFFFFFF.toInt()
    }
}