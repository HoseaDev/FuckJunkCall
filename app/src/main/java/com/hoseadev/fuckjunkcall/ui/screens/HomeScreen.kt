package com.hoseadev.fuckjunkcall.ui.screens

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.hoseadev.fuckjunkcall.util.PreferenceHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current

    // 状态
    var blockEnabled by remember { mutableStateOf(PreferenceHelper.isBlockEnabled(context)) }
    var autoSmsEnabled by remember { mutableStateOf(PreferenceHelper.isAutoSmsEnabled(context)) }
    var smsTemplate by remember { mutableStateOf(PreferenceHelper.getSmsTemplate(context)) }
    var showSmsDialog by remember { mutableStateOf(false) }

    // 权限状态
    var hasCallScreeningRole by remember { mutableStateOf(checkCallScreeningRole(context)) }
    var hasContactsPermission by remember { mutableStateOf(checkContactsPermission(context)) }
    var hasSmsPermission by remember { mutableStateOf(checkSmsPermission(context)) }

    // 权限请求 Launcher
    val roleRequestLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        hasCallScreeningRole = checkCallScreeningRole(context)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasContactsPermission = permissions[Manifest.permission.READ_CONTACTS] == true
        hasSmsPermission = permissions[Manifest.permission.SEND_SMS] == true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("垃圾来电拦截") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 权限检查卡片
            PermissionCheckCard(
                hasCallScreeningRole = hasCallScreeningRole,
                hasContactsPermission = hasContactsPermission,
                hasSmsPermission = hasSmsPermission,
                onRequestCallScreening = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val roleManager = context.getSystemService(RoleManager::class.java)
                        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                        roleRequestLauncher.launch(intent)
                    }
                },
                onRequestPermissions = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.READ_CONTACTS,
                            Manifest.permission.SEND_SMS
                        )
                    )
                }
            )

            // 拦截功能开关
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "启用拦截功能",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "自动拦截陌生来电",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = blockEnabled,
                            onCheckedChange = {
                                blockEnabled = it
                                PreferenceHelper.setBlockEnabled(context, it)
                            },
                            enabled = hasCallScreeningRole && hasContactsPermission
                        )
                    }
                }
            }

            // 短信回复设置
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "自动短信回复",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "拦截后自动发送短信",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoSmsEnabled,
                            onCheckedChange = {
                                autoSmsEnabled = it
                                PreferenceHelper.setAutoSmsEnabled(context, it)
                            },
                            enabled = hasSmsPermission && blockEnabled
                        )
                    }

                    HorizontalDivider()

                    // 短信模板编辑
                    OutlinedButton(
                        onClick = { showSmsDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = autoSmsEnabled
                    ) {
                        Text("编辑短信模板")
                    }

                    Text(
                        text = "当前模板：$smsTemplate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 说明文字
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "使用说明",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "• 本应用只拦截不在通讯录中的陌生号码\n" +
                                "• 拦截后对方会听到\"正在通话中\"提示\n" +
                                "• 您的手机不会响铃也不会显示来电\n" +
                                "• 可选择自动发送短信告知对方",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    // 短信模板编辑对话框
    if (showSmsDialog) {
        var tempTemplate by remember { mutableStateOf(smsTemplate) }

        AlertDialog(
            onDismissRequest = { showSmsDialog = false },
            title = { Text("编辑短信模板") },
            text = {
                OutlinedTextField(
                    value = tempTemplate,
                    onValueChange = { tempTemplate = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("短信内容") },
                    maxLines = 5
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    smsTemplate = tempTemplate
                    PreferenceHelper.setSmsTemplate(context, tempTemplate)
                    showSmsDialog = false
                }) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSmsDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun PermissionCheckCard(
    hasCallScreeningRole: Boolean,
    hasContactsPermission: Boolean,
    hasSmsPermission: Boolean,
    onRequestCallScreening: () -> Unit,
    onRequestPermissions: () -> Unit
) {
    val allGranted = hasCallScreeningRole && hasContactsPermission && hasSmsPermission

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (allGranted)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (allGranted) "✓ 权限已就绪" else "⚠ 需要授权",
                style = MaterialTheme.typography.titleMedium
            )

            if (!hasCallScreeningRole) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "❌ 来电筛选权限",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onRequestCallScreening) {
                        Text("授权")
                    }
                }
            } else {
                Text(
                    text = "✓ 来电筛选权限",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (!hasContactsPermission || !hasSmsPermission) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (!hasContactsPermission) {
                            Text(
                                text = "❌ 读取通讯录",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        if (!hasSmsPermission) {
                            Text(
                                text = "❌ 发送短信",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    TextButton(onClick = onRequestPermissions) {
                        Text("授权")
                    }
                }
            } else {
                Text(
                    text = "✓ 通讯录和短信权限",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

fun checkCallScreeningRole(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(RoleManager::class.java)
        roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
    } else {
        false
    }
}

fun checkContactsPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_CONTACTS
    ) == PackageManager.PERMISSION_GRANTED
}

fun checkSmsPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.SEND_SMS
    ) == PackageManager.PERMISSION_GRANTED
}
