package com.example.studymate.ui.matches

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studymate.data.model.User
import com.example.studymate.data.repository.MatchRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MatchesViewModel(
    private val matchRepository: MatchRepository,
    private val context: Context
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val _matches = MutableStateFlow<List<Pair<String, User>>>(emptyList())
    val matches: StateFlow<List<Pair<String, User>>> = _matches.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadMatches()
    }

    fun loadMatches() {
        val currentUid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val result = matchRepository.getMatches(context, currentUid)
            _matches.value = result
            _isLoading.value = false
        }
    }
}
