# Tudástár – séma, bővítés, hitelesítés, licenc, migráció

> **Állapot: VÁZLAT.** Ez a dokumentum a Fázis 3.3 lépésben kerül kiadott állapotba, a
> tényleges JSON séma és a demó adatkészlet elkészítésével együtt. A normatív sémaleírás
> jelenleg a fejlesztői skillben él: **`.claude/skills/kyo-knowledge-base/SKILL.md`**.
> A tartalmi integritás szabályai: **`.claude/skills/kyokushin-content/SKILL.md`**.

## Tartalomjegyzék (a kiadott változat szerkezete)

1. **Fájlszerkezet** – `assets/knowledge/manifest.json`, `techniques.json`,
   `assets/knowledge/illustrations/`.
2. **`manifest.json` séma** – `schemaVersion`, `contentVersion`, `datasetKind`,
   `categories[]`, `illustrations[]` (licencnyilvántartás).
3. **`techniques.json` séma** – rekordmezők, kötelező és opcionális mezők, példák.
   *Forrás:* `docs/REQUIREMENTS.md` DR-01…DR-03.
4. **Validációs szabályok** – V001…V010, súlyosság, és hogy melyik szabály mit okoz.
   *Forrás:* skill 4. pont; tesztek: TR-01.
5. **Új technika hozzáadása** – a hat lépéses folyamat, UI- és kvízmotor-kódmódosítás nélkül.
   *Forrás:* FR-30, AC-08.
6. **Hitelesítési folyamat** – ki és hogyan állítja `verified: true`-ra egy rekordot;
   a demó → éles átmenet. **Nyitott kérdés: OQ-02 (ki a szakmai hitelesítő).**
   *Forrás:* CR-01, CR-02.
7. **Illusztrációk és licencelés** – forrás, szerző, licenc nyilvántartása; mi tilos
   (harmadik fél appjából kinyert kép, generált „technikarajz"); képhelyőrző használata.
   *Forrás:* CR-04, CR-05, CR-06.
8. **Migráció** – `schemaVersion` emelés, technika átnevezése, visszavonása (`retired`),
   és a felhasználói előrehaladás megőrzésének garanciája.
   *Forrás:* DR-06…DR-08, ADR-004; teszt: TR-05.
9. **Éles adatkészlet importja** – a szakmai ellenőrzésre előkészített CSV/JSON formátum.
   *Forrás:* FR-34.

## Jelenlegi tartalmi állapot

**Nincs még adatkészlet.** A projektben jelenleg **nulla** technikarekord és **nulla**
illusztráció van. Ez tudatos: a mesterprompt 3. pontja és a `kyokushin-content` skill
szerint kitalált japán terminológiát vagy nem ellenőrzött rajzot nem állítunk elő hiteles
tananyagként.

A Fázis 3.2 lépésben **elkülönített demó adatkészlet** készül:
`datasetKind: "demo"`, minden rekord `verified: false` és `quizEligible: false`,
képhelyőrzőkkel. Következmény: a kép-alapú kvíztípusok (C, D) nem aktiválhatók, és a
kvíz hitelesként nem indítható – a UI ezt konkrét üzenettel közli (FR-22).

**Nyitott kérdés (OQ-01):** van-e jogtisztán felhasználható, szakmailag ellenőrzött
technikalista és illusztrációkészlet, amiből éles adatkészlet építhető?
