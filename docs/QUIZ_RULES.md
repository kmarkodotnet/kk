# Kvízszabályok

> **Állapot: KIADOTT (T-609).** Ez a dokumentum a tényleges `core/quiz`
> `DefaultQuizGenerator`/`DistractorSelector`/`QuizCandidateFilter`/`QuizEligibility`/
> `QuizScoring` implementációt írja le, nem egy tervezett motort. Minden állítás egy
> konkrét, meglévő unit teszthez köthető (TR-02, TR-03, TR-04 – lásd az egyes pontok
> végén a teszthivatkozást). A normatív szabályleírás ezen felül a
> **`.claude/skills/kyo-quiz-engine/SKILL.md`**-ben él.

## Tartalomjegyzék

1. [Kérdéstípusok](#1-kérdéstípusok)
2. [Bemenet és kimenet](#2-bemenet-és-kimenet)
3. [Invariánsok](#3-invariánsok)
4. [Distraktorválasztás](#4-distraktorválasztás)
5. [Fallback és UI-üzenetek](#5-fallback-és-ui-üzenetek)
6. [Válaszkezelés](#6-válaszkezelés)
7. [Eredményszámítás](#7-eredményszámítás)

---

## 1. Kérdéstípusok

A kérdéstípus (`core/model` `QuestionType`) négy értékű enum; a név mindig
`<kérdés>_TO_<válasz>` irányú.

| Típus | Kérdés | Opciók | Jogosultsági mezőkövetelmény (`QuizEligibility.hasFieldsFor`) |
|---|---|---|---|
| `JP_TO_HU` (A) | romaji név | 4 magyar név | `romaji` és `hungarian` nem üres |
| `HU_TO_JP` (B) | magyar név | 4 romaji név | ugyanaz |
| `IMAGE_TO_JP` (C) | illusztráció | 4 romaji név | `romaji` nem üres **és** a **helyes** válasznak kvízben használható képe van |
| `JP_TO_IMAGE` (D) | romaji név | 4 illusztráció | `romaji` nem üres **és** **minden** opciónak kvízben használható képe van |

**A kérdés és az opciók oldala külön mezőkövetelményt kaphat** (`QuizCandidateFilter.optionRequirement`):
a (C) típusnál a disztraktoroknak **nem** kell kép – a kép a kérdés oldalán van, az
opciók romaji nevek, ezért a (C) opciói a (A)-éval azonos (`JP_TO_HU`) követelményt
kapják. A másik három típusnál a kérdés és az opciók követelménye megegyezik.

**„Kvízben használható kép"** = `IllustrationRef.Asset` **és** `quizUsable == true`.
Egy `IllustrationRef.Placeholder` (hiányzó vagy fel nem oldható kép) és egy
**feliratos** `Asset` (`quizUsable == false` – a technika neve bele van égetve a képbe,
lásd `docs/KNOWLEDGE_BASE.md` 8. pont) **egyformán** kizáró: sem helyesként, sem
distraktorként nem jöhet szóba (`QuizEligibility.hasQuizUsableIllustration`).

A C és D típus a jelenlegi, csomagolt tartalmon (161 rekord) **csak a `stances`
kategóriában** aktiválható: ott van mind a 10 illusztráció, 7 kvízben használható. A
többi 8 kategóriában a kvízbeállító ezt a két típust letiltja és megindokolja (FR-13) –
`RealContentQuizGenerationTest.realContent_stancesCategory_yieldsImageQuestionsInThePlan_TR03`
és `realContent_everyOtherCategory_yieldsNoImageQuestion_TR03`.

## 2. Bemenet és kimenet

```kotlin
data class QuizRequest(
    val candidates: QuizCandidateSet,       // technikák + V006 szinonimacsoportok + kép-ujjlenyomatok
    val enabledTypes: Set<QuestionType>,    // a felhasználó által engedélyezett típusok
    val categoryFilter: Set<CategoryId> = emptySet(),  // üres = minden kategória
    val requestedCount: Int,                // 5 / 10 / 20 – FELSŐ korlát, nem garancia
    val seed: Long,                         // a hívó injektált RNG-jéből sorsolva
    val datasetTrust: DatasetTrust,         // a manifest szerkesztői állapotából, sosem UI-ból
)

data class QuizPlan(
    val questions: List<QuizQuestion> = emptyList(),
    val skippedTypes: Map<QuestionType, SkipReason> = emptyMap(),
    val shortfall: Shortfall? = null,
)

data class QuizQuestion(
    val id: QuestionId,
    val type: QuestionType,
    val correctTechniqueId: TechniqueId,
    val optionTechniqueIds: List<TechniqueId>,  // pontosan 4, tartalmazza a helyeset
)
```

**A kérdés kizárólag azonosítókat tárol** (ADR-006, DR-03) – nincs benne megjelenítendő
szöveg, kép vagy `contentDescription`. Három szándékos következmény:

1. A válasz kiértékelése mindig **azonosító-összehasonlítás**
   (`selectedTechniqueId == question.correctTechniqueId`), soha nem szöveges egyezés –
   két azonos magyar jelentésű technikánál a szöveges egyezés hamis találatot adna.
2. A kérdés szerializálható és kicsi, ezért a teljes kvízállapot elfér a
   `SavedStateHandle`-ben (ADR-007) anélkül, hogy a tartalmat cipelné.
3. Tartalomfrissítés után egy félbehagyott kvíz a **friss** szövegekkel jelenik meg,
   miközben a helyesség változatlan.

`QuizCandidateSet` (a `candidates` mező tartalmi fele) a technikák listáján **felül** két
olyan ismeretet hordoz, amit egy technikarekord önmagában nem:

- `synonymGroups`: a validátor V006 szabályának kimenete (`ValidationReport.synonymGroups`)
  – azonos jelentésű technikák kölcsönös distraktor-kizárási csoportjai (CR-07).
- `illustrationDigests`: technikaazonosító → a hozzá tartozó kép **azonosság-ujjlenyomata**.
  A jelenlegi `TechniqueRepository.getQuizCandidates` ezt a csomagolt illusztrációfájl
  **nevéből** tölti (nincs SHA-256 oszlop a sémában, becsültet nem gyártunk) – egyező
  fájlnév garantáltan ugyanazt a bájtsorozatot jelenti, de a *fordított* eset (két eltérő
  fájlnevű, bitre azonos kép) ezzel a kulccsal nem ismerhető fel; az a
  `knowledgebase_transfer` konverter `I003` diagnosztikájának hatóköre, nem a motoré.

A `dryRun()` kimenete (`QuizReadiness`) a kvízbeállító élő összegzéséhez:

```kotlin
data class QuizReadiness(
    val requestedCount: Int,
    val expectedQuestionCount: Int,           // generate() ugyanerre a kérésre pontosan ennyit ad
    val availableByType: Map<QuestionType, Int> = emptyMap(),  // típusonkénti KAPACITÁS, nem kérdéskiosztás
    val skippedTypes: Map<QuestionType, SkipReason> = emptyMap(),
    val expectedShortfall: Shortfall? = null,
)
```

`availableByType` értékeinek **összege nagyobb lehet**, mint `expectedQuestionCount` –
ugyanaz a technika több típusban is lehet helyes válasz, de a motor egy kvízen belül
legfeljebb egyszer használja. A mező tehát típusonkénti *elérhetőséget* mutat a
kvízbeállítón, nem a végleges kérdéskiosztást.

*Forrás:* `core/quiz/.../generator/QuizRequest.kt`, `QuizPlan.kt`, `QuizQuestion.kt`,
`QuizReadiness.kt`, `QuizCandidateSet.kt`; ADR-006, ADR-007.

## 3. Invariánsok

Mind a nyolc invariánsra van unit teszt (`QuizPlanInvariantTest`,
`GeneratorContractFixtures.assertQuizPlanInvariants`, `RealContentQuizGenerationTest`,
`QuizQuestionContractTest`):

1. **Pontosan 4 opció** minden kérdésben – a `QuizQuestion` konstruktora `require()`-rel
   kényszeríti ki (`optionTechniqueIds.size == OPTION_COUNT`), még egy sérült/manipulált
   mentett állapot visszafejtésekor is.
2. **Pontosan 1 helyes** – `correctTechniqueId in optionTechniqueIds`, konstruktorban
   kikényszerítve.
3. **A 3 distraktor páronként és a helyestől is különböző** – `optionTechniqueIds.toSet().size == OPTION_COUNT`.
4. **Egyetlen distraktor sem szinonimája a helyesnek** – `DistractorSelector.acceptableDistractors`
   kizárja a `QuizCandidateSet.synonymsOf(correct.id)` halmazt (CR-07).
   Teszt: `DistractorSelectorTest.selectDistractors_synonymOfCorrectAnswer_isNeverADistractor_CR07`.
5. **`JP_TO_IMAGE`-nél nincs két vizuálisan azonos kép az opciók között** – a kiválasztás
   menet közben zárja ki az addig választott opciókkal azonos `illustrationDigest`-et.
   Teszt: `DistractorSelectorTest.selectDistractors_jpToImage_neverRepeatsAVisuallyIdenticalIllustration_TR02`.
6. **Nincs ismétlés** – ugyanaz a helyes technika egy tervben legfeljebb egyszer szerepel,
   akkor is, ha több típusban is jogosult lenne; ha a kért szám nagyobb, mint a különböző
   helyes jelöltek száma, a terv **kevesebb** kérdést ad (`Shortfall`), nem ismétel.
   Teszt: `DefaultQuizGeneratorTest.generate_neverRepeatsTheSameCorrectTechnique_TR02`.
7. **Determinizmus** – ugyanaz a `QuizRequest` (ugyanaz a `seed`) bitre ugyanazt a
   `QuizPlan`-t adja, az opciók sorrendjével együtt; a jelöltbemeneti sorrend sem
   befolyásolja az eredményt (rendezett bejárás, nem `Set`-iteráció).
   Tesztek: `DefaultQuizGeneratorTest.generate_sameRequestAndSeedTwice_producesIdenticalPlan_NFR07`,
   `generate_equalRequestsBuiltSeparately_produceIdenticalPlan_NFR07`,
   `generate_candidateInputOrder_doesNotChangeThePlan_NFR07`,
   `generate_differentSeeds_produceDifferentQuestionOrder_NFR07`,
   `quizModuleMainSources_containNoUnseededRandomOrClock_NFR07` (forrásszöveg-vizsgálat:
   a `core/quiz` modulban nincs `Random.Default` vagy közvetlen óra-hívás),
   `RealContentQuizGenerationTest.realContent_sameSeed_producesIdenticalPlan_TR02`.
8. **A motor soha nem gyárt opciót** – minden opció létező `Technique.id`; ha négy valódi
   opció nem áll össze, a kérdés egyszerűen nem jön létre (lásd 4–5. pont).
   Teszt: `DistractorSelectorTest.selectDistractors_everyOptionIsAnExistingTechniqueRecord_FR14`.

**A `dryRun()` és a `generate()` nem tud elválni egymástól**, mert mindkettő a *közös*
`questionSlots(request, skippedTypes)` függvényt hívja (`DefaultQuizGenerator`): a
`dryRun()` nem épít kérdést és nem sorsol, de ugyanazt a szűrést és jogosultsági kaput
futtatja le, mint a `generate()`. Teszt: `DefaultQuizGeneratorTest.dryRun_matchesGenerateOnEveryBranch_FR13`,
`dryRun_bodyBuildsNoQuestionAndDrawsNoRandom_FR13`,
`RealContentQuizGenerationTest.realContent_dryRunMatchesGenerateForEveryCategory_TR03`.

## 4. Distraktorválasztás

A szűrés **két, szándékosan elkülönített lépésben** történik:

1. **`QuizCandidateFilter`** – *melyik technika melyik típusban jöhet szóba* (kategóriaszűrő
   + a 5. pont jogosultsági kapuja). Ez technikánkénti és típusonkénti döntés.
2. **`DistractorSelector`** – *összeáll-e négy opció egy adott helyes válasz mellé* (a
   helyes válaszhoz **relatív** döntés: szinonimák, kategória-elsőbbség, képazonosság).

### A jogosultsági kapu (`QuizEligibility`, ADR-010)

```
isEligible(technique, type, trust) =
     technique.status != INCOMPLETE
  && !technique.quizExcluded && !technique.retired
  && hasFieldsFor(technique, type)
  && (trust == REVIEWED || trust == PRACTICE_ON_UNREVIEWED)   // ma MINDKÉT érték átengedi
```

**Fontos, kódból levezetett tény:** a `DatasetTrust` enumnak jelenleg csak két értéke van,
és a formula **mindkettőre** `true`-t ad – a bizalmi szint ma **nem** zár ki technikát a
kvízből, csak a UI „Lektorálatlan gyakorlóanyag" szalagját vezérli. A tényleges szűrést a
`status != INCOMPLETE`, a `quizExcluded`/`retired` flag és a mezőteljesség végzi. Ez
dokumentáltan szándékos: ADR-010 pontosan azért vezette be a kétértékű `DatasetTrust`-ot,
hogy a kvíz lektorálatlan tartalommal is futhasson, feltűnő jelzéssel, ahelyett, hogy a
161 rekordból egy sem lenne kvízképes a lektorálásig.

A validátor a **saját** V007/V008 jelentéséhez ugyanezt a `QuizEligibility` függvényt
hívja (`hasQuizUsableIllustration`) – a jelentés és a tényleges motor-viselkedés emiatt nem
tud elválni egymástól.

### Prioritási sorrend egy adott helyes válaszhoz (`DistractorSelector.acceptableDistractors`)

1. **Ugyanaz a kategória**, nem szinonim, a típus opciókészletén belüli jelöltek – ez az
   elsődleges forrás, mert a kategórián belüli megkülönböztetés a tanulási cél.
2. Ha a kategórián belül kevesebb mint 3 elfogadható jelölt van: **feltöltés** más
   kategóriából, ugyanazon szűrőkkel.
   Teszt: `DistractorSelectorTest.selectDistractors_prefersSameCategoryWhenThreeAreAvailable_FR14`,
   `selectDistractors_fewerThanThreeInCategory_fillsFromOtherCategories_FR14`.
3. Ha így sincs 3 elfogadható distraktor: **a kérdés nem jön létre** (`selectDistractors` → `null`).
   Teszt: `DistractorSelectorTest.selectDistractors_fewerThanThreeAcceptable_buildsNoQuestion_FR14_TR02`.

**Kizárási szűrő minden jelöltre** (`acceptableDistractors`):

| Feltétel | Indok |
|---|---|
| benne van a típus opciókészletében (`QuizCandidateFilter.optionPool`) | a jogosultsági kapu |
| nem a helyes válasz | pontosan egy helyes |
| nem szinonimája a helyesnek (`synonymsOf`) | CR-07 |
| (D) `JP_TO_IMAGE`: a képe nem azonos ujjlenyomatú a helyes válasz képével | FR-14 |

**A kiválasztás közbeni** (a jelöltek **egymás** közti) kizárás `selectDistractors`-ban:
egy jelölt kimarad, ha egy **már kiválasztott** distraktor szinonimája, vagy (D) típusnál
egy már kiválasztott opcióval azonos ujjlenyomatú a képe.
Teszt: `DistractorSelectorTest.selectDistractors_twoSynonymousCandidates_neverShareOneQuestion_CR07`.

**A kizárás konzervatív**, nem optimális párosítású: a bejárás az első elfogadható
jelöltet választja, nem keres maximális lefedést. Egy ritka, egymást keresztező
szinonimacsoport-elrendezés esetén elvileg kimaradhat egy elvben összeállítható kérdés –
ez a biztonságos irány (kevesebb, de garantáltan helyes kérdés), és mivel a kapacitás-
számítás (`buildableCorrectAnswers`) ugyanezt a függvényt hívja, a *számolt* és a
*tényleges* kérdésszám nem tud elválni egymástól (lásd 3. pont, `dryRun`↔`generate` egyezés).

**Determinizmus itt:** a bejárás a `QuizCandidateFilter.optionPool` azonosító szerint
rendezett, kategóriasorrendbe particionált listájából jön; ez a lépés **nem** használ RNG-t
– a seedelt keverés (lásd 1–2. pont a `DefaultQuizGenerator`-ban) csak *utána* permutálja a
már kiválasztott opciókat.
Teszt: `DistractorSelectorTest.acceptableDistractors_areOrderedSameCategoryFirstThenById_NFR07`,
`selectDistractors_isDeterministicAndIndependentOfInputOrder_NFR07`.

*Forrás:* `core/quiz/.../generator/DistractorSelector.kt`, `QuizCandidateFilter.kt`;
`.claude/skills/kyo-quiz-engine/SKILL.md` 4. pont; CR-07.

## 5. Fallback és UI-üzenetek

A motor **soha nem dob kivételt** elégtelen adatra – minden „nem jött össze" eset a
`QuizPlan`/`QuizReadiness` strukturált mezőiben jelenik meg, amiből a UI `strings.xml`-ből
konkrét magyar üzenetet és javaslatot állít elő. A motor maga **szövegfüggetlen**.

### `SkipReason` – melyik **típus** maradt ki, és miért (`QuizCandidateFilter.skipReason`)

| Ok | Mikor |
|---|---|
| `TYPE_DISABLED` | A felhasználó nem engedélyezte ezt a típust (`enabledTypes`). Ez **minden** tartalmi oknál előbbre való – letiltott típusról nem állítunk adathiányt. |
| `NOT_ENOUGH_IMAGES` | Képes típus, és nincs elég validált, kvízben használható, vizuálisan különböző illusztráció. (C)-nél: a helyes-jelölt oldal üres. (D)-nél: a megkülönböztethető opciók száma 4-nél kevesebb. |
| `NOT_ENOUGH_CANDIDATES` | Egyébként: nincs elég kvízképes technika a négy opcióhoz. |

Csak a **kihagyott** típusok szerepelnek a térképben; a sorrend a `QuestionType`
deklarációs sorrendje (determinizmus). Teszt:
`DefaultQuizGeneratorTest.generate_imageTypesWithoutIllustrations_reportSkipReasonAndShortfall_TR03`.

### `Shortfall` – a **teljes terv** kapott-e kevesebb kérdést a kértnél

```kotlin
data class Shortfall(val requestedCount: Int, val actualCount: Int, val reason: ShortfallReason)
```

`null`, ha a kért szám pontosan kijött. Az ok **prioritási sorrendje** (a leghasznosabb
magyar üzenet felé rendezve, `DefaultQuizGenerator.shortfallReason`):

1. **`ALL_TYPES_SKIPPED`** – egyetlen kérdés sem jött össze (minden engedélyezett típus
   kimaradt).
2. **`NOT_ENOUGH_IMAGES`** – egy engedélyezett képes típus **egésze** kiesett képhiány
   miatt; ez a legnagyobb egyszeri veszteség, konkrét javaslattal (kapcsold ki a képes
   típusokat, vagy válassz más kategóriát).
3. **`NOT_ENOUGH_DISTRACTORS`** – volt a kapun átjutott helyes jelölt, amely mellé nem
   állt össze három elfogadható distraktor (CR-07 szinonimakizárás miatt).
4. **`NOT_ENOUGH_CANDIDATES`** – egyébként: kevesebb kvízképes technika volt, mint a kért
   kérdésszám.

Ez a sorrend **tesztelt motordöntés**, nem dokumentációs javaslat:
`DefaultQuizGeneratorTest.generate_fewerCandidatesThanRequested_yieldsShortfallInsteadOfRepetition_FR22`,
`generate_exactlyEnoughCandidates_hasNoShortfall_FR13`,
`generate_noUsableCandidate_returnsExplainedEmptyPlan_FR22`,
`DistractorSelectorTest.skipReason_everyCorrectAnswerBlockedBySynonyms_isCandidateShortage_FR22`.
A `NOT_ENOUGH_DISTRACTORS` ág **csak átfedő** szinonimacsoportokkal áll elő: egyetlen
szinonimacsoport szimmetrikusan zár, és akkor a típus **egésze** esik ki
(`NOT_ENOUGH_CANDIDATES`), nem csak egy-két kérdés.

### Üres terv

Ha `questions.isEmpty()` (`QuizPlan.canStart == false`), a UI **nem indít** kvízt, hanem a
`skippedTypes`/`shortfall` alapján konkrét magyar üzenetet és javaslatot ad (pl. „A
kiválasztott kategóriákban nincs elég hitelesített technika a kvízhez.” + „Válassz több
kategóriát / engedélyezz több kérdéstípust / csökkentsd a kérdésszámot.”, egyetlen,
szó szerinti `strings.xml` mondatként, `docs/CONVENTIONS.md` 1. szakasz szerint – nem
szétbontva több bejegyzésre).

*Forrás:* `core/quiz/.../generator/QuizPlan.kt` (`SkipReason`, `ShortfallReason`,
`Shortfall`), `DefaultQuizGenerator.kt`; FR-22.

## 6. Válaszkezelés

Egy kérdés **csak egyszer** válaszolható meg (FR-18), **három védelmi vonallal**
(ADR-005), ebben a sorrendben:

1. **`QuizRunViewModel`**: ha `questionId` már szerepel a megválaszoltak között, a
   válaszkezelő **no-op** – nem rögzít új próbálkozást, nem módosítja az eredményt.
2. **Room**: `UNIQUE(sessionId, questionId)` index a `quiz_attempt` táblán +
   `OnConflictStrategy.IGNORE` – a versenyhelyzetben átcsúszó második beszúrás `-1`-et ad
   vissza, nem dob kivételt és nem hoz létre második sort. A `ProgressRepository.recordAttempt`
   ezért `Boolean`-t ad vissza (`false` = már volt válasz) – az „egyszeri válasz" szabály a
   repository szerződésének része, nem UI-konvenció.
3. **Compose**: a válaszgombok letiltása válasz után – csak kényelmi/vizuális réteg,
   **nem** erre épül a helyesség.

**Állapotmegőrzés (FR-19, ADR-007).** A kvízállapot (seed, generált kérdéslista,
`currentIndex`, válaszok, `shortfall`) a `QuizRunViewModel` + `SavedStateHandle`
kombinációban túléli a konfigurációváltást (forgatás). A seed és a kérdéslista
**mindkettő** a mentett állapot része: a seed a motor reprodukálhatóságát garantálja, a
kész kérdéslista pedig elkerüli, hogy minden forgatás újragenerálja a kvízt (ami, ha a
tartalom közben frissülne, más kvízt adna ugyanarra a seedre). A komplex,
`kotlinx.serialization`-nal szerializálható állapot kódolása a `core/quiz`-ben él
(`QuizRunStateCodec`), **nem** a `feature/quiz` modulban – az utóbbi explicit tiltja a
`kotlinx.serialization` közvetlen használatát (ADR-001 L3) –, és négy `String`
be-/kimenetű függvényt ad kifelé.

**Megszakítás.** A befejezetlen `QuizSession.finishedAt` **`null`** marad; az addig
megválaszolt kérdések próbálkozásai érvényesek és beszámítanak a statisztikába. A
megszakítás **explicit nem hívja** a `finishSession`-t – ez nem hiányzó funkció, hanem a
specifikáció szerinti végállapot.

**Hibás kérdések újragyakorlása (FR-21).** Ez **új munkamenetet** indít, **új seeddel**
(ADR-007 – szándékosan más sorrend, mint az előző kvíz; ez nem mond ellent a
determinizmusnak, mert az új munkamenet a *saját* seedjére nézve ugyanúgy reprodukálható),
ugyanazokra a (hibásan megválaszolt) technikákra korlátozva. A motornak nincs
„csak ezekből a konkrét technikákból legyen helyes válasz" bemenete – a hívó oldal ezért
egy `QuizCandidateSet`-et állít össze, amelynek `techniques` mezője **kizárólag** a kívánt
technikákat tartalmazza, társ-/kategóriatárs technika hozzáadása **nélkül**. Mivel a motor
a helyes-válasz-poolt **és** a disztraktorpoolt is ugyanabból a jelöltkészletből meríti,
ez a szűkítés egyszerre korlátozza mindkettőt – és vele együtt jár a motor dokumentált,
nem-optimista szabálya: 4-nél kevesebb egyedi célponttal a `generate()`/`dryRun()`
**0** kérdést ad (lásd 3. és 5. pont), ezért a hívó előzetes `dryRun()`-nal ismeri fel és
kerüli el ezt, nem nyit üres munkamenetet.

*Forrás:* ADR-005, ADR-007; `docs/CONVENTIONS.md` 1. szakasz („Egyetlen, már ismert menet…”,
„Egy motor nélküli technikaszintű allow-list…”, „Normál befejezés és megszakítás…”).

## 7. Eredményszámítás

```kotlin
data class Score(val answered: Int, val correct: Int, val ratio: Double?)

object QuizScoring {
    fun score(attempts: List<QuizAttempt>): Score
}
```

- A `ratio` **csak a tényleges megválaszolt kérdésekből** számolódik: `correct / answered`.
  A meg nem válaszolt kérdés **nem** számít hibásnak, és nem is kerül bele a nevezőbe.
- **0 megválaszolt kérdés → `ratio = null`, nem `0.0`** (FR-27). A `Score` konstruktora
  `require()`-rel kényszeríti ki: `answered == 0` esetén `ratio` **csak** `null` lehet,
  egyébként **csak** `0.0..1.0` tartománybeli érték. A „nincs adat" és a „0%" emiatt a
  típus szintjén nem keveredhet össze – a UI a `null`-ból „nincs adat" szöveget állít elő,
  sosem `0%`-ot.
- **0% és 100% érvényes határeset**, mindkettőre van teszt (`QuizScoringTest`,
  `QuizScoringTr04Test`).
- **Egy kérdés egyszer számít** (FR-18): ugyanarra a `(sessionId, questionId)` párra
  érkezett több próbálkozásból a **legkorábbi** `answeredAt` nyer (azonos időbélyegnél a
  lista elsőként felsorolt eleme) – ez a számítás szintjén *is* érvényesül, nem csak a
  Room egyedi indexén (ADR-005) keresztül, tehát egy UI dupla koppintása vagy egy
  újrajátszott eseménysor sem duplázhatja az eredményt.
- **Nem számol újra helyességet.** A `QuizAttempt.correct` mezőt összegzi, nem hasonlítja
  újra a két azonosítót – a szinonimacsoportos helyesség megítélése a kvízmotor dolga volt
  a válasz pillanatában, és azt az ítéletet kell megőrizni (DR-09).
- **Nincs redundáns, tárolt számláló** – a statisztika a `quiz_attempt` táblából mindig
  **frissen számolt** (DR-09): a kvízmenet lezárása és a haladás-áttekintő aggregálása
  **ugyanezt** a tiszta függvényt hívja, nem két külön számolást.
- A tanulókártya „Tudtam” önértékelése (`study_review.selfRating`) **nem** kvíztalálat –
  külön tábla, külön megjelenítés, sosem keveredik a kvízsikerarányba.

*Forrás:* `core/quiz/.../scoring/QuizScoring.kt`; `QuizScoringTest.kt`,
`QuizScoringTr04Test.kt`; FR-20, FR-27, DR-09.

---

## `/kyo-doc-sync` ellenőrzési pontok

Ha egy jövőbeli módosítás a következő szimbólumok bármelyikét átnevezi vagy a
viselkedésüket megváltoztatja, ez a dokumentum vele együtt frissítendő:
`QuizRequest`, `QuizPlan`, `QuizReadiness`, `QuizQuestion`, `SkipReason`, `ShortfallReason`,
`QuizEligibility.isEligible`/`hasFieldsFor`, `DistractorSelector.acceptableDistractors`/
`selectDistractors`, `QuizCandidateFilter.optionRequirement`/`skipReason`, `QuizScoring.score`,
`Score`. A fenti teszthivatkozások (`DefaultQuizGeneratorTest`, `DistractorSelectorTest`,
`QuizCandidateFilterTest`, `RealContentQuizGenerationTest`, `QuizPlanInvariantTest`,
`QuizScoringTest`, `QuizScoringTr04Test`) a dokumentum minden állításának ellenőrzési
pontjai – egy bukó teszt itt azt jelenti, hogy ez a dokumentum is elavult.
