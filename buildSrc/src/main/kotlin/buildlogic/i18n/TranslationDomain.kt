package buildlogic.i18n

/**
 * The translation domains uSkyBlock splits its message catalog into. Each extracted message is
 * routed to exactly one domain by the source file it is referenced from; the domain with the
 * highest [precedence] wins when a message is referenced from more than one file.
 */
enum class TranslationDomain(val id: String, val precedence: Int) {
    PLAYER_FACING("player_facing", 3),
    ADMIN_OPS("admin_ops", 2),
    SYSTEM_DEBUG("system_debug", 1);

    companion object {
        /** Domains in declaration order, i.e. the order their catalogs are concatenated when merged. */
        val ordered: List<TranslationDomain> = entries.toList()

        private val systemDebugSuffixes = listOf(
            "command/admin/DebugCommand.java",
            "command/admin/GetIslandDataCommand.java",
            "command/admin/SetIslandDataCommand.java",
            "command/admin/ItemInfoCommand.java",
            "command/admin/ImportCommand.java",
            "command/admin/FlushCommand.java",
            "command/admin/task/PurgeTask.java",
            "command/admin/task/PurgeScanTask.java",
            "command/admin/task/ProtectAllTask.java",
            "imports/USBImporterExecutor.java",
            "bukkit-utils/src/main/java/dk/lockfuglsang/minecraft/command/DocumentCommand.java",
            "bukkit-utils/src/main/java/dk/lockfuglsang/minecraft/command/PlainTextCommandVisitor.java",
        )

        /** Classifies a single `#:` reference path into the domain that owns it. */
        fun classifyReferencePath(referencePath: String): TranslationDomain {
            val normalized = referencePath.replace('\\', '/')
            if (systemDebugSuffixes.any { normalized.endsWith(it) }) {
                return SYSTEM_DEBUG
            }
            if (normalized.contains("/command/admin/")) {
                return ADMIN_OPS
            }
            return PLAYER_FACING
        }

        /**
         * Classifies a whole `.pot` entry (its raw text, including `#:` reference comments) into a
         * domain. An entry with no references defaults to [PLAYER_FACING]; otherwise the highest
         * precedence among its referenced domains wins.
         */
        fun classifyEntry(entry: PoEntry): TranslationDomain {
            val referenced = entry.references.map { classifyReferencePath(it) }
            return referenced.maxByOrNull { it.precedence } ?: PLAYER_FACING
        }
    }
}
