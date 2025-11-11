package com.hoseadev.fuckjunkcall.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.hoseadev.fuckjunkcall.R
import com.hoseadev.fuckjunkcall.util.PreferenceHelper

/**
 * 短信发送前台服务
 */
class SmsService : Service() {

    companion object {
        private const val TAG = "SmsService"
        private const val CHANNEL_ID = "sms_service_channel"
        private const val NOTIFICATION_ID = 1001
        private const val EXTRA_PHONE_NUMBER = "phone_number"

        /**
         * 启动短信发送服务
         */
        fun sendAutoReply(context: Context, phoneNumber: String) {
            val intent = Intent(context, SmsService::class.java).apply {
                putExtra(EXTRA_PHONE_NUMBER, phoneNumber)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val phoneNumber = intent?.getStringExtra(EXTRA_PHONE_NUMBER)

        if (phoneNumber.isNullOrEmpty()) {
            Log.e(TAG, "号码为空，无法发送短信")
            stopSelf()
            return START_NOT_STICKY
        }

        // 启动前台服务
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ 需要指定服务类型
            startForeground(
                NOTIFICATION_ID,
                createNotification(phoneNumber),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
            )
        } else {
            startForeground(NOTIFICATION_ID, createNotification(phoneNumber))
        }

        // 发送短信
        sendSms(phoneNumber)

        // 发送完成后停止服务
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * 发送短信
     */
    private fun sendSms(phoneNumber: String) {
        try {
            // 检查是否启用自动短信
            if (!PreferenceHelper.isAutoSmsEnabled(this)) {
                Log.d(TAG, "自动短信功能未启用，跳过发送")
                return
            }

            val message = PreferenceHelper.getSmsTemplate(this)
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            smsManager.sendTextMessage(
                phoneNumber,
                null,
                message,
                null,
                null
            )

            Log.i(TAG, "短信发送成功: $phoneNumber")
        } catch (e: SecurityException) {
            Log.e(TAG, "缺少短信发送权限", e)
        } catch (e: Exception) {
            Log.e(TAG, "短信发送失败", e)
        }
    }

    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "短信发送服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "用于发送自动回复短信"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 创建前台服务通知
     */
    private fun createNotification(phoneNumber: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("发送自动回复")
            .setContentText("正在向 $phoneNumber 发送短信...")
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
