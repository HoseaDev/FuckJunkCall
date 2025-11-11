package com.hoseadev.fuckjunkcall.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import com.hoseadev.fuckjunkcall.data.model.WhitelistNumber
import kotlinx.coroutines.flow.Flow

/**
 * 白名单数据访问对象
 */
@Dao
interface WhitelistDao {

    @Insert
    suspend fun insert(whitelistNumber: WhitelistNumber): Long

    @Query("SELECT * FROM whitelist ORDER BY addedTime DESC")
    fun getAllWhitelistNumbers(): Flow<List<WhitelistNumber>>

    @Query("SELECT EXISTS(SELECT 1 FROM whitelist WHERE phoneNumber = :phoneNumber LIMIT 1)")
    suspend fun isInWhitelist(phoneNumber: String): Boolean

    @Delete
    suspend fun delete(whitelistNumber: WhitelistNumber)

    @Query("DELETE FROM whitelist WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM whitelist")
    suspend fun deleteAll()
}
