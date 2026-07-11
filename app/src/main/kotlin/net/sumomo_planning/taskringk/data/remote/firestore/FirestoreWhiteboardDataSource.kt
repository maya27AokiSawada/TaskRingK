package net.sumomo_planning.taskringk.data.remote.firestore

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import net.sumomo_planning.taskringk.data.mapper.toDomain
import net.sumomo_planning.taskringk.data.mapper.toFirestoreMap
import net.sumomo_planning.taskringk.data.remote.dto.DrawingStrokeDto
import net.sumomo_planning.taskringk.data.remote.dto.WhiteboardDto
import net.sumomo_planning.taskringk.domain.model.DrawingStroke
import net.sumomo_planning.taskringk.domain.model.Whiteboard

@Singleton
class FirestoreWhiteboardDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    private fun whiteboardsCollection(groupId: String) =
        firestore.collection("SharedGroups").document(groupId).collection("whiteboards")

    private fun strokesCollection(groupId: String, whiteboardId: String) =
        whiteboardsCollection(groupId).document(whiteboardId).collection("strokes")

    fun observeByGroup(groupId: String): Flow<List<Whiteboard>> = callbackFlow {
        val listener = whiteboardsCollection(groupId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val whiteboards = snapshot?.documents?.mapNotNull { doc ->
                    runCatching { doc.toObject(WhiteboardDto::class.java)?.toDomain() }
                        .onFailure { Log.w(TAG, "Failed to deserialize whiteboard ${doc.id}", it) }
                        .getOrNull()
                } ?: emptyList()
                trySend(whiteboards)
            }
        awaitClose { listener.remove() }
    }

    fun observeStrokes(groupId: String, whiteboardId: String): Flow<List<DrawingStroke>> = callbackFlow {
        val listener = strokesCollection(groupId, whiteboardId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val strokes = snapshot?.documents?.mapNotNull { doc ->
                    runCatching { doc.toObject(DrawingStrokeDto::class.java)?.toDomain() }
                        .onFailure { Log.w(TAG, "Failed to deserialize stroke ${doc.id}", it) }
                        .getOrNull()
                }?.sortedBy { it.createdAt } ?: emptyList()
                trySend(strokes)
            }
        awaitClose { listener.remove() }
    }

    suspend fun createWhiteboard(groupId: String, whiteboard: Whiteboard) {
        whiteboardsCollection(groupId)
            .document(whiteboard.whiteboardId)
            .set(whiteboard.toFirestoreMap())
            .await()
    }

    suspend fun deleteWhiteboard(groupId: String, whiteboardId: String) {
        whiteboardsCollection(groupId).document(whiteboardId).delete().await()
    }

    suspend fun upsertStroke(groupId: String, whiteboardId: String, stroke: DrawingStroke) {
        strokesCollection(groupId, whiteboardId)
            .document(stroke.strokeId)
            .set(stroke.toFirestoreMap())
            .await()
    }

    suspend fun deleteStroke(groupId: String, whiteboardId: String, strokeId: String) {
        strokesCollection(groupId, whiteboardId).document(strokeId).delete().await()
    }

    suspend fun listStrokes(groupId: String, whiteboardId: String): List<DrawingStroke> {
        val snapshot = strokesCollection(groupId, whiteboardId).get().await()
        return snapshot.documents.mapNotNull { doc ->
            runCatching { doc.toObject(DrawingStrokeDto::class.java)?.toDomain() }
                .onFailure { Log.w(TAG, "Failed to deserialize stroke ${doc.id}", it) }
                .getOrNull()
        }.sortedBy { it.createdAt }
    }

    suspend fun clearStrokes(groupId: String, whiteboardId: String) {
        val snapshot = strokesCollection(groupId, whiteboardId).get().await()
        if (snapshot.isEmpty) return

        snapshot.documents.chunked(MAX_BATCH_SIZE).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { doc -> batch.delete(doc.reference) }
            batch.commit().await()
        }
    }

    private companion object {
        const val TAG = "FirestoreWhiteboardDS"
        const val MAX_BATCH_SIZE = 500
    }
}
