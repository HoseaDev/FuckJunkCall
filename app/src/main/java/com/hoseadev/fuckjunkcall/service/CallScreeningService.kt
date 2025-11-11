package com.hoseadev.fuckjunkcall.service

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService as AndroidCallScreeningService
import android.util.Log
import com.hoseadev.fuckjunkcall.data.database.AppDatabase
import com.hoseadev.fuckjunkcall.data.model.BlockedCall
import com.hoseadev.fuckjunkcall.util.ContactsHelper
import com.hoseadev.fuckjunkcall.util.PreferenceHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 来电筛选服务
 * 核心功能：拦截陌生来电并触发自动短信回复
 */
class CallScreeningService : AndroidCallScreeningService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // 保存 callDetails 用于异步处理后响应
    private lateinit var callDetails: Call.Details

    companion object {
        private const val TAG = "CallScreeningService"
    }

    override fun onScreenCall(callDetails: Call.Details) {
        this.callDetails = callDetails
        Log.d(TAG, "收到来电筛选请求")

        // 获取来电号码
        val phoneNumber = callDetails.handle?.schemeSpecificPart

        if (phoneNumber.isNullOrEmpty()) {
            Log.w(TAG, "来电号码为空，允许通过")
            allowCall()
            return
        }

        Log.d(TAG, "来电号码: $phoneNumber")

        // 检查是否启用拦截功能
        if (!PreferenceHelper.isBlockEnabled(this)) {
            Log.d(TAG, "拦截功能未启用，允许所有来电")
            allowCall()
            return
        }

        // 异步判断是否为陌生号码
        serviceScope.launch {
            try {
                val isStranger = ContactsHelper.isStrangerNumber(this@CallScreeningService, phoneNumber)

                if (isStranger) {
                    Log.i(TAG, "陌生号码 $phoneNumber，执行拦截")
                    blockCall(phoneNumber)
                } else {
                    Log.d(TAG, "已知号码 $phoneNumber，允许通过")
                    allowCall()
                }
            } catch (e: Exception) {
                Log.e(TAG, "判断号码失败，默认允许通过", e)
                allowCall()
            }
        }
    }

    /**
     * 拦截来电
     */
    private fun blockCall(phoneNumber: String) {
        // 构建拦截响应
        val response = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            CallResponse.Builder()
                .setDisallowCall(true)          // 禁止来电
                .setRejectCall(true)             // 拒接来电
                .setSkipCallLog(true)            // 不记录到通话记录
                .setSkipNotification(true)       // 不显示通知
                .build()
        } else {
            CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .build()
        }

        // 执行拦截
        respondToCall(callDetails, response)

        // 记录到数据库
        saveBlockedCall(phoneNumber)

        // 发送自动回复短信
        if (PreferenceHelper.isAutoSmsEnabled(this)) {
            SmsService.sendAutoReply(this, phoneNumber)
        }

        Log.i(TAG, "成功拦截来电: $phoneNumber")
    }

    /**
     * 允许来电通过
     */
    private fun allowCall() {
        val response = CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .build()

        respondToCall(callDetails, response)
    }

    /**
     * 保存拦截记录到数据库
     */
    private fun saveBlockedCall(phoneNumber: String) {
        serviceScope.launch(Dispatchers.IO) {
            try {
                val database = AppDatabase.getDatabase(this@CallScreeningService)
                val blockedCall = BlockedCall(
                    phoneNumber = phoneNumber,
                    timestamp = System.currentTimeMillis(),
                    smsSent = PreferenceHelper.isAutoSmsEnabled(this@CallScreeningService),
                    smsContent = if (PreferenceHelper.isAutoSmsEnabled(this@CallScreeningService)) {
                        PreferenceHelper.getSmsTemplate(this@CallScreeningService)
                    } else {
                        null
                    }
                )
                database.blockedCallDao().insert(blockedCall)
                Log.d(TAG, "拦截记录已保存")
            } catch (e: Exception) {
                Log.e(TAG, "保存拦截记录失败", e)
            }
        }
    }
}
