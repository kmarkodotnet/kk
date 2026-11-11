package hu.kyokushin.trainer.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import hu.kyokushin.trainer.core.quiz.generator.DefaultQuizGenerator
import hu.kyokushin.trainer.core.quiz.generator.QuizGenerator
import hu.kyokushin.trainer.core.quiz.validation.DefaultKnowledgeValidator
import hu.kyokushin.trainer.core.quiz.validation.KnowledgeValidator
import javax.inject.Singleton
import kotlin.random.Random

/**
 * A kvízmotor és a tartalomvalidátor kötése (ARCHITECTURE 7.: `QuizModule`, T-203).
 *
 * A `core/quiz` tiszta Kotlin, Android és Hilt nélkül (ADR-001, L1): egyik osztályán sincs
 * `@Inject` annotáció, és nem is lesz – különben a modul Hilt-függőséget kapna, és a
 * kvízhelyesség nem lenne sima JVM teszttel ellenőrizhető. Ezért itt `@Provides`, nem
 * `@Binds`.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object QuizModule {

    /**
     * A kvízgenerátor. A [DefaultQuizGenerator] `object`: állapotmentes, és a determinizmusa
     * kizárólag a [hu.kyokushin.trainer.core.quiz.generator.QuizRequest] seedjéből
     * következik (ADR-007), tehát a megosztott példány nem hordoz futások közti állapotot.
     */
    @Provides
    @Singleton
    fun provideQuizGenerator(): QuizGenerator = DefaultQuizGenerator

    /** A tartalmi szabályok érvényesítője (V001…V010); állapotmentes, példányosítható. */
    @Provides
    @Singleton
    fun provideKnowledgeValidator(): KnowledgeValidator = DefaultKnowledgeValidator()

    /**
     * Az alkalmazás egyetlen véletlenforrása (NFR-07, ADR-007).
     *
     * ## Mire kell, ha a generátor seedelt?
     *
     * Pontosan a **seed sorsolására**. A [QuizGenerator] soha nem állít elő seedet: a kérés
     * készen kapja, a munkamenet pedig elteszi (`QuizSession.seed`), hogy a kvíz
     * folytatható és reprodukálható legyen. Az új kvíz seedjét tehát a hívónak kell
     * sorsolnia – ha ezt `Random.Default`-tal tenné, a ViewModel tesztje nem tudná
     * megadni, melyik kvíz jöjjön ki. Injektált RNG mellett a teszt `Random(seed)`-et köt,
     * és a teljes futás determinisztikus.
     *
     * ## Miért `Random` és nem `RandomFactory`?
     *
     * Az ARCHITECTURE 7. táblázata `RandomFactory` néven említi ezt a kötést. Egy gyártó
     * interfész akkor indokolt, ha hívásonként **új, külön sorozatot** kell adni; itt
     * viszont egyetlen, folytonos sorozatból húzunk seedeket, és a seedelt sorozatokat már
     * a generátor építi a kapott `Long`-ból. Egy üres gyártóréteg a nevén kívül semmit nem
     * adna, viszont kötelezővé tenné, hogy a hívók egy `di`-ben lakó típust ismerjenek –
     * azt a modult, amelytől a `feature` modulok szándékosan nem függenek. A [Random]
     * stdlib típus minden rétegben látszik.
     *
     * A [Random.Default] szálbiztos, így a megosztott példány párhuzamos hívásból is
     * biztonságos. Ez a fájl az egyetlen hely az üzleti kódban, ahol a [Random.Default]
     * megjelenik.
     */
    @Provides
    @Singleton
    fun provideRandom(): Random = Random.Default
}
