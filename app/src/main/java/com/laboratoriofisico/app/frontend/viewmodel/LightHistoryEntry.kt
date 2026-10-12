package com.laboratoriofisico.app.frontend.viewmodel

import com.laboratoriofisico.app.sensores.util.LightClassifier
import com.laboratoriofisico.app.sensores.util.LightLevel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class LightHistoryEntry(
    val timestamp: Long,
    val value: Double,
    val unit: String,
    val level: LightLevel
) {
    fun formatLine(): String {
        val time = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.getDefault())
            .format(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()))
        val valueText = if (unit.equals("raw", ignoreCase = true)) {
            "%.0f".format(value)
        } else {
            "%.0f".format(value)
        }
        return "$time | $valueText $unit | ${LightClassifier.levelDisplayName(level)}"
    }
}
