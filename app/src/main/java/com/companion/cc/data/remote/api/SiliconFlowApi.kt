package com.companion.cc.data.remote.api

import com.companion.cc.data.remote.model.ChatRequest
import com.companion.cc.data.remote.model.ChatResponse
import com.companion.cc.data.remote.model.ModelsResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST

interface SiliconFlowApi {

    @Headers("Content-Type: application/json")
    @POST("chat/completions")
    suspend fun chatCompletion(
        @Body request: ChatRequest
    ): ChatResponse

    @GET("models")
    suspend fun getModels(): ModelsResponse
}
