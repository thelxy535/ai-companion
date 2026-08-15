package com.companion.cc.domain.usecase

import com.companion.cc.domain.model.Message
import com.companion.cc.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMessagesUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    operator fun invoke(userId: String, companionId: String): Flow<List<Message>> {
        return messageRepository.getMessages(userId, companionId)
    }
}
