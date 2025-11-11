package com.hoseadev.fuckjunkcall.util

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import com.hoseadev.fuckjunkcall.data.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 通讯录查询工具类
 */
object ContactsHelper {

    private const val TAG = "ContactsHelper"

    /**
     * 判断号码是否为陌生号码（不在通讯录且不在白名单）
     * @return true = 陌生号码，需要拦截
     */
    suspend fun isStrangerNumber(context: Context, phoneNumber: String): Boolean {
        return withContext(Dispatchers.IO) {
            // 1. 检查是否在白名单
            val database = AppDatabase.getDatabase(context)
            val inWhitelist = database.whitelistDao().isInWhitelist(normalizePhoneNumber(phoneNumber))
            if (inWhitelist) {
                Log.d(TAG, "号码 $phoneNumber 在白名单中，允许通过")
                return@withContext false
            }

            // 2. 检查是否在系统通讯录
            val inContacts = isNumberInContacts(context, phoneNumber)
            if (inContacts) {
                Log.d(TAG, "号码 $phoneNumber 在通讯录中，允许通过")
                return@withContext false
            }

            Log.d(TAG, "号码 $phoneNumber 是陌生号码，需要拦截")
            return@withContext true
        }
    }

    /**
     * 检查号码是否在系统通讯录中
     */
    private fun isNumberInContacts(context: Context, phoneNumber: String): Boolean {
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup._ID),
                null,
                null,
                null
            )

            val exists = cursor?.use { it.count > 0 } ?: false
            cursor?.close()
            exists
        } catch (e: Exception) {
            Log.e(TAG, "查询通讯录失败", e)
            false
        }
    }

    /**
     * 规范化电话号码（去除空格、横线等）
     */
    fun normalizePhoneNumber(phoneNumber: String): String {
        return phoneNumber.replace(Regex("[\\s\\-()]+"), "")
    }

    /**
     * 获取联系人姓名（如果存在）
     */
    fun getContactName(context: Context, phoneNumber: String): String? {
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null,
                null,
                null
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        return it.getString(nameIndex)
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "获取联系人姓名失败", e)
            null
        }
    }
}
