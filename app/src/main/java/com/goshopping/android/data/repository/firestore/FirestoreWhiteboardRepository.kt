package com.goshopping.android.data.repository.firestore

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.goshopping.android.data.model.DrawingStroke
import com.goshopping.android.data.model.Whiteboard
import com.goshopping.android.data.repository.WhiteboardRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID

class FirestoreWhiteboardRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : WhiteboardRepository {

    private fun whiteboardsCollection(groupId: String) =
        firestore.collection("SharedGroups").document(groupId).collection("whiteboards")

    private fun strokesCollection(groupId: String, whiteboardId: String) =
        whiteboardsCollection(groupId).document(whiteboardId).collection("strokes")

    override suspend fun getOrCreateGroupWhiteboard(groupId: String): Whiteboard {
        val snapshot = whiteboardsCollection(groupId)
            .whereEqualTo("isPrivate", false)
            .get()
            .await()
        val existing = snapshot.documents.firstNotNullOfOrNull { doc ->
            doc.data?.let { Whiteboard.fromMap(it) }?.takeIf { it.isGroupShared }
        }
        if (existing != null) return existing

        val whiteboard = Whiteboard(
            whiteboardId = UUID.randomUUID().toString(),
            groupId = groupId,
            ownerId = null,
            isPrivate = false,
            createdAt = Date()
        )
        val data = whiteboard.toMap().toMutableMap<String, Any?>()
        data["createdAt"] = FieldValue.serverTimestamp()
        whiteboardsCollection(groupId).document(whiteboard.whiteboardId).set(data).await()
        return whiteboard
    }

    override suspend fun getOrCreatePrivateWhiteboard(
        groupId: String,
        ownerId: String
    ): Whiteboard {
        val snapshot = whiteboardsCollection(groupId)
            .whereEqualTo("ownerId", ownerId)
            .whereEqualTo("isPrivate", true)
            .get()
            .await()
        val existing = snapshot.documents.firstNotNullOfOrNull { doc ->
            doc.data?.let { Whiteboard.fromMap(it) }
        }
        if (existing != null) return existing

        val whiteboard = Whiteboard(
            whiteboardId = UUID.randomUUID().toString(),
            groupId = groupId,
            ownerId = ownerId,
            isPrivate = true,
            createdAt = Date()
        )
        val data = whiteboard.toMap().toMutableMap<String, Any?>()
        data["createdAt"] = FieldValue.serverTimestamp()
        whiteboardsCollection(groupId).document(whiteboard.whiteboardId).set(data).await()
        return whiteboard
    }

    override suspend fun addStrokesToSubcollection(
        groupId: String,
        whiteboardId: String,
        newStrokes: List<DrawingStroke>
    ) {
        if (newStrokes.isEmpty()) return
        val batch = firestore.batch()
        val strokes = strokesCollection(groupId, whiteboardId)

        newStrokes.forEach { stroke ->
            val docRef = strokes.document(stroke.strokeId)
            batch.set(docRef, stroke.toMap())
        }
        // 親ドキュメントの updatedAt も更新
        batch.update(
            whiteboardsCollection(groupId).document(whiteboardId),
            mapOf("updatedAt" to FieldValue.serverTimestamp())
        )
        batch.commit().await()
    }

    override suspend fun deleteStroke(
        groupId: String,
        whiteboardId: String,
        strokeId: String
    ) {
        strokesCollection(groupId, whiteboardId).document(strokeId).delete().await()
    }

    override suspend fun clearStrokes(groupId: String, whiteboardId: String) {
        val snapshot = strokesCollection(groupId, whiteboardId).get().await()
        val batch = firestore.batch()
        snapshot.documents.forEach { batch.delete(it.reference) }
        batch.commit().await()
    }

    /**
     * ストローク監視。
     * orderBy は使用せずクライアント側で createdAt ソートする（インデックス不要）。
     */
    override fun watchStrokes(groupId: String, whiteboardId: String): Flow<List<DrawingStroke>> =
        callbackFlow {
            val listener = strokesCollection(groupId, whiteboardId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    val strokes = snapshot?.documents
                        ?.mapNotNull { doc -> doc.data?.let { DrawingStroke.fromMap(it) } }
                        ?.sortedBy { it.createdAt }
                        ?: emptyList()
                    trySend(strokes)
                }
            awaitClose { listener.remove() }
        }
}
