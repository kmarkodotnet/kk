# T-722 · Vegyes kvízmotor: generált és szerkesztett kérdések

| | |
|---|---|
| Epik / user story | E11 / US-33 |
| Modul | core/quiz |
| Méret | L |
| Függ | T-721, T-717 |
| Követelmény | KB-07, KB-08 ([E11 követelmények](../BACKLOG.md#e11--a-konyv-alapu-tudastar-bovitese)); a normatív FR/DR/TR megfeleltetés T-713 feladata |
| Teszt | JVM motorinvariánsok, régi+új típusmátrix, dryRun/generate, kérdésrevízió-váltás és codec regresszió. |
| Állapot | Nyitott; a 2026-11-16-i felhasználói kérés alapján backlogba véve |

## Cél

A meglévő név-/képkvíz és az új tudás-/helyzetkérdés ugyanabban a megbízható folyamatban fusson.

## Teendők

- [ ] Bővítsd a request/plan/question modellt a generált és szerkesztett eredettel, kérdés- és tanulásicél-azonosítókkal.
- [ ] A single_choice kérdésekre is a közös jogosultsági kapu, kategória-/vizsgaszűrés, seedelt keverés, dryRun és shortfall működjön.
- [ ] Ne generálj tényeket vagy opciókat futás közben; a szerkesztett kérdés saját megoldását értékeld a stabil option_id alapján.
- [ ] A mentett terv rögzítse a kérdésrevíziót és a megjelenítéshez/kiértékeléshez szükséges verziózott tartalmat vagy azzal egyenértékű immutábilis hivatkozást. Tartalomfrissítés ne változtassa meg egy folyamatban levő kérdés helyes válaszát.
- [ ] A régi négy kérdéstípus invariánsai és tiltott distraktorai maradjanak érvényesek.

## Kész, ha

- [ ] Azonos kérés, tartalomverzió és seed azonos vegyes tervet ad.
- [ ] Minden kérdésnek 4 opciója és pontosan 1 helyes válasza van; egyetlen opcióról sem feltételezzük, hogy technikarekord.
- [ ] Tartalomfrissítés/folyamat-visszaállítás után is ugyanaz az elkezdett kérdés és eredmény értelmezhető.

## Ellenőrzés és Definition of Done

- JVM motorinvariánsok, régi+új típusmátrix, dryRun/generate, kérdésrevízió-váltás és codec regresszió.
- Az E11 közös szabályai és a releváns globális DoD teljesülnek; a dokumentációs/tartalmi feladat ellenőrzése a változás jellegéhez igazodik.
- Az implementációs állás és a ténylegesen futtatott ellenőrzések dokumentáltak. Ami nem futott, annak oka szerepel.
- Csak elkészült és ellenőrzött munka után kerülhet a jegy a `docs/done/` mappába.

## Hatókör

Ez végrehajtandó backlogjegy, nem a funkció elkészültének igazolása. A jegy létrehozása nem módosítja az alkalmazás kódját vagy az éles tudástárat. A nem ismert szakmai/bibliográfiai adatot jelölni kell; kitalálni és hitelesként tárolni tilos.
