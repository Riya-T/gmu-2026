package com.example.studymate.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class AuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // --------------------
    // EMAIL/PASSWORD LOGIN
    // --------------------
    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                onResult(it.isSuccessful, it.exception?.message)
            }
    }

    // --------------------
    // EMAIL/PASSWORD REGISTER
    // --------------------
    fun register(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        if (!email.trim().lowercase().endsWith(".edu")) {
            onResult(false, "Only .edu emails are allowed")
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                onResult(it.isSuccessful, it.exception?.message)
            }
    }

    // --------------------
    // GOOGLE SIGN IN
    // --------------------
    fun signInWithGoogle(idToken: String, onResult: (Boolean, String?) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener {
                onResult(it.isSuccessful, it.exception?.message)
            }
    }

    // --------------------
    // CHECK IF PROFILE COMPLETE
    // --------------------
    fun checkIfProfileComplete(onResult: (Boolean) -> Unit) {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val completed = doc.getBoolean("profileComplete") ?: false
                onResult(completed)
            }
            .addOnFailureListener {
                onResult(false)
            }
    }

    // --------------------
    // SAVE PROFILE AFTER ONBOARDING
    // --------------------
    fun saveUserProfile(
        name: String,
        major: String,
        year: String,
        bio: String,
        onComplete: () -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: return

        val data = mapOf(
            "name" to name,
            "major" to major,
            "year" to year,
            "bio" to bio,
            "profileComplete" to true
        )

        db.collection("users")
            .document(uid)
            .set(data)
            .addOnSuccessListener { onComplete() }
    }

    // --------------------
    // SESSION HELPERS
    // --------------------
    fun isLoggedIn(): Boolean = auth.currentUser != null

    fun logout() {
        auth.signOut()
    }
}
