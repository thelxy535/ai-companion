package com.companion.cc.data.local.database

import androidx.room.TypeConverter
import com.companion.cc.domain.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Room 类型转换器：处理 LocalDateTime 和其他自定义类型
 *
 * LocalDateTime 存储为 epoch milliseconds (UTC)
 */
class AppTypeConverters {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * LocalDateTime -> Long (epoch millis)
     * 将 LocalDateTime 转换为 UTC epoch 毫秒数存储到数据库
     */
    @TypeConverter
    fun localDateTimeToEpochMillis(value: LocalDateTime?): Long? {
        return value?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    }

    /**
     * Long (epoch millis) -> LocalDateTime
     * 从数据库读取 epoch 毫秒数并转换为系统时区的 LocalDateTime
     */
    @TypeConverter
    fun epochMillisToLocalDateTime(value: Long?): LocalDateTime? {
        return value?.let {
            LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault())
        }
    }

    // PersonalityTraits 转换器
    @TypeConverter
    fun personalityTraitsToString(value: PersonalityTraits?): String? {
        return value?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun stringToPersonalityTraits(value: String?): PersonalityTraits? {
        return value?.let { json.decodeFromString<PersonalityTraits>(it) }
    }

    // ExampleDialogue 列表转换器
    @TypeConverter
    fun exampleDialoguesToString(value: List<ExampleDialogue>?): String? {
        return value?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun stringToExampleDialogues(value: String?): List<ExampleDialogue>? {
        return value?.let { json.decodeFromString<List<ExampleDialogue>>(it) }
    }

    // VoiceConfig 转换器
    @TypeConverter
    fun voiceConfigToString(value: VoiceConfig?): String? {
        return value?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun stringToVoiceConfig(value: String?): VoiceConfig? {
        return value?.let { json.decodeFromString<VoiceConfig>(it) }
    }

    // BehaviorRules 转换器
    @TypeConverter
    fun behaviorRulesToString(value: BehaviorRules?): String? {
        return value?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun stringToBehaviorRules(value: String?): BehaviorRules? {
        return value?.let { json.decodeFromString<BehaviorRules>(it) }
    }

    // String 列表转换器
    @TypeConverter
    fun stringListToString(value: List<String>?): String? {
        return value?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun stringToStringList(value: String?): List<String>? {
        return value?.let { json.decodeFromString<List<String>>(it) }
    }
}
