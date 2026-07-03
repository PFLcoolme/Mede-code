package com.medemini.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.medemini.model.EditorFile

@Composable
fun TopStatusBar(file: EditorFile?) {
    val language = file?.language?.name ?: "Plain Text"
    val lineCount = file?.content?.lines()?.size ?: 0
    val charCount = file?.content?.length ?: 0
    val encoding = "UTF-8"

    Row(
        modifier = Modifier.fillMaxWidth().height(28.dp).background(Color.White.copy(alpha = 0.35f)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Default.Code, null, modifier = Modifier.size(12.dp), tint = Color(0xFF555555))
            Text(language, style = TextStyle(fontSize = 11.sp, color = Color(0xFF222222), fontWeight = FontWeight.Medium))
            
            Box(modifier = Modifier.width(1.dp).height(12.dp).background(Color(0xFFCCCCCC).copy(alpha = 0.5f)))
            
            Icon(Icons.Default.TextFields, null, modifier = Modifier.size(12.dp), tint = Color(0xFF555555))
            Text("$lineCount 行", style = TextStyle(fontSize = 11.sp, color = Color(0xFF222222)))
            
            Box(modifier = Modifier.width(1.dp).height(12.dp).background(Color(0xFFCCCCCC).copy(alpha = 0.5f)))
            
            Text("$charCount 字符", style = TextStyle(fontSize = 11.sp, color = Color(0xFF222222)))
            
            Box(modifier = Modifier.width(1.dp).height(12.dp).background(Color(0xFFCCCCCC).copy(alpha = 0.5f)))
            
            Icon(Icons.Default.Language, null, modifier = Modifier.size(12.dp), tint = Color(0xFF555555))
            Text(encoding, style = TextStyle(fontSize = 11.sp, color = Color(0xFF222222)))
        }
    }
}