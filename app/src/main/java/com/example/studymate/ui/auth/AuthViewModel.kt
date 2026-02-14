package com.example.studymate.ui.auth

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.example.studymate.data.auth.AuthRepository

class AuthViewModel(
    private val repo: AuthRepository = AuthRepository()
) : ViewModel() {

    var loading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        loading = true
        errorMessage = null

        repo.login(email, password) { success, error ->
            loading = false
            if (success) {
                onSuccess()
            } else {
                errorMessage = error ?: "Login failed"
            }
        }
    }

    fun register(email: String, password: String, onSuccess: () -> Unit) {
        loading = true
        errorMessage = null

        repo.register(email, password) { success, error ->
            loading = false
            if (success) {
                onSuccess()
            } else {
                errorMessage = error ?: "Registration failed"
            }
        }
    }
}

