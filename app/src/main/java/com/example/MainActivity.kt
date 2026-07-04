package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.ChatScreen
import com.example.ui.ChatViewModel
import com.example.ui.ChatViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val appContainer = AppContainer(this)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val viewModel: ChatViewModel = viewModel(
          factory = ChatViewModelFactory(appContainer.companionRepository)
        )
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          ChatScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
        }
      }
    }
  }
}
