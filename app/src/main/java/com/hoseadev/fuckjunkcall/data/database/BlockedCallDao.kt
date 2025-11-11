package com.hoseadev.fuckjunkcall.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.hoseadev.fuckjunkcall.data.model.BlockedCall
import kotlinx.coroutines.flow.Flow

/**
 * 拦截记录数据访问对象
 */
@Dao
interface BlockedCallDao {

    @Insert
    suspend fun insert(blockedCall: BlockedCall): Long

    @Query("SELECT * FROM blocked_calls ORDER BY timestamp DESC")
    fun getAllBlockedCalls(): Flow<List<BlockedCall>>

    @Query("SELECT * FROM blocked_calls ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentBlockedCalls(limit: Int): Flow<List<BlockedCall>>

    @Query("DELETE FROM blocked_calls WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM blocked_calls")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM blocked_calls")
    fun getBlockedCallCount(): Flow<Int>
}
