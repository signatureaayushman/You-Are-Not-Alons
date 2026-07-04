package com.example.data

import kotlinx.coroutines.flow.Flow

open class CompanionRepository(private val companionDao: CompanionDao) {
    open val allMessages: Flow<List<Message>> = companionDao.getAllMessages()
    open val allMemories: Flow<List<Memory>> = companionDao.getAllMemories()
    open val relationshipState: Flow<RelationshipState?> = companionDao.getRelationshipState()

    open suspend fun insertMessage(message: Message) = companionDao.insertMessage(message)
    open suspend fun insertMemory(memory: Memory) = companionDao.insertMemory(memory)
    open suspend fun updateRelationshipState(state: RelationshipState) = companionDao.updateRelationshipState(state)
}
