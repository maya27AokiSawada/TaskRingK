package com.goshopping.android.data.repository

import com.goshopping.android.data.model.DrawingStroke
import com.goshopping.android.data.model.Whiteboard
import kotlinx.coroutines.flow.Flow

interface WhiteboardRepository {
    /** グループ共有ホワイトボードを取得（存在しなければ作成） */
    suspend fun getOrCreateGroupWhiteboard(groupId: String): Whiteboard

    /** 個人用ホワイトボードを取得（存在しなければ作成） */
    suspend fun getOrCreatePrivateWhiteboard(groupId: String, ownerId: String): Whiteboard

    /**
     * 未保存ストロークをサブコレクションにバッチ書き込みする。
     * fire-and-forget で呼び出すこと（await しない）。
     */
    suspend fun addStrokesToSubcollection(
        groupId: String,
        whiteboardId: String,
        newStrokes: List<DrawingStroke>
    )

    suspend fun deleteStroke(groupId: String, whiteboardId: String, strokeId: String)

    suspend fun clearStrokes(groupId: String, whiteboardId: String)

    /**
     * ストロークをリアルタイムで監視する Flow を返す。
     * orderBy は使用せずクライアントソートで代替する。
     */
    fun watchStrokes(groupId: String, whiteboardId: String): Flow<List<DrawingStroke>>
}
