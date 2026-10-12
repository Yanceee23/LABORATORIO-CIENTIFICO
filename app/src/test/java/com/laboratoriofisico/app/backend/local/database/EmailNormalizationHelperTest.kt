package com.laboratoriofisico.app.backend.local.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmailNormalizationHelperTest {

    @Test
    fun normalize_trimsAndLowercases() {
        assertEquals("ana@test.com", EmailNormalizationHelper.normalize("  Ana@Test.COM "))
    }

    @Test
    fun deduplicate_keepsLowestIdAndReportsDuplicates() {
        val result = EmailNormalizationHelper.deduplicate(
            listOf(
                EmailNormalizationHelper.NormalizedEmail(3, " Ana@Test.com ", "ana@test.com"),
                EmailNormalizationHelper.NormalizedEmail(1, "ana@test.com", "ana@test.com"),
                EmailNormalizationHelper.NormalizedEmail(2, "ANA@TEST.COM", "ana@test.com"),
                EmailNormalizationHelper.NormalizedEmail(4, "otra@test.com", "otra@test.com")
            )
        )
        assertEquals(1L to "ana@test.com", result.updates.first { it.first == 1L })
        assertEquals(4L to "otra@test.com", result.updates.first { it.first == 4L })
        assertTrue(result.updates.any { it.first == 2L && it.second.contains("+dup") })
        assertTrue(result.updates.any { it.first == 3L && it.second.contains("+dup") })
        assertEquals(2, result.duplicateReport.size)
    }
}

class Migration11To12Test {

    @Test
    fun migration_versionsAreCorrect() {
        assertEquals(11, MIGRATION_11_12.startVersion)
        assertEquals(12, MIGRATION_11_12.endVersion)
    }

    @Test
    fun dedupLogic_doesNotDropUsers() {
        val emails = listOf(
            EmailNormalizationHelper.NormalizedEmail(10, "a@x.com", "a@x.com"),
            EmailNormalizationHelper.NormalizedEmail(11, "A@x.com", "a@x.com")
        )
        val result = EmailNormalizationHelper.deduplicate(emails)
        assertEquals(2, result.updates.size)
        assertTrue(result.duplicateReport.isNotEmpty())
    }
}
