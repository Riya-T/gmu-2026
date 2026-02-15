package com.example.studymate

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.text.font.FontWeight
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
import com.example.studymate.data.model.Swipe
import com.example.studymate.data.model.User
import com.example.studymate.data.repository.ChatRepository
import com.example.studymate.data.repository.MatchRepository
import com.example.studymate.data.repository.SwipeRepository
import com.example.studymate.data.repository.UserRepository
import com.example.studymate.data.util.SampleData
import com.example.studymate.ui.chat.ChatViewModel
import com.example.studymate.ui.matches.MatchesViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

// ----------------------------------------------------

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = CalendarRepository()
        val manager = CalendarManager(repository)
        val chatRepository = ChatRepository()
        val matchRepository = MatchRepository()
        val userRepository = UserRepository()

        setContent {
            MaterialTheme {
                val rootNavController = rememberNavController()

                NavHost(
                    navController = rootNavController,
                    startDestination = "login"
                ) {

                    // ---------- LOGIN ----------
                    composable("login") {
                        val authRepository = remember { AuthRepository() }
                        LoginScreen(
                            onExistingUser = {
                                // Check if profile is complete before navigating
                                authRepository.checkIfProfileComplete { completed ->
                                    if (completed) {
                                        rootNavController.navigate("main") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    } else {
                                        rootNavController.navigate("onboarding") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    }
                                }
                            },
                            onNewUser = {
                                rootNavController.navigate("onboarding") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        )
                    }

                    // ---------- ONBOARDING ----------
                    composable("onboarding") {
                        OnboardingScreen(
                            onContinueClick = {
                                // After completing onboarding, go to main
                                rootNavController.navigate("main") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        )
                    }

                    // ---------- MAIN ----------
                    composable("main") {
                        MainAppScreen(manager, chatRepository, matchRepository, userRepository)
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
    val context = LocalContext.current
    val userRepository = remember { UserRepository() }
    val auth = FirebaseAuth.getInstance()
    
    var name by remember { mutableStateOf("") }
    var major by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
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
            value = year,
            onValueChange = { year = it },
            label = { Text("Year (e.g. Junior)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("Bio") })

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                val uid = auth.currentUser?.uid ?: ""
                val email = auth.currentUser?.email ?: ""
                val newUser = User(
                    uid = uid,
                    name = name,
                    email = email,
                    major = major,
                    year = year,
                    bio = bio,
                    createdAt = Timestamp.now(),
                    profileComplete = true // NEW
                )
                
                userRepository.saveUser(newUser) { success, error ->
                    if (success) onContinueClick()
                    else Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Continue") }
    }
}


// -------------------- SWIPE --------------------
@Composable
fun SwipeScreen(userRepository: UserRepository) {
    val context = LocalContext.current
    val swipeRepository = remember { SwipeRepository() }
    val auth = FirebaseAuth.getInstance()
    val currentUid = auth.currentUser?.uid ?: ""
    
    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    val offsetX = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val remoteUsers = userRepository.getAllUsers().filter { it.uid != currentUid }
        users = if (remoteUsers.isEmpty()) SampleData.sampleUsers else remoteUsers
        isLoading = false
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFFFDE2E4)),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator()
        } else if (users.isNotEmpty()) {
            val user = users.first()
            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.value.toInt(), 0) }
                    .rotate(offsetX.value / 40)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {
                                coroutineScope.launch {
                                    val liked = offsetX.value > 300
                                    val disliked = offsetX.value < -300
                                    
                                    if (liked || disliked) {
                                        if (liked) {
                                            val swipe = Swipe(
                                                fromUser = currentUid,
                                                toUser = user.uid,
                                                liked = true,
                                                timestamp = Timestamp.now()
                                            )
                                            swipeRepository.performSwipe(swipe) { success, error, isMatch ->
                                                if (isMatch) {
                                                    Toast.makeText(context, "Added to Matches!", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                        users = users.drop(1)
                                    }
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
        } else {
            Text("No more students to swipe on!", color = Color.Gray)
        }

        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 60.dp),
            horizontalArrangement = Arrangement.spacedBy(40.dp)
        ) {
            IconButton(onClick = { if (users.isNotEmpty()) users = users.drop(1) }, modifier = Modifier.size(70.dp)) {
                Text("❌", fontSize = 32.sp)
            }
            IconButton(onClick = { 
                if (users.isNotEmpty()) {
                    val targetUser = users.first()
                    val swipe = Swipe(currentUid, targetUser.uid, true, Timestamp.now())
                    swipeRepository.performSwipe(swipe) { _, _, isMatch ->
                        if (isMatch) Toast.makeText(context, "Added to Matches!", Toast.LENGTH_SHORT).show()
                    }
                    users = users.drop(1)
                }
            }, modifier = Modifier.size(70.dp)) {
                Text("❤️", fontSize = 32.sp)
            }
        }
    }
}

@Composable
fun ProfileCard(user: User) {
    Card(
        modifier = Modifier.fillMaxWidth(0.9f).height(500.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(user.name, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Major: ${user.major}", fontSize = 18.sp)
            Text("Year: ${user.year}", fontSize = 18.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(user.bio, fontSize = 16.sp, color = Color.Gray)
        }
    }
}

// -------------------- MAIN APP SCREEN --------------------
@Composable
fun MainAppScreen(calendarManager: CalendarManager, chatRepository: ChatRepository, matchRepository: MatchRepository, userRepository: UserRepository) {
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
            composable("swipe_tab") { SwipeScreen(userRepository) }
            composable("matches_tab") { 
                val matchesViewModel: MatchesViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return MatchesViewModel(matchRepository) as T
                        }
                    }
                )
                MatchesScreen(navController, matchesViewModel) 
            }
            composable("profile_tab") { ProfileScreen(userRepository = UserRepository()) }
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
            composable("profile_tab") { ProfileScreen(userRepository) }
            composable("chat/{matchId}/{otherUserName}") { backStackEntry ->
                val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
                val otherUserName = backStackEntry.arguments?.getString("otherUserName") ?: ""
                
                val chatViewModel: ChatViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return ChatViewModel(chatRepository, matchId) as T
                        }
                    }
                )
                ChatScreen(otherUserName, chatViewModel)
            }
        }
    }
}

// -------------------- MATCHES --------------------
@Composable
fun MatchesScreen(navController: NavController, viewModel: MatchesViewModel) {
    val matches by viewModel.matches.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadMatches()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text("Your Matches", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (matches.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No matches yet. Keep swiping!", color = Color.Gray)
            }
        } else {
            LazyColumn {
                items(matches) { (matchId, user) ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        onClick = {
                            navController.navigate("chat/$matchId/${user.name}")
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(user.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(user.major, style = MaterialTheme.typography.bodySmall)
                            }
                            Text("💬", fontSize = 24.sp)
                        }
                    }
                }
            }
        }
    }
}

// -------------------- CHAT --------------------
@Composable
fun ChatScreen(userName: String, viewModel: ChatViewModel) {
    val messages by viewModel.messages.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Surface(shadowElevation = 4.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Chat with $userName", style = MaterialTheme.typography.titleLarge)
            }
        }

        // Message List
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
            reverseLayout = false
        ) {
            items(messages) { message ->
                val isMe = message.senderId == currentUid
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Surface(
                        color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = message.text,
                            modifier = Modifier.padding(12.dp),
                            color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        // Input Field
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...") },
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (messageText.isNotBlank()) {
                        viewModel.sendMessage(messageText)
                        messageText = ""
                    }
                },
                enabled = messageText.isNotBlank()
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun ProfileScreen(userRepository: UserRepository) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val currentUid = auth.currentUser?.uid ?: ""
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var major by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var originalUser by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(currentUid) {
        if (currentUid.isNotEmpty()) {
            val user = userRepository.getUserSuspend(currentUid)
            if (user != null) {
                originalUser = user
                name = user.name
                major = user.major
                year = user.year
                bio = user.bio
            }
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Your Profile", fontSize = 28.sp, fontWeight = FontWeight.Bold)
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
                value = year,
                onValueChange = { year = it },
                label = { Text("Year") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Bio") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    val updatedUser = originalUser?.copy(
                        name = name,
                        major = major,
                        year = year,
                        bio = bio
                    ) ?: User(
                        uid = currentUid,
                        name = name,
                        major = major,
                        year = year,
                        bio = bio,
                        email = auth.currentUser?.email ?: ""
                    )
                    
                    userRepository.saveUser(updatedUser) { success, error ->
                        if (success) {
                            Toast.makeText(context, "Profile updated!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Update failed: $error", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Save Changes")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(onClick = {
                auth.signOut()
                // You might need a way to navigate back to login here
            }) {
                Text("Sign Out", color = Color.Red)
            }
        }
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
