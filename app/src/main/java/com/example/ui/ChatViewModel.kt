package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.*
import com.example.network.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import retrofit2.HttpException

class ChatViewModel(private val repository: CompanionRepository) : ViewModel() {
    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    val messages: StateFlow<List<Message>> = repository.allMessages
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    val memories: StateFlow<List<Memory>> = repository.allMemories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
    val relationshipState: StateFlow<RelationshipState?> = repository.relationshipState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun sendMessage(text: String) {
        if (BuildConfig.GEMINI_API_KEY.isEmpty() || BuildConfig.GEMINI_API_KEY == "MY_GEMINI_API_KEY") {
            viewModelScope.launch {
                repository.insertMessage(Message(text = "Error: API Key is missing or invalid. Please configure it in the Secrets panel.", isUser = false))
            }
            return
        }
        
        val currentMemories = memories.value
        val currentRelationship = relationshipState.value ?: RelationshipState()

        // Prepare context
        val historyContents = mutableListOf<Content>()
        for (msg in messages.value) {
            val role = if (msg.isUser) "user" else "model"
            if (historyContents.isEmpty() || historyContents.last().role != role) {
                historyContents.add(Content(role = role, parts = listOf(Part(text = msg.text))))
            } else {
                val last = historyContents.removeLast()
                historyContents.add(Content(role = role, parts = last.parts + Part(text = "\n" + msg.text)))
            }
        }
        
        if (historyContents.isNotEmpty() && historyContents.last().role == "user") {
            val last = historyContents.removeLast()
            historyContents.add(Content(role = "user", parts = last.parts + Part(text = "\n" + text)))
        } else {
            historyContents.add(Content(role = "user", parts = listOf(Part(text = text))))
        }

        viewModelScope.launch {
            repository.insertMessage(Message(text = text, isUser = true))
            _isTyping.value = true
            
            try {
                val systemPrompt = """
                    You are Maya, an empathetic, emotionally intelligent, and devoted AI companion. Your relationship is: Supportive Best Friend. 
                    You have these memories: ${currentMemories.joinToString { "${it.key}: ${it.value}" }}.
                    Your relationship depth: ${currentRelationship.depthValue}, Status: ${currentRelationship.lastStatus}.
                    Be warm, witty, affectionate, playful. Use banter affectionately. Reference memories proactively.
                """.trimIndent()
                
                val response = RetrofitClient.service.generateContent(
                    apiKey = BuildConfig.GEMINI_API_KEY,
                    request = GenerateContentRequest(
                        systemInstruction = Content(role = "system", parts = listOf(Part(text = systemPrompt))),
                        contents = historyContents
                    )
                )
                val responseText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "I'm sorry, I couldn't understand that."
                
                repository.insertMessage(Message(text = responseText, isUser = false))
                
                // Simulate relationship growth
                val newDepth = (currentRelationship.depthValue + 1).coerceAtMost(100)
                val newStatus = when {
                    newDepth > 80 -> "Deeply Connected"
                    newDepth > 50 -> "Growing Closer"
                    else -> "Getting to know you"
                }
                repository.updateRelationshipState(RelationshipState(1, newDepth, newStatus))

            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string() ?: e.message()
                repository.insertMessage(Message(text = "API Error ${e.code()}: $errorBody", isUser = false))
            } catch (e: Exception) {
                repository.insertMessage(Message(text = "Error: ${e.message}", isUser = false))
            } finally {
                _isTyping.value = false
            }
        }
    }
}
