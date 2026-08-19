package com.companion.cc.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.companion.cc.data.local.entity.MemoryRetrievalFeedbackEntity
import com.companion.cc.data.local.entity.MemoryRetrievalTraceEntity

@Dao
interface MemoryRetrievalDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTrace(trace: MemoryRetrievalTraceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(feedback: MemoryRetrievalFeedbackEntity)
}
