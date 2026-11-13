package hu.kyokushin.trainer.feature.progress.technique

import androidx.annotation.StringRes
import hu.kyokushin.trainer.core.model.QuestionType
import hu.kyokushin.trainer.core.model.SelfRating
import hu.kyokushin.trainer.feature.progress.R

/**
 * A kérdéstípus és az önértékelés feliratainak leképezése, `@Composable`-mentes formában
 * (FUNCTIONAL_SPEC 6.2, T-506) – ugyanaz a minta, mint a `feature/progress`
 * `progressScoreDisplay`-je (CONVENTIONS 4.1 „Compose döntési logika tesztelése emulátor
 * nélkül"): a kimerítő `when` itt él, nem a Composable függvény törzsében, így a
 * fordító rögtön jelez egy jövőbeli új [QuestionType]/[SelfRating] esetén.
 */
object ProgressTechniqueLabels {

    @StringRes
    fun questionTypeLabelRes(type: QuestionType): Int = when (type) {
        QuestionType.JP_TO_HU -> R.string.progress_technique_type_jp_to_hu
        QuestionType.HU_TO_JP -> R.string.progress_technique_type_hu_to_jp
        QuestionType.IMAGE_TO_JP -> R.string.progress_technique_type_image_to_jp
        QuestionType.JP_TO_IMAGE -> R.string.progress_technique_type_jp_to_image
    }

    @StringRes
    fun selfRatingLabelRes(rating: SelfRating): Int = when (rating) {
        SelfRating.KNEW -> R.string.progress_technique_self_rating_knew
        SelfRating.NEEDS_PRACTICE -> R.string.progress_technique_self_rating_needs_practice
    }
}
