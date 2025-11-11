# FuckJunkCall - 陌生来电拦截器

一个简洁高效的 Android 应用，自动拦截不在通讯录中的陌生来电，并发送自动回复短信。

## ✨ 功能特性

- 🚫 **自动拦截陌生来电** - 不在通讯录的号码直接拦截，不响铃、不显示
- 📱 **智能识别** - 基于系统通讯录和自定义白名单判断
- 💬 **自动短信回复** - 拦截后自动发送预设短信告知对方
- 📝 **自定义模板** - 可编辑短信回复内容
- 📊 **拦截记录** - 保存所有被拦截的来电历史
- ⚪ **白名单管理** - 支持添加例外号码
- 🎨 **Material 3 设计** - 现代化的用户界面

## 📱 系统要求

- **最低版本**: Android 7.0 (API 24)
- **目标版本**: Android 15 (API 35)
- **推荐设备**: 真机（模拟器可能不支持来电筛选功能）

## 🚀 快速开始

### 下载安装

1. 下载最新版本的 APK
2. 在手机上安装
3. 打开应用并按照引导完成权限设置

### 权限设置

应用需要以下权限才能正常工作：

1. **来电筛选角色** ⭐ 核心权限
   - 用途：拦截来电
   - 设置：应用内点击"授权"按钮

2. **读取通讯录**
   - 用途：判断来电号码是否为联系人
   - 设置：运行时权限，应用内申请

3. **发送短信**
   - 用途：自动回复被拦截的号码
   - 设置：运行时权限，应用内申请

### 使用步骤

1. 打开应用
2. 点击权限卡片中的"授权"按钮，依次授予所有权限
3. 开启"启用拦截功能"开关
4. （可选）编辑短信模板
5. 完成！现在陌生来电会被自动拦截

## 🔧 工作原理

### 技术架构

```
来电 → CallScreeningService 拦截
  ↓
查询白名单（Room 数据库）
  ↓
查询通讯录（ContactsContract）
  ↓
判断是否陌生号码？
  ├─ 是 → 拦截 + 记录 + 发短信
  └─ 否 → 允许通过（正常响铃）
```

### 拦截效果

**被拦截的来电：**
- ✅ 手机不会响铃
- ✅ 屏幕不会显示来电界面
- ✅ 不会有未接来电通知
- ✅ 不会在通话记录中留下痕迹（Android 10+）

**对方体验：**
- 听到"您拨打的电话正在通话中"或转到语音信箱
- 收到自动回复短信（如果启用）

## 📖 技术栈

- **语言**: Kotlin 2.0.21
- **UI 框架**: Jetpack Compose + Material 3
- **数据库**: Room 2.6.1
- **架构**: 单 Activity + Service 架构
- **异步处理**: Kotlin Coroutines
- **构建工具**: Gradle 8.11.1 + KSP

### 核心组件

| 组件 | 说明 |
|------|------|
| `CallScreeningService` | 来电筛选服务，拦截陌生来电 |
| `SmsService` | 前台服务，发送自动回复短信 |
| `ContactsHelper` | 通讯录查询工具 |
| `AppDatabase` | Room 数据库，存储拦截记录和白名单 |
| `HomeScreen` | Compose UI 主界面 |

## 🛠️ 开发指南

### 环境准备

- Android Studio Ladybug (2024.2.1) 或更高版本
- JDK 17
- Android SDK 35

### 克隆项目

```bash
git clone https://github.com/yourusername/FuckJunkCall.git
cd FuckJunkCall
```

### 构建项目

```bash
# 清理构建
./gradlew clean

# 构建 Debug 版本
./gradlew assembleDebug

# 安装到设备
./gradlew installDebug
```

### 运行测试

```bash
# 单元测试
./gradlew test

# 集成测试（需要连接设备）
./gradlew connectedAndroidTest
```

### 代码检查

```bash
# Lint 检查
./gradlew lint

# 查看报告
open app/build/reports/lint-results-debug.html
```

## 📂 项目结构

```
app/src/main/java/com/hoseadev/fuckjunkcall/
├── MainActivity.kt                      # 应用入口
├── service/
│   ├── CallScreeningService.kt          # 来电筛选核心服务
│   └── SmsService.kt                    # 短信发送服务
├── data/
│   ├── model/
│   │   ├── BlockedCall.kt               # 拦截记录实体
│   │   └── WhitelistNumber.kt           # 白名单实体
│   └── database/
│       ├── AppDatabase.kt               # Room 数据库
│       ├── BlockedCallDao.kt            # 拦截记录 DAO
│       └── WhitelistDao.kt              # 白名单 DAO
├── ui/
│   ├── screens/
│   │   └── HomeScreen.kt                # 主界面
│   └── theme/                           # Material 3 主题
└── util/
    ├── ContactsHelper.kt                # 通讯录工具
    └── PreferenceHelper.kt              # 配置管理
```

## ⚠️ 常见问题

### Q: 为什么来电没有被拦截？

**A:** 请检查：
1. 是否授予了"来电筛选"角色权限
2. 是否开启了"启用拦截功能"开关
3. 号码是否在通讯录或白名单中
4. 查看 logcat 日志：`adb logcat -s CallScreeningService:D`

### Q: 短信为什么没有自动发送？

**A:** 可能的原因：
1. 未授予"发送短信"权限
2. 未开启"自动短信回复"开关
3. 余额不足或运营商限制
4. **小米手机**：MIUI 会弹出确认框，首次发送时勾选"记住选择"

### Q: 小米手机短信需要手动确认？

**A:** 这是 MIUI 的安全策略，解决方法：
1. 安全中心 → 应用管理 → FuckJunkCall → 权限 → 短信 → 始终允许
2. 第一次发送时勾选"记住我的选择"或"不再询问"
3. 之后应该就能自动发送了

### Q: 拦截后对方听到什么？

**A:** 取决于运营商：
- 大部分运营商："您拨打的电话正在通话中"
- 部分运营商：直接转到语音信箱

### Q: 会不会漏接重要电话？

**A:** 不会，只要满足以下任一条件就不会被拦截：
- 号码在系统通讯录中
- 号码在应用白名单中

## 🔐 隐私说明

- ✅ 所有数据本地存储，不上传到服务器
- ✅ 不收集用户隐私信息
- ✅ 通讯录查询仅用于判断是否陌生号码
- ✅ 拦截记录仅保存在本地数据库

## 📝 开源协议

本项目采用 MIT 协议开源。

```
MIT License

Copyright (c) 2025 FuckJunkCall

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

### 提交 Issue

- Bug 报告：请描述复现步骤、设备型号、Android 版本
- 功能建议：请说明使用场景和预期效果

### 提交 PR

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 📧 联系方式

- 项目主页: [GitHub](https://github.com/yourusername/FuckJunkCall)
- 问题反馈: [Issues](https://github.com/yourusername/FuckJunkCall/issues)

## 🙏 致谢

- [Android Jetpack](https://developer.android.com/jetpack)
- [Material Design 3](https://m3.material.io/)
- [Room Persistence Library](https://developer.android.com/training/data-storage/room)

## ⚖️ 免责声明

本应用仅供学习和个人使用，使用前请确保：
- 遵守当地法律法规
- 不用于商业用途
- 自行承担使用风险

开发者不对因使用本应用导致的任何损失负责。

---

**如果觉得有用，请给个 ⭐ Star！**
