package com.example.studymate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                        MainAppScreen()
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
                onClick = { if (users.isNotEmpty()) users.removeAt(0) },
                modifier = Modifier.size(70.dp)
            ) {
                Text("❌", fontSize = 32.sp)
            }

            IconButton(
                onClick = { if (users.isNotEmpty()) users.removeAt(0) },
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
fun MainAppScreen() {
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
            composable("chat/{userName}") { backStackEntry ->
                val userName = backStackEntry.arguments?.getString("userName") ?: ""
                ChatScreen(userName)
            }
        }
    }
}

@Composable
fun MatchesScreen(navController: NavController) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Your Matches", fontSize = 24.sp)
        Spacer(modifier = Modifier.height(16.dp))
        sampleUsers.forEach { user ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                onClick = { navController.navigate("chat/${user.name}") }
            ) {
                Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(user.name)
                    Text("💬")
                }
            }
        }
    }
}

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
}
