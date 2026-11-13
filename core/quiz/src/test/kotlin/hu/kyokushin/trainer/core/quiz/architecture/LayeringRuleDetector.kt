package hu.kyokushin.trainer.core.quiz.architecture

import java.io.File

/**
 * A TR-07 forrásellenőrzésének szabálylogikája, elválasztva a fájlrendszer-bejárástól.
 *
 * Miért külön objektum, és miért sima `String`-eken dolgozik? Mert így a szabályok
 * önmagukban, szintetikus bemeneten is bizonyíthatók (lásd [LayeringRuleDetectorTest]) –
 * ez adja a task „Kész, ha" pontjának („a teszt elbukik, ha valaki véletlenül behúz egy
 * tiltott importot") a tényleges bizonyítását, nem csak azt, hogy a mai forrás éppen
 * tiszta. A valódi forrásfák ellenőrzését a [LayeringRuleTest] végzi, ugyanezekkel a
 * függvényekkel.
 */
internal object LayeringRuleDetector {
    private val FORBIDDEN_IMPORT_KEYWORDS = listOf("android.", "androidx.", "room", "compose")
    private val FORBIDDEN_RUNTIME_CALLS =
        listOf("Random.Default", "System.currentTimeMillis", "Instant.now", "Math.random")
    private val FORBIDDEN_TYPE_SUFFIXES = listOf("Entity", "Dao", "Dto")
    private val BLOCK_COMMENT_REGEX = Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL)

    /** Egy `core/model`/`core/quiz` forrásfájlban tiltott import-sorok (NFR-03, ADR-001 L1). */
    fun forbiddenImportsIn(sourceText: String): List<String> =
        importLinesIn(sourceText).filter { importLine ->
            FORBIDDEN_IMPORT_KEYWORDS.any { keyword -> importLine.contains(keyword, ignoreCase = true) }
        }

    /**
     * Egy `core/model`/`core/quiz` forrásfájlban tiltott, közvetlen rendszeróra-/
     * véletlenhívás (NFR-07) – a KDoc/sorvégi megjegyzésekben szereplő, dokumentáló
     * célú mondatokat (lásd pl. `QuizGenerator.kt`) a hívás előtt eltávolítjuk, különben
     * a modul saját, a hiányt magyarázó KDoc-ja buktatná el a keresést.
     */
    fun forbiddenRuntimeCallsIn(sourceText: String): List<String> {
        val code = stripComments(sourceText)
        return FORBIDDEN_RUNTIME_CALLS.filter { call -> code.contains(call) }
    }

    /** Egy feature modulbeli forrásfájlban Room entitás/DAO/DTO típusra mutató import (NFR-03, ADR-001 L5). */
    fun forbiddenDataLayerImportsIn(sourceText: String): List<String> =
        importLinesIn(sourceText).filter { importLine -> isForbiddenDataLayerImport(importLine) }

    fun mainSourceFiles(repoRoot: File, modules: List<String>): List<File> =
        modules.flatMap { module ->
            File(repoRoot, "$module/src/main/kotlin")
                .walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .toList()
        }

    private fun isForbiddenDataLayerImport(importLine: String): Boolean {
        if (importLine.contains("androidx.room", ignoreCase = true)) return true
        val importedName = importLine.removePrefix("import").trim().removeSuffix(";").trim()
        val simpleName = importedName.substringAfterLast('.')
        return FORBIDDEN_TYPE_SUFFIXES.any { suffix -> simpleName.endsWith(suffix) }
    }

    private fun importLinesIn(sourceText: String): List<String> =
        sourceText.lineSequence().map { it.trim() }.filter { it.startsWith("import ") }.toList()

    private fun stripComments(sourceText: String): String {
        val withoutBlockComments = BLOCK_COMMENT_REGEX.replace(sourceText, "")
        return withoutBlockComments.lineSequence().joinToString("\n") { it.substringBefore("//") }
    }
}
