package hu.kyokushin.trainer.core.data.content

import hu.kyokushin.trainer.core.quiz.validation.CategoryInput
import hu.kyokushin.trainer.core.quiz.validation.IllustrationInput
import hu.kyokushin.trainer.core.quiz.validation.KnowledgeInput
import hu.kyokushin.trainer.core.quiz.validation.ManifestInput
import hu.kyokushin.trainer.core.quiz.validation.TechniqueInput

/*
 * A kanonikus séma DTO-i → a validátor bemenete (ADR-011, DR-05).
 *
 * Ez a leképezés **az egyetlen** hely, ahol a `snake_case` tartalomséma és a validátor
 * `camelCase` bemeneti típusai találkoznak. Szándékosan egyirányú és mellékhatásmentes:
 * nem validál, nem normalizál, nem pótol hiányzó értéket. A hibás tartalomnak
 * reprezentálhatónak kell maradnia, különben a hiba már itt kivételt dobna, és nem
 * kerülhetne be a jelentésbe (FR-31).
 *
 * **Tudatos szinkron:** a `core/quiz` TR-01 tesztkészlete ugyanezt a leképezést végzi a
 * saját fixture-jében (`RealKnowledgeBaseFixture.kt`), hogy a validátor a valódi
 * tartalmon is lefuthasson. A két leképezés eltérése maga is hiba lenne – a
 * `RealKnowledgeContentReaderTest` ezért ugyanazokat az elvárásokat állítja, mint a
 * `core/quiz` oldali `RealKnowledgeBaseValidationTest`.
 */

/** A `review_status` egyetlen olyan értéke, amely hitelesítést jelent (ADR-011). */
internal const val REVIEW_STATUS_REVIEWED: String = "reviewed"

/**
 * A manifest nyers alakja a validátor bemeneti alakjára.
 *
 * Két tudatos eltérés a mezőnevek átírásán túl:
 *
 * 1. `dataset_version` → [ManifestInput.contentVersion]: a validátor szótárában a
 *    *tartalomkiadás* verziója a `contentVersion`, a séma verziója a `schemaVersion`.
 * 2. `editorial_status` → [ManifestInput.datasetKind]: a bizalmi szint a **szerkesztői
 *    állapotból** jön, nem a `dataset_trust` önbevallásából – a tartalom nem emelheti a
 *    saját bizalmi szintjét (CR-02).
 */
internal fun ManifestDto.toManifestInput(): ManifestInput = ManifestInput(
    schemaVersion = schemaVersion,
    contentVersion = datasetVersion,
    datasetKind = editorialStatus,
    categories = categories.map { it.toCategoryInput() },
    illustrations = illustrations.map { it.toIllustrationInput() },
)

/** Egy kategória nyers alakja a validátor bemeneti alakjára. */
internal fun CategoryDto.toCategoryInput(): CategoryInput = CategoryInput(
    id = id,
    nameHu = nameHu,
    sortOrder = sortOrder,
)

/**
 * Egy illusztráció-nyilvántartási bejegyzés a validátor bemeneti alakjára.
 *
 * A technikai metaadatok (`content_type`, `width`, `height`, `sha256`, `origin`,
 * `review_status`, `has_embedded_label`) nem kerülnek át: a validátor szabályai közül
 * egyik sem támaszkodik rájuk. A `has_embedded_label` hatása már *le van képezve* a
 * [IllustrationDto.quizUsable] és [IllustrationDto.hideableOnStudyCard] kapukra – a
 * kettős igazság elkerülése végett a kapukat adjuk át, nem a megfigyelést (ADR-011).
 */
internal fun IllustrationDto.toIllustrationInput(): IllustrationInput = IllustrationInput(
    ref = ref,
    file = file,
    license = license,
    source = source,
    author = author,
    quizUsable = quizUsable,
    hideableOnStudyCard = hideableOnStudyCard,
)

/**
 * Egy technikarekord nyers alakja a validátor bemeneti alakjára.
 *
 * Amit a leképezés **nem** ad át, és miért:
 *
 * | Fájlmező | Miért marad ki |
 * |---|---|
 * | `japanese_romaji_ascii` | származtatott érték; a normalizálás szabálya egy helyen, a validátorban él |
 * | `search_romaji`, `search_hungarian` | ugyanaz: előre normalizált keresési alak, nem szerkesztett adat (FR-03) |
 * | `incomplete` | a konverter megállapítása; a validátor újraszámolja (ADR-002) |
 * | `tags` | egyetlen validációs szabály sem használja |
 *
 * És amit a leképezés **számol**, nem olvas:
 *
 * - [TechniqueInput.verified]: a fájlban nincs `verified` mező, a `review_status`
 *   helyettesíti, és **csak** a `reviewed` érték jelent hitelesítést. A pontos
 *   egyenlőség szándékos: egy ismeretlen vagy elírt státusz a szigorúbb olvasatot kapja,
 *   mert hitelesítési szintet kódból soha nem emelünk (CR-01, CR-02).
 * - [TechniqueInput.quizEligible]: szándékosan `null`, mert a kvízjogosultság
 *   **számított** érték (ADR-010), nem tartalmi adat – a fájlban nem is szerepel.
 */
internal fun EntryDto.toTechniqueInput(): TechniqueInput = TechniqueInput(
    id = id,
    categoryId = categoryId,
    romaji = japaneseRomaji,
    hungarian = hungarianName,
    japaneseScript = japaneseScript,
    aliases = aliases,
    illustration = illustrationRef,
    descriptionHu = descriptionHu,
    verified = reviewStatus == REVIEW_STATUS_REVIEWED,
    quizEligible = null,
    quizExcluded = quizExcluded,
    retired = retired,
)

/**
 * A beolvasott tudástárfájlok és a csomagolt képfájllista → a validátor teljes bemenete.
 *
 * @param availableIllustrationFiles a csomagban **tényleg** megtalálható képfájlok
 *   csupasz nevei. Üres halmaz azt jelenti: nincs csomagolt kép – ez a V005 szabály
 *   rendes bemenete, nem betöltési hiba.
 */
internal fun toKnowledgeInput(
    manifest: ManifestDto,
    entries: EntriesFileDto,
    availableIllustrationFiles: Set<String>,
): KnowledgeInput = KnowledgeInput(
    manifest = manifest.toManifestInput(),
    entries = entries.entries.map { it.toTechniqueInput() },
    availableIllustrationFiles = availableIllustrationFiles,
)
