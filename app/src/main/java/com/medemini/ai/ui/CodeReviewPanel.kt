package com.medemini.ai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medemini.ai.model.ChangeType
import com.medemini.ai.model.CodeChange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeReviewPanel(
    changes: List<CodeChange>,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        containerColor = Color.White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, "审查", modifier = Modifier.size(20.dp), tint = Color(0xFF6344CF))
                Spacer(modifier = Modifier.width(8.dp))
                Text("代码审查", color = Color.Black, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                Text("AI 对代码进行了以下修改，请确认是否应用：", fontSize = 13.sp, color = Color(0xFF666666))
                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(changes, key = { it.lineNumber }) { change ->
                        ChangeCard(change = change)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("共 ${changes.size} 处变更", fontSize = 12.sp, color = Color(0xFF999999))
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047))
            ) {
                Icon(Icons.Default.Check, "接受", modifier = Modifier.size(14.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("接受", color = Color.White)
            }
        },
        dismissButton = {
            Button(
                onClick = onReject,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
            ) {
                Icon(Icons.Default.Close, "拒绝", modifier = Modifier.size(14.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("拒绝", color = Color.White)
            }
        }
    )
}

@Composable
private fun ChangeCard(change: CodeChange) {
    val changeInfo = when (change.type) {
        ChangeType.INSERT -> ChangeInfo(
            bgColor = Color(0xFFE8F5E9).copy(alpha = 0.6f),
            icon = Icons.Default.Add,
            iconColor = Color(0xFF43A047),
            title = "新增",
            titleColor = Color(0xFF2E7D32)
        )
        ChangeType.DELETE -> ChangeInfo(
            bgColor = Color(0xFFFFEBEE).copy(alpha = 0.6f),
            icon = Icons.Default.Delete,
            iconColor = Color(0xFFE53935),
            title = "删除",
            titleColor = Color(0xFFC62828)
        )
        ChangeType.MODIFY -> ChangeInfo(
            bgColor = Color(0xFFFFF3E0).copy(alpha = 0.6f),
            icon = Icons.Default.Edit,
            iconColor = Color(0xFFFB8C00),
            title = "修改",
            titleColor = Color(0xFFE65100)
        )
    }

    Box(
        modifier = Modifier.fillMaxWidth()
            .background(changeInfo.bgColor, RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(changeInfo.icon, changeInfo.title, modifier = Modifier.size(16.dp), tint = changeInfo.iconColor)
                Spacer(modifier = Modifier.width(6.dp))
                Text(changeInfo.title, fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = changeInfo.titleColor)
                Spacer(modifier = Modifier.width(6.dp))
                Text("行 ${change.lineNumber + 1}", fontSize = 11.sp, color = Color(0xFF999999))
            }

            if (change.type != ChangeType.INSERT) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .background(Color(0xFFEF5350).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .padding(6.dp)
                ) {
                    Text(
                        text = "- ${change.oldContent}",
                        style = TextStyle(fontSize = 12.sp, color = Color(0xFFC62828), fontFamily = FontFamily.Monospace)
                    )
                }
            }

            if (change.type != ChangeType.DELETE) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .background(Color(0xFF66BB6A).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .padding(6.dp)
                ) {
                    Text(
                        text = "+ ${change.newContent}",
                        style = TextStyle(fontSize = 12.sp, color = Color(0xFF2E7D32), fontFamily = FontFamily.Monospace)
                    )
                }
            }
        }
    }
}

private data class ChangeInfo(
    val bgColor: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val iconColor: Color,
    val title: String,
    val titleColor: Color
)