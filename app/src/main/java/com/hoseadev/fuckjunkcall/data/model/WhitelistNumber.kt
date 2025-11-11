package com.hoseadev.fuckjunkcall.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 白名单号码实体类
 */
@Entity(tableName = "whitelist")
data class WhitelistNumber(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val phoneNumber: String,        // 白名单号码
    val name: String? = null,       // 备注名称
    val addedTime: Long             // 添加时间戳
)
