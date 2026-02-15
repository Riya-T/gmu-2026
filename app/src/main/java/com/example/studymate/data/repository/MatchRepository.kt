package com.example.studymate.data.repository

import android.content.Context
import android.util.Log
import com.example.studymate.data.model.Match
import com.example.studymate.data.model.User
import com.example.studymate.data.util.SampleData
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

class MatchRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getMatches(context: Context, currentUid: String): List<Pair<String, User>> {
        Log.d("MatchRepository", "getMatches called for UID: '$currentUid'")
        if (currentUid.isBlank()) {
            Log.e("MatchRepository", "getMatches: currentUid is BLANK!")
            return emptyList()
        }

        var retries = 0
        val maxRetries = 5
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
                    
                    // Pre-load sample users from JSON if not already cached
                    val sampleUsers = SampleData.getSampleUsers(context)

                    for (doc in snapshot.documents) {
                        val match = doc.toObject(Match::class.java)
                        if (match != null) {
                            val otherUid = match.users.find { it != currentUid }
                            Log.d("MatchRepository", "Processing match ${doc.id}, otherUid='$otherUid'")
                            
                            if (!otherUid.isNullOrBlank()) {
                                // 1. Try finding in sample users first (from JSON)
                                var user = sampleUsers.find { it.uid == otherUid }
                                
                                // 2. If not found in sample, try finding in real Firestore users
                                if (user == null) {
                                    try {
                                        val userDoc = db.collection("users").document(otherUid).get().await()
                                        if (userDoc.exists()) {
                                            user = userDoc.toObject(User::class.java)
                                        }
                                    } catch (e: Exception) {
                                        Log.w("MatchRepository", "Failed to fetch remote user $otherUid: ${e.message}")
                                    }
                                }
                                
                                if (user != null) {
                                    result.add(doc.id to user)
                                } else {
                                    Log.w("MatchRepository", "Could not find user data for UID: $otherUid")
                                }
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
