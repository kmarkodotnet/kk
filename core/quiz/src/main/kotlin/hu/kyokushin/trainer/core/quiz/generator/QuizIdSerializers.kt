package hu.kyokushin.trainer.core.quiz.generator

import hu.kyokushin.trainer.core.model.QuestionId
import hu.kyokushin.trainer.core.model.TechniqueId
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * A domain azonosítók szerializálása a kvízterv mentéséhez (ADR-007, T-405).
 *
 * Miért külön szerializátor, és miért nem `@Serializable` a `core/model` value classokon?
 *
 * 1. A [TechniqueId] és a [QuestionId] konstruktora **privát**: példány kizárólag a
 *    `of(...)` gyáron keresztül jöhet létre, mert az ellenőrzi, hogy az azonosító nem
 *    üres. A visszafejtés ugyanezen a gyáron megy át, így a mentett állapotból
 *    visszaolvasott azonosító sem tudja megkerülni a domain invariánst – egy manipulált
 *    vagy sérült `SavedStateHandle`-tartalom nem tud üres azonosítót becsempészni.
 * 2. A `core/model` így szerializációmentes marad (ADR-001): a domain modell nem
 *    hordoz perzisztencia- vagy átviteli formátumot. A kvízterv mentési formátuma a
 *    kvízmodul döntése.
 *
 * A soron lévő forma szándékosan **nyers sztring**, nem objektum: a mentett kvízállapot
 * kicsi és stabil marad (ADR-006 következménye).
 */
object TechniqueIdSerializer : KSerializer<TechniqueId> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("hu.kyokushin.trainer.core.model.TechniqueId", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: TechniqueId) = encoder.encodeString(value.value)

    override fun deserialize(decoder: Decoder): TechniqueId = TechniqueId.of(decoder.decodeString())
}

/** A [QuestionId] sztring alakú szerializálása – a [TechniqueIdSerializer] indoklásával. */
object QuestionIdSerializer : KSerializer<QuestionId> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("hu.kyokushin.trainer.core.model.QuestionId", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: QuestionId) = encoder.encodeString(value.value)

    override fun deserialize(decoder: Decoder): QuestionId = QuestionId.of(decoder.decodeString())
}
