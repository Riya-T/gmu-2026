package com.example.studymate.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studymate.data.model.Message
import com.example.studymate.data.repository.ChatRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val matchId: String
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val currentUid = auth.currentUser?.uid ?: ""
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    init {
        observeMessages()
    }

    private fun observeMessages() {
        if (currentUid.isBlank()) return
        
        viewModelScope.launch {
            chatRepository.getMessages(matchId, currentUid).collect {
                _messages.value = it
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || currentUid.isBlank()) return
        
        val message = Message(
            id = UUID.randomUUID().toString(),
            senderId = currentUid,
            text = text,
            timestamp = Timestamp.now(),
            received = false
        )
        
        viewModelScope.launch {
            chatRepository.sendMessage(matchId, message)
        }
    }
}
