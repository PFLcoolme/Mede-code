package com.medemini.ai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medemini.ai.model.AIConfig
import com.medemini.ai.model.MCPService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AISettingsDialog(
    config: AIConfig,
    onConfigChange: (AIConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var settingsPage by remember { mutableStateOf("main") }
    var apiBaseUrl by remember { mutableStateOf(config.apiBaseUrl) }
    var apiKey by remember { mutableStateOf(config.apiKey) }
    var model by remember { mutableStateOf(config.model) }
    var temperature by remember { mutableStateOf(config.temperature) }
    var maxTokens by remember { mutableStateOf(config.maxTokens) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (settingsPage != "main") {
                    Icon(Icons.Default.ArrowBack, "返回", modifier = Modifier.size(20.dp).clickable { settingsPage = "main" }, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    when (settingsPage) {
                        "api" -> "API 配置"
                        "params" -> "AI 参数"
                        else -> "AI 设置"
                    },
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            when (settingsPage) {
                "main" -> Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingsNavItem(Icons.Default.Link, "API 配置", subtitle = "配置 API 链接和密钥") { settingsPage = "api" }
                    SettingsNavItem(Icons.Default.Tune, "AI 参数", subtitle = "温度、最大 Token 数") { settingsPage = "params" }
                    SettingsNavItem(Icons.Default.Info, "关于", subtitle = "MedeMini AI 助手 v1.0")
                }
                "api" -> Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingSection("API 地址") {
                        BasicTextField(
                            value = apiBaseUrl,
                            onValueChange = { apiBaseUrl = it },
                            modifier = Modifier.fillMaxWidth().height(40.dp)
                                .background(Color(0xFFFAFAFA), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp),
                            textStyle = TextStyle(fontSize = 13.sp, color = Color.Black),
                            decorationBox = { innerTextField ->
                                Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                                    if (apiBaseUrl.isEmpty()) {
                                        Text("https://api.openai.com/v1", style = TextStyle(fontSize = 13.sp, color = Color(0xFFBBBBBB)))
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }

                    SettingSection("API 密钥") {
                        BasicTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            modifier = Modifier.fillMaxWidth().height(40.dp)
                                .background(Color(0xFFFAFAFA), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp),
                            textStyle = TextStyle(fontSize = 13.sp, color = Color.Black),
                            decorationBox = { innerTextField ->
                                Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                                    if (apiKey.isEmpty()) {
                                        Text("sk-...", style = TextStyle(fontSize = 13.sp, color = Color(0xFFBBBBBB)))
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }

                    SettingSection("模型名称") {
                        BasicTextField(
                            value = model,
                            onValueChange = { model = it },
                            modifier = Modifier.fillMaxWidth().height(40.dp)
                                .background(Color(0xFFFAFAFA), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp),
                            textStyle = TextStyle(fontSize = 13.sp, color = Color.Black),
                            decorationBox = { innerTextField ->
                                Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                                    if (model.isEmpty()) {
                                        Text("gpt-4o-mini", style = TextStyle(fontSize = 13.sp, color = Color(0xFFBBBBBB)))
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }

                    Text("提示: 请确保你的 API 地址和模型名称正确匹配", fontSize = 11.sp, color = Color(0xFF999999))
                }
                "params" -> Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingSection("温度 (Temperature)") {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${(temperature * 100).toInt() / 100f}", fontSize = 13.sp, color = Color.Black)
                                Text("0.0 - 2.0", fontSize = 11.sp, color = Color(0xFF999999))
                            }
                            Slider(
                                value = temperature,
                                onValueChange = { temperature = it },
                                valueRange = 0f..2f,
                                steps = 20,
                                colors = SliderDefaults.colors(activeTrackColor = Color(0xFF333333))
                            )
                            Text("较高的值会使输出更具创意，较低的值更确定性", fontSize = 11.sp, color = Color(0xFF999999))
                        }
                    }

                    SettingSection("最大 Token 数") {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("$maxTokens", fontSize = 13.sp, color = Color.Black)
                                Text("1024 - 8192", fontSize = 11.sp, color = Color(0xFF999999))
                            }
                            Slider(
                                value = maxTokens.toFloat(),
                                onValueChange = { maxTokens = it.toInt() },
                                valueRange = 1024f..8192f,
                                steps = 14,
                                colors = SliderDefaults.colors(activeTrackColor = Color(0xFF333333))
                            )
                            Text("限制 AI 响应的长度，较大的值会消耗更多费用", fontSize = 11.sp, color = Color(0xFF999999))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfigChange(
                        AIConfig(
                            apiBaseUrl = apiBaseUrl,
                            apiKey = apiKey,
                            model = model,
                            temperature = temperature,
                            maxTokens = maxTokens,
                            mcpServices = config.mcpServices
                        )
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A))
            ) {
                Text("保存", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Color(0xFF666666))
            }
        }
    )
}

@Composable
private fun SettingSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.Black)
        content()
    }
}

@Composable
private fun SettingsNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(Color(0xFFFAFAFA))
            .clickable { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(22.dp), tint = Color(0xFF666666))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp, color = Color.Black)
            if (subtitle != null) Text(subtitle, fontSize = 12.sp, color = Color(0xFF999999))
        }
        if (onClick != null) {
            Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(18.dp), tint = Color(0xFFCCCCCC))
        }
    }
}