package com.medemini.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WelcomeScreen(
    onOpenProject: () -> Unit,
    onOpenFile: () -> Unit,
    recentFiles: List<RecentFile>,
    onRecentSelected: (String, String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color.White).verticalScroll(androidx.compose.foundation.rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(96.dp).background(Color(0xFF1A1A1A), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Code, null, modifier = Modifier.size(48.dp), tint = Color.White)
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text("MedeMini", style = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.Black))
        Text("代码编辑器", style = TextStyle(fontSize = 15.sp, color = Color(0xFF666666)))

        Spacer(modifier = Modifier.height(56.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            WelcomeButton(
                icon = Icons.Default.FolderOpen,
                label = "打开项目",
                onClick = onOpenProject,
                backgroundColor = Color(0xFF1A1A1A),
                textColor = Color.White
            )
            WelcomeButton(
                icon = Icons.Default.Description,
                label = "打开文件",
                onClick = onOpenFile,
                backgroundColor = Color.White,
                textColor = Color(0xFF1A1A1A),
                border = true
            )
        }

        AnimatedVisibility(visible = recentFiles.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 64.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("最近文件", style = TextStyle(fontSize = 16.sp, color = Color.Black, fontWeight = FontWeight.SemiBold))
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.History, null, modifier = Modifier.size(18.dp), tint = Color(0xFF999999))
                }

                Spacer(modifier = Modifier.height(20.dp))

                LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(recentFiles.take(6), key = { it.path }) { file ->
                        RecentFileCard(file = file, onClick = { onRecentSelected(file.path, file.name) })
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
private fun WelcomeButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    backgroundColor: Color,
    textColor: Color,
    border: Boolean = false
) {
    Box(
        modifier = Modifier.height(56.dp).background(backgroundColor, RoundedCornerShape(16.dp))
            .let { if (border) it.border(1.5.dp, Color(0xFFE0E0E0), RoundedCornerShape(16.dp)) else it }
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.padding(end = 10.dp), tint = textColor)
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = textColor)
        }
    }
}

@Composable
private fun RecentFileCard(file: RecentFile, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp).background(Color(0xFFFAFAFA), RoundedCornerShape(12.dp))
            .clickable { onClick() }.padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).background(Color(0xFFF0F0F0), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Description, null, tint = Color(0xFF666666), modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(file.name, style = TextStyle(fontSize = 15.sp, color = Color.Black, fontWeight = FontWeight.Medium), maxLines = 1)
            Text(file.path, style = TextStyle(fontSize = 12.sp, color = Color(0xFF999999)), maxLines = 1)
        }
        Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(18.dp), tint = Color(0xFFCCCCCC))
    }
}