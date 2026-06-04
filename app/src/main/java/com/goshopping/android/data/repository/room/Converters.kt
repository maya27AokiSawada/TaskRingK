package com.goshopping.android.data.repository.room

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time

    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun stringListToJson(list: List<String>?): String =
        gson.toJson(list ?: emptyList<String>())

    @TypeConverter
    fun fromMembersJson(value: String?): List<MemberJson> {
        if (value.isNullOrEmpty()) return emptyList()
        val type = object : TypeToken<List<MemberJson>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun membersToJson(members: List<MemberJson>?): String =
        gson.toJson(members ?: emptyList<MemberJson>())
}

/** Room 保存用の SharedGroupMember 簡略表現 */
data class MemberJson(
    val memberId: String = "",
    val name: String = "",
    val contact: String = "",
    val role: String = "member",
    val isSignedIn: Boolean = false,
    val invitationStatus: String = "self",
    val securityKey: String? = null,
    val invitedAt: Long? = null,
    val acceptedAt: Long? = null
)
