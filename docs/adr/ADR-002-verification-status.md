# ADR-002 – `VerificationStatus` háromállapotú enum a `verified: Boolean` helyett

- **Állapot:** elfogadva (2026-11-11)
- **Kontextus:** CR-01, CR-02, FR-05; `docs/FUNCTIONAL_SPEC.md` 2.2 három különböző
  UI-jelölőt ír elő.

## Döntés

A domain modellben:

```kotlin
enum class VerificationStatus { VERIFIED, AWAITING_REVIEW, INCOMPLETE }
```

A **JSON sémában marad** a `verified: Boolean` mező – a tartalomfejlesztő számára ez a
természetes jelölés, és a séma nem bonyolódik. A harmadik állapotot (`INCOMPLETE`) a
validátor **származtatja**: ha a rekord kötelező mezője hiányzik vagy a képhivatkozása
érvénytelen, a státusz `INCOMPLETE` lesz, függetlenül a `verified` értéktől.

Leképezés:

| JSON `verified` | validátor eredménye | domain `status` | UI jelölő |
|---|---|---|---|
| `true` | hibátlan | `VERIFIED` | nincs |
| `false` | hibátlan | `AWAITING_REVIEW` | „Ellenőrzésre vár" |
| bármi | hiányos mező / rossz képhivatkozás | `INCOMPLETE` | „Hiányos" + a hiányzó mezők |

A validátor **soha nem emel** státuszt: `INCOMPLETE` → `VERIFIED` átmenet kódból lehetetlen
(CR-02). A `VERIFIED` kizárólag a JSON-ban, emberi döntés nyomán keletkezhet.

## Mérlegelt alternatívák

| Alternatíva | Miért nem |
|---|---|
| Sima `verified: Boolean` a domainben | A UI nem tudná megkülönböztetni a „még nem ellenőrzött, de ép" és a „hibás/hiányos" rekordot, pedig a felhasználónak más üzenet jár. |
| Külön `isComplete: Boolean` mező a `verified` mellé | Két boolean négy állapotot enged, amiből kettő értelmetlen (`verified && !complete`). Az enum kizárja az érvénytelen kombinációt. |

## Következmények

- A UI-logika kimerítő `when` ágakkal íródik; új státusz felvétele fordítási hibát okoz
  minden érintett helyen – ez kívánatos.
- A Room `technique` tábla `status` oszlopa **String**-ként tárolja az enumot (nem ordinal),
  hogy az értékek átrendezése ne rontsa el a meglévő adatot.
- A `quizEligible` továbbra is külön mező: `VERIFIED` státusz szükséges, de nem elégséges
  feltétele (a mezőknek is elegendőnek kell lenniük az adott kérdéstípushoz).
