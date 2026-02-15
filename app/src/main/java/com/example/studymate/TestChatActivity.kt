package com.example.studymate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class TestChatActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TestChatScreen()
        }
    }
}

@Composable
fun TestChatScreen() {

    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("Response will appear here") }

    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.padding(16.dp)) {

        Text("Chatbot Test", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = input,
            onValueChange = { input = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Ask something") }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {

                scope.launch {

                    try {

                        val response = RetrofitClient.api.sendMessage(
                            ChatRequest(message = input)
                        )

                        output =
                            if (response.isSuccessful) {
                                response.body()?.reply ?: "Empty reply"
                            } else {
                                "Server error: ${response.code()}"
                            }

                    } catch (e: Exception) {

                        output = "Error: ${e.message}"

                    }

                }

            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Send")
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(output)

    }
}