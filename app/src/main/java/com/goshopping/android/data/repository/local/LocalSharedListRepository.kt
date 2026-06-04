package com.goshopping.android.data.repository.local

import com.goshopping.android.data.model.ListType
import com.goshopping.android.data.model.SharedItem
import com.goshopping.android.data.model.SharedList
import com.goshopping.android.data.repository.SharedListRepository
import com.goshopping.android.data.repository.room.SharedItemEntity
import com.goshopping.android.data.repository.room.SharedListDao
import com.goshopping.android.data.repository.room.SharedListEntity
import com.goshopping.android.data.service.DeviceIdService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Date

class LocalSharedListRepository(
    private val dao: SharedListDao
) : SharedListRepository {

    override suspend fun createSharedList(
        ownerUid: String,
        groupId: String,
        listName: String,
        description: String?,
        customListId: String?
    ): SharedList {
        val listId = customListId ?: DeviceIdService.generateListId(
            DeviceIdService.getDevicePrefix()
        )
        val list = SharedList(
            listId = listId,
            listName = listName,
            ownerUid = ownerUid,
            groupId = groupId,
            groupName = "",
            description = description ?: "",
            listType = ListType.SHOPPING,
            items = emptyMap(),
            createdAt = Date()
        )
        dao.upsert(list.toEntity())
        return list
    }

    override suspend fun getSharedListById(groupId: String, listId: String): SharedList? {
        val entity = dao.getById(listId) ?: return null
        val items = dao.getItemsByList(listId)
        return entity.toModel(items)
    }

    override suspend fun getSharedListsByGroup(groupId: String): List<SharedList> {
        val entities = dao.getByGroup(groupId)
        return entities.map { entity ->
            val items = dao.getItemsByList(entity.listId)
            entity.toModel(items)
        }
    }

    override suspend fun updateSharedList(list: SharedList) {
        dao.upsert(list.toEntity())
    }

    override suspend fun deleteSharedList(groupId: String, listId: String) {
        dao.deleteListWithItems(listId)
    }

    override suspend fun addSingleItem(groupId: String, listId: String, item: SharedItem) {
        dao.upsertItem(item.toEntity(listId, groupId))
    }

    override suspend fun updateSingleItem(groupId: String, listId: String, item: SharedItem) {
        dao.upsertItem(item.toEntity(listId, groupId))
    }

    override suspend fun removeSingleItem(groupId: String, listId: String, itemId: String) {
        dao.softDeleteItem(itemId, listId, Date().time)
    }

    /**
     * ローカルDBはリアルタイムリスナーを持たないため、
     * 現在の状態を1度だけ emit する冷たい Flow を返す。
     */
    override fun watchSharedList(groupId: String, listId: String): Flow<SharedList?> = flow {
        emit(getSharedListById(groupId, listId))
    }

    suspend fun saveLists(lists: List<SharedList>) {
        lists.forEach { list ->
            dao.upsert(list.toEntity())
            dao.upsertItems(list.items.values.map { it.toEntity(list.listId, list.groupId) })
        }
    }

    suspend fun deleteGroupData(groupId: String) {
        dao.deleteGroupData(groupId)
    }

    suspend fun deleteAll() {
        dao.deleteAllItems()
        dao.deleteAllLists()
    }

    // ――― 変換 ―――

    private fun SharedList.toEntity(): SharedListEntity = SharedListEntity(
        listId = listId,
        listName = listName,
        ownerUid = ownerUid,
        groupId = groupId,
        groupName = groupName,
        description = description,
        listType = listType.toFirestoreValue(),
        createdAt = createdAt.time,
        updatedAt = updatedAt?.time
    )

    private fun SharedListEntity.toModel(items: List<SharedItemEntity>): SharedList = SharedList(
        listId = listId,
        listName = listName,
        ownerUid = ownerUid,
        groupId = groupId,
        groupName = groupName,
        description = description,
        listType = ListType.fromFirestoreValue(listType),
        items = items.associate { it.itemId to it.toModel() },
        createdAt = Date(createdAt),
        updatedAt = updatedAt?.let { Date(it) }
    )

    private fun SharedItem.toEntity(listId: String, groupId: String): SharedItemEntity =
        SharedItemEntity(
            itemId = itemId,
            listId = listId,
            groupId = groupId,
            name = name,
            quantity = quantity,
            memberId = memberId,
            registeredDate = registeredDate.time,
            purchaseDate = purchaseDate?.time,
            isPurchased = isPurchased,
            isDeleted = isDeleted,
            deletedAt = deletedAt?.time,
            shoppingInterval = shoppingInterval,
            deadline = deadline?.time
        )

    private fun SharedItemEntity.toModel(): SharedItem = SharedItem(
        itemId = itemId,
        name = name,
        quantity = quantity,
        memberId = memberId,
        registeredDate = Date(registeredDate),
        purchaseDate = purchaseDate?.let { Date(it) },
        isPurchased = isPurchased,
        isDeleted = isDeleted,
        deletedAt = deletedAt?.let { Date(it) },
        shoppingInterval = shoppingInterval,
        deadline = deadline?.let { Date(it) }
    )
}
