package hu.kyokushin.trainer.feature.progress.technique

import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.model.QuestionType
import hu.kyokushin.trainer.core.model.SelfRating
import hu.kyokushin.trainer.feature.progress.R
import org.junit.Test

/**
 * [ProgressTechniqueLabels] – `@Composable`-mentes leképezés (FUNCTIONAL_SPEC 6.2, T-506).
 *
 * Sima JUnit, Robolectric/Compose nélkül (CONVENTIONS 4.1): a függvények csak egy
 * `@StringRes Int`-et adnak vissza, nem oldanak fel szöveget.
 */
class ProgressTechniqueLabelsTest {
    @Test
    fun questionTypeLabelRes_mapsEveryTypeToItsOwnResource_FR25() {
        assertThat(ProgressTechniqueLabels.questionTypeLabelRes(QuestionType.JP_TO_HU))
            .isEqualTo(R.string.progress_technique_type_jp_to_hu)
        assertThat(ProgressTechniqueLabels.questionTypeLabelRes(QuestionType.HU_TO_JP))
            .isEqualTo(R.string.progress_technique_type_hu_to_jp)
        assertThat(ProgressTechniqueLabels.questionTypeLabelRes(QuestionType.IMAGE_TO_JP))
            .isEqualTo(R.string.progress_technique_type_image_to_jp)
        assertThat(ProgressTechniqueLabels.questionTypeLabelRes(QuestionType.JP_TO_IMAGE))
            .isEqualTo(R.string.progress_technique_type_jp_to_image)
    }

    @Test
    fun selfRatingLabelRes_mapsEveryRatingToItsOwnResource_FR28() {
        assertThat(ProgressTechniqueLabels.selfRatingLabelRes(SelfRating.KNEW))
            .isEqualTo(R.string.progress_technique_self_rating_knew)
        assertThat(ProgressTechniqueLabels.selfRatingLabelRes(SelfRating.NEEDS_PRACTICE))
            .isEqualTo(R.string.progress_technique_self_rating_needs_practice)
    }
}
