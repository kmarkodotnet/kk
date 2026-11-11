---
name: kyo-quiz-engine
description: A tiszta Kotlin kvízmotor szabályai - kérdéstípusok, distraktorválasztás, szinonimakizárás, seedelt determinizmus, jogosultsági kapuk, fallback és eredményszámítás. Használd amikor QuizGenerator, kvízkérdés, válaszopció, distraktor, seed, kérdéstípus, kvízállapot, kvízeredmény vagy kvízstatisztika a munka tárgya.
---

# Kvízmotor

## 0. Architekturális kötelem

A `QuizGenerator` és minden kvízszabály **tiszta Kotlin**: nincs `android.*` import, nincs
`Context`, nincs Room, nincs Compose. Bemenet és kimenet immutable adatstruktúra. Ez azért
kritikus, mert a kvízszabályok a termék helyességének magja, és JVM unit teszttel kell
teljesen lefedni, emulátor nélkül.

## 1. Kérdéstípusok

| Típus | Kérdés | Opciók | Előfeltétel |
|---|---|---|---|
| `JP_TO_HU` (A) | romaji név | 4 magyar név | a helyes és 3 distraktor `quizEligible` |
| `HU_TO_JP` (B) | magyar név | 4 romaji név | ugyanaz |
| `IMAGE_TO_JP` (C) | illusztráció | 4 romaji név | a **helyesnek** van validált, egyedi képe |
| `JP_TO_IMAGE` (D) | romaji név | 4 illusztráció | a helyesnek ÉS mind a 3 distraktornak van validált, **vizuálisan nem azonos** képe |

A C és D típus csak akkor aktiválható, ha elegendő, egyedi, validált kép van. Placeholder
kép nem számít validált képnek.

## 2. Bemenet / kimenet

```kotlin
data class QuizRequest(
    val techniques: List<QuizCandidate>,   // MÁR validált, quizEligible rekordok
    val enabledTypes: Set<QuestionType>,
    val categoryFilter: Set<CategoryId>,   // üres = mind
    val requestedCount: Int,               // 5 / 10 / 20
    val seed: Long,
)

data class QuizPlan(
    val questions: List<QuizQuestion>,     // size <= requestedCount
    val skippedTypes: Map<QuestionType, SkipReason>,
    val shortfall: Shortfall?,             // ha kevesebb kérdés jött ki a kértnél
)

data class QuizQuestion(
    val id: QuestionId,
    val type: QuestionType,
    val correctTechniqueId: TechniqueId,
    val optionTechniqueIds: List<TechniqueId>,  // pontosan 4, tartalmazza a helyeset
)
```

**A kérdés nem tárol megjelenítendő szöveget vagy képet.** Csak technikaazonosítókat.
A megjelenítést a UI oldja fel a tudástárból. Ez garantálja, hogy a helyes válasz
összehasonlítása azonosító alapú.

## 3. Invariánsok (ezek mindegyikére kell unit teszt)

1. Minden kérdésben **pontosan 4** opció van.
2. Pontosan **egy** opció egyezik a `correctTechniqueId`-val.
3. A 3 distraktor **egymástól és a helyestől is különböző** technikaazonosító.
4. Egyetlen distraktor sem szinonimája / azonos fogalma a helyesnek (V006 kizárási lista).
5. `JP_TO_IMAGE` esetén nincs két vizuálisan azonos illusztráció az opciók között.
6. Ugyanaz a helyes technika nem ismétlődik egy teszten belül, amíg van fel nem használt
   jelölt. Ha a `requestedCount` nagyobb, mint a jelöltek száma, a motor **kevesebb**
   kérdést ad (`shortfall`), nem ismétel.
7. **Determinizmus:** ugyanaz a `QuizRequest` (ugyanaz a seed) → bitre ugyanaz a `QuizPlan`,
   beleértve az opciók sorrendjét is. A motor kizárólag a seedelt RNG-t használja; nincs
   `Random.Default`, nincs `System.currentTimeMillis()`, nincs `Set` iterációs sorrendre
   épülő logika (determinisztikus, rendezett bejárás kötelező).
8. A motor soha nem gyárt opciót: nem képez „közeli", „hihető", generált hamis választ.
   Minden opció létező technikarekord.

## 4. Distraktorválasztás

Prioritási sorrend:
1. **Ugyanaz a kategória**, nem szinonim, kvízképes jelöltek – ezek az elsődlegesek, mert
   a kategórián belüli megkülönböztetés a tanulási cél.
2. Ha a kategórián belül kevesebb mint 3 elfogadható jelölt van: feltöltés más
   kategóriából, szintén nem szinonim, kvízképes rekordokból.
3. Ha így sincs 3 elfogadható distraktor: **a kérdés nem jön létre.** Nincs 3 opciós kérdés,
   nincs kitalált opció, nincs szinonim opció.

Kizárási szűrő minden jelöltre: `quizEligible == true` ÉS `id != correctId` ÉS nincs
szinonima-/azonosfogalom-kapcsolat a helyessel ÉS (D típusnál) nem vizuálisan azonos kép.

## 5. Fallback és világos UI-üzenet

A motor nem dob kivételt elégtelen adat esetén. `QuizPlan` jelzi:
- `skippedTypes`: melyik kérdéstípus maradt ki és miért (`NOT_ENOUGH_IMAGES`,
  `NOT_ENOUGH_CANDIDATES`, `TYPE_DISABLED`).
- `shortfall`: a kért vs. a tényleges kérdésszám és az ok.
- Ha `questions` üres: a UI nem indít kvízt, hanem konkrét magyar üzenetet ad
  (pl. „A kiválasztott kategóriákban nincs elég hitelesített technika a kvízhez."),
  és javaslatot (több kategória, más kérdéstípus, kisebb kérdésszám).

A felhasználó soha ne lásson félrevezető, hibásan összeállított kérdést. Egy kevesebb
kérdéses, de helyes kvíz jobb, mint egy teljes, de hibás.

## 6. Válaszkezelés és állapot

- Egy kérdés **egyszer** válaszolható meg. A második válaszkísérlet no-op: nem rögzít
  új próbálkozást, nem módosítja az eredményt. Ezt a kvízállapot (`answeredQuestionIds`)
  biztosítja, nem a UI gombletiltása (a gombletiltás csak másodlagos védelem).
- A kvízállapot túléli a konfigurációváltást (képernyőforgatás): ViewModel + `SavedStateHandle`.
  A seed és a kérdéslista a mentett állapot része, hogy az újraépítés azonos kvízt adjon.
- Megszakítás: a befejezetlen `QuizSession` `finishedAt = null` marad; az addig megválaszolt
  kérdések próbálkozásai érvényesek és beszámítanak.
- Hibás kérdések újragyakorlása: **új munkamenet** ugyanazokkal a helyes technikákkal,
  új seeddel. Nem ugyanaz a session, hogy a statisztika ne duplázódjon.

## 7. Eredmény és statisztika

- A százalék **csak a tényleges megválaszolt kérdésekből** számolódik:
  `helyes / megválaszolt`. A meg nem válaszolt kérdés nem számít hibásnak.
- 0 megválaszolt kérdés → nincs százalék (`null`), nem `0%`. A UI „nincs adat"-ot mutat.
- 0% és 100% érvényes határeset, mindkettőre kell teszt.
- A statisztikát a `quiz_attempt` táblából **számold** (aggregált query), ne tarts
  redundáns, elromlani képes számlálót.
- A tanulókártya „Tudtam" önértékelése (`study_review.selfRating`) **nem** kvíztalálat.
  Külön tábla, külön megjelenítés, és soha nem keveredik a kvízsikerarányba.
