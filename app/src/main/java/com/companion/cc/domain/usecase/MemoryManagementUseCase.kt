package com.companion.cc.domain.usecase

import com.companion.cc.data.local.dao.MemoryDao
import com.companion.cc.data.local.dao.UserProfileDao
import com.companion.cc.data.local.entity.MemoryEntity
import com.companion.cc.data.local.entity.UserProfileEntity
import com.companion.cc.domain.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * 记忆管理UseCase
 * 参考原Web版的EnhancedMemorySystem
 */
class MemoryManagementUseCase @Inject constructor(
    private val memoryDao: MemoryDao,
    private val userProfileDao: UserProfileDao
) {

    /**
     * 保存记忆
     */
    suspend fun saveMemory(
        userId: String,
        role: MessageRole,
        content: String,
        emotion: String?,
        timestamp: Long = System.currentTimeMillis(),
        isUserMarked: Boolean = false
    ) {
        val date = getDateString(timestamp)
        val importance = calculateImportance(content, emotion)
        val topics = extractTopics(content)

        val memory = MemoryEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            role = role.name,
            content = content,
            timestamp = timestamp,
            date = date,
            importance = importance,
            emotion = emotion,
            topics = JSONArray(topics).toString()
        )

        memoryDao.insertMemory(memory)

        // 定期清理旧记忆（保留最近1000条）
        val count = memoryDao.getMemoryCount(userId)
        if (count > 1200) {
            memoryDao.pruneOldMemories(userId, 1000)
        }
    }

    /**
     * 获取记忆上下文（用于生成AI回复）
     */
    suspend fun getMemoryContext(
        userId: String,
        currentMessage: String
    ): MemoryContext {
        // 1. 短期记忆（最近10条）
        val shortTermMemories = memoryDao.getRecentMemories(userId, 10)
            .map { it.toMemory() }

        // 2. 用户画像
        val profile = userProfileDao.getProfile(userId)?.toUserProfile()

        // 3. 相关记忆检索（简化版：搜索关键词）
        val keywords = extractKeywords(currentMessage)
        val relevantMemories = mutableListOf<Memory>()

        for (keyword in keywords.take(3)) { // 最多搜索3个关键词
            val results = memoryDao.searchMemories(userId, keyword, 5)
            relevantMemories.addAll(results.map { it.toMemory() })
        }

        // 4. 重要记忆（importance >= 70）
        val importantMemories = memoryDao.getImportantMemories(userId, 70, 10)
            .map { it.toMemory() }

        // 去重并按重要性排序
        val allRelevant = (relevantMemories + importantMemories)
            .distinctBy { it.id }
            .sortedByDescending { it.importance }
            .take(10)

        return MemoryContext(
            shortTermMemories = shortTermMemories,
            userProfile = profile,
            relevantMemories = allRelevant
        )
    }

    /**
     * 更新用户画像
     * 参考原Web版的extractUserProfile
     */
    suspend fun updateUserProfile(userId: String, message: String) {
        val currentProfile = userProfileDao.getProfile(userId)?.toUserProfile()
            ?: UserProfile(userId = userId)

        // 提取信息
        val newInfo = extractProfileInfo(message, currentProfile)

        // 合并更新
        val updatedProfile = currentProfile.copy(
            basicInfo = mergeBasicInfo(currentProfile.basicInfo, newInfo.basicInfo),
            personality = mergePersonality(currentProfile.personality, newInfo.personality),
            interests = (currentProfile.interests + newInfo.interests).distinct(),
            relationships = (currentProfile.relationships + newInfo.relationships).distinct(),
            importantEvents = (currentProfile.importantEvents + newInfo.importantEvents).distinct(),
            habits = (currentProfile.habits + newInfo.habits).distinct(),
            updatedAt = System.currentTimeMillis()
        )

        userProfileDao.insertOrUpdateProfile(updatedProfile.toEntity())
    }

    /**
     * 计算记忆重要性（0-1范围）
     * 完全照搬原Web版 EnhancedMemorySystem.js assessImportance()
     */
    private fun calculateImportance(content: String, emotion: String?): Int {
        var score = 0.5f // 基础分

        // 1. 首次提及个人信息 +0.4（完全照搬原版）
        val personalInfoPatterns = listOf(
            Regex("我叫|我是|我的名字"),
            Regex("我(\\d{1,2})岁|我今年"),
            Regex("我是.{2,10}?(工程师|程序员|医生|教师|学生|设计师)"),
            Regex("我在.{2,10}?(住|工作)"),
            Regex("我的?(男朋友|女朋友|老公|老婆|爸爸|妈妈)"),
            Regex("我的?生日")
        )
        for (pattern in personalInfoPatterns) {
            if (pattern.containsMatchIn(content)) {
                score += 0.4f
                break
            }
        }

        // 2. 强烈情绪 +0.3（完全照搬原版）
        val strongEmotionPatterns = listOf(
            Regex("太(开心|高兴|棒|好|爽)了|超级(开心|高兴)"),
            Regex("很(难过|伤心|痛苦|绝望)|非常(难过|伤心)"),
            Regex("气死了|超级生气|太气人了"),
            Regex("吓死了|太可怕了|害怕死了")
        )
        for (pattern in strongEmotionPatterns) {
            if (pattern.containsMatchIn(content)) {
                score += 0.3f
                break
            }
        }

        // 3. 重要决定/事件 +0.3（完全照搬原版）
        val importantDecisionPatterns = listOf(
            Regex("我决定|我打算|我要"),
            Regex("结婚|离婚|辞职|跳槽|搬家|买房"),
            Regex("考试|面试|毕业|入学")
        )
        for (pattern in importantDecisionPatterns) {
            if (pattern.containsMatchIn(content)) {
                score += 0.3f
                break
            }
        }

        // 4. 具体计划/日期 +0.2（完全照搬原版）
        val planPatterns = listOf(
            Regex("明天|后天|下周|下个月"),
            Regex("\\d+月\\d+日|\\d+号"),
            Regex("计划|安排|约好了")
        )
        for (pattern in planPatterns) {
            if (pattern.containsMatchIn(content)) {
                score += 0.2f
                break
            }
        }

        // 5. 问题求助 +0.2（完全照搬原版）
        val helpPatterns = listOf(
            Regex("怎么办|该怎么|如何"),
            Regex("帮我|帮忙"),
            Regex("不知道.*怎么")
        )
        for (pattern in helpPatterns) {
            if (pattern.containsMatchIn(content)) {
                score += 0.2f
                break
            }
        }

        // 6. 感谢/认可 +0.1（完全照搬原版）
        val thanksPatterns = listOf(
            Regex("谢谢|感谢"),
            Regex("你真(好|棒|厉害)"),
            Regex("帮了我大忙")
        )
        for (pattern in thanksPatterns) {
            if (pattern.containsMatchIn(content)) {
                score += 0.1f
                break
            }
        }

        // 7. 简短闲聊 -0.2（完全照搬原版）
        if (content.length < 5) {
            score -= 0.2f
        }

        // 8. 常见寒暄 -0.2（完全照搬原版）
        val smallTalkPatterns = listOf(
            Regex("^(嗯|哦|好|行|可以|ok|OK)$"),
            Regex("^你好|早上好|晚安|拜拜|再见$"),
            Regex("今天天气")
        )
        for (pattern in smallTalkPatterns) {
            if (pattern.containsMatchIn(content)) {
                score -= 0.2f
                break
            }
        }

        // 9. 重复内容检测（如果metadata中有标记）
        // TODO: 需要在调用时传入isRepeat标志
        // if (metadata.isRepeat) { score -= 0.3f }

        // 10. 用户明确标记的重要性
        // TODO: 需要UI支持用户标记
        // if (metadata.userMarkedImportant) { score = 1.0f }

        // 限制在 0-1 范围（完全照搬原版）
        score = score.coerceIn(0f, 1f)

        // 转换为0-100范围存储到数据库
        return (score * 100).toInt()
    }

    /**
     * 提取话题
     */
    private fun extractTopics(content: String): List<String> {
        val topics = mutableListOf<String>()

        val topicKeywords = mapOf(
            "工作" to listOf("工作", "加班", "项目", "开会", "同事", "领导", "公司"),
            "学习" to listOf("学习", "考试", "作业", "课程", "老师", "同学"),
            "感情" to listOf("男朋友", "女朋友", "喜欢", "爱", "恋爱", "分手"),
            "家庭" to listOf("爸爸", "妈妈", "家人", "父母", "兄弟", "姐妹"),
            "健康" to listOf("生病", "医院", "身体", "健康", "锻炼", "睡眠"),
            "娱乐" to listOf("电影", "游戏", "音乐", "旅游", "运动"),
            "情绪" to listOf("开心", "难过", "生气", "焦虑", "压力", "孤独")
        )

        for ((topic, keywords) in topicKeywords) {
            if (keywords.any { content.contains(it) }) {
                topics.add(topic)
            }
        }

        return topics
    }

    /**
     * 提取关键词（用于检索）
     */
    private fun extractKeywords(message: String): List<String> {
        val keywords = mutableListOf<String>()

        // 简单分词（按空格和标点分割）
        val words = message.split(Regex("[\\s，。！？、]"))
            .filter { it.length >= 2 } // 至少2个字
            .distinct()

        // 过滤停用词
        val stopWords = setOf("的", "了", "是", "在", "我", "你", "他", "她", "我们", "你们", "他们")
        keywords.addAll(words.filter { it !in stopWords })

        return keywords.take(10)
    }

    /**
     * 提取用户画像信息
     */
    /**
     * 提取用户画像信息
     * 完全照搬原Web版 EnhancedMemorySystem.js updateUserProfile()
     */
    private fun extractProfileInfo(message: String, currentProfile: UserProfile): UserProfile {
        var basicInfo = currentProfile.basicInfo
        val interests = currentProfile.interests.toMutableList()
        val relationships = currentProfile.relationships.toMutableList()
        val importantEvents = currentProfile.importantEvents.toMutableList()
        val habits = currentProfile.habits.toMutableList()

        // 1. 提取姓名（完全照搬原版4种模式）
        if (basicInfo.name == null) {
            val namePatterns = listOf(
                Regex("我叫(.{2,10})"),
                Regex("叫我(.{2,10})"),
                Regex("我的名字是(.{2,10})"),
                Regex("大家叫我(.{2,10})")
            )

            // 单独处理"我是XX"模式（容易与职业混淆）
            val iAmMatch = Regex("我是(.{2,10})").find(message)
            if (iAmMatch != null) {
                val extracted = iAmMatch.groupValues[1].trim()
                // 只有不是职业词时才当作姓名
                val occupationWords = listOf("工程师", "程序员", "医生", "教师", "老师", "学生", "设计师", "律师", "会计", "销售")
                if (!occupationWords.any { extracted.contains(it) }) {
                    basicInfo = basicInfo.copy(name = extracted)
                }
            }

            // 尝试其他模式
            for (pattern in namePatterns) {
                val match = pattern.find(message)
                if (match != null) {
                    basicInfo = basicInfo.copy(name = match.groupValues[1].trim())
                    break
                }
            }
        }

        // 2. 提取年龄（完全照搬原版3种模式）
        if (basicInfo.age == null) {
            val agePatterns = listOf(
                Regex("我(\\d{1,2})岁"),
                Regex("我今年(\\d{1,2})"),
                Regex("(\\d{1,2})岁")
            )
            for (pattern in agePatterns) {
                val match = pattern.find(message)
                if (match != null) {
                    val age = match.groupValues[1].toIntOrNull()
                    if (age != null && age in 1..120) {
                        basicInfo = basicInfo.copy(age = age)
                        break
                    }
                }
            }
        }

        // 3. 提取职业（完全照搬原版4种模式）
        if (basicInfo.occupation == null) {
            val occupationPatterns = listOf(
                Regex("我是(.{2,10}?)(工程师|程序员|医生|教师|老师|学生|设计师|律师|会计|销售)"),
                Regex("我在(.{2,20}?)工作"),
                Regex("我做(.{2,10}?)的"),
                Regex("我的职业是(.{2,15})")
            )
            for (pattern in occupationPatterns) {
                val match = pattern.find(message)
                if (match != null) {
                    basicInfo = basicInfo.copy(occupation = match.groupValues[1].trim())
                    break
                }
            }
        }

        // 4. 提取地点（完全照搬原版2种模式）
        if (basicInfo.location == null) {
            val locationPatterns = listOf(
                Regex("我在(.{2,10}?)(住|生活|工作)"),
                Regex("我家在(.{2,10})")
            )
            for (pattern in locationPatterns) {
                val match = pattern.find(message)
                if (match != null) {
                    basicInfo = basicInfo.copy(location = match.groupValues[1].trim())
                    break
                }
            }
        }

        // 5. 提取兴趣爱好（完全照搬原版）
        val interestPatterns = listOf(
            Regex("我喜欢(.{2,20})"),
            Regex("我爱(.{2,20})"),
            Regex("我的爱好是(.{2,20})"),
            Regex("我对(.{2,20}?)很?感兴趣"),
            Regex("我喜欢(玩|看|听|打|踢|跑|游泳|爬山|旅游|摄影|画画|唱歌|跳舞)")
        )
        for (pattern in interestPatterns) {
            val match = pattern.find(message)
            if (match != null) {
                val interest = match.groupValues[1].trim()
                if (interest.isNotEmpty() && !interests.contains(interest)) {
                    interests.add(interest)
                }
            }
        }

        // 6. 提取人际关系（完全照搬原版）
        val relationshipPatterns = listOf(
            Regex("我的?(男朋友|女朋友|老公|老婆|对象)"),
            Regex("我的?(爸爸|妈妈|父亲|母亲)"),
            Regex("我的?(儿子|女儿|孩子)"),
            Regex("我的?(朋友|同事|室友|同学)")
        )
        for (pattern in relationshipPatterns) {
            val match = pattern.find(message)
            if (match != null) {
                val relation = match.groupValues[1].trim()
                if (!relationships.contains(relation)) {
                    relationships.add(relation)
                }
            }
        }

        // 7. 提取重要日期（完全照搬原版）
        val datePatterns = listOf(
            Regex("我的?生日(是)?(\\d{1,2})月(\\d{1,2})"),
            Regex("我(\\d{1,2})月(\\d{1,2})生日"),
            Regex("(\\d{1,2})月(\\d{1,2})(日|号)是我的生日")
        )
        for (pattern in datePatterns) {
            val match = pattern.find(message)
            if (match != null) {
                val event = "生日: ${match.groupValues[2]}月${match.groupValues[3]}日"
                if (!importantEvents.contains(event)) {
                    importantEvents.add(event)
                }
            }
        }

        return currentProfile.copy(
            basicInfo = basicInfo,
            interests = interests,
            relationships = relationships,
            importantEvents = importantEvents,
            habits = habits
        )
    }

    private fun mergeBasicInfo(old: BasicInfo, new: BasicInfo): BasicInfo {
        return old.copy(
            name = new.name ?: old.name,
            age = new.age ?: old.age,
            gender = new.gender ?: old.gender,
            occupation = new.occupation ?: old.occupation,
            location = new.location ?: old.location
        )
    }

    private fun mergePersonality(old: Personality, new: Personality): Personality {
        return old.copy(
            traits = (old.traits + new.traits).distinct(),
            values = (old.values + new.values).distinct(),
            communicationStyle = new.communicationStyle ?: old.communicationStyle
        )
    }

    private fun getDateString(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    // 扩展函数
    private fun MemoryEntity.toMemory(): Memory {
        return Memory(
            id = id,
            userId = userId,
            role = MessageRole.valueOf(role),
            content = content,
            timestamp = timestamp,
            date = date,
            importance = importance,
            emotion = emotion,
            topics = try {
                val jsonArray = JSONArray(topics)
                List(jsonArray.length()) { jsonArray.getString(it) }
            } catch (e: Exception) {
                emptyList()
            }
        )
    }

    private fun UserProfileEntity.toUserProfile(): UserProfile {
        return UserProfile(
            userId = userId,
            basicInfo = BasicInfo(
                name = name,
                age = age,
                gender = gender,
                occupation = occupation,
                location = location
            ),
            personality = try {
                val json = JSONObject(personality)
                Personality(
                    traits = json.optJSONArray("traits")?.toStringList() ?: emptyList(),
                    values = json.optJSONArray("values")?.toStringList() ?: emptyList(),
                    communicationStyle = json.optString("communicationStyle")
                )
            } catch (e: Exception) {
                Personality()
            },
            interests = interests.toStringList(),
            relationships = relationships.toStringList(),
            importantEvents = importantEvents.toStringList(),
            habits = habits.toStringList(),
            updatedAt = updatedAt
        )
    }

    private fun UserProfile.toEntity(): UserProfileEntity {
        return UserProfileEntity(
            userId = userId,
            name = basicInfo.name,
            age = basicInfo.age,
            gender = basicInfo.gender,
            occupation = basicInfo.occupation,
            location = basicInfo.location,
            personality = JSONObject().apply {
                put("traits", JSONArray(personality.traits))
                put("values", JSONArray(personality.values))
                put("communicationStyle", personality.communicationStyle ?: "")
            }.toString(),
            interests = JSONArray(interests).toString(),
            relationships = JSONArray(relationships).toString(),
            importantEvents = JSONArray(importantEvents).toString(),
            habits = JSONArray(habits).toString(),
            updatedAt = updatedAt
        )
    }

    private fun String.toStringList(): List<String> {
        return try {
            val jsonArray = JSONArray(this)
            List(jsonArray.length()) { jsonArray.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun JSONArray.toStringList(): List<String> {
        return List(length()) { getString(it) }
    }
}
