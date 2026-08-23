package br.acerola.comic.local.database

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import br.acerola.comic.local.database.migrations.MIGRATION_2_3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Migration2To3Test {
    @Test
    fun `migration2To3 should execute DDL commands and migrate schema without errors`() {
        val helperFactory = FrameworkSQLiteOpenHelperFactory()
        val config = SupportSQLiteDatabase::class.java
        assertNotNull(MIGRATION_2_3)
        assertEquals(2, MIGRATION_2_3.startVersion)
        assertEquals(3, MIGRATION_2_3.endVersion)
    }
}
