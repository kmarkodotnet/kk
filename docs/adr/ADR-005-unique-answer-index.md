# ADR-005 – `UNIQUE(sessionId, questionId)` index a dupla válasz kizárására

- **Állapot:** elfogadva (2026-11-11)
- **Kontextus:** FR-18 – egy kérdés csak egyszer válaszolható meg; a dupla kattintás nem
  számolhat dupla eredményt. TR-04 ezt méri.

## Döntés

Három védelmi vonal, ebben a sorrendben:

| # | Hol | Mit véd |
|---|---|---|
| 1 | `QuizRunViewModel` | ha `questionId ∈ state.answers`, a válaszkezelő **no-op** |
| 2 | Room: `UNIQUE(sessionId, questionId)` index a `quiz_attempt` táblán | a versenyhelyzetben átcsúszó második beszúrás ütközik, és nem jön létre második sor |
| 3 | Compose: a válaszgombok letiltása válasz után | csak kényelmi/vizuális réteg, **nem** erre épül a helyesség |

A `ProgressRepository.recordAttempt` visszatérési típusa `Boolean`: `false`, ha a kérdésre
már volt válasz. Így az „egyszeri válasz" szabály a **repository szerződésének része**, nem
UI-konvenció.

A `quiz_attempt` ezért tárolja a `questionId`-t is, nem csak a `techniqueId`-t – ugyanaz a
technika ugyanabban a munkamenetben elvileg több kérdésben is szerepelhetne, ezért a
`(sessionId, techniqueId)` pár nem lenne helyes kulcs.

## Mérlegelt alternatívák

| Alternatíva | Miért nem |
|---|---|
| Csak a gomb letiltása | A konfigurációváltás, a gyors dupla érintés és a folyamat-újraindítás kicsúszhat alóla. A helyesség nem múlhat a UI állapotán. |
| Csak ViewModel-ellenőrzés | Elegendő lenne a gyakorlatban, de a folyamat-visszaállítás és a párhuzamos coroutine-ok miatt nem bizonyítható. A DB index olcsó és végleges garancia. |
| Idempotens upsert a válaszra | Az „első válasz számít" szabályt sértené: egy későbbi válasz felülírná a korábbit. |

## Következmények

- A `recordAttempt` ütközéskor `IGNORE` konfliktusstratégiával nem dob kivételt, hanem
  `false`-t ad vissza; a hívó ebből tudja, hogy nem kell az állapotot módosítani.
- A statisztika emiatt sosem duplázódhat, még akkor sem, ha a UI hibás (DR-09).
- A „hibák újragyakorlása" **új** `sessionId`-t kap (FR-21), így ugyanarra a technikára
  jogosan keletkezik új próbálkozás – az index nem akadályozza.
