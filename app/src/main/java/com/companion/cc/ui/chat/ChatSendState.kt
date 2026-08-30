package com.companion.cc.ui.chat

sealed interface ChatSendState {
    data object Idle : ChatSendState
    data class Sending(val companionId: String) : ChatSendState
    data class Streaming(val companionId: String, val partialResponse: String) : ChatSendState
    data class Cancelled(val companionId: String, val partialResponse: String) : ChatSendState
    data class RetryableFailure(
        val companionId: String,
        val partialResponse: String,
        val message: String
    ) : ChatSendState
    data class TerminalFailure(val message: String) : ChatSendState
}
