# ADR-011 – A kapott tudástár sémája a kanonikus formátum

- **Állapot:** elfogadva (2026-11-11, felhasználói döntés)
- **Felülírja:** az `ARCHITECTURE.md` 5. és a `kyo-knowledge-base` skill 2–3. pontjában
  tervezett saját sémát (`techniques`, `verified`, `illustration`, camelCase).
- **Kontextus:** a `knowledgebase/kyokushin_knowledge_hu.json` és a vele párhuzamos
  CSV a tényleges tartalom forrása, saját dokumentált sémával és README-vel.

## Döntés

A projekt **a kapott séma** szerint dolgozik, kiegészítve a hiányzó mezőkkel.
Nincs konverziós réteg és nincs párhuzamos „belső" JSON formátum.

### A kanonikus manifest (az `entries`-en kívüli rész)

```json
{
  "schema_version": "1.0.0",
  "dataset_version": "0.1.0",
  "language": "hu-HU",
  "martial_art": "Kyokushin karate",
  "editorial_status": "draft_review_required",
  "notes": ["..."],
  "categories": [ { "id": "etiquette", "name_hu": "Etikett és köszönések" } ]
}
```

### A kanonikus rekord

```json
{
  "id": "KY-0001",
  "category_id": "etiquette",
  "japanese_romaji": "Osu",
  "japanese_romaji_ascii": "Osu",
  "japanese_script": null,
  "hungarian_name": "Tiszteletteljes köszönés és válasz",
  "description_hu": "...",
  "image_path": null,
  "aliases": [],
  "tags": [],
  "review_status": "editorial_review_required"
}
```

### Kiegészítések (visszafelé kompatibilisen, mind opcionális)

| Mező | Hol | Miért |
|---|---|---|
| `categories[].sort_order` | manifest | a kategóriák sorrendje a UI-ban (DR-02); hiányában a tömbsorrend |
| `illustrations[]` (`ref`, `file`, `license`, `source`, `author`) | manifest | képlicenc-nyilvántartás (CR-04); amíg nincs kép, üres tömb |
| `quiz_excluded` | rekord | explicit, emberi kézzel megadott kizárás a kvízből, ha egy tétel pedagógiailag nem kvízelhető |
| `retired` | rekord | ADR-004: a tananyagból kivont, de az előrehaladásban megőrzött tétel |

A `quiz_eligible` **szándékosan nem** kerül a fájlba: az számított érték
(ADR-010 formulája), nem tartalmi adat. Ugyanígy a `verified` sem – azt a
`review_status` helyettesíti.

### Leképezés a domain modellre

| Fájl | Domain (`core/model`) |
|---|---|
| `review_status: "reviewed"` | `VerificationStatus.VERIFIED` |
| `review_status: "editorial_review_required"` | `VerificationStatus.AWAITING_REVIEW` |
| validátor jelölte hiányosság | `VerificationStatus.INCOMPLETE` (ADR-002) |
| `image_path: null` | `IllustrationRef` = `null` |
| `image_path` megadva, fájl létezik | `IllustrationRef.Asset` |
| `image_path` megadva, fájl hiányzik | `IllustrationRef.Placeholder` (ADR-003, V005) |
| `japanese_romaji` | `romaji` |
| `japanese_romaji_ascii` | a normalizált keresési index alapja (FR-03) |
| `hungarian_name` | `hungarian` |

A `schema_version` **string** (semver), nem egész szám: a támogatott verziók halmaza
`{"1.0.0"}`; az ismeretlen major verzió a betöltés elutasítása (V001).

## Mérlegelt alternatívák

| Alternatíva | Miért nem |
|---|---|
| Marad a tervezett séma, egyszeri konverterrel | Minden későbbi tartalomfrissítés újrakonvertálást kívánna, és a tartalomért felelős ember nem abban a fájlban dolgozna, amit az app olvas. Két igazság = idővel eltérő igazság. |
| Kettős támogatás (mindkét séma olvasása) | Kétszer annyi validátor-kód és tesztfelület, valódi haszon nélkül. |

## Következmények

- A `techniques.json` név helyett a csomagolt fájl a kapott elnevezést követi; az
  `app/src/main/assets/knowledge/` alá a `knowledgebase/` fájljai kerülnek (a build
  bemásolja, a `knowledgebase/` marad a szerkesztési forrás).
- Az `aliases` és `tags` tömb jelenleg mindenhol üres, de a séma része – a validátor és a
  szinonimakizárás (V006, CR-07) ettől függetlenül működik.
- A **CSV a szerkesztési felület**, a JSON az app bemenete. Ha a kettő eltér, a JSON a
  hiteles; a `/kyo-validate-kb` parancs ellenőrzi az egyezést is.
- A `docs/KNOWLEDGE_BASE.md` és a `kyo-knowledge-base` skill sémaleírását ehhez kell
  igazítani (Fázis 3.3).
