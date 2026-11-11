package hu.kyokushin.trainer.core.data.content

import android.content.res.AssetManager
import hu.kyokushin.trainer.core.quiz.validation.IssueCode
import hu.kyokushin.trainer.core.quiz.validation.KnowledgeInput
import hu.kyokushin.trainer.core.quiz.validation.ValidationIssue
import hu.kyokushin.trainer.core.quiz.validation.ValidationReport
import java.io.IOException
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * A csomagolt tudástár beolvasása és a validátor bemenetének összeállítása
 * (FR-29, DR-05, ADR-011).
 *
 * Ez a réteg **nem validál**: egyetlen döntése az, hogy a fájlok egyáltalán
 * értelmezhetők-e. Ha igen, nyers [KnowledgeInput] készül, és minden tartalmi szabályt a
 * `core/quiz` validátora érvényesít. Ha nem, `V010` kódú, BLOKKOLÓ, adathalmaz-hatókörű
 * észrevétel keletkezik – **ezt a kódot a validátor sosem állítja elő**, mert addig el sem
 * jut (lásd [IssueCode.V010]).
 *
 * ## Miért jelentés, és miért nem kivétel?
 *
 * Mert az érvénytelen tartalom nem omlaszthatja össze az appot (FR-31). A hívó (a
 * tartalomimportáló, T-117) mindkét ágon [ValidationReport]-ot kap, és ugyanazon a
 * `datasetRejected` kapun dönt: a korábban betöltött tartalommal működik tovább, és
 * diagnosztikát ad (FR-45).
 *
 * @property source a tudástárfájlok forrása – assets éles kódban, memóriában tartott
 *   tartalom a tesztekben.
 * @property json a deszerializáló konfiguráció; alapértelmezésben [KnowledgeJson].
 */
internal class KnowledgeContentReader(
    private val source: KnowledgeAssetSource,
    private val json: Json = KnowledgeJson,
) {

    /**
     * Beolvassa a `manifest.json`-t, az `entries.json`-t és a csomagolt képfájlok listáját.
     *
     * A képfájllista nem mellékhatás, hanem a validátor *bemenete*: a V005 szabály így
     * tisztán, fájlrendszer nélkül is tesztelhető (TR-01).
     */
    fun read(): KnowledgeContentReadResult =
        try {
            val manifest = decode(KnowledgeAssetLayout.MANIFEST_FILE, ManifestDto.serializer())
            val entries = decode(KnowledgeAssetLayout.ENTRIES_FILE, EntriesFileDto.serializer())
            KnowledgeContentReadResult.Parsed(
                input = toKnowledgeInput(
                    manifest = manifest,
                    entries = entries,
                    availableIllustrationFiles = source.listFileNames(
                        KnowledgeAssetLayout.ILLUSTRATIONS_DIRECTORY,
                    ),
                ),
            )
        } catch (unreadable: ContentUnreadableException) {
            KnowledgeContentReadResult.Unreadable(unreadable.toReport())
        }

    /**
     * Egy tudástárfájl beolvasása és deszerializálása.
     *
     * A hibát [ContentUnreadableException]-ként továbbítja, hogy a *melyik fájlon*
     * információ ne veszjen el: a `V010` diagnosztikájának meg kell tudnia mondani, melyik
     * fájl olvashatatlan, különben a tartalomszerkesztő nem tudja, mit javítson.
     *
     * - [SerializationException]: a JSON szintaxisa hibás, vagy a szerkezete nem illik a
     *   sémára (pl. tömb helyett objektum). A hiányzó mező **nem** tartozik ide: minden
     *   DTO-mezőnek `null` az alapértéke, azt a validátor jelenti (V001/V002).
     * - [IOException]: a fájl nincs a csomagban, vagy nem olvasható.
     * - [IllegalArgumentException]: a `kotlinx.serialization` egyéb bemeneti hibája
     *   (a [SerializationException] is ennek a leszármazottja).
     */
    private fun <T> decode(fileName: String, deserializer: DeserializationStrategy<T>): T =
        try {
            json.decodeFromString(deserializer, source.readText(fileName))
        } catch (error: SerializationException) {
            throw ContentUnreadableException(fileName, error)
        } catch (error: IOException) {
            throw ContentUnreadableException(fileName, error)
        } catch (error: IllegalArgumentException) {
            throw ContentUnreadableException(fileName, error)
        }

    /**
     * Osztályon belüli jelzés az olvashatatlan fájlról.
     *
     * Soha nem hagyja el a [KnowledgeContentReader]-t: a [read] elfogja, és
     * [KnowledgeContentReadResult.Unreadable] jelentéssé fordítja. Azért kivétel, mert a
     * beolvasás két egymásra épülő lépésből áll, és az első hibájánál a másodiknak már nem
     * szabad lefutnia.
     */
    private class ContentUnreadableException(
        val fileName: String,
        override val cause: Throwable,
    ) : Exception(cause) {

        /** A `V010` jelentés: a teljes betöltés elutasítva, diagnosztikával. */
        fun toReport(): ValidationReport = ValidationReport(
            issues = listOf(
                ValidationIssue(
                    code = IssueCode.V010,
                    severity = IssueCode.V010.defaultSeverity,
                    field = fileName,
                    detail = "A tudástárfájl nem értelmezhető: $fileName " +
                        "(${cause::class.java.simpleName}: ${cause.message ?: "nincs részletes üzenet"}). " +
                        "A betöltés elutasítva, a korábban betöltött tartalom érvényben marad.",
                ),
            ),
        )
    }

    internal companion object {

        /** Olvasó a csomagolt assetek felett (`assets/knowledge/`). */
        fun forAssets(
            assets: AssetManager,
            rootDirectory: String = KnowledgeAssetLayout.ROOT,
        ): KnowledgeContentReader = KnowledgeContentReader(
            source = AndroidKnowledgeAssetSource(assets, rootDirectory),
        )
    }
}

/**
 * A tudástár beolvasásának kimenete.
 *
 * Két ág, mert a két eset *más döntést* kíván a hívótól: az egyikből validálható bemenet
 * lett, a másikból kész elutasító jelentés. Egyetlen, nullozható mezős típus ezt a
 * különbséget elrejtené.
 */
internal sealed interface KnowledgeContentReadResult {

    /**
     * A fájlok értelmezhetők voltak; a tartalmi szabályok vizsgálata még **nem** történt
     * meg.
     *
     * @property input a validátor nyers bemenete – lehet benne érvénytelen rekord, ez a
     *   rendes működés része.
     */
    data class Parsed(val input: KnowledgeInput) : KnowledgeContentReadResult

    /**
     * A fájlok nem értelmezhetők; a betöltés elutasítva (`V010`).
     *
     * @property report kész, `V010` észrevételt tartalmazó jelentés. A
     *   [ValidationReport.datasetRejected] értéke `true`, az `accepted` lista üres.
     */
    data class Unreadable(val report: ValidationReport) : KnowledgeContentReadResult
}
