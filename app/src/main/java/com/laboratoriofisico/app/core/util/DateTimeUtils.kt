package com.laboratoriofisico.app.core.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateTimeUtils {
    private val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    fun format(timestamp: Long): String = formatter.format(Date(timestamp))

    fun formatDuration(ms: Long): String {
        val totalSeconds = (ms / 1_000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return when {
            minutes > 0 -> "${minutes} min ${seconds} s"
            else -> "${seconds} s"
        }
    }
}
