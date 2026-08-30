package com.companion.cc.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.companion.cc.data.local.entity.MemoryRetrievalFeedbackEntity
import com.companion.cc.data.local.entity.MemoryRetrievalTraceEntity

@Dao
interface MemoryRetrievalDao {
    @Query("DELETE FROM memory_retrieval_traces WHERE scopeKey = :scopeKey")
    suspend fun deleteTracesByScope(scopeKey: String): Int

    @Query("DELETE FROM memory_retrieval_feedback WHERE traceId IN (SELECT id FROM memory_retrieval_traces WHERE scopeKey = :scopeKey)")
    suspend fun deleteFeedbackByScope(scopeKey: String): Int

    @Query(
        "SELECT feedback.* FROM memory_retrieval_feedback AS feedback " +
            "INNER JOIN memory_retrieval_traces AS trace " +
            "ON feedback.traceId = trace.id " +
            "WHERE trace.scopeKey = :scopeKey AND feedback.nodeId IN (:nodeIds) " +
            "ORDER BY feedback.createdAt DESC"
    )
    suspend fun findFeedbackForNodes(
        scopeKey: String,
        nodeIds: List<String>
    ): List<MemoryRetrievalFeedbackEntity>


    @Query("SELECT * FROM memory_retrieval_traces WHERE scopeKey = :scopeKey ORDER BY createdAt ASC, id ASC")
    suspend fun findAllTracesInScope(scopeKey: String): List<MemoryRetrievalTraceEntity>

    @Query(
        "SELECT feedback.* FROM memory_retrieval_feedback AS feedback " +
            "INNER JOIN memory_retrieval_traces AS trace ON feedback.traceId = trace.id " +
            "INNER JOIN memory_nodes AS node ON feedback.nodeId = node.id " +
            "WHERE trace.scopeKey = :scopeKey AND node.scopeKey = :scopeKey " +
            "ORDER BY feedback.createdAt ASC, feedback.traceId ASC, feedback.nodeId ASC"
    )
    suspend fun findAllFeedbackInScope(scopeKey: String): List<MemoryRetrievalFeedbackEntity>

    @Query("SELECT * FROM memory_retrieval_traces WHERE scopeKey = :scopeKey AND id = :traceId LIMIT 1")
    suspend fun findTraceByIdInScope(scopeKey: String, traceId: String): MemoryRetrievalTraceEntity?

    @Query("SELECT * FROM memory_retrieval_traces WHERE id = :traceId LIMIT 1")
    suspend fun findTraceById(traceId: String): MemoryRetrievalTraceEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTrace(trace: MemoryRetrievalTraceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(feedback: MemoryRetrievalFeedbackEntity)

}
