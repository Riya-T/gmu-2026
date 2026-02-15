package com.example.studymate.data.repository

import android.util.Log
import com.example.studymate.data.model.Match
import com.example.studymate.data.model.Swipe
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

class SwipeRepository {
    private val db = FirebaseFirestore.getInstance()
    private val swipesCollection = db.collection("swipes")
    private val matchesCollection = db.collection("matches")

    fun performSwipe(swipe: Swipe, onResult: (Boolean, String?, Boolean) -> Unit) {
        Log.d("SwipeRepository", "performSwipe: fromUser='${swipe.fromUser}', toUser='${swipe.toUser}', liked=${swipe.liked}")

        if (swipe.fromUser.isBlank() || swipe.toUser.isBlank()) {
            Log.e("SwipeRepository", "ERROR: fromUser or toUser is blank!")
            onResult(false, "Invalid user IDs", false)
            return
        }
        
        val swipeId = "${swipe.fromUser}_${swipe.toUser}"
        
        swipesCollection.document(swipeId).set(swipe)
            .addOnSuccessListener {
                Log.d("SwipeRepository", "Successfully saved swipe to Firestore")
                if (swipe.liked) {
                    createMatch(swipe.fromUser, swipe.toUser, onResult)
                } else {
                    onResult(true, null, false)
                }
            }
            .addOnFailureListener {
                Log.e("SwipeRepository", "Failed to save swipe: ${it.message}")
                onResult(false, it.message, false)
            }
    }

    private fun createMatch(uid1: String, uid2: String, onResult: (Boolean, String?, Boolean) -> Unit) {
        Log.d("SwipeRepository", "createMatch: uid1='$uid1', uid2='$uid2'")
        if (uid1.isBlank() || uid2.isBlank()) {
            Log.e("SwipeRepository", "ERROR: Cannot create match with blank UID")
            onResult(false, "Cannot create match with blank UID", true)
            return
        }
        
        val uids = listOf(uid1, uid2).sorted()
        val matchId = "${uids[0]}_${uids[1]}"
        Log.d("SwipeRepository", "Generating matchId: $matchId")
        
        val match = Match(
            users = uids,
            createdAt = Timestamp.now(),
            lastMessage = "You matched! Say hello.",
            lastMessageTime = Timestamp.now()
        )
        
        matchesCollection.document(matchId).set(match)
            .addOnSuccessListener {
                Log.d("SwipeRepository", "Successfully created match document: $matchId")
                onResult(true, null, true)
            }
            .addOnFailureListener {
                Log.e("SwipeRepository", "Failed to create match document: ${it.message}")
                onResult(false, it.message, true)
            }
    }
}
