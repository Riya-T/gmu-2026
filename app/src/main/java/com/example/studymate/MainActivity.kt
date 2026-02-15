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
                    composable("login") {
                        LoginScreen(onLoginClick = { rootNavController.navigate("onboarding") })
                    }
                    composable("onboarding") {
                        OnboardingScreen(onContinueClick = {
                            rootNavController.navigate("main") {
                                popUpTo("login") { inclusive = true }
                            }
                        })
                    }
                    composable("main") {
                        MainAppScreen(manager, chatRepository, matchRepository, userRepository)
                    }
                }
            }
        }
    }
}

// -------------------- LOGIN SCREEN --------------------
@Composable
fun LoginScreen(onLoginClick: () -> Unit) {
    val context = LocalContext.current
    val authRepository = remember { AuthRepository() }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf<String?>(null) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task: Task<com.google.android.gms.auth.api.signin.GoogleSignInAccount> =
            GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val email = account?.email
            val idToken = account?.idToken

            if (email == null || !email.trim().lowercase().endsWith(".edu")) {
                loginError = "Only .edu emails are allowed"
                return@rememberLauncherForActivityResult
            }

            if (idToken != null) {
                authRepository.signInWithGoogle(idToken) { success, error ->
                    if (success) onLoginClick()
                    else loginError = error ?: "Google sign-in failed"
                }
            } else loginError = "Google sign-in failed"

        } catch (e: ApiException) {
            loginError = e.message ?: "Google sign-in failed"
        }
    }

    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(context.getString(R.string.default_web_client_id))
        .requestEmail()
        .build()
    val googleSignInClient = GoogleSignIn.getClient(context, gso)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFFDE2E4), Color(0xFFFFF1E6))))
    ) {
        Box(modifier = Modifier.fillMaxSize().alpha(0.08f)) {}
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("StudyBuddy", fontSize = 32.sp, color = Color(0xFFB23A48))
            Spacer(modifier = Modifier.height(8.dp))
            Text("Find your academic soulmate", fontSize = 14.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(32.dp))

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            if (!email.trim().lowercase().endsWith(".edu")) {
                                loginError = "Only .edu emails are allowed"
                                return@Button
                            }
                            authRepository.login(email, password) { success, error ->
                                if (success) onLoginClick()
                                else loginError = error ?: "Login failed"
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Sign In")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            val signInIntent: Intent = googleSignInClient.signInIntent
                            googleSignInLauncher.launch(signInIntent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Sign In with Google")
                    }
                    loginError?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(it, color = Color.Red)
                    }
                }
            }
        }
    }
}

// -------------------- ONBOARDING --------------------
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
        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            label = { Text("Short Bio") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))
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
                    createdAt = Timestamp.now()
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
