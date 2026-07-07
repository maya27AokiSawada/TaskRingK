package net.sumomo_planning.goshopping.data.local.room

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.sumomo_planning.goshopping.data.local.room.entity.SharedListEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SharedListDaoTest {

    private lateinit var database: GoShopDatabase

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, GoShopDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    @Throws(IOException::class)
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun upsertAndDeleteByGroup_updatesObservedRows() = runTest {
        val dao = database.sharedListDao()
        dao.upsert(entity("list-1", "group-1"))
        dao.upsert(entity("list-2", "group-1"))
        dao.upsert(entity("list-3", "group-2"))

        assertEquals(listOf("list-1", "list-2"), dao.observeByGroup("group-1").first().map { it.listId }.sorted())

        dao.deleteByGroup("group-1")

        assertEquals(emptyList<SharedListEntity>(), dao.observeByGroup("group-1").first())
        assertEquals(listOf("list-3"), dao.observeByGroup("group-2").first().map { it.listId })
    }

    private fun entity(listId: String, groupId: String): SharedListEntity = SharedListEntity(
        listId = listId,
        listName = listId,
        ownerUid = "owner",
        groupId = groupId,
        groupName = groupId,
        description = "",
        listType = "shopping",
        listKind = "shoppingList",
        itemsJson = "{}",
        createdAt = 1782777600000,
        updatedAt = null,
    )
}
