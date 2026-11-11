package hu.kyokushin.trainer.core.data.db

import com.google.common.truth.Truth.assertThat
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

/**
 * A Room **exportált sémája** mint szerződés (DR-07, DR-08, FR-18).
 *
 * Miért az exportált JSON-t olvassuk, és nem egy futó adatbázist?
 * Mert a séma JSON az a műtermék, ami a migrációk ellenőrzésének és a jövőbeli
 * `SchemaValidation`-nak az alapja: ha egy entitás annotációja elromlik, a hiba itt jelenik
 * meg először. A fájl a `kspDebugKotlin` kimenete, ami a tesztfordítás előfeltétele, tehát
 * a teszt futásakor mindig a *jelenlegi* séma van a lemezen. Így a „nincs cascade a
 * tartalomról a felhasználói adatra" és az „egyedi index a dupla válasz ellen"
 * megszorítások Android-eszköz nélkül, JVM-en bizonyíthatók.
 *
 * Ezek az ellenőrzések a séma **alakját** mérik; a tartalomfrissítés és az előrehaladás
 * együttes viselkedését (TR-05) a T-118 in-memory adatbázison futó tesztjei mérik.
 */
class RoomSchemaContractTest {

    private companion object {
        /** A tartalomtáblák: a tudástár frissítésekor upserttel újraírhatók. */
        val CONTENT_TABLES = setOf(
            "category",
            "technique",
            "technique_alias",
            "synonym_group",
            "content_state",
        )

        /** A felhasználói táblák: tartalomfrissítéskor soha nem törlődnek (DR-07). */
        val USER_TABLES = setOf("quiz_session", "quiz_attempt", "study_review")
    }

    private val schema: JsonObject by lazy {
        val repoRoot = File(
            requireNotNull(System.getProperty("kyo.repoRoot")) {
                "A kyo.repoRoot rendszertulajdonság nincs beállítva – lásd core/data/build.gradle.kts."
            },
        )
        val file = File(
            repoRoot,
            "core/data/schemas/hu.kyokushin.trainer.core.data.db.KyokushinDatabase/" +
                "${KyokushinDatabase.VERSION}.json",
        )
        check(file.isFile) {
            "A Room sémaexport nem jött létre: ${file.absolutePath}. " +
                "A room.schemaLocation beállítását a core/data/build.gradle.kts tartalmazza."
        }
        Json.parseToJsonElement(file.readText()).jsonObject["database"]!!.jsonObject
    }

    private val entities: Map<String, JsonObject> by lazy {
        schema["entities"]!!.jsonArray.associate { entity ->
            val obj = entity.jsonObject
            obj["tableName"]!!.jsonPrimitive.content to obj
        }
    }

    @Test
    fun schema_isExportedForTheCurrentVersion_DR08() {
        assertThat(schema["version"]!!.jsonPrimitive.int).isEqualTo(KyokushinDatabase.VERSION)
        // Az identityHash a migrációellenőrzés alapja: léteznie kell az exportban.
        assertThat(schema["identityHash"]!!.jsonPrimitive.content).isNotEmpty()
    }

    @Test
    fun schema_containsEveryContentAndUserTable_DR04() {
        assertThat(entities.keys).containsExactlyElementsIn(CONTENT_TABLES + USER_TABLES)
    }

    // --- ADR-005: adatbázis-szintű védelem a dupla válasz ellen --------------------

    @Test
    fun quizAttempt_hasUniqueIndexOnSessionAndQuestion_FR18() {
        val uniqueIndices = entities.getValue("quiz_attempt")["indices"]!!.jsonArray
            .map { it.jsonObject }
            .filter { it["unique"]!!.jsonPrimitive.boolean() }
            .map { index -> index["columnNames"]!!.jsonArray.map { it.jsonPrimitive.content } }

        // Pontosan egy egyedi index van, és az a (sessionId, questionId) páron (ADR-005).
        // A `techniqueId` NEM lehet része: ugyanaz a technika egy munkameneten belül több
        // kérdésben is szerepelhet, az index mégsem akadályozhatja meg a rögzítést.
        assertThat(uniqueIndices).containsExactly(listOf("sessionId", "questionId"))
    }

    @Test
    fun quizAttempt_createSqlDeclaresTheUniqueIndex_FR18() {
        val createSql = entities.getValue("quiz_attempt")["indices"]!!.jsonArray
            .map { it.jsonObject }
            .single { it["unique"]!!.jsonPrimitive.boolean() }["createSql"]!!
            .jsonPrimitive.content

        // A tényleges DDL is UNIQUE INDEX: a megszorítás az adatbázisban él, nem csak a
        // Room metaadatában. (A tábla nevét a Room `${'$'}{TABLE_NAME}` helyőrzővel írja az
        // exportba, ezért az index nevére és az oszlopokra ellenőrizünk.)
        assertThat(createSql).contains("CREATE UNIQUE INDEX")
        assertThat(createSql).contains("index_quiz_attempt_sessionId_questionId")
        assertThat(createSql).contains("(`sessionId`, `questionId`)")
    }

    // --- ADR-004: nincs cascade a tartalomról a felhasználói adatra ----------------

    @Test
    fun userTables_haveNoForeignKeyToContentTables_DR07() {
        USER_TABLES.forEach { table ->
            val referencedTables = entities.getValue(table).foreignKeys()
                .map { it["table"]!!.jsonPrimitive.content }

            assertThat(referencedTables.filter { it in CONTENT_TABLES }).isEmpty()
        }
    }

    @Test
    fun userTables_techniqueReferenceIsPlainIndexedColumn_TR05() {
        // A `techniqueId` sima, indexelt TEXT oszlop idegen kulcs nélkül: a próbálkozás
        // és az önértékelés akkor is érvényes marad, ha a technika visszavont vagy egy
        // jövőbeli migráció fizikailag eltávolítja a tartalomtáblából (ADR-004, DR-06).
        listOf("quiz_attempt", "study_review").forEach { table ->
            val entity = entities.getValue(table)
            val column = entity.fields().single { it["columnName"]!!.jsonPrimitive.content == "techniqueId" }

            assertThat(column["affinity"]!!.jsonPrimitive.content).isEqualTo("TEXT")
            assertThat(column["notNull"]!!.jsonPrimitive.boolean()).isTrue()
            assertThat(entity.indexedColumnSets()).contains(listOf("techniqueId"))
        }
    }

    @Test
    fun contentTables_neverCascadeIntoUserTables_DR07() {
        // A megfordított irány is tilos: egyetlen idegen kulcs sem mutathat tartalomtáblából
        // felhasználói táblára, bármilyen törlési szabállyal.
        CONTENT_TABLES.forEach { table ->
            val referenced = entities.getValue(table).foreignKeys()
                .map { it["table"]!!.jsonPrimitive.content }

            assertThat(referenced.filter { it in USER_TABLES }).isEmpty()
        }
    }

    @Test
    fun technique_referencesCategoryWithoutCascade_ADR004() {
        val foreignKey = entities.getValue("technique").foreignKeys().single()

        assertThat(foreignKey["table"]!!.jsonPrimitive.content).isEqualTo("category")
        // Egy kategória eltüntetése nem törölhet technikákat: a visszavonás jelölés
        // (`retired = true`), nem törlés (ADR-004).
        assertThat(foreignKey["onDelete"]!!.jsonPrimitive.content).isEqualTo("NO ACTION")
    }

    @Test
    fun quizAttempt_cascadesOnlyFromItsOwnSession_DR07() {
        val foreignKey = entities.getValue("quiz_attempt").foreignKeys().single()

        // Az egyetlen cascade felhasználói adat → felhasználói adat irányban van: egy menet
        // nélküli válasz értelmezhetetlen. A tartalomra ez nem vonatkozik.
        assertThat(foreignKey["table"]!!.jsonPrimitive.content).isEqualTo("quiz_session")
        assertThat(foreignKey["columns"]!!.jsonArray.map { it.jsonPrimitive.content })
            .containsExactly("sessionId")
        assertThat(foreignKey["onDelete"]!!.jsonPrimitive.content).isEqualTo("CASCADE")
    }

    // --- Oszloptípusok: Instant → INTEGER, enum → TEXT ----------------------------

    @Test
    fun schema_storesInstantsAsIntegerEpochMilli_DR08() {
        val instantColumns = mapOf(
            "quiz_session" to listOf("startedAt", "finishedAt"),
            "quiz_attempt" to listOf("answeredAt"),
            "study_review" to listOf("reviewedAt"),
            "content_state" to listOf("importedAt"),
        )

        instantColumns.forEach { (table, columns) ->
            columns.forEach { column ->
                assertThat(entities.getValue(table).affinityOf(column)).isEqualTo("INTEGER")
            }
        }
    }

    @Test
    fun schema_storesEnumsAsTextNotOrdinal_DR08() {
        val enumColumns = mapOf(
            "technique" to listOf("status", "illustrationKind"),
            "quiz_attempt" to listOf("questionType"),
            "study_review" to listOf("selfRating"),
            "content_state" to listOf("datasetTrust"),
        )

        enumColumns.forEach { (table, columns) ->
            columns.forEach { column ->
                // INTEGER affinitás itt ordinal-tárolást jelentene – az enum-értékek
                // átrendezése csendben átértelmezné a meglévő sorokat.
                assertThat(entities.getValue(table).affinityOf(column)).isEqualTo("TEXT")
            }
        }
    }

    @Test
    fun schema_identifiersAreTextAndGeneratedKeysAreInteger_DR04() {
        // A menet azonosítóját a domain adja (SessionId → TEXT), a próbálkozás és az
        // önértékelés kulcsát az adatbázis (autoGenerate → INTEGER).
        assertThat(entities.getValue("quiz_session").affinityOf("id")).isEqualTo("TEXT")
        assertThat(entities.getValue("quiz_attempt").affinityOf("id")).isEqualTo("INTEGER")
        assertThat(entities.getValue("study_review").affinityOf("id")).isEqualTo("INTEGER")

        listOf("quiz_attempt", "study_review").forEach { table ->
            val primaryKey = entities.getValue(table)["primaryKey"]!!.jsonObject
            assertThat(primaryKey["autoGenerate"]!!.jsonPrimitive.boolean()).isTrue()
        }
    }

    @Test
    fun technique_hasNoQuizEligibleColumn_ADR010() {
        // A kvízjogosultság származtatott érték, nem tárolt adat: egy elavult `true`
        // kvízbe engedhetne hiányos rekordot (ADR-010).
        val columns = entities.getValue("technique").fields()
            .map { it["columnName"]!!.jsonPrimitive.content }

        assertThat(columns).doesNotContain("quizEligible")
        assertThat(columns).containsAtLeast("status", "quizExcluded", "retired")
    }

    @Test
    fun techniqueTable_hasThePreNormalizedSearchColumns_FR03() {
        val columns = entities.getValue("technique").fields()
            .map { it["columnName"]!!.jsonPrimitive.content }

        // A keresés ezeken az oszlopokon fut `LIKE '%…%'`-kal (ADR-012). Ha a sémából
        // kiesnének, a keresés csendben nem találna semmit – ezért szerződés.
        assertThat(columns).containsAtLeast("search_romaji", "search_hungarian")
    }

    // --- DR-08: destruktív migráció tilalma, nincs főszálú lekérdezés -------------

    @Test
    fun dataModule_neverFallsBackToDestructiveMigration_DR08() {
        val banned = listOf("fallbackToDestructiveMigration", "allowMainThreadQueries")

        val offenders = kotlinSources().flatMap { file ->
            file.readText().lines()
                .map { it.trim() }
                // A KDoc és a sorkommentek maguk is megnevezik a tilalmat (épp azt
                // dokumentálják, miért tilos) – a kódhívást keressük, nem a szót.
                .filterNot { it.startsWith("*") || it.startsWith("//") || it.startsWith("/*") }
                .filter { line -> banned.any { line.contains("$it(") } }
                .map { "${file.name}: $it" }
        }

        assertThat(offenders).isEmpty()
    }

    private fun kotlinSources(): List<File> {
        val repoRoot = File(requireNotNull(System.getProperty("kyo.repoRoot")) { "kyo.repoRoot" })
        return listOf("main", "test")
            .map { File(repoRoot, "core/data/src/$it/kotlin") }
            .filter { it.isDirectory }
            .flatMap { it.walkTopDown().filter { file -> file.extension == "kt" }.toList() }
    }

    // --- JSON segédek -------------------------------------------------------------

    private fun JsonObject.fields(): List<JsonObject> =
        this["fields"]!!.jsonArray.map { it.jsonObject }

    private fun JsonObject.foreignKeys(): List<JsonObject> =
        this["foreignKeys"]?.jsonArray?.map { it.jsonObject } ?: emptyList()

    private fun JsonObject.indexedColumnSets(): List<List<String>> =
        this["indices"]?.jsonArray.orEmpty().map { index ->
            index.jsonObject["columnNames"]!!.jsonArray.map { it.jsonPrimitive.content }
        }

    private fun JsonObject.affinityOf(columnName: String): String =
        fields().single { it["columnName"]!!.jsonPrimitive.content == columnName }
            .let { it["affinity"]!!.jsonPrimitive.content }

    private fun kotlinx.serialization.json.JsonPrimitive.boolean(): Boolean =
        requireNotNull(booleanOrNull) { "Logikai értéket vártam, ez jött: $this" }
}
