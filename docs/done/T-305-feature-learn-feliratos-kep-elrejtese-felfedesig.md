# T-305 · feature/learn: feliratos kép elrejtése felfedésig

| | |
|---|---|
| Epik | **E3** – Tanulás tanulókártyákkal |
| User story | US-10 |
| Modul | `feature/learn` |
| Méret | S (1–3 ó) |
| Függ | [T-304](T-304-feature-learn-kartya-ui.md) |
| Követelmény | FR-07, ADR-011 |
| Teszt | — |

## Cél

A képbe égetett név ne fedje fel a választ a felfedés előtt.

## Teendők

- [x] Ha `IllustrationRef.Asset.hideableOnStudyCard == false`, a kép **csak felfedés után** jelenik meg.
- [x] Addig semleges helyőrző + rövid magyarázat, miért nem látszik még a kép.

## Kész, ha

- [x] A KY-0045, KY-0050 és KY-0052 kártyáján a kép felfedés előtt nem látható.
- [x] A többi kép a szokásos módon megjelenik.

## Figyelj rá

- Ez a mostani tartalom konkrét következménye: 3 képen rajta a név. Ha később felirat nélküli képek érkeznek, a konverter újrafuttatása után ez az ág magától kikapcsol.

## Definition of Done

A task-specifikus pontokon felül a **globális DoD** is érvényes (`docs/BACKLOG.md` → „Globális Definition of Done”):

- [x] `./gradlew assembleDebug` exit 0
- [x] `./gradlew testDebugUnitTest` exit 0, a teszt neve hivatkozik a lefedett követelményre
- [x] Nem sért rétegszabályt (ADR-001, L1–L6)
- [x] Nincs közvetlen `Clock` / `Random` hívás üzleti kódban (NFR-07)
- [x] Minden felhasználói szöveg `strings.xml`-ben, magyarul
- [x] UI esetén: `contentDescription`, ≥ 48 dp célpont, `@Preview` világos + sötét témára
- [x] `docs/IMPLEMENTATION_PLAN.md` állása frissítve
- [x] Ami nem futott le, az `docs/TEST_REPORT.md`-ben „NEM FUTOTT – ok” sorként szerepel

## Kapcsolódó dokumentumok

- `docs/REQUIREMENTS.md` – a hivatkozott követelmények
- `docs/FUNCTIONAL_SPEC.md` – a képernyő és a viselkedés leírása
- `docs/ARCHITECTURE.md` + `docs/adr/` – a szerkezeti döntések
- `.claude/skills/` – `kyo-quiz-engine`, `kyo-knowledge-base`, `kyo-android-module`, `kyokushin-content`

---

## Elkészült

| | |
|---|---|
| Dátum | 2026-11-12 |
| Commit | `ca1e230` (kód), közös a T-304/T-307 lezárásával (G-05 csoport) |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew.bat :feature:learn:testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL` – `LearnCardPoliciesTest`: 4 teszt a `LearnCardImagePolicy`-ra |
| `./gradlew.bat test testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL` – **484 teszt, 0 hiba** (471 → 484) |
| `./gradlew.bat assembleDebug` | 0 | `BUILD SUCCESSFUL` – `app/build/outputs/apk/debug/app-debug.apk` |

**Megjegyzés:** A döntést a `LearnCardImagePolicy.shouldHideUntilRevealed(illustration,
revealed)` tiszta függvénye hozza (`feature/learn/cards/LearnCardPolicies.kt`): igaz,
ha `illustration is IllustrationRef.Asset && !hideableOnStudyCard && !revealed`. A
`knowledgebase_transfer/manifest.json`-ban ellenőrizve: a KY-0045, KY-0050, KY-0052
rekord mindegyike `"has_embedded_label": true` és `"hideable_on_study_card": false` –
tehát a „Kész ha" 1. pontja a **tényleges** tartalomra igaz, nem csak szintetikus
teszt­adatra. Felfedésig a kép helyén egy magyarázó felület jelenik meg
(`HiddenCaptionedImageNotice`, `learn_cards_image_hidden_title`/`_explanation`
`strings.xml` kulcsok), egyetlen TalkBack bejárási egységként
(`clearAndSetSemantics`). A `Placeholder`/`null` eset és a normál (`hideableOnStudyCard
== true`) kép sosem rejtett – ezt is a policy 4 tesztje bizonyítja.
