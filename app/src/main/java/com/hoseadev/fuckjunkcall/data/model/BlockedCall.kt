package com.hoseadev.fuckjunkcall.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 拦截记录实体类
 */
@Entity(tableName = "blocked_calls")
data class BlockedCall(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val phoneNumber: String,        // 被拦截的号码
    val timestamp: Long,             // 拦截时间戳
    val smsSent: Boolean = false,   // 是否成功发送短信
    val smsContent: String? = null   // 发送的短信内容
)
