package com.example.studymate

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.example.studymate.Calendar.data.repository.CalendarRepository
import com.example.studymate.Calendar.domain.CalendarManager
import com.example.studymate.Calendar.ui.CalendarScreen
import com.example.studymate.Calendar.ui.CalendarViewModel
import com.example.studymate.data.auth.AuthRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.launch

// ----------------------------------------------------

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = CalendarRepository()
        val manager = CalendarManager(repository)

        setContent {
            MaterialTheme {
                val rootNavController = rememberNavController()

                NavHost(
                    navController = rootNavController,
                    startDestination = "login"
                ) {

                    // ---------- LOGIN ----------
                    composable("login") {
                        LoginScreen(
                            onExistingUser = {
                                rootNavController.navigate("main") {
                                    popUpTo("login") { inclusive = true }
                                }
                            },
                            onNewUser = {
                                rootNavController.navigate("onboarding")
                            }
                        )
                    }

                    // ---------- ONBOARDING ----------
                    composable("onboarding") {
                        OnboardingScreen(
                            onContinueClick = {
                                rootNavController.navigate("main") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        )
                    }

                    // ---------- MAIN ----------
                    composable("main") {
                        MainAppScreen(manager)
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// DATA
// ----------------------------------------------------

data class UserProfile(val name: String, val major: String, val year: String)

val sampleUsers = mutableStateListOf(
    UserProfile("Aarav", "CS", "Junior"),
    UserProfile("Maya", "Biology", "Sophomore"),
    UserProfile("Ethan", "Math", "Senior"),
    UserProfile("Zara", "Engineering", "Freshman")
)

// ----------------------------------------------------
// LOGIN SCREEN
// ----------------------------------------------------

@Composable
fun LoginScreen(
    onExistingUser: () -> Unit,
    onNewUser: () -> Unit
) {
    val context = LocalContext.current
    val authRepository = remember { AuthRepository() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf<String?>(null) }

    // ---------- GOOGLE RESULT ----------
    val googleLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            val task: Task<com.google.android.gms.auth.api.signin.GoogleSignInAccount> =
                GoogleSignIn.getSignedInAccountFromIntent(result.data)

            try {
                val account = task.getResult(ApiException::class.java)
                val email = account?.email
                val idToken = account?.idToken

                if (email == null || !email.endsWith(".edu")) {
                    loginError = "Only .edu emails allowed"
                    return@rememberLauncherForActivityResult
                }

                if (idToken != null) {
                    authRepository.signInWithGoogle(idToken) { success, error ->
                        if (success) {
                            authRepository.checkIfProfileComplete { completed ->
                                if (completed) onExistingUser()
                                else onNewUser()
                            }
                        } else loginError = error
                    }
                }
            } catch (e: Exception) {
                loginError = e.message
            }
        }

    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(context.getString(R.string.default_web_client_id))
        .requestEmail()
        .build()

    val googleClient = GoogleSignIn.getClient(context, gso)

    // ---------- UI ----------
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFFDE2E4), Color(0xFFFFF1E6))))
    ) {

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text("StudyBuddy", fontSize = 32.sp, color = Color(0xFFB23A48))
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(Modifier.padding(24.dp)) {

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Student Email") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(24.dp))

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            authRepository.login(email, password) { success, error ->
                                if (success) {
                                    authRepository.checkIfProfileComplete { completed ->
                                        if (completed) onExistingUser()
                                        else onNewUser()
                                    }
                                } else loginError = error
                            }
                        }
                    ) {
                        Text("Sign In")
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            googleLauncher.launch(googleClient.signInIntent)
                        }
                    ) {
                        Text("Sign In With Google")
                    }

                    loginError?.let {
                        Spacer(Modifier.height(12.dp))
                        Text(it, color = Color.Red)
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// ONBOARDING
// ----------------------------------------------------

@Composable
fun OnboardingScreen(onContinueClick: () -> Unit) {

    val authRepository = remember { AuthRepository() }

    var name by remember { mutableStateOf("") }
    var major by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Text("Create Your Profile", fontSize = 28.sp)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
        OutlinedTextField(value = major, onValueChange = { major = it }, label = { Text("Major") })
        OutlinedTextField(
            value = year,
            onValueChange = { year = it },
            label = { Text("Year (Freshman, Sophomore, Junior, Senior)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("Bio") })

        Spacer(Modifier.height(24.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                authRepository.saveUserProfile(name, major,year, bio) {
                    onContinueClick()
                }
            }
        ) {
            Text("Continue")
        }
    }
}


// -------------------- SWIPE --------------------
@Composable
fun SwipeScreen() {
    val users = sampleUsers
    val offsetX = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFFFDE2E4)),
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
                                    if (offsetX.value > 300 || offsetX.value < -300) users.removeAt(0)
                                    offsetX.animateTo(0f, tween(300))
                                }
                            }
                        ) { _, dragAmount ->
                            coroutineScope.launch { offsetX.snapTo(offsetX.value + dragAmount.x) }
                        }
                    }
            ) {
                ProfileCard(user)
            }
        }

        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 60.dp),
            horizontalArrangement = Arrangement.spacedBy(40.dp)
        ) {
            IconButton(onClick = { if (users.isNotEmpty()) users.removeAt(0) }, modifier = Modifier.size(70.dp)) {
                Text("❌", fontSize = 32.sp)
            }
            IconButton(onClick = { if (users.isNotEmpty()) users.removeAt(0) }, modifier = Modifier.size(70.dp)) {
                Text("❤️", fontSize = 32.sp)
            }
        }
    }
}

@Composable
fun ProfileCard(user: UserProfile) {
    Card(
        modifier = Modifier.fillMaxWidth(0.9f).height(500.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(user.name, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Major: ${user.major}")
            Text("Year: ${user.year}")
        }
    }
}

// -------------------- MAIN APP SCREEN --------------------
@Composable
fun MainAppScreen(calendarManager: CalendarManager) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White
            ) {
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
                NavigationBarItem(
                    selected = currentRoute == "calendar_tab",
                    onClick = { navController.navigate("calendar_tab") },
                    icon = { Text("📅", fontSize = 20.sp) },
                    label = { Text("Calendar") }
                )
                NavigationBarItem(
                    selected = currentRoute == "chatbot_tab",
                    onClick = { navController.navigate("chatbot_tab") },
                    icon = { Text("🤖", fontSize = 20.sp) },
                    label = { Text("AI Buddy") }
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
            composable("swipe_tab") { SwipeScreen() }
            composable("matches_tab") { MatchesScreen(navController) }
            composable("profile_tab") { ProfileScreen() }
            composable("chatbot_tab") { ChatbotScreen() }
            composable("calendar_tab") {
                val viewModel: CalendarViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return CalendarViewModel(calendarManager) as T
                        }
                    }
                )
                CalendarScreen(viewModel = viewModel, onBack = { })
            }
            composable("profile_tab") { ProfileScreen() }
            composable("chat/{userName}") { backStackEntry ->
                val userName = backStackEntry.arguments?.getString("userName") ?: ""
                ChatScreen(userName)
            }
        }
    }
}

// -------------------- MATCHES --------------------
@Composable
fun MatchesScreen(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text("Your Matches", fontSize = 24.sp)
        Spacer(modifier = Modifier.height(16.dp))
        sampleUsers.forEach { user ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), onClick = {
                navController.navigate("chat/${user.name}")
            }) {
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

// -------------------- CHAT --------------------
@Composable
fun ChatScreen(userName: String) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Chat with $userName", fontSize = 24.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text("This is where messages will appear.")
    }
}

@Composable
fun ProfileScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("User Profile Page")
    }
}

// --- NEW BOT-LIKE CHAT UI ---

data class ChatMessage(val text: String, val isUser: Boolean)

@Composable
fun ChatbotScreen() {
    var input by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf(ChatMessage("Hi! I'm your StudyBuddy AI. Ask me anything about your classes or major!", false)) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9F9F9))
            .padding(16.dp)
    ) {
        Text(
            text = "AI Study Assistant",
            fontSize = 20.sp,
            color = Color(0xFFB23A48),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                ChatBubble(msg)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...") },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFB23A48),
                    unfocusedBorderColor = Color.LightGray
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    if (input.isNotBlank()) {
                        val userText = input
                        messages.add(ChatMessage(userText, true))
                        input = ""
                        
                        scope.launch {
                            listState.animateScrollToItem(messages.size - 1)
                            
                            try {
                                val response = RetrofitClient.api.sendMessage(
                                    ChatRequest(message = userText, context = "General Study Help")
                                )
                                if (response.isSuccessful) {
                                    val reply = response.body()?.reply ?: "I'm thinking..."
                                    messages.add(ChatMessage(reply, false))
                                } else {
                                    messages.add(ChatMessage("Sorry, my brain is a bit foggy (Error ${response.code()})", false))
                                }
                            } catch (e: Exception) {
                                messages.add(ChatMessage("Can't connect to server. Make sure your Node.js backend is running!", false))
                            }
                            listState.animateScrollToItem(messages.size - 1)
                        }
                    }
                },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB23A48)),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(50.dp)
            ) {
                Text("🚀")
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val bgColor = if (message.isUser) Color(0xFFB23A48) else Color(0xFFE9E9EB)
    val textColor = if (message.isUser) Color.White else Color.Black
    val shape = if (message.isUser) {
        RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp)
    }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Surface(
            color = bgColor,
            shape = shape,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = textColor,
                fontSize = 15.sp
            )
        }
    }
    @Composable
    fun ProfileScreen() {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("User Profile Page")
        }
    }
}
