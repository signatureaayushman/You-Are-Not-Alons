package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "relationship_state")
data class RelationshipState(
    @PrimaryKey val id: Int = 1,
    val depthValue: Int = 0,
    val lastStatus: String = "Just Meeting"
)
