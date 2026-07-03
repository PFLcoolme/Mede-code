package com.medemini.ai.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medemini.ai.model.AIMessage
import com.medemini.ai.viewmodel.AIViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingAIAssistant(
    viewModel: AIViewModel,
    isOpen: Boolean,
    onToggle: () -> Unit,
    onSettings: () -> Unit
) {
    val messages = viewModel.messages
    val isLoading = viewModel.isLoading.value
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(messages.size - 1)
        }
    }

    AnimatedVisibility(
        visible = isOpen,
        enter = slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ),
        exit = slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(250)
        )
    ) {
        Box(modifier = Modifier.fillMaxHeight().width(280.dp).background(Color.White.copy(alpha = 0.35f))) {
            Column(modifier = Modifier.fillMaxHeight()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(26.dp).background(Color(0xFF333333), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SmartToy, null, modifier = Modifier.size(14.dp), tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI 助手", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A)))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = onSettings, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Settings, "设置", modifier = Modifier.size(14.dp), tint = Color(0xFF666666))
                        }
                        IconButton(onClick = onToggle, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, "关闭", modifier = Modifier.size(14.dp), tint = Color(0xFF666666))
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFDDDDDD).copy(alpha = 0.3f)))

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (messages.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.SmartToy, null, modifier = Modifier.size(40.dp), tint = Color(0xFF888888))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("欢迎使用 AI 助手", style = TextStyle(fontSize = 13.sp, color = Color(0xFF555555)))
                                Text("输入问题开始对话", style = TextStyle(fontSize = 11.sp, color = Color(0xFF888888)))
                            }
                        }
                    } else {
                        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(6.dp)) {
                            items(messages, key = { it.id }) { message ->
                                MessageBubble(message = message)
                            }
                            if (isLoading) {
                                item {
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.Start) {
                                        Box(
                                            modifier = Modifier.size(20.dp).background(Color(0xFF333333), RoundedCornerShape(6.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.SmartToy, null, modifier = Modifier.size(10.dp), tint = Color.White)
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(modifier = Modifier.background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(10.dp)).padding(10.dp)) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                repeat(3) {
                                                    Box(
                                                        modifier = Modifier.size(5.dp).background(Color(0xFF888888), RoundedCornerShape(3.dp))
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFDDDDDD).copy(alpha = 0.3f)))

                Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    var inputText by remember { mutableStateOf(TextFieldValue()) }

                    BasicTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f).height(40.dp)
                            .background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp),
                        textStyle = TextStyle(fontSize = 13.sp, color = Color(0xFF1A1A1A)),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                                if (inputText.text.isEmpty()) {
                                    Text("输入消息...", style = TextStyle(fontSize = 13.sp, color = Color(0xFF888888)))
                                }
                                innerTextField()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier.size(40.dp).background(Color(0xFF333333), RoundedCornerShape(20.dp))
                            .clickable {
                                if (inputText.text.isNotEmpty() && !isLoading) {
                                    viewModel.sendMessage(inputText.text)
                                    inputText = TextFieldValue()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Send, "发送", modifier = Modifier.size(14.dp), tint = Color.White)
                    }
                }
            }
        }
    }

    AnimatedVisibility(
        visible = !isOpen,
        enter = fadeIn(animationSpec = tween(200)),
        exit = fadeOut(animationSpec = tween(150))
    ) {
        Box(
            modifier = Modifier.fillMaxHeight().width(56.dp).background(Color.White.copy(alpha = 0.35f))
                .clickable { onToggle() },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.SmartToy, "AI助手", modifier = Modifier.size(24.dp), tint = Color(0xFF333333))
                Spacer(modifier = Modifier.height(4.dp))
                Text("AI", fontSize = 10.sp, color = Color(0xFF666666))
            }
        }
    }
}

@Composable
private fun MessageBubble(message: AIMessage) {
    val isUser = message.role == "user"
    val isSystem = message.role == "system"

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier.size(20.dp).background(if (isSystem) Color(0xFF999999) else Color(0xFF333333), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isSystem) Icons.Default.Info else Icons.Default.SmartToy,
                    null, modifier = Modifier.size(10.dp), tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        val bgColor = when {
            isUser -> Color(0xFF333333)
            isSystem -> Color(0xFFDDDDDD).copy(alpha = 0.5f)
            else -> Color.White.copy(alpha = 0.5f)
        }

        val textColor = when {
            isUser -> Color.White
            isSystem -> Color(0xFF555555)
            else -> Color(0xFF1A1A1A)
        }

        Box(
            modifier = Modifier.then(Modifier.width(240.dp))
                .background(bgColor, RoundedCornerShape(12.dp))
                .padding(8.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (message.content.isNotEmpty()) {
                    Text(
                        message.content,
                        style = TextStyle(fontSize = 12.sp, color = textColor, lineHeight = 16.sp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (message.toolCalls != null && message.toolCalls.isNotEmpty()) {
                    message.toolCalls.forEach { toolCall ->
                        Box(
                            modifier = Modifier.fillMaxWidth().background(Color(0xFFEEEEEE).copy(alpha = 0.5f), RoundedCornerShape(6.dp)).padding(4.dp)
                        ) {
                            Column {
                                Text(
                                    "工具调用: ${toolCall.function.name}",
                                    style = TextStyle(fontSize = 10.sp, color = Color(0xFF555555))
                                )
                                toolCall.function.arguments.forEach { (key, value) ->
                                    Text(
                                        "$key: $value",
                                        style = TextStyle(fontSize = 9.sp, color = Color(0xFF888888))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}