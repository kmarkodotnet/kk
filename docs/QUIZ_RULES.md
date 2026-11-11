# Kvízszabályok

> **Állapot: VÁZLAT.** Ez a dokumentum a Fázis 8.5 lépésben kerül kiadott állapotba, a
> tényleges `QuizGenerator` implementációval összevetve. A normatív szabályleírás jelenleg
> a fejlesztői skillben él: **`.claude/skills/kyo-quiz-engine/SKILL.md`**.
> A `/kyo-doc-sync` parancs ellenőrzi, hogy a kettő nem tér el a kódtól.

## Tartalomjegyzék (a kiadott változat szerkezete)

1. **Kérdéstípusok** – A: romaji → magyar, B: magyar → romaji, C: ábra → romaji,
   D: romaji → ábra; mindegyik előfeltétele.
   *Forrás:* `docs/REQUIREMENTS.md` FR-12, FR-13; skill 1. pont.
2. **Bemenet és kimenet** – `QuizRequest`, `QuizPlan`, `QuizQuestion`; a kérdés csak
   technikaazonosítókat tárol.
   *Forrás:* `docs/ARCHITECTURE.md` 4.2, ADR-006; skill 2. pont.
3. **Invariánsok** – pontosan 4 opció, pontosan 1 helyes, 3 különböző téves, nincs szinonima
   a tévesek között, nincs vizuálisan azonos ábra, nincs indokolatlan ismétlés, seed-determinizmus,
   nincs generált opció.
   *Forrás:* skill 3. pont; tesztek: TR-02.
4. **Distraktorválasztás** – prioritás: kategórián belül → más kategóriából → a kérdés
   kihagyása. Kizárási szűrők.
   *Forrás:* skill 4. pont; CR-07.
5. **Fallback és UI-üzenetek** – `skippedTypes`, `shortfall`, üres `questions` kezelése.
   *Forrás:* skill 5. pont; FR-22; `docs/FUNCTIONAL_SPEC.md` 5.1.
6. **Válaszkezelés** – egyszeri válasz (ViewModel + DB egyedi index), állapotmegőrzés,
   megszakítás, hibák újragyakorlása új munkamenetben.
   *Forrás:* skill 6. pont; FR-18, FR-19, FR-21; ADR-005.
7. **Eredményszámítás** – csak a megválaszolt kérdésekből; nulla válasz → nincs százalék;
   az önértékelés nem kvíztalálat.
   *Forrás:* skill 7. pont; FR-27, FR-28.

## Miért nincs még kiadva

A dokumentum normatív állításai csak akkor hitelesek, ha a kódhoz vannak mérve. Mivel a
`QuizGenerator` még nem létezik (Fázis 2.4) és egyetlen teszt sem futott
(`docs/TEST_REPORT.md`, B-01 blokkoló), a kiadott változat a tényleges implementáció és a
zöld TR-02/TR-03 tesztek után íródik meg – pontos példákkal és a seed-viselkedés
dokumentált kimenetével.
