package com.example.studymate

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ChatApi {

    @POST("/chatbot")
    suspend fun sendMessage(
        @Body request: ChatRequest
    ): Response<ChatResponse>

}
// need to change this into a kotlin file
