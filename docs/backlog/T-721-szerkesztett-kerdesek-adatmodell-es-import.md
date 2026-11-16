# T-721 · Szerkesztett tudás- és helyzetkérdések modellje és importja

| | |
|---|---|
| Epik / user story | E11 / US-33 |
| Modul | core/model, core/data, knowledgebase_transfer/tools |
| Méret | L |
| Függ | T-715, T-716 |
| Követelmény | KB-07, KB-08 ([E11 követelmények](../BACKLOG.md#e11--a-konyv-alapu-tudastar-bovitese)); a normatív FR/DR/TR megfeleltetés T-713 feladata |
| Teszt | Question domain/DTO/import/Room tesztek; hibás opciók, források, fordítások és revíziók. |
| Állapot | Nyitott; a 2026-11-16-i felhasználói kérés alapján backlogba véve |

## Cél

A történelem és etikett tényleges kérdéseket igényel a név–jelentés generáláson túl.

## Teendők

- [ ] Implementáld a szerkesztett Question modellt: id, revision, objective_id, entry_ids, magyar prompt/opciók/magyarázat és fordításaik, 4 stabil option_id, correct_option_id, source_refs, review_status, retired.
- [ ] Az opcióazonosító nem TechniqueId/EntryId: a válasz lehet önálló szöveg. Az entry-kapcsolatok a tanulási és magyarázati hátteret jelölik.
- [ ] Bővítsd a szerkesztési formátumot, konvertert, DTO-t, validátort, Room-tárolást és repositoryt.
- [ ] Validáld az egyedi opcióazonosítókat, pontosan egy létező helyes választ, a 4 különböző értelmű opciót a szerkesztői ellenőrzés dokumentált részével, a hivatkozásokat és a fordítások teljességét.
- [ ] A kérdésnek saját review állapota legyen; az OCR-javítás vagy az entry ellenőrzése önmagában ne hitelesítse a kérdés szövegét és megoldását.

## Kész, ha

- [ ] Technikához és cikkhez is csatolható több kérdés, önálló opciószöveggel.
- [ ] Hibás correct_option_id és hiányos nyelvi változat nem eredményez hibásan megjelenő kérdést.
- [ ] A várt, ténylegesen teljes nyelvi változat kiválasztása a T-713-ban rögzített szabályt követi.

## Könyv–Kyo egyeztetési szabály (2026-11-16)

A felhasználói döntés kötelező: [BOOK_CONTENT_POLICY.md](../BOOK_CONTENT_POLICY.md).
Ez az itt szereplő általános lektorálási megfogalmazások pontosítása; nem kell minden
könyvbeli adatot újra szakmailag lektorálni.

- [ ] A háttérállítás match/minor_difference elfogadása ne követeljen ismételt szakmai lektorálást a kérdéshez. A kérdés saját szerkesztési ellenőrzése (szöveg, opciók, megoldás, magyarázat) ettől kötelező és külön nyilvántartott.
- [ ] Kérdésmodell-/importteszt fedje le az elfogadott háttértényt és a lényegi konfliktusra vagy előzmény nélküli új állításra épülő kérdést; az utóbbiak nem örökölhetnek egyezésalapú elfogadást.

## Ellenőrzés és Definition of Done

- Question domain/DTO/import/Room tesztek; hibás opciók, források, fordítások és revíziók.
- Az E11 közös szabályai és a releváns globális DoD teljesülnek; a dokumentációs/tartalmi feladat ellenőrzése a változás jellegéhez igazodik.
- Az implementációs állás és a ténylegesen futtatott ellenőrzések dokumentáltak. Ami nem futott, annak oka szerepel.
- Csak elkészült és ellenőrzött munka után kerülhet a jegy a `docs/done/` mappába.

## Hatókör

Ez végrehajtandó backlogjegy, nem a funkció elkészültének igazolása. A jegy létrehozása nem módosítja az alkalmazás kódját vagy az éles tudástárat. A nem ismert szakmai/bibliográfiai adatot jelölni kell; kitalálni és hitelesként tárolni tilos.
