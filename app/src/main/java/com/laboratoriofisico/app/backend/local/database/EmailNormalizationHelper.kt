package com.laboratoriofisico.app.backend.local.database

/**
 * Lógica pura de normalización/deduplicación de correos para migración y tests JVM.
 */
object EmailNormalizationHelper {
    data class NormalizedEmail(
        val userId: Long,
        val original: String,
        val normalized: String
    )

    data class DedupResult(
        val updates: List<Pair<Long, String>>,
        val duplicateReport: List<String>
    )

    fun normalize(email: String): String = email.trim().lowercase()

    /**
     * Conserva el usuario con menor id para cada email normalizado.
     * A los demás les añade sufijo +dupN antes del @ para no romper el índice único.
     */
    fun deduplicate(emails: List<NormalizedEmail>): DedupResult {
        val updates = mutableListOf<Pair<Long, String>>()
        val report = mutableListOf<String>()
        val byNorm = emails.groupBy { it.normalized.ifBlank { "sin-correo-${it.userId}" } }

        byNorm.forEach { (norm, group) ->
            val sorted = group.sortedBy { it.userId }
            val keeper = sorted.first()
            updates.add(keeper.userId to norm)
            sorted.drop(1).forEachIndexed { index, dup ->
                val local = norm.substringBefore("@").ifBlank { "usuario" }
                val domain = norm.substringAfter("@", missingDelimiterValue = "laboratorio.local")
                    .ifBlank { "laboratorio.local" }
                val newEmail = "$local+dup${index + 1}@$domain"
                updates.add(dup.userId to newEmail)
                report.add("Duplicado: id=${dup.userId} '${dup.original}' → '$newEmail' (se mantuvo id=${keeper.userId})")
            }
        }
        return DedupResult(updates = updates, duplicateReport = report)
    }
}
