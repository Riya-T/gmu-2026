package com.example.studymate

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
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
import com.example.studymate.ui.theme.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
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
            StudymateTheme {
                val rootNavController = rememberNavController()

                NavHost(
                    navController = rootNavController,
                    startDestination = "login"
                ) {
                    composable("login") {
                        LoginScreen(
                            onLoginSuccess = { hasProfile ->
                                if (hasProfile) {
                                    rootNavController.navigate("main") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                } else {
                                    rootNavController.navigate("onboarding")
                                }
                            }
                        )
                    }
                    composable("onboarding") {
                        OnboardingScreen(
                            userRepository = userRepository,
                            onContinueClick = {
                                rootNavController.navigate("main") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("main") {
                        MainAppScreen(rootNavController, manager, chatRepository, matchRepository, userRepository)
                    }
                }
            }
        }
    }
}

// -------------------- LOGIN SCREEN (Valentine's Theme) --------------------
@Composable
fun LoginScreen(
    onLoginSuccess: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val authRepository = remember { AuthRepository() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf<String?>(null) }

    val googleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account?.idToken != null) {
                authRepository.signInWithGoogle(account.idToken!!) { success, error ->
                    if (success) {
                        authRepository.checkIfProfileComplete { completed ->
                            onLoginSuccess(completed)
                        }
                    } else loginError = error
                }
            }
        } catch (e: Exception) { loginError = e.message }
    }

    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(context.getString(R.string.default_web_client_id))
        .requestEmail()
        .build()
    val googleClient = GoogleSignIn.getClient(context, gso)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ValentineSoftPink, ValentinePink)))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("💖", fontSize = 64.sp)
            Text("StudyMate", style = MaterialTheme.typography.displayLarge, color = ValentineDeepRose)
            Text("Find Your Study Match", style = MaterialTheme.typography.titleLarge, color = ValentineDeepRose.copy(alpha = 0.7f))
            
            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(Modifier.padding(0.dp), horizontalAlignment = Alignment.CenterHorizontally) {

                    Button(
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        onClick = { googleLauncher.launch(googleClient.signInIntent) },
                        colors = ButtonDefaults.buttonColors(containerColor = ValentineDeepRose),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Sign In with Google", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    loginError?.let {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(it, color = ValentineDeepRose, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

// -------------------- ONBOARDING --------------------
@Composable
fun OnboardingScreen(userRepository: UserRepository, onContinueClick: () -> Unit) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    
    var name by remember { mutableStateOf("") }
    var major by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().background(ValentineSoftPink).padding(32.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Build Your Profile", style = MaterialTheme.typography.displayLarge, color = ValentineDeepRose)
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = major,
            onValueChange = { major = it },
            label = { Text("Major") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = year,
            onValueChange = { year = it },
            label = { Text("Year") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            label = { Text("Bio") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            minLines = 3
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = {
                if (name.isNotBlank()) {
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
                        profileComplete = true
                    )
                    userRepository.saveUser(newUser) { success, error ->
                        if (success) onContinueClick() else Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ValentineDeepRose),
            shape = RoundedCornerShape(16.dp)
        ) { Text("Get Started", fontWeight = FontWeight.Bold) }
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

    LaunchedEffect(currentUid) {
        val remoteUsers = userRepository.getAllUsers().filter { it.uid != currentUid && it.name.isNotBlank() }
        val sampleUsers = SampleData.getSampleUsers(context)
        val swipedIds = swipeRepository.getSwipedUserIds(currentUid)
        users = (remoteUsers + sampleUsers).distinctBy { it.uid }.filter { it.uid !in swipedIds }
        isLoading = false
    }

    Box(
        modifier = Modifier.fillMaxSize().background(ValentineSoftPink),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = ValentineDeepRose)
        } else if (users.isNotEmpty()) {
            val user = users.first()
            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.value.toInt(), 0) }
                    .rotate(offsetX.value / 40)
                    .pointerInput(user.uid) {
                        detectDragGestures(
                            onDragEnd = {
                                coroutineScope.launch {
                                    val liked = offsetX.value > 300
                                    val disliked = offsetX.value < -300
                                    if (liked || disliked) {
                                        val swipe = Swipe(fromUser = currentUid, toUser = user.uid, liked = liked, timestamp = Timestamp.now())
                                        swipeRepository.performSwipe(swipe) { _, _, isMatch ->
                                            if (isMatch) Toast.makeText(context, "It's a Match! 💖", Toast.LENGTH_SHORT).show()
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
            Text("No more students! 💕", color = ValentineDeepRose, style = MaterialTheme.typography.titleLarge)
        }

        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            Surface(
                onClick = {
                    if (users.isNotEmpty()) {
                        val target = users.first()
                        swipeRepository.performSwipe(Swipe(currentUid, target.uid, false, Timestamp.now())) { _, _, _ -> }
                        users = users.drop(1)
                    }
                },
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Box(Modifier.fillMaxSize().border(2.dp, Color.LightGray, CircleShape), contentAlignment = Alignment.Center) {
                    Text("❌", fontSize = 28.sp)
                }
            }
            Surface(
                onClick = {
                    if (users.isNotEmpty()) {
                        val target = users.first()
                        swipeRepository.performSwipe(Swipe(currentUid, target.uid, true, Timestamp.now())) { _, _, isMatch ->
                            if (isMatch) Toast.makeText(context, "Matched! 💖", Toast.LENGTH_SHORT).show()
                        }
                        users = users.drop(1)
                    }
                },
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Box(Modifier.fillMaxSize().border(2.dp, ValentineDeepRose, CircleShape), contentAlignment = Alignment.Center) {
                    Text("❤️", fontSize = 28.sp)
                }
            }
        }
    }
}

@Composable
fun ProfileCard(user: User) {
    Card(
        modifier = Modifier.fillMaxWidth(0.85f).height(540.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(24.dp)).background(ValentinePink.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                Text(user.name.take(1), fontSize = 80.sp, color = ValentineDeepRose)
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(user.name, style = MaterialTheme.typography.headlineMedium, color = ValentineDeepRose)
            Text("${user.major} • ${user.year}", style = MaterialTheme.typography.titleLarge, color = TextDark)
            Spacer(modifier = Modifier.height(12.dp))
            Text(user.bio, style = MaterialTheme.typography.bodyLarge, color = TextDark.copy(alpha = 0.8f))
        }
    }
}

// -------------------- MAIN APP SCREEN --------------------
@Composable
fun MainAppScreen(rootNavController: NavController, calendarManager: CalendarManager, chatRepository: ChatRepository, matchRepository: MatchRepository, userRepository: UserRepository) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                listOf(
                    "swipe_tab" to "🔥", 
                    "matches_tab" to "💌", 
                    "calendar_tab" to "📅", 
                    "chatbot_tab" to "🤖",
                    "profile_tab" to "👤"
                ).forEach { (route, icon) ->
                    NavigationBarItem(
                        selected = currentRoute == route,
                        onClick = { navController.navigate(route) },
                        icon = { Text(icon, fontSize = 24.sp) },
                        label = { Text(when(route){
                            "swipe_tab" -> "Swipe"
                            "matches_tab" -> "Matches"
                            "calendar_tab" -> "Calendar"
                            "chatbot_tab" -> "AI Buddy"
                            else -> "Profile"
                        }) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = ValentineDeepRose, indicatorColor = ValentinePink.copy(alpha = 0.3f))
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = "swipe_tab", Modifier.padding(padding)) {
            composable("swipe_tab") { SwipeScreen(userRepository) }
            composable("matches_tab") { 
                val vm: MatchesViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = MatchesViewModel(matchRepository, context.applicationContext) as T
                })
                MatchesScreen(navController, vm) 
            }
            composable("chatbot_tab") { ChatbotScreen() }
            composable("calendar_tab") {
                val vm: CalendarViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = CalendarViewModel(calendarManager) as T
                })
                CalendarScreen(vm, onBack = { })
            }
            composable("profile_tab") { 
                ProfileScreen(userRepository) { 
                    rootNavController.navigate("login") { 
                        popUpTo("main") { inclusive = true } 
                    } 
                } 
            }
            composable("chat/{matchId}/{otherUserName}") { backStackEntry ->
                val mid = backStackEntry.arguments?.getString("matchId") ?: ""
                val name = backStackEntry.arguments?.getString("otherUserName") ?: ""
                val vm: ChatViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = ChatViewModel(chatRepository, mid) as T
                })
                ChatScreen(name, vm)
            }
        }
    }
}

@Composable
fun MatchesScreen(navController: NavController, viewModel: MatchesViewModel) {
    val matches by viewModel.matches.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadMatches() }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Matches", style = MaterialTheme.typography.displayLarge, color = ValentineDeepRose)
        Spacer(Modifier.height(24.dp))
        if (isLoading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = ValentineDeepRose) }
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(matches) { (mid, user) ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { navController.navigate("chat/$mid/${user.name}") },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(56.dp).clip(CircleShape).background(ValentinePink), contentAlignment = Alignment.Center) {
                            Text(user.name.take(1), color = ValentineDeepRose, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(user.name, style = MaterialTheme.typography.titleLarge, color = ValentineDeepRose)
                            Text(user.major, style = MaterialTheme.typography.bodyLarge, color = TextDark)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatScreen(userName: String, viewModel: ChatViewModel) {
    val messages by viewModel.messages.collectAsState()
    var text by remember { mutableStateOf("") }
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    Column(Modifier.fillMaxSize().background(BackgroundLight)) {
        Surface(shadowElevation = 4.dp, color = Color.White) {
            Row(Modifier.fillMaxWidth().padding(20.dp)) {
                Text(userName, style = MaterialTheme.typography.titleLarge, color = ValentineDeepRose)
            }
        }
        LazyColumn(Modifier.weight(1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(messages) { msg ->
                val isMe = msg.senderId == uid
                Box(Modifier.fillMaxWidth(), contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart) {
                    Surface(
                        color = if (isMe) ValentineDeepRose else Color.White,
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 2.dp
                    ) {
                        Text(msg.text, Modifier.padding(12.dp), color = if (isMe) Color.White else Color.Black)
                    }
                }
            }
        }
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(24.dp), placeholder = { Text("Send some love...") })
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { if (text.isNotBlank()) { viewModel.sendMessage(text); text = "" } }) {
                Icon(Icons.AutoMirrored.Filled.Send, null, tint = ValentineDeepRose)
            }
        }
    }
}

@Composable
fun ProfileScreen(userRepository: UserRepository, onSignOut: () -> Unit) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid ?: ""

    var name by remember { mutableStateOf("") }
    var major by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var originalUser by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(Unit) {
        userRepository.getUserSuspend(uid)?.let {
            originalUser = it
            name = it.name; major = it.major; year = it.year; bio = it.bio
        }
        loading = false
    }

    if (loading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = ValentineDeepRose) }
    else Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Your Profile", style = MaterialTheme.typography.displayLarge, color = ValentineDeepRose)
        Spacer(Modifier.height(32.dp))
        
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = major, onValueChange = { major = it }, label = { Text("Major") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = year, onValueChange = { year = it }, label = { Text("Year") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("Bio") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), minLines = 3)
        
        Spacer(Modifier.height(32.dp))
        
        Button(
            onClick = {
                val updatedUser = originalUser?.copy(name = name, major = major, year = year, bio = bio) 
                    ?: User(uid = uid, name = name, major = major, year = year, bio = bio, email = auth.currentUser?.email ?: "", profileComplete = true)
                
                userRepository.saveUser(updatedUser) { success, error ->
                    if (success) {
                        Toast.makeText(context, "Profile Updated!", Toast.LENGTH_SHORT).show()
                        originalUser = updatedUser
                    } else {
                        Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ValentineDeepRose),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Save Changes", fontWeight = FontWeight.Bold)
        }
        
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = { auth.signOut(); onSignOut() }) { Text("Sign Out", color = Color.Gray) }
    }
}

// --- CHATBOT SCREEN ---

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
            .background(BackgroundLight)
            .padding(16.dp)
    ) {
        Text(
            text = "AI Study Assistant",
            style = MaterialTheme.typography.headlineMedium,
            color = ValentineDeepRose,
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
                    focusedBorderColor = ValentineDeepRose,
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
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = ValentineDeepRose),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(56.dp)
            ) {
                Text("🚀", fontSize = 24.sp)
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val bgColor = if (message.isUser) ValentineDeepRose else Color.White
    val textColor = if (message.isUser) Color.White else TextDark
    val shape = if (message.isUser) {
        RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp)
    }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Surface(
            color = bgColor,
            shape = shape,
            modifier = Modifier.widthIn(max = 280.dp),
            shadowElevation = 2.dp
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = textColor,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
