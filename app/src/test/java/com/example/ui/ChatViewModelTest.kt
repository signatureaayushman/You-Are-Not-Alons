package com.example.ui

import com.example.data.CompanionDao
import com.example.data.CompanionRepository
import com.example.data.Memory
import com.example.data.Message
import com.example.data.RelationshipState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

class FakeCompanionRepository : CompanionRepository(mockDao()) {
    val fakeMessages = MutableStateFlow<List<Message>>(emptyList())
    val fakeMemories = MutableStateFlow<List<Memory>>(emptyList())
    val fakeRelationship = MutableStateFlow<RelationshipState?>(null)

    override val allMessages: Flow<List<Message>> = fakeMessages
    override val allMemories: Flow<List<Memory>> = fakeMemories
    override val relationshipState: Flow<RelationshipState?> = fakeRelationship

    override suspend fun insertMessage(message: Message) {
        val list = fakeMessages.value.toMutableList()
        list.add(message)
        fakeMessages.value = list
    }
}

private fun mockDao(): CompanionDao {
    return object : CompanionDao {
        override fun getAllMessages(): Flow<List<Message>> = MutableStateFlow(emptyList())
        override fun getAllMemories(): Flow<List<Memory>> = MutableStateFlow(emptyList())
        override fun getRelationshipState(): Flow<RelationshipState?> = MutableStateFlow(null)
        override suspend fun insertMessage(message: Message) {}
        override suspend fun insertMemory(memory: Memory) {}
        override suspend fun updateRelationshipState(state: RelationshipState) {}
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ChatViewModelTest {

    private lateinit var viewModel: ChatViewModel
    private lateinit var repository: FakeCompanionRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeCompanionRepository()
        viewModel = ChatViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test typing indicator and error boundary management`() = runTest {
        assertFalse("Typing indicator should initially be false", viewModel.isTyping.value)
        assertTrue("Messages list should be empty", viewModel.messages.value.isEmpty())
        
        viewModel.sendMessage("Hello API")
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertFalse("Typing indicator should be false after completion or error", viewModel.isTyping.value)
        
        val currentMessages = repository.fakeMessages.value
        assertTrue("Messages should not be empty", currentMessages.isNotEmpty())
        if (currentMessages[0].isUser) {
            assertEquals("Hello API", currentMessages[0].text)
        } else {
            assertTrue(currentMessages[0].text.contains("API Key is missing"))
        }
    }
}
