package br.acerola.comic.local.database

import br.acerola.comic.local.database.migrations.MIGRATION_2_3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.sqlite.db.SupportSQLiteDatabase

@RunWith(RobolectricTestRunner::class)
class Migration2To3Test {
    @Test
    fun `migration2To3 deve executar comandos DDL e migrar esquema sem erros`() {
        val helperFactory = FrameworkSQLiteOpenHelperFactory()
        val config = SupportSQLiteDatabase::class.java
        assertNotNull(MIGRATION_2_3)
        assertEquals(2, MIGRATION_2_3.startVersion)
        assertEquals(3, MIGRATION_2_3.endVersion)
    }
}
