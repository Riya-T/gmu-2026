package com.example.studymate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.studymate.Calendar.data.repository.CalendarRepository
import com.example.studymate.Calendar.domain.CalendarManager
import com.example.studymate.Calendar.ui.CalendarScreen
import com.example.studymate.Calendar.ui.CalendarViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Calendar dependencies
        val repository = CalendarRepository()
        val manager = CalendarManager(repository)
        
        setContent {
            MaterialTheme {

                val rootNavController = rememberNavController()

                NavHost(
                    navController = rootNavController,
                    startDestination = "login"
                ) {

                    composable("login") {
                        LoginScreen(
                            onLoginClick = {
                                rootNavController.navigate("onboarding")
                            }
                        )
                    }

                    composable("onboarding") {
                        OnboardingScreen(
                            onContinueClick = {
                                rootNavController.navigate("main") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("main") {
                        MainAppScreen(manager)
                    }
                }
            }
        }
    }
}


data class UserProfile(
    val name: String,
    val major: String,
    val year: String
)

val sampleUsers = mutableStateListOf(
    UserProfile("Aarav", "CS", "Junior"),
    UserProfile("Maya", "Biology", "Sophomore"),
    UserProfile("Ethan", "Math", "Senior"),
    UserProfile("Zara", "Engineering", "Freshman")
)

@Composable
fun LoginScreen(onLoginClick: () -> Unit) {

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFDE2E4),
                        Color(0xFFFFF1E6)
                    )
                )
            )
    ) {

        // Decorative floating vibe layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.08f)
        ) {
            // You can later add heart/book SVG background here
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "StudyBuddy",
                fontSize = 32.sp,
                color = Color(0xFFB23A48)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Find your academic soulmate",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier = Modifier.padding(24.dp)
                ) {

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Student Email") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { onLoginClick() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Sign In")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { /* TODO Google Sign In */ },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Sign In with Google")
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingScreen(onContinueClick: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var major by remember { mutableStateOf("") }
    var classes by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Create Your Profile", fontSize = 28.sp)

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = major,
            onValueChange = { major = it },
            label = { Text("Major") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = classes,
            onValueChange = { classes = it },
            label = { Text("Classes (comma separated)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            label = { Text("Short Bio") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onContinueClick() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue")
        }
    }
}

@Composable
fun SwipeScreen() {
    val users = sampleUsers
    val offsetX = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFDE2E4)),
        contentAlignment = Alignment.Center
    ) {
        if (users.isNotEmpty()) {
            val user = users.first()
            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.value.toInt(), 0) }
                    .rotate(offsetX.value / 40)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {
                                coroutineScope.launch {
                                    if (offsetX.value > 300 || offsetX.value < -300) {
                                        users.removeAt(0)
                                    }
                                    offsetX.animateTo(0f, tween(300))
                                }
                            }
                        ) { _, dragAmount ->
                            coroutineScope.launch {
                                offsetX.snapTo(offsetX.value + dragAmount.x)
                            }
                        }
                    }
            ) {
                ProfileCard(user)
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp),
            horizontalArrangement = Arrangement.spacedBy(40.dp)
        ) {
            IconButton(
                onClick = {
                    if (users.isNotEmpty()) users.removeAt(0)
                },
                modifier = Modifier.size(70.dp)
            ) {
                Text("❌", fontSize = 32.sp)
            }

            IconButton(
                onClick = {
                    if (users.isNotEmpty()) users.removeAt(0)
                },
                modifier = Modifier.size(70.dp)
            ) {
                Text("❤️", fontSize = 32.sp)
            }
        }
    }
}

@Composable
fun ProfileCard(user: UserProfile) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .height(500.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(user.name, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Major: ${user.major}")
            Text("Year: ${user.year}")
        }
    }
}

@Composable
fun MainAppScreen(calendarManager: CalendarManager) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute == "swipe_tab",
                    onClick = { navController.navigate("swipe_tab") },
                    icon = { Text("🔥", fontSize = 20.sp) },
                    label = { Text("Swipe") }
                )

                NavigationBarItem(
                    selected = currentRoute == "matches_tab",
                    onClick = { navController.navigate("matches_tab") },
                    icon = { Text("💌", fontSize = 20.sp) },
                    label = { Text("Matches") }
                )

                // Calendar Icon added right after matches
                NavigationBarItem(
                    selected = currentRoute == "calendar_tab",
                    onClick = { navController.navigate("calendar_tab") },
                    icon = { Text("📅", fontSize = 20.sp) },
                    label = { Text("Calendar") }
                )

                NavigationBarItem(
                    selected = currentRoute == "profile_tab",
                    onClick = { navController.navigate("profile_tab") },
                    icon = { Text("👤", fontSize = 20.sp) },
                    label = { Text("Profile") }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "swipe_tab",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("swipe_tab") {
                SwipeScreen()
            }

            composable("matches_tab") {
                MatchesScreen(navController)
            }

            composable("calendar_tab") {
                val viewModel: CalendarViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return CalendarViewModel(calendarManager) as T
                        }
                    }
                )
                CalendarScreen(
                    viewModel = viewModel,
                    onBack = { /* Stay on tab */ }
                )
            }

            composable("profile_tab") {
                ProfileScreen()
            }

            composable("chat/{userName}") { backStackEntry ->
                val userName = backStackEntry.arguments?.getString("userName") ?: ""
                ChatScreen(userName)
            }
        }
    }
}

@Composable
fun MatchesScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Your Matches", fontSize = 24.sp)
        Spacer(modifier = Modifier.height(16.dp))
        sampleUsers.forEach { user ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                onClick = {
                    navController.navigate("chat/${user.name}")
                }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(user.name)
                    Text("💬")
                }
            }
        }
    }
}

@Composable
fun ChatScreen(userName: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Chat with $userName", fontSize = 24.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text("This is where messages will appear.")
    }
}

@Composable
fun ProfileScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("User Profile Page")
    }
}
