package com.example.studymate.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class AuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // Existing email/password login
    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                onResult(it.isSuccessful, it.exception?.message)
            }
    }

    // Existing email/password registration
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

    fun isLoggedIn(): Boolean = auth.currentUser != null

    fun logout() {
        auth.signOut()
    }

    // New method: Sign in with Google ID token
    fun signInWithGoogle(idToken: String, onResult: (Boolean, String?) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener {
                onResult(it.isSuccessful, it.exception?.message)
            }
    }
}
