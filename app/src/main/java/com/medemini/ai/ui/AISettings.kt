package com.medemini.ai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.medemini.ai.viewmodel.AIViewModel

/**
 * AI 设置窗口
 * 配置 API 密钥、MCP 服务、AI 模型和输出参数
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AISettingsDialog(
    viewModel: AIViewModel,
    onDismiss: () -> Unit
) {
    var apiKey by remember { mutableStateOf("") }
    var apiEndpoint by remember { mutableStateOf("") }
    var mcpEndpoint by remember { mutableStateOf("") }
    var modelName by remember { mutableStateOf("") }
    var temperature by remember { mutableStateOf("") }
    var maxTokens by remember { mutableStateOf("") }
    var showSaveConfirm by remember { mutableStateOf(false) }
    
    // 加载当前设置
    LaunchedEffect(Unit) {
        // 这里需要从 ViewModel 获取当前设置，简化处理
    }
    
    ModalAlertDialog(
        onDismiss = onDismiss,
        title = "AI 设置",
        containerColor = Color.White
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // API 配置
            SettingsSection("API 配置") {
                SettingsTextField("API Key", "你的 OpenAI API 密钥", icon = Icons.Default.Key)
                SettingsTextField("API 端点", "https://api.openai.com/v1", icon = Icons.Default.Link)
            }
            
            // MCP 服务配置
            SettingsSection("MCP 服务") {
                SettingsTextField("MCP 端点", "https://your-mcp-server.com", icon = Icons.Default.Devices)
                Text("MCP (Model Context Protocol) 服务允许 AI 访问本地工具和资源", 
                    style = TextStyle(fontSize = 11.sp, color = Color(0xFF999999)))
            }
            
            // AI 模型设置
            SettingsSection("AI 模型") {
                Column(modifier = Modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("模型选择", style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.Black))
                    ModelSelector()
                }
            }
            
            // 输出参数
            SettingsSection("输出参数") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("温度 (0-1)", style = TextStyle(fontSize = 12.sp, color = Color(0xFF666666)))
                        TextField(
                            value = "0.7",
                            onValueChange = {},
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("最大 Token", style = TextStyle(fontSize = 12.sp, color = Color(0xFF666666)))
                        TextField(
                            value = "4096",
                            onValueChange = {},
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
        }
        
        // 按钮区域
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f).height(40.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("取消", fontSize = 13.sp)
            }
            Button(
                onClick = {
                    // 保存设置
                    // viewModel.updateSettings(apiKey, apiEndpoint, mcpEndpoint, modelName, 0.7, 4096)
                    onDismiss()
                },
                modifier = Modifier.weight(1f).height(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6344CF)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("保存", fontSize = 13.sp, color = Color.White)
            }
        }
    }
}

/**
 * 模型选择器
 */
@Composable
private fun ModelSelector() {
    val models = listOf(
        "GPT-4 (推荐)",
        "GPT-4 Turbo",
        "GPT-3.5 Turbo",
        "Claude 3 Opus",
        "Claude 3 Sonnet",
        "Claude 3 Haiku",
        "PaLM 2",
        "本地模型"
    )
    
    var selectedModel by remember { mutableStateOf(0) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column {
            models.forEachIndexed { index, model ->
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clickable { selectedModel = index }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (index == selectedModel) {
                        Icon(Icons.Default.Check, "选中", modifier = Modifier.size(18.dp), tint = Color(0xFF6344CF))
                        Spacer(modifier = Modifier.width(12.dp))
                    } else {
                        Spacer(modifier = Modifier.size(24.dp))
                    }
                    Text(model, style = TextStyle(fontSize = 13.sp, color = Color.Black))
                    if (index == 0) {
                        Spacer(modifier = Modifier.weight(1f))
                        Text("推荐", style = TextStyle(fontSize = 10.sp, color = Color(0xFF6344CF), fontWeight = FontWeight.Medium))
                    }
                }
                if (index < models.size - 1) {
                    Divider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(start = 48.dp))
                }
            }
        }
    }
}

/**
 * 设置区块
 */
@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text(title, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black))
        Spacer(modifier = Modifier.height(8.dp))
        content()
        Divider(modifier = Modifier.padding(vertical = 8.dp))
    }
}

/**
 * 设置文本字段
 */
@Composable
private fun SettingsTextField(label: String, placeholder: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = TextStyle(fontSize = 12.sp, color = Color(0xFF666666)))
        Spacer(modifier = Modifier.height(4.dp))
        BasicTextField(
            value = "",
            onValueChange = {},
            modifier = Modifier.fillMaxWidth()
                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                .padding(12.dp),
            textStyle = TextStyle(fontSize = 13.sp, color = Color.Black)
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
}

/**
 * 自定义模态对话框
 */
@Composable
private fun ModalAlertDialog(
    onDismiss: () -> Unit,
    title: String,
    containerColor: Color,
    content: @Composable () -> Unit
) {
    val listState = rememberScrollState()
    
    Card(
        modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Settings, "设置", modifier = Modifier.size(20.dp), tint = Color(0xFF6344CF))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black))
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "关闭", tint = Color(0xFF999999))
                }
            }
            
            Divider()
            
            // 内容区域
            Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp)) {
                content()
            }
        }
    }
}