package com.example.studymate.data.repository

import android.util.Log
import com.example.studymate.data.model.Message
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository {
    private val db = FirebaseFirestore.getInstance()

    private fun getChatCollection(matchId: String) =
        db.collection("matches").document(matchId).collection("messages")

    /**
     * Provides a real-time stream of messages. 
     * Using a snapshot listener is better than manual polling as it receives 
     * updates instantly when the other user sends a message.
     */
    fun getMessages(matchId: String, currentUid: String): Flow<List<Message>> = callbackFlow {
        val listenerRegistration = getChatCollection(matchId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatRepository", "Error fetching messages", error)
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Message::class.java)?.also { msg ->
                            // If we receive a message from the other user that isn't marked received
                            if (msg.senderId != currentUid && !msg.received) {
                                markAsReceived(matchId, doc.id)
                            }
                        }
                    }
                    trySend(messages)
                }
            }
        awaitClose { listenerRegistration.remove() }
    }

    private fun markAsReceived(matchId: String, messageId: String) {
        getChatCollection(matchId).document(messageId)
            .update("received", true)
            .addOnFailureListener { Log.e("ChatRepository", "Failed to update received flag", it) }
    }

    suspend fun sendMessage(matchId: String, message: Message) {
        try {
            getChatCollection(matchId).document(message.id).set(message).await()
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error sending message", e)
        }
    }
}
