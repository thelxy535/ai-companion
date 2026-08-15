package com.companion.cc.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 向量记忆实体
 */
@Entity(tableName = "vector_memories")
data class VectorMemoryEntity(
    @PrimaryKey
    val id: String,
    val content: String,
    val embedding: String,  // 存储为JSON字符串
    val type: String,
    val timestamp: Long,
    val userId: String,
    val companionId: String,
    val metadata: String    // 存储为JSON字符串
)

/**
 * 类型转换器
 */
class VectorMemoryConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromFloatArray(value: FloatArray): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toFloatArray(value: String): FloatArray {
        val type = object : TypeToken<FloatArray>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun fromMetadata(value: Map<String, String>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toMetadata(value: String): Map<String, String> {
        val type = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(value, type) ?: emptyMap()
    }
}
