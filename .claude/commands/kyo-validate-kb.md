---
description: A tudástár JSON (manifest + techniques) teljes validálása és jelentés.
allowed-tools: Bash, PowerShell, Read, Grep, Glob, Skill
---

Töltsd be a `kyo-knowledge-base` skillt, majd validáld a tudástárat:
`app/src/main/assets/knowledge/manifest.json` és `.../techniques.json`.

Ellenőrzések (mind kötelező, mind soronként jelentve):
1. **Séma:** `schemaVersion` támogatott-e; `contentVersion` jelen van-e.
2. **Kötelező mezők:** `id`, `categoryId`, `romaji`, `hungarian` nem üres.
3. **Egyedi ID:** nincs duplikált `id`; az ID stabil formátumú.
4. **Kategória:** minden `categoryId` létezik a manifest kategórialistájában.
5. **Kép:** ha `illustration` nem null, a fájl TÉNYLEGESEN létezik az assetsben.
6. **Duplikátum/szinonima:** azonos vagy ütközően hasonló `romaji`/`hungarian`/`aliases`
   párok felsorolása – ezek distraktorként tilosak (lásd `docs/QUIZ_RULES.md`).
7. **Kvízképesség:** `quizEligible = true` csak akkor jogos, ha `verified = true` ÉS a
   szükséges mezők (romaji, hungarian, és képes kvíztípushoz `illustration`) megvannak.
8. **Kvízkészenlét kategóriánként:** van-e elég jelölt a 4 válaszos kérdésekhez
   (kategórián belül legalább 4 egymást nem kizáró, nem szinonim rekord).

Jelentés: táblázatos hibalista (súlyosság: BLOKKOLÓ / FIGYELMEZTETÉS), kategóriánkénti
kvízkészenléti összegzés, és mely kérdéstípus aktiválható jelenleg.
Ha a tudástár fájl még nem létezik, mondd ki egyszerűen, és ne találj ki adatot.
