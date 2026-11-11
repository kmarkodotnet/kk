# ADR-007 – A seed a munkamenetben és a mentett állapotban is tárolódik

- **Állapot:** elfogadva (2026-11-11)
- **Kontextus:** NFR-07 (determinizmus), FR-19 (a kvízállapot túléli a konfigurációváltást).

## Döntés

A kvíz indításakor az **injektált RNG-ből** sorsolt `seed: Long` két helyre kerül:

1. `QuizSession.seed` – a Room `quiz_session` táblában, a munkamenet részeként;
2. `QuizRunState.seed` – a `SavedStateHandle`-be mentett futásállapotban, a generált
   kérdéslistával és a válaszokkal együtt.

A `QuizGenerator` kizárólag ezt a seedet használja: nincs `Random.Default`, nincs
`System.currentTimeMillis()`, és nincs `Set`-iterációs sorrendre épülő logika – a jelöltek
bejárása `id` szerint rendezett (`docs/ARCHITECTURE.md` 4.2, algoritmus 3. lépés).

## Miért mindkét helyen

| Hely | Mit old meg |
|---|---|
| `SavedStateHandle` | Forgatás után a kérdéslista és az opciósorrend **bitre ugyanaz**; ha a folyamat újraindul és a lista újragenerálódik, a seed garantálja az azonos eredményt. |
| `quiz_session` tábla | Egy befejezett vagy megszakított kvíz utólag reprodukálható – hibajelentésnél, teszteléskor és a „pontosan ezt a kvízt láttam" kérdés tisztázásakor. Diagnosztikai érték. |

## Mérlegelt alternatívák

| Alternatíva | Miért nem |
|---|---|
| Csak a kérdéslista mentése, seed nélkül | A lista visszaáll, de a motor viselkedése nem reprodukálható; egy hibás kvíz utólag nem játszható vissza. |
| Csak a seed mentése, kérdéslista nélkül | Minden forgatásnál újragenerálás futna. Determinisztikus volna, de fölösleges munka, és ha a tartalom közben frissül, más kvízt adna ugyanarra a seedre. |
| Rendszeróra mint seed, tárolás nélkül | Nem reprodukálható, és teszteléskor sem rögzíthető. |

## Következmények

- A `TimeModule` és a `QuizModule` **kötelezően** injektálja a `Clock`-ot és az RNG-t;
  a `kyo-spec-auditor` külön ellenőrzi, hogy üzleti kódban nincs közvetlen óra- vagy
  `Random.Default`-hívás.
- A TR-02 teszt „azonos seed → azonos terv" állítása közvetlenül mérhető.
- A „hibák újragyakorlása" (FR-21) **új seedet** kap, hogy a sorrend ne legyen azonos az
  előző kvízzel – ez szándékos, és nem mond ellent a determinizmusnak: az új munkamenet a
  saját seedjére nézve ugyanúgy reprodukálható.
