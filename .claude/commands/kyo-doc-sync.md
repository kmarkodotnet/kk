---
description: Dokumentáció és kód közötti eltérések kimutatása (drift audit).
allowed-tools: Bash, Read, Grep, Glob
---

Vesd össze a `docs/` tartalmát a tényleges kóddal, és listázd az eltéréseket.

Ellenőrizd:
1. **Modulstruktúra:** a `docs/ARCHITECTURE.md`-ben leírt csomagok/modulok tényleg léteznek-e.
2. **Adatmodell:** a `docs/REQUIREMENTS.md` / `ARCHITECTURE.md` entitásmezői egyeznek-e a
   tényleges `@Entity` / domain modellekkel.
3. **Követelménylefedettség:** minden FR-* azonosítóhoz van-e implementáció vagy teszt
   (keresd az FR-azonosítókat a kódkommentekben és tesztnevekben).
4. **Kvízszabályok:** a `docs/QUIZ_RULES.md` állításai megfelelnek-e a `QuizGenerator` kódnak.
5. **Elfogadási kritériumok:** mesterprompt 9. pontja – melyik teljesül bizonyítottan.
6. **TEST_REPORT valóságtartalma:** a dokumentált futtatások dátuma/parancsa plauzibilis-e,
   nincs-e nem futtatott eredmény beírva.

Kimenet: eltéréslista (dokumentum → állítás → tényleges állapot → javasolt javítás).
A dokumentumokat NE írd át automatikusan; javaslatot adj, a módosítás külön lépés.
