package com.nothingness.bruhpatcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothingness.bruhpatcher.model.LogEntry
import com.nothingness.bruhpatcher.model.LogTag
import com.nothingness.bruhpatcher.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TerminalLog(
    logs: List<LogEntry>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new logs arrive
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.TerminalBackground)
            .padding(12.dp)
    ) {
        LazyColumn(state = listState) {
            items(logs) { entry ->
                LogLine(entry)
            }
        }
    }
}

@Composable
private fun LogLine(entry: LogEntry) {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val timestamp = timeFormat.format(Date(entry.timestamp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .horizontalScroll(rememberScrollState())
    ) {
        // Timestamp
        Text(
            text = timestamp,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = AppColors.TextMuted
        )
        Spacer(modifier = Modifier.width(8.dp))

        // Tag
        Text(
            text = "[${entry.tag.displayName}]",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = getTagColor(entry.tag)
        )
        Spacer(modifier = Modifier.width(8.dp))

        // Message
        Text(
            text = entry.message,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = AppColors.TerminalText
        )
    }
}

private fun getTagColor(tag: LogTag) = when (tag) {
    LogTag.INFO -> AppColors.TerminalInfo
    LogTag.EXTRACT -> AppColors.TerminalExtract
    LogTag.UPLOAD -> AppColors.TerminalUpload
    LogTag.REMOTE -> AppColors.TerminalRemote
    LogTag.WAITING -> AppColors.TerminalWarning
    LogTag.DOWNLOAD -> AppColors.TerminalDownload
    LogTag.INSTALL -> AppColors.Primary
    LogTag.SUCCESS -> AppColors.TerminalSuccess
    LogTag.ERROR -> AppColors.TerminalError
    LogTag.WARN -> AppColors.TerminalWarning
    // Local patching tags
    LogTag.DI -> AppColors.TerminalInfo
    LogTag.PATCH -> AppColors.Primary
    LogTag.MODULE -> AppColors.Success
}

@Composable
fun StatusBanner(
    title: String,
    subtitle: String? = null,
    isError: Boolean = false,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isError) AppColors.Error.copy(alpha = 0.15f) else AppColors.Primary.copy(alpha = 0.15f)
    val textColor = if (isError) AppColors.Error else AppColors.Primary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary
            )
        }
    }
}
