package com.example.studymate.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.*
import com.example.studymate.data.auth.AuthRepository
import com.example.studymate.ui.auth.LoginScreen
import com.example.studymate.ui.home.HomeScreen

import androidx.compose.material3.Text
@Composable
fun AppNavGraph() {

    val repo = AuthRepository()
    val startDestination =
        if (repo.isLoggedIn()) "home" else "login"

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("login") {
            LoginScreen(navController)
        }

        composable("home") {
            HomeScreen(navController)
        }
    }
}
