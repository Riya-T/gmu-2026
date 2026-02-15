package com.example.studymate.data.repository

import android.util.Log
import com.example.studymate.data.model.Match
import com.example.studymate.data.model.User
import com.example.studymate.data.util.SampleData
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

class MatchRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getMatches(currentUid: String): List<Pair<String, User>> {
        Log.d("MatchRepository", "getMatches called for UID: '$currentUid'")
        if (currentUid.isBlank()) {
            Log.e("MatchRepository", "getMatches: currentUid is BLANK!")
            return emptyList()
        }

        var retries = 0
        val maxRetries = 3
        val delayMillis = 1000L

        while (retries < maxRetries) {
            try {
                val snapshot = db.collection("matches")
                    .whereArrayContains("users", currentUid)
                    .get()
                    .await()
                
                Log.d("MatchRepository", "Query for UID '$currentUid' found ${snapshot.size()} match documents")

                if (!snapshot.isEmpty) {
                    val result = mutableListOf<Pair<String, User>>()
                    
                    for (doc in snapshot.documents) {
                        val match = doc.toObject(Match::class.java)
                        if (match != null) {
                            val otherUid = match.users.find { it != currentUid }
                            Log.d("MatchRepository", "Processing match ${doc.id}, users=${match.users}, otherUid='$otherUid'")
                            
                            if (!otherUid.isNullOrBlank()) {
                                var user = SampleData.sampleUsers.find { it.uid == otherUid }
                                if (user == null) {
                                    val userDoc = db.collection("users").document(otherUid).get().await()
                                    user = userDoc.toObject(User::class.java)
                                }
                                
                                if (user != null) {
                                    result.add(doc.id to user)
                                } else {
                                    Log.w("MatchRepository", "Could not find user data for UID: $otherUid")
                                }
                            } else {
                                Log.w("MatchRepository", "Match ${doc.id} has invalid otherUid")
                            }
                        }
                    }
                    return result
                } else {
                    Log.d("MatchRepository", "No matches found in Firestore for $currentUid")
                }
            } catch (e: Exception) {
                Log.e("MatchRepository", "Error fetching matches: ${e.message}", e)
            }
            
            if (retries < maxRetries - 1) delay(delayMillis)
            retries++
        }
        
        return emptyList()
    }
}
