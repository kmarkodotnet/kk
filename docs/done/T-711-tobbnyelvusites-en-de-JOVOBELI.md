# T-711 · Többnyelvűsítés (tetszőleges számú nyelv, valódi EN/DE tartalommal)

| | |
|---|---|
| Modul | összes `feature/*`, `core/model`, `core/data`, `core/quiz`, `core/ui` |
| Függ | — (2026-11-14: **OQ-07 feloldva**, felhasználói döntés – lásd [ADR-013](../adr/ADR-013-multi-language-content.md)) |
| Követelmény | FR-43 (`docs/REQUIREMENTS.md` 3.7) |
| Teszt | `core/model` lokalizáció-visszaesés, `core/data` `MIGRATION_2_3` + séma-szerződés + valós-tartalom fordítás-ellenőrzés |

> **2026-11-14: a felhasználó explicit felülírta** a `CLAUDE.md` korábbi, kizárólag
> magyar vasszabályát és a mesterprompt 9. pontjának erre vonatkozó korlátozását –
> azzal a kéréssel, hogy **tetszőleges számú nyelv** támogatható legyen, ne csak az
> eredeti cím szerinti angol/német. A task ezért a korábbi, "JÖVŐBELI, NEM INDÍTHATÓ"
> blokkolt állapotból kikerült és elkészült. A fájlnév a történeti nyomkövetésért
> maradt `-JOVOBELI` végű.

A felhasználó a magyar mellett angol és német felhasználói szöveget és
technikatartalmat kért, rendszernyelv-követéssel (nincs saját in-app nyelvválasztó).

## Teendők és készültségi feltételek

- [x] `CLAUDE.md` vasszabályának módosítása (feloldja OQ-07-et), `docs/REQUIREMENTS.md`
  OQ-07 lezárása + FR-43, `docs/BACKLOG.md` E9→E10 átszervezés, [ADR-013](../adr/ADR-013-multi-language-content.md).
- [x] UI-kezelőszövegek: minden `strings.xml` mellé `values-en/`, `values-de/` (7 modul),
  ugyanazokkal a kulcsokkal – új nyelv felvétele ezen a rétegen ezután egyetlen új
  `values-<lang>/strings.xml`, nulla kódmódosítás.
- [x] A technikatartalomra is kiterjed: egy párhuzamos munka már elkészítette a 161
  technika és 9 kategória valódi angol/német fordítását a szerkesztési forrásban
  (`docs/CONTENT_TRANSLATIONS.md`) – a `knowledgebase_transfer/tools/convert.py`
  bővítve, hogy ezt nyitott kulcsú `name_translations`/`description_translations`
  map-ekbe (nem fix mezőnév nyelvenként) vezesse át a kanonikus sémába, `dataset_version`
  léptetve.
- [x] `core/model` (`Technique`/`Category` fordítás-map + `localizedName`/
  `localizedDescription`, magyar visszaesés, a `hu` kulcs tiltva a mapben),
  `core/data` (DTO-k, mapperek, Room séma `2 → 3`: `MIGRATION_2_3`, JSON-string
  oszlop `@TypeConverter`-rel – nem relációs tábla, mert a fordítás sosem
  SQL-szűrési cél), `core/quiz` (validátor bemenet és leképezés bővítve, nincs új
  blokkoló szabály).
- [x] Megjelenítés: `core/ui` `currentLanguageTag()` (`Locale.getDefault().language` –
  Android szinkronban tartja a rendszer/alkalmazás-nyelvvel). A `feature/learn`,
  `feature/library`, `feature/progress` és a `feature/quiz` kategória-szűrője
  `localizedName()`-et hív a korábbi `.hungarian`/`.nameHu` helyett.
- [x] Nyelvválasztás: rendszerbeállítás követése (a felhasználó kérése), nincs saját
  in-app választó.
- [x] Szándékosan kimarad (dokumentált, flaggelt követő feladat): a kvízmotor
  (`core/quiz`, `feature/quiz` `QuizRunScreen`/`ViewModel`, `QuizResultViewModel`)
  nyelve – a kérdés/válasz nyelve önálló pedagógiai döntés (ADR-013 6. pont); a
  `translation_review_status` alapú "nem lektorált fordítás" UI-jelölés (a meglévő
  `VerificationBadge`/`UnreviewedBanner` mintáját követné).
- [x] `./gradlew testDebugUnitTest assembleDebug` és `ktlintCheck lint` zöld.
- [x] `docs/IMPLEMENTATION_PLAN.md` bővítési szakaszának frissítése, `docs/TEST_REPORT.md`
  bejegyzés, commit a taskra hivatkozva.

---

## Elkészült

| | |
|---|---|
| Dátum | 2026-11-14 |
| Commit | (lásd a T-711-re hivatkozó commitsorozatot) |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `python knowledgebase_transfer/tools/convert.py` | 0 | 161 rekord, 9 kategória, 0 BLOKKOLÓ; mindegyik kapott `name_translations`/`description_translations` map-et. |
| `.\gradlew.bat testDebugUnitTest assembleDebug --console=plain` | 0 | `BUILD SUCCESSFUL`, 667 teszt (659 → +8), 0 hiba. |
| `.\gradlew.bat ktlintCheck lint --console=plain` | 0 | `BUILD SUCCESSFUL`, minden modul lint-jelentése üres. |

**Megjegyzés:** Két, útközben feltárt mellékhatást javítottam ugyanebben a körben
(részletesen dokumentálva `docs/IMPLEMENTATION_PLAN.md` és `docs/CONVENTIONS.md`
8./10. szakaszában): (1) a `convert.py` első futtatása mellékhatásként felvett 10,
a `knowledgebase/pics/`-ben már létező, de nem regisztrált illusztrációt, amitől a
`core/quiz` valós-tartalmú tesztjei megbuktak – ezeket a fájlokat a konverter
futtatásából ideiglenesen kizártam, a lemezen megmaradtak egy önálló, T-707 6a.
pontja szerinti követő feladatra; (2) az új `values-en/` erőforrások miatt 13
Robolectric teszt kapott `@Config(qualifiers = "hu")`-t, hogy a JVM alapértelmezett
locale-ja helyett a tesztelt magyar szöveget lássák.

**Javasolt, fel nem vett követő feladatok:** (a) a kvízmotor nyelvi konzisztenciája
(milyen nyelven kérdezzen a kvíz, hogyan viselkedjen a szinonima-kizárás többnyelvű
névkészleten); (b) egy "nem lektorált fordítás" UI-jelölés a
`translation_review_status` mezőre, a meglévő `VerificationBadge` mintáját követve;
(c) a 10, `knowledgebase/pics/`-ben már létező, de nem regisztrált illusztráció
(`KY-0054`…`KY-0063`) rendes befogadása (`docs/KNOWLEDGE_BASE.md` 6a. pont).
