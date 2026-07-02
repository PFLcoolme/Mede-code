package com.medemini.ai.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medemini.ai.model.AIMessage
import com.medemini.ai.model.MessageType
import com.medemini.ai.viewmodel.AIViewModel
import com.medemini.ai.viewmodel.ChatState
import com.medemini.ai.viewmodel.PendingReview

/**
 * AI 助手侧边栏
 * 点击按钮弹出 AI 对话窗口
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIAssistantPanel(
    viewModel: AIViewModel,
    currentFileContent: String? = null,
    currentFilePath: String? = null,
    onShowSettings: () -> Unit = {}
) {
    val chatState by viewModel.chatState.collectAsState()
    var pendingReview by remember { mutableStateOf<PendingReview?>(null) }
    LaunchedEffect(viewModel.pendingReview) {
        viewModel.pendingReview.collect { pendingReview = it }
    }
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    
    // 自动滚动到底部
    val messages = viewModel.messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
    }
    
    // 半透明背景（更透明，能看到下方代码）
    val translucentBackground = Color(0x80000000) // 50% 透明度的黑色
    
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(180.dp)
            .background(translucentBackground)
    ) {
        // 顶部按钮栏
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 新建对话按钮
            IconButton(
                onClick = { viewModel.clearChat() },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.Add, "新建对话",
                    modifier = Modifier.size(16.dp),
                    tint = Color.White.copy(0.9f)
                )
            }
            // 历史对话按钮
            IconButton(
                onClick = { /* TODO: 显示历史 */ },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.AccessTime, "历史对话",
                    modifier = Modifier.size(16.dp),
                    tint = Color.White.copy(0.9f)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            // 设置按钮
            IconButton(
                onClick = onShowSettings,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.Settings, "AI设置",
                    modifier = Modifier.size(16.dp),
                    tint = Color.White.copy(0.9f)
                )
            }
            // 关闭按钮
            IconButton(
                onClick = viewModel::closeAIChat,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.Close, "关闭",
                    modifier = Modifier.size(16.dp),
                    tint = Color.White.copy(0.9f)
                )
            }
        }
        
        // 对话消息区域
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 欢迎消息
                if (messages.isEmpty()) {
                    item {
                        WelcomeMessage()
                    }
                }
                
                // AI 工具列表（仅在开始时显示）
                if (messages.isEmpty()) {
                    item {
                        QuickActions()
                    }
                }
                
                // 对话消息
                items(messages, key = { msg -> msg.timestamp.toString() }) { message ->
                    MessageBubble(message)
                }
                
                // 加载指示器
                if (chatState is ChatState.Loading) {
                    item {
                        LoadingIndicator()
                    }
                }
            }
        }
        
        // 代码审查区域
        AnimatedVisibility(pendingReview != null) {
            if (pendingReview != null) {
                Column {
                    CodeReviewSection(pendingReview!!, viewModel)
                }
            }
        }
        
        // 输入区域
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("提问...", color = Color.White.copy(0.5f)) },
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedPlaceholderColor = Color.White.copy(0.5f),
                    unfocusedPlaceholderColor = Color.White.copy(0.5f),
                    focusedContainerColor = Color.White.copy(alpha = 0.15f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                maxLines = 1
            )
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(inputText, currentFileContent, currentFilePath)
                        inputText = ""
                    }
                },
                enabled = chatState !is ChatState.Loading && inputText.isNotBlank()
            ) {
                Icon(
                    Icons.Default.Send, "发送",
                    modifier = Modifier.size(18.dp),
                    tint = if (chatState is ChatState.Loading || inputText.isBlank()) Color.White.copy(0.3f) else Color.White.copy(0.9f)
                )
            }
        }
    }
}

/**
 * 欢迎消息组件
 */
@Composable
private fun WelcomeMessage() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Code, null, modifier = Modifier.size(16.dp), tint = Color(0xFF6344CF))
                Spacer(modifier = Modifier.width(4.dp))
                Text("MedeMini AI 编程助手", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("我可以帮助你:", style = TextStyle(fontSize = 13.sp, color = Color(0xFF666666)))
            Spacer(modifier = Modifier.height(4.dp))
            Text("• 编写和修改代码\n• 调试和查找Bug\n• 解释代码逻辑\n• 重构和优化\n• 生成测试用例\n• 运行构建和测试", style = TextStyle(fontSize = 12.sp, color = Color(0xFF888888)))
        }
    }
}

/**
 * 快捷操作
 */
@Composable
private fun QuickActions() {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val actions = listOf("修复Bug", "优化代码", "添加注释", "生成测试")
        items(actions) { action ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0EFFF)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    action,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = TextStyle(fontSize = 12.sp, color = Color(0xFF6344CF))
                )
            }
        }
    }
}

/**
 * 消息气泡
 */
@Composable
private fun MessageBubble(message: AIMessage) {
    val isUser = message.role == MessageType.USER
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) Color(0xFF6344CF) else Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    message.content,
                    style = TextStyle(fontSize = 13.sp, color = if (isUser) Color.White else Color.Black),
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    formatTime(message.timestamp),
                    style = TextStyle(fontSize = 10.sp, color = if (isUser) Color.White.copy(0.7f) else Color(0xFFAAAAAA))
                )
            }
        }
    }
}

/**
 * 加载指示器
 */
@Composable
private fun LoadingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFF6344CF)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI 思考中...", style = TextStyle(fontSize = 12.sp, color = Color(0xFF999999)))
            }
        }
    }
}

/**
 * 代码审查区域
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CodeReviewSection(
    review: PendingReview,
    viewModel: AIViewModel
) {
    Column(
        modifier = Modifier.fillMaxWidth().background(Color(0xFFFFF8E1)).padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Description, "代码审查", modifier = Modifier.size(18.dp), tint = Color(0xFFF59E0B))
            Spacer(modifier = Modifier.width(6.dp))
            Text("AI 建议修改代码", style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333)))
            Spacer(modifier = Modifier.weight(1f))
            Text("${review.operations.size} 个操作", style = TextStyle(fontSize = 11.sp, color = Color(0xFF999999)))
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 代码差异预览
        CodeDiffPreview(review)
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = viewModel::rejectReview,
                modifier = Modifier.weight(1f).height(36.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("拒绝", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            Button(
                onClick = viewModel::applyReview,
                modifier = Modifier.weight(1f).height(36.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("接受", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

/**
 * 代码差异预览
 */
@Composable
private fun CodeDiffPreview(review: PendingReview) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text("预览更改:", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF666666)))
            Spacer(modifier = Modifier.height(4.dp))
            
            // 使用自定义渲染显示差异
            DiffCodeView(review.originalFileContent, review.newFileContent)
        }
    }
}

/**
 * 代码差异视图
 * 用颜色标记新增、修改和删除的行
 */
@Composable
private fun DiffCodeView(original: String, modified: String) {
    val diffLines = calculateDiff(original, modified)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 150.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            state = rememberLazyListState(),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            items(diffLines) { line ->
                Text(
                    text = buildString {
                        append(if (line.isAdded) "+" else if (line.isRemoved) "-" else " ")
                        append(line.content)
                    },
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = when {
                            line.isAdded -> Color(0x404CAF50)  // 半透明绿色
                            line.isRemoved -> Color(0x40F44336) // 半透明红色
                            else -> Color(0xFF666666)
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * 简易行级差异计算
 */
private data class DiffLine(val content: String, val isAdded: Boolean, val isRemoved: Boolean)

private fun calculateDiff(original: String, modified: String): List<DiffLine> {
    val origLines = original.lines()
    val modLines = modified.lines()
    val result = mutableListOf<DiffLine>()
    
    val origSet = origLines.toSet()
    val modSet = modLines.toSet()
    
    // 添加的行
    modLines.forEach { line ->
        if (line !in origSet) {
            result.add(DiffLine(line, true, false))
        }
    }
    
    // 删除的行
    origLines.forEach { line ->
        if (line !in modSet) {
            result.add(DiffLine(line, false, true))
        }
    }
    
    return if (result.isEmpty()) {
        // 如果没有找到差异，显示所有修改的行
        modLines.forEach { line ->
            result.add(DiffLine(line, false, false))
        }
        result
    } else {
        result
    }
}

private fun formatTime(timestamp: Long): String {
    val seconds = (System.currentTimeMillis() - timestamp) / 1000
    return when {
        seconds < 60 -> "刚刚"
        seconds < 3600 -> "${seconds / 60}分钟前"
        else -> "${seconds / 3600}小时前"
    }
}