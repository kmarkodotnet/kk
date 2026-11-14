# Konverziós és validációs jelentés

**Generálva:** 2026-11-14 · `python knowledgebase_transfer/tools/convert.py`

Ez a fájl **generált** – ne szerkeszd kézzel. A tartalom forrása a `knowledgebase/` mappa.


## Összegzés

| Mutató | Érték |
|---|---|
| Beolvasott CSV sor | 161 |
| Kiírt rekord | 161 |
| Ebből hiányos (INCOMPLETE) | 0 |
| Kategória | 9 |
| Illusztráció összesen | 10 |
| Ebből kvízben használható | 7 |
| Ebből névfelirattal (kvízből kizárva) | 3 |
| Szinonima-/ütközéscsoport | 0 |
| BLOKKOLÓ megállapítás | 0 |
| FIGYELMEZTETÉS | 3 |

## Kategóriánkénti kvízkészenlét

Egy kérdéstípus akkor indítható egy kategóriában, ha legalább **4** alkalmas jelölt van (1 helyes + 3 distraktor).

| Kategória | Rekord | Szöveges kvízre alkalmas | (A)/(B) indítható | Kép | Képes kvízre alkalmas | (C)/(D) indítható |
|---|---|---|---|---|---|---|
| Etikett és köszönések (`etiquette`) | 9 | 9 | igen | 0 | 0 | **nem** |
| Vezényszavak és alapfogalmak (`commands`) | 34 | 34 | igen | 0 | 0 | **nem** |
| Állások (`stances`) | 15 | 15 | igen | 10 | 7 | igen |
| Védések (`blocks`) | 14 | 14 | igen | 0 | 0 | **nem** |
| Kéz- és könyöktechnikák (`punches`) | 23 | 23 | igen | 0 | 0 | **nem** |
| Rúgások és térdtechnikák (`kicks`) | 22 | 22 | igen | 0 | 0 | **nem** |
| Ütőfelületek és testrészek (`body_parts`) | 14 | 14 | igen | 0 | 0 | **nem** |
| Japán számok (`numbers`) | 10 | 10 | igen | 0 | 0 | **nem** |
| Katák (`kata`) | 20 | 20 | igen | 0 | 0 | **nem** |

## Megállapítások

| Kód | Súlyosság | Rekord | Mező | Leírás |
|---|---|---|---|---|
| `I004` | FIGYELM. | KY-0045 | illustration | A képbe bele van égetve a technika neve: kvízben nem használható, és tanulókártyán felfedés előtt nem mutatható. |
| `I004` | FIGYELM. | KY-0050 | illustration | A képbe bele van égetve a technika neve: kvízben nem használható, és tanulókártyán felfedés előtt nem mutatható. |
| `I004` | FIGYELM. | KY-0052 | illustration | A képbe bele van égetve a technika neve: kvízben nem használható, és tanulókártyán felfedés előtt nem mutatható. |

## Kódjegyzék

| Kód | Jelentés |
|---|---|
| `V002` | kötelező mező hiányzik -> a rekord INCOMPLETE, kvízből kizárva |
| `V003` | duplikált azonosító -> a későbbi rekord elvetve |
| `V003b` | az azonosító nem a KY-NNNN formát követi |
| `V004` | ismeretlen kategóriaazonosító |
| `V006` | azonos normalizált név -> kölcsönös distraktor-kizárás |
| `V009` | hiányzó ékezetmentes alak -> származtatva |
| `I001` | a manifest hivatkozik rá, de a képfájl hiányzik |
| `I002` | képfájl manifest-bejegyzés nélkül |
| `I003` | bitre azonos kép két rekordnál |
| `I004` | a képbe bele van égetve a technika neve |
| `C001` | kategória csak a CSV-ben |
| `C002` | kategórianév eltér CSV és JSON között |
| `X001` | rekord csak a CSV-ben |
| `X002` | rekord csak a forrás JSON-ban |
| `X003` | CSV és JSON mezőérték eltér |
