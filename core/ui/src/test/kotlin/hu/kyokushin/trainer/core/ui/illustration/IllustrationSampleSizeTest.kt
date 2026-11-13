package hu.kyokushin.trainer.core.ui.illustration

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * A nagy illusztrációk méretezési szabálya (T-204: „nagy képeknél méretezés",
 * FUNCTIONAL_SPEC 3.2 – képarány megtartva, NFR-05 memóriakorlát).
 *
 * Tiszta JVM teszt: a ritkítási szabály Android nélkül is ellenőrizhető.
 */
class IllustrationSampleSizeTest {
    @Test
    fun sampleSize_forPackagedIllustrations_downscalesLargeImages_NFR05() {
        // A jelenlegi tudástár-készlet tényleges mérete (manifest.json): 1122x1402.
        assertThat(inSampleSizeFor(sourceWidth = 1122, sourceHeight = 1402, maxDimension = 1024))
            .isEqualTo(2)
    }

    @Test
    fun sampleSize_imageWithinLimit_isNotDownscaled_NFR05() {
        assertThat(inSampleSizeFor(sourceWidth = 800, sourceHeight = 1024, maxDimension = 1024))
            .isEqualTo(1)
    }

    @Test
    fun sampleSize_isSmallestPowerOfTwoKeepingLongestEdgeWithinLimit_NFR05() {
        val maxDimension = 1024
        val sizes = listOf(
            1024 to 1,
            1025 to 2,
            2048 to 2,
            2049 to 2,
            4096 to 4,
            4097 to 4,
        )
        sizes.forEach { (longestEdge, expected) ->
            val sampleSize = inSampleSizeFor(
                sourceWidth = longestEdge,
                sourceHeight = longestEdge / 2,
                maxDimension = maxDimension,
            )
            assertThat(sampleSize).isEqualTo(expected)
            // Az invariáns: a ritkított leghosszabb él már belül van a korláton...
            assertThat(longestEdge / sampleSize).isAtMost(maxDimension)
            // ...és a feleannyi lépték már nem lenne elég (tehát nem ritkítunk túl).
            if (sampleSize > 1) {
                assertThat(longestEdge / (sampleSize / 2)).isGreaterThan(maxDimension)
            }
        }
    }

    @Test
    fun sampleSize_unknownBounds_fallsBackToFullResolution_NFR05() {
        // A BitmapFactory hibánál -1-et ad outWidth/outHeight-ban: ilyenkor nem
        // találgatunk léptéket, a rosszabb memóriahasználat jobb, mint a hibás kép.
        assertThat(inSampleSizeFor(sourceWidth = -1, sourceHeight = -1, maxDimension = 1024))
            .isEqualTo(1)
        assertThat(inSampleSizeFor(sourceWidth = 1122, sourceHeight = 1402, maxDimension = 0))
            .isEqualTo(1)
    }
}
