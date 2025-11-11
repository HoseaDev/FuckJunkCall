# 陌生来电拦截 + 自动短信回复 技术方案

## 一、功能需求

### 核心功能
1. 检测来电是否为陌生号码（不在通讯录中）
2. 陌生来电 → 自动挂断/拦截
3. 陌生来电 → 自动发送预设短信
4. 熟人来电 → 正常响铃接听

### 用户可配置项
- 开启/关闭拦截功能
- 自定义回复短信内容
- 白名单管理（部分陌生号码允许通过）
- 查看拦截记录

---

## 二、核心技术难点详解

### 2.1 "挂断来电"的三种实现方式

#### **方式 1：CallScreeningService（推荐）**

**技术原理**：
```
来电 → 系统先交给 CallScreeningService → 你的 App 判断 → 决定是否阻止
```

**能做什么**：
```kotlin
respondToCall(callDetails, CallResponse.Builder()
    .setDisallowCall(true)        // 阻止来电显示
    .setRejectCall(true)           // 拒接来电
    .setSkipCallLog(true)          // 不记录到通话记录
    .setSkipNotification(true)     // 不显示未接来电通知
    .build()
)
```

**实际效果**：
- ✅ 用户手机不会响铃
- ✅ 屏幕上不会弹出来电界面
- ✅ 不会有未接来电通知
- ⚠️ **但对方会听到"您拨打的电话正在通话中"或直接转到语音信箱**
- ⚠️ **某些运营商可能会在通话记录中留下痕迹**

**优点**：
- 不需要成为默认电话应用
- API 正规，兼容性好
- 用户体验友好（只需授权"来电筛选"权限）

**缺点**：
- 不是"真正的挂断"，而是"提前拦截"
- 部分厂商 ROM 可能有额外限制

---

#### **方式 2：TelecomManager.endCall()（需要默认电话应用）**

**技术原理**：
```kotlin
// 需要 ANSWER_PHONE_CALLS 权限（Android 9+）
val telecomManager = getSystemService(TelecomManager::class.java)
telecomManager.endCall()  // 直接挂断正在进行的通话
```

**实际效果**：
- ✅ 真正的"挂断"，对方会听到忙音
- ✅ 完全控制通话流程

**缺点**：
- ❌ 必须成为默认电话应用
- ❌ 需要实现完整的拨号界面（InCallService、通话记录等）
- ❌ 用户切换默认应用的门槛高
- ❌ Android 9+ 后这个权限被严格限制

---

#### **方式 3：监听电话状态 + 模拟挂断（不推荐）**

**技术原理**（过时方案）：
```kotlin
// Android 6 以前可以通过反射调用隐藏 API
// 现在已经被封禁，国产 ROM 可能还有效但极不稳定
```

**结论**：
- ❌ Android 9+ 基本不可用
- ❌ 违反 Google Play 政策
- ❌ 不推荐使用

---

### 2.2 自动发送短信的技术挑战

#### **需要的权限**
```xml
<uses-permission android:name="android.permission.SEND_SMS"/>
```

#### **代码实现**
```kotlin
val smsManager = SmsManager.getDefault()
smsManager.sendTextMessage(
    phoneNumber,    // 对方号码
    null,           // ServiceCenter（一般为 null）
    "您好，我现在不方便接电话，稍后回复您。", // 短信内容
    null,           // sentIntent（发送成功回调）
    null            // deliveryIntent（送达回调）
)
```

#### **实际问题**

**问题 1：后台限制**
- Android 10+ 对后台发送短信有限制
- 解决方案：在来电时启动前台服务（显示通知），发送短信后停止

**问题 2：运行时权限**
- 必须在首次使用前动态申请 `SEND_SMS` 权限
- 如果用户拒绝，功能将无法使用

**问题 3：短信发送失败**
- 可能原因：余额不足、运营商限制、号码格式错误
- 解决方案：监听 `sentIntent`，失败时记录到日志

**问题 4：国产 ROM 限制**
- 小米/华为/OPPO 可能需要额外授权"后台自启动"
- 解决方案：引导用户在系统设置中手动授权

---

### 2.3 判断陌生号码的实现

#### **技术方案**
```kotlin
fun isStrangerNumber(phoneNumber: String): Boolean {
    // 1. 查询系统通讯录
    val uri = Uri.withAppendedPath(
        ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
        Uri.encode(phoneNumber)
    )
    val cursor = contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)

    val inContacts = cursor?.use { it.count > 0 } ?: false
    cursor?.close()

    // 2. 检查白名单（用户自定义）
    val inWhitelist = checkWhitelist(phoneNumber)

    return !inContacts && !inWhitelist
}
```

#### **需要的权限**
```xml
<uses-permission android:name="android.permission.READ_CONTACTS"/>
```

---

## 三、推荐实现方案（方案 A）

### 3.1 技术选型

| 功能 | 实现方式 | 说明 |
|------|----------|------|
| 来电拦截 | `CallScreeningService` | 不需要默认电话应用 |
| 短信发送 | `SmsManager` + 前台服务 | 规避后台限制 |
| 通讯录查询 | `ContactsContract` | 标准 API |
| 数据存储 | Room 数据库 | 存储拦截记录、白名单 |

### 3.2 架构设计

```
MainActivity（主界面）
    ├── 设置开关（启用/禁用拦截）
    ├── 短信模板编辑
    ├── 白名单管理
    └── 拦截记录查看

CallScreeningService（来电筛选服务）
    ├── onScreenCall() → 判断是否陌生号码
    ├── 是陌生号码 → respondToCall(disallow + reject)
    └── 触发 SmsService 发送短信

SmsService（前台服务）
    ├── 显示通知（规避后台限制）
    ├── 调用 SmsManager 发送短信
    └── 记录发送结果到数据库

Database（Room）
    ├── BlockedCall（拦截记录表）
    ├── Whitelist（白名单表）
    └── SmsTemplate（短信模板表）
```

### 3.3 核心代码结构

```
app/src/main/java/com/hoseadev/fuckjunkcall/
├── service/
│   ├── MyCallScreeningService.kt       # 来电筛选服务
│   └── SmsService.kt                    # 短信发送服务
├── data/
│   ├── database/
│   │   ├── AppDatabase.kt               # Room 数据库
│   │   ├── BlockedCallDao.kt            # 拦截记录 DAO
│   │   └── WhitelistDao.kt              # 白名单 DAO
│   └── model/
│       ├── BlockedCall.kt               # 拦截记录实体
│       └── WhitelistNumber.kt           # 白名单实体
├── ui/
│   ├── MainActivity.kt                  # 主界面
│   ├── screens/
│   │   ├── SettingsScreen.kt            # 设置界面
│   │   ├── WhitelistScreen.kt           # 白名单管理
│   │   └── BlockedCallsScreen.kt        # 拦截记录
│   └── components/
│       └── PermissionGuide.kt           # 权限引导组件
└── util/
    ├── ContactsHelper.kt                # 通讯录查询工具
    └── PermissionHelper.kt              # 权限检查工具
```

---

## 四、详细实现步骤

### Step 1：配置 AndroidManifest.xml

```xml
<!-- 权限声明 -->
<uses-permission android:name="android.permission.READ_PHONE_STATE"/>
<uses-permission android:name="android.permission.READ_CALL_LOG"/>
<uses-permission android:name="android.permission.READ_CONTACTS"/>
<uses-permission android:name="android.permission.SEND_SMS"/>

<application>
    <!-- 来电筛选服务 -->
    <service
        android:name=".service.MyCallScreeningService"
        android:permission="android.permission.BIND_SCREENING_SERVICE"
        android:exported="true">
        <intent-filter>
            <action android:name="android.telecom.CallScreeningService"/>
        </intent-filter>
    </service>

    <!-- 短信发送服务 -->
    <service
        android:name=".service.SmsService"
        android:foregroundServiceType="phoneCall"/>
</application>
```

### Step 2：实现 CallScreeningService

```kotlin
class MyCallScreeningService : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        val phoneNumber = callDetails.handle.schemeSpecificPart

        // 检查是否为陌生号码
        val isStranger = ContactsHelper.isStrangerNumber(this, phoneNumber)

        if (isStranger && PreferenceHelper.isBlockEnabled(this)) {
            // 拦截陌生来电
            respondToCall(callDetails, CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipCallLog(true)
                .setSkipNotification(true)
                .build()
            )

            // 记录到数据库
            saveBlockedCall(phoneNumber)

            // 发送短信
            SmsService.sendAutoReply(this, phoneNumber)
        } else {
            // 允许来电
            respondToCall(callDetails, CallResponse.Builder()
                .setDisallowCall(false)
                .setRejectCall(false)
                .build()
            )
        }
    }
}
```

### Step 3：实现短信发送服务

```kotlin
class SmsService : Service() {
    companion object {
        fun sendAutoReply(context: Context, phoneNumber: String) {
            val intent = Intent(context, SmsService::class.java).apply {
                putExtra("phone_number", phoneNumber)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val phoneNumber = intent?.getStringExtra("phone_number") ?: return START_NOT_STICKY

        // 启动前台服务
        startForeground(1, createNotification())

        // 发送短信
        try {
            val message = PreferenceHelper.getSmsTemplate(this)
            SmsManager.getDefault().sendTextMessage(phoneNumber, null, message, null, null)
            Log.i("SmsService", "短信发送成功: $phoneNumber")
        } catch (e: Exception) {
            Log.e("SmsService", "短信发送失败", e)
        }

        // 停止服务
        stopForeground(true)
        stopSelf()

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
```

### Step 4：实现权限引导

```kotlin
@Composable
fun PermissionGuideScreen() {
    val context = LocalContext.current
    val roleManager = context.getSystemService(RoleManager::class.java)

    Column {
        // 1. 来电筛选权限
        PermissionCard(
            title = "来电筛选权限",
            description = "允许应用识别和拦截陌生来电",
            granted = roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
        ) {
            val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
            context.startActivity(intent)
        }

        // 2. 通讯录权限
        PermissionCard(
            title = "读取通讯录",
            description = "用于判断来电号码是否为联系人",
            granted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        ) {
            // 请求运行时权限
        }

        // 3. 短信权限
        PermissionCard(
            title = "发送短信",
            description = "自动回复拦截的陌生来电",
            granted = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        ) {
            // 请求运行时权限
        }
    }
}
```

---

## 五、兼容性和风险评估

### 5.1 Android 版本兼容性

| Android 版本 | API Level | 兼容性 | 注意事项 |
|-------------|-----------|--------|----------|
| 7.0 - 8.1   | 24-27     | ✅ 完全支持 | CallScreeningService 首次引入 |
| 9.0         | 28        | ✅ 完全支持 | 需要 ANSWER_PHONE_CALLS 权限 |
| 10.0        | 29        | ✅ 支持 | 后台短信限制，需前台服务 |
| 11.0+       | 30+       | ✅ 支持 | 权限检查更严格 |

### 5.2 厂商 ROM 兼容性

| 厂商 | 风险等级 | 主要问题 | 解决方案 |
|------|----------|----------|----------|
| 小米 MIUI | ⚠️ 中 | 后台自启动限制 | 引导用户设置自启动 |
| 华为 HarmonyOS | ⚠️ 中 | 自带骚扰拦截冲突 | 提示用户关闭系统拦截 |
| OPPO ColorOS | ⚠️ 中 | 权限管理严格 | 详细的权限引导 |
| vivo OriginOS | ⚠️ 中 | 后台服务限制 | 加入白名单 |
| 原生 Android | ✅ 低 | 无特殊问题 | - |

### 5.3 已知风险

1. **短信余额不足**：发送失败但用户可能不知道
2. **运营商限制**：部分运营商可能限制自动短信
3. **隐私问题**：需要明确告知用户数据使用情况
4. **误拦截**：通讯录同步延迟可能导致新联系人被拦截

---

## 六、后续优化方向

### 6.1 核心功能优化
- [ ] 智能识别：接入腾讯手机管家等第三方号码库
- [ ] 黑名单模式：支持仅拦截特定号码
- [ ] 时段控制：夜间/会议时段自动启用

### 6.2 用户体验优化
- [ ] 拦截统计：每日/每周拦截报告
- [ ] 多短信模板：不同场景使用不同回复
- [ ] 快捷操作：拦截记录中快速添加白名单

### 6.3 高级功能
- [ ] 云同步：备份设置和白名单
- [ ] AI 回复：根据来电时间智能生成回复内容
- [ ] 通话录音：合法地区支持录音功能

---

## 七、总结

### 推荐方案
**使用 CallScreeningService + 前台服务发送短信**

### 核心优势
1. ✅ 不需要成为默认电话应用
2. ✅ API 正规，审核通过率高
3. ✅ 用户体验好（只需授权来电筛选）
4. ✅ 兼容性好（支持 Android 7+）

### 核心妥协
1. ⚠️ 不是"真正挂断"，而是"提前拦截"
2. ⚠️ 依赖用户授权多个权限
3. ⚠️ 国产 ROM 需要额外设置

### 开发复杂度
- 核心功能：**中等**（3-5 天）
- UI + 权限引导：**中等**（2-3 天）
- 测试 + 兼容性调试：**较高**（5-7 天）

**预计总工期：10-15 天（个人开发）**
