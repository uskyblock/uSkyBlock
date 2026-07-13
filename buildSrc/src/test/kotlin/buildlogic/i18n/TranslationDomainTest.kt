package buildlogic.i18n

import buildlogic.i18n.TranslationDomain.ADMIN_OPS
import buildlogic.i18n.TranslationDomain.PLAYER_FACING
import buildlogic.i18n.TranslationDomain.SYSTEM_DEBUG
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TranslationDomainTest {
    private val base = "uSkyBlock-Core/src/main/java/us/talabrek/ultimateskyblock"

    @Test
    fun `debug and import commands are system-debug`() {
        assertEquals(SYSTEM_DEBUG, TranslationDomain.classifyReferencePath("$base/command/admin/DebugCommand.java"))
        assertEquals(SYSTEM_DEBUG, TranslationDomain.classifyReferencePath("$base/imports/USBImporterExecutor.java"))
    }

    @Test
    fun `other admin commands are admin-ops`() {
        assertEquals(ADMIN_OPS, TranslationDomain.classifyReferencePath("$base/command/admin/RemoveCommand.java"))
    }

    @Test
    fun `everything else is player-facing`() {
        assertEquals(PLAYER_FACING, TranslationDomain.classifyReferencePath("$base/handler/WorldGuardHandler.java"))
    }

    @Test
    fun `windows-style backslash paths are normalized`() {
        assertEquals(ADMIN_OPS, TranslationDomain.classifyReferencePath("$base\\command\\admin\\RemoveCommand.java"))
    }

    @Test
    fun `an entry takes the highest-precedence domain among its references`() {
        val entry = PoEntry(
            comments = listOf(
                "#: $base/command/admin/DebugCommand.java",
                "#: $base/handler/WorldGuardHandler.java",
            ),
            msgid = "shared",
            msgstr = "",
        )
        assertEquals(PLAYER_FACING, TranslationDomain.classifyEntry(entry))
    }

    @Test
    fun `an entry with no references defaults to player-facing`() {
        assertEquals(PLAYER_FACING, TranslationDomain.classifyEntry(PoEntry(emptyList(), "x", "")))
    }
}
