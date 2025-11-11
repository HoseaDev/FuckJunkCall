package com.hoseadev.fuckjunkcall.util

import android.content.Context
import android.content.SharedPreferences

/**
 * SharedPreferences 工具类
 */
object PreferenceHelper {

    private const val PREF_NAME = "fuck_junk_call_prefs"

    private const val KEY_BLOCK_ENABLED = "block_enabled"
    private const val KEY_SMS_TEMPLATE = "sms_template"
    private const val KEY_AUTO_SMS_ENABLED = "auto_sms_enabled"

    private const val DEFAULT_SMS_TEMPLATE = "您好，我现在不方便接听电话，稍后回复您。"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 是否启用拦截功能
     */
    fun isBlockEnabled(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_BLOCK_ENABLED, false)
    }

    fun setBlockEnabled(context: Context, enabled: Boolean) {
        getPreferences(context).edit().putBoolean(KEY_BLOCK_ENABLED, enabled).apply()
    }

    /**
     * 是否启用自动短信回复
     */
    fun isAutoSmsEnabled(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_AUTO_SMS_ENABLED, true)
    }

    fun setAutoSmsEnabled(context: Context, enabled: Boolean) {
        getPreferences(context).edit().putBoolean(KEY_AUTO_SMS_ENABLED, enabled).apply()
    }

    /**
     * 获取短信模板
     */
    fun getSmsTemplate(context: Context): String {
        return getPreferences(context).getString(KEY_SMS_TEMPLATE, DEFAULT_SMS_TEMPLATE)
            ?: DEFAULT_SMS_TEMPLATE
    }

    fun setSmsTemplate(context: Context, template: String) {
        getPreferences(context).edit().putString(KEY_SMS_TEMPLATE, template).apply()
    }
}
