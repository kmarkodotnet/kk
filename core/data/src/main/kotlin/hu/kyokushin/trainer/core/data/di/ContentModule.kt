package hu.kyokushin.trainer.core.data.di

import android.content.Context
import android.content.res.AssetManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import hu.kyokushin.trainer.core.data.content.AndroidKnowledgeAssetSource
import hu.kyokushin.trainer.core.data.content.KnowledgeAssetSource
import hu.kyokushin.trainer.core.data.content.KnowledgeContentReader
import javax.inject.Singleton

/**
 * A csomagolt tudástár beolvasásának kötései (T-115, T-117, T-203).
 *
 * A [KnowledgeAssetSource] és a [KnowledgeContentReader] egyaránt `internal` (ADR-001, L3):
 * a `core/data` kifelé csak a [hu.kyokushin.trainer.core.data.content.ContentImporter]
 * interfészt mutatja. Ezért – a `DatabaseModule`-lal azonos okból – ez a kötés is itt él,
 * nem a `di` modulban.
 *
 * A [KnowledgeContentReader] nem `@Inject constructor`: a konstruktora `json` paramétere
 * alapértelmezett értékkel rendelkezik, és a tesztek memóriában tartott
 * [KnowledgeAssetSource]-szal építik közvetlenül. A `@Provides` így nem kényszerít
 * Hilt-annotációt egy egyébként tiszta, JVM-en tesztelhető osztályra.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object ContentModule {

    /**
     * Az assetek olvasója az alkalmazás [AssetManager]-e felett.
     *
     * A gyökérkönyvtár az osztály alapértelmezése (`knowledge/`), hogy a fájlelrendezés
     * egyetlen helyen, a `KnowledgeAssetLayout`-ban legyen rögzítve (ADR-011).
     */
    @Provides
    @Singleton
    fun provideKnowledgeAssetSource(
        @ApplicationContext context: Context,
    ): KnowledgeAssetSource = AndroidKnowledgeAssetSource(context.assets)

    @Provides
    @Singleton
    fun provideKnowledgeContentReader(source: KnowledgeAssetSource): KnowledgeContentReader =
        KnowledgeContentReader(source)
}
