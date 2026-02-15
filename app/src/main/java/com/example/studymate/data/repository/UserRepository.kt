package com.example.studymate.data.repository

import com.example.studymate.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    fun saveUser(user: User, onResult: (Boolean, String?) -> Unit) {
        if (user.uid.isEmpty()) {
            onResult(false, "User UID is empty")
            return
        }
        usersCollection.document(user.uid)
            .set(user, SetOptions.merge())
            .addOnCompleteListener {
                onResult(it.isSuccessful, it.exception?.message)
            }
    }

    suspend fun getUserSuspend(uid: String): User? {
        return try {
            val document = usersCollection.document(uid).get().await()
            document.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun getUser(uid: String, onResult: (User?, String?) -> Unit) {
        usersCollection.document(uid).get()
            .addOnSuccessListener { document ->
                val user = document.toObject(User::class.java)
                onResult(user, null)
            }
            .addOnFailureListener {
                onResult(null, it.message)
            }
    }

    suspend fun getAllUsers(): List<User> {
        return try {
            val snapshot = usersCollection.get().await()
            snapshot.toObjects(User::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
