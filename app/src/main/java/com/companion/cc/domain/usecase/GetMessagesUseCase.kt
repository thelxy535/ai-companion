package com.companion.cc.domain.usecase

import com.companion.cc.domain.model.Message
import com.companion.cc.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMessagesUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    operator fun invoke(userId: String, companionId: String): Flow<List<Message>> {
        // V9PM 修复：聊天页必须加载最新的 N 条——旧实现取最旧的 N 条，
        // 消息数超过 50 后新消息全部落在窗口外，导致发消息看不见
        return messageRepository.getLatestMessages(userId, companionId)
    }
}
