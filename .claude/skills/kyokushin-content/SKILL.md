---
name: kyokushin-content
description: Kyokushin karate tartalmi integritás szabályai - mikor szabad egy technikanevet, magyar jelentést, övfokot vagy illusztrációt hitelesnek jelölni, és hogyan kezeld a nem ellenőrzött adatot. Használd minden olyan munkánál, ami karate technikaneveket, romaji átírást, magyar megnevezést, kategóriát, övfokot, illusztrációt vagy demó adatkészletet érint.
---

# Kyokushin tartalmi integritás

Ez a skill **nem** karate-tudástár. Ez a szabálykészlet arról, hogy mit **nem** szabad
kitalálni, és hogyan jelöld azt, amit nem tudsz ellenőrizni.

## 1. Alapelv: a modell nem hiteles forrás

A nyelvi modell generálhat hihetőnek tűnő japán terminológiát, ami téves átírás, rossz
kategória vagy nem létező kifejezés. Egy karate tananyagban ez káros: a felhasználó
rosszat tanul meg, és a kvíz megerősíti a hibát.

**Ezért:** a modell által generált technikanév, magyar jelentés, japán íráskép, övfok-hozzárendelés
vagy magyarázat **soha nem** kaphat `verified = true` vagy `quizEligible = true` értéket.

## 2. Hitelesítési státuszok

| Státusz | `verified` | `quizEligible` | UI jelzés | Honnan jöhet |
|---|---|---|---|---|
| Hitelesített | `true` | `true` (ha a mezők elegendőek) | nincs külön jelzés | emberi szakmai ellenőrzés (dojo/szenszei/hivatalos szövetségi anyag), dokumentált forrással |
| Ellenőrzésre vár | `false` | `false` | „Demó / ellenőrzésre vár" badge | import, de még nincs szakmai jóváhagyás |
| Hiányos | `false` | `false` | „Hiányos rekord" + a hiányzó mező megnevezése | validátor jelölte |

A `verified = true` átállítása **emberi döntés**. Kódból, importból, migrációból
automatikusan `true`-ra állítani tilos. A validátor csak `false`-ra állíthat le egy rekordot,
soha nem emelhet státuszt.

## 3. Demó adatkészlet

Amíg nincs jogtisztán felhasználható, szakmailag ellenőrzött tartalom:
- Készíts **elkülönített** demó adatkészletet (`contentVersion` jelzi, pl. `0.1.0-demo`).
- A manifest `datasetKind: "demo"` mezője legyen explicit.
- Minden rekord `verified = false`, `quizEligible = false`.
- A demó adatkészlet mellett a **séma** és a **motor** teljes értékű legyen – a demó az
  adat minőségére vonatkozik, nem a kód minőségére.
- A UI-ban globálisan is jelenjen meg, hogy demó adatkészlet van betöltve.

Fontos következmény: demó adatkészlettel a kvíz **nem** indítható el hitelesként.
A kvíz képernyőnek világos üzenetet kell adnia: „Nincs elég hitelesített technika a kvízhez."
Ez nem hiba, hanem a tartalmi integritás helyes működése (lásd `docs/QUIZ_RULES.md` fallback).

## 4. Illusztrációk

- **Tilos:** harmadik fél alkalmazásából, weboldalról, könyvből kinyert, licenc nélküli kép.
- **Tilos:** generatív képmodellel előállított „karate technika rajz" hiteles tananyagként –
  a testhelyzet és a mozgásirány tipikusan hibás.
- **Megengedett:** explicit képhelyőrző (placeholder). Ilyenkor a kép-alapú kvíztípusok
  (C: rajz → japán név, D: japán név → 4 rajz) **nem aktiválhatók**.
- **A képbe égetett név felfedi a választ.** Ha a képen látszik a technika azonosítója,
  japán vagy magyar neve, akkor a kép:
  - a (C) és (D) kvíztípusban **nem használható** (leírja a helyes választ), és
  - a tanulókártyán felfedés előtt **nem mutatható**.
  Ez független a szakmai hitelességtől: egy tökéletesen ellenőrzött, de feliratos kép is
  használhatatlan kvízben. A manifest ezért külön `has_embedded_label`, `quiz_usable` és
  `hideable_on_study_card` mezőt hordoz (ADR-011). A megállapítás emberi megfigyelés –
  minden új képet meg kell nézni. A jó megoldás: felirat nélküli képek, a nevet az app rajzolja.
- Minden kép mellé dokumentálni kell: forrás, szerző, licenc, a felhasználás jogalapja.
  Hely: `docs/KNOWLEDGE_BASE.md` licenctáblázat + a manifest `illustrations[]` bejegyzései.
- A rajznak egyértelműen mutatnia kell a testhelyzetet és a mozgásirányt. Többfázisú
  mozdulatokhoz a séma később képsorozatot támogat – de MOST egy kép/technika.

## 5. Kategóriák

A kategóriarendszer szerkezeti, nem tartalmi kérdés, ezért kódolható; de a kategóriába
sorolás tartalmi döntés. Szerkezetileg használt kategóriaazonosítók (a mesterprompt
példái alapján): állások (`tachi`), ütések (`tsuki`), védések (`uke`), rúgások (`geri`).
Bármely további kategória felvétele szakmai ellenőrzéshez kötött; a magyar
kategórianevek a manifestben vannak, nem a kódban hardcode-olva.

## 6. Szinonimák és névváltozatok

Egy technikának lehet több elfogadható névváltozata (`aliases`). Ebből két szabály következik:
- A helyes válasz összehasonlítása **mindig technikaazonosítóval** történik, soha nem
  szabad szöveges egyezéssel.
- Két rekord, amelyek szinonimák vagy azonos fogalmat jelölnek, **nem lehetnek egymás
  distraktorai** egy kvízkérdésben. Ezt a tudástár validátora tárja fel és jelöli meg.

## 7. Ellenőrzőlista minden tartalmi változtatáshoz

- [ ] Nem generáltam új japán kifejezést, átírást vagy magyar jelentést hitelesként.
- [ ] Minden új/módosított rekord `verified`/`quizEligible` értéke indokolt és dokumentált.
- [ ] Minden képhivatkozás létező fájlra mutat, vagy explicit placeholder.
- [ ] Minden kép licence dokumentált.
- [ ] A demó/éles adatkészlet elkülönítése nem sérült.
- [ ] A UI jelzi a nem hitelesített státuszt.
