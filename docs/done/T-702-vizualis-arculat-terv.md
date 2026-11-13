# T-702 · Vizuális arculat terv – ne hasson "webes űrlapnak"

| | |
|---|---|
| Modul | docs, core/ui |
| Függ | T-701 |
| Követelmény | FR-36 (`docs/REQUIREMENTS.md` 3.6) |
| Teszt | — (tervdokumentum; a belőle következő UI-taskok kapják a tényleges tesztet) |

A felhasználó konkrét fő panasza: az app jelenlegi megjelenése "nagyon úgy néz ki, mint
egy webes űrlap". Ez a task **nem ír Compose-kódot**, hanem egy rövid, konkrét
dizájnjavaslat-dokumentumot készít, amire a további UI-finomító taskok (T-705, T-706 és
a későbbiek) építhetnek.

---

## Elkészült

| | |
|---|---|
| Dátum | 2026-11-13 |
| Commit | lásd a következő commit üzenetét (`docs(design): …[T-702]`) |

**Futtatott parancsok:** nincs (tisztán dokumentációs task, nincs Kotlin/Compose-kód
változás – a `Teszt` mező szerint ennek a tasknak nincs saját automatizált tesztje).

**Megjegyzés:** a tervet kódba ültető feladatot felvettem a backlogba (`T-712 · core/ui:
egyedi Typography és Shapes bevezetése`), `Függ: T-702`. A szín-/forma-döntések
véglegesítése és a `ThemeContrastTest` újrafuttatása ott történik, nem itt.

## Teendők és készültségi feltételek

- [x] A jelenlegi Material 3 téma (szín, tipográfia, komponensalapértelmezések)
  áttekintése – melyik elem adja a "generikus form" hatást (pl. szabványos
  `OutlinedTextField`-ek, alapértelmezett `Card` árnyékok, egyenletes rácsos
  elrendezés, hiányzó egyedi ikonográfia). **Eredmény:** `docs/DESIGN_NOTES.md` 1.
  szakasza – nincs egyedi `Typography`/`Shapes` a `Theme.kt`-ban, `OutlinedTextField`
  a keresőn, nyolc fájl duplikálja ugyanazt a `RoundedCornerShape(12.dp)`-t.
- [x] Konkrét javaslatok írása (nem kódként, hanem döntési lista formájában) –
  `docs/DESIGN_NOTES.md` 3–6. szakasza: szín-paletta a T-701 logóból levezetve
  (`primary` → `#17317A`), tipográfiai szerepkiosztás (romaji **Bold** cím, magyar
  jelentés alárendelt), kategória-ikonográfia iránya (T-705-nek), megnevezett
  `Shapes` a duplikált literálok helyett.
- [x] A javaslatok **nem** tartalmaznak kitalált kyokushin-szimbólumot vagy
  hitelesítetlen motívumot – `docs/DESIGN_NOTES.md` 7. szakasza explicit felsorolja,
  mi NEM szabad (CR-08 + CLAUDE.md tartalmi integritás).
- [x] A dokumentum konkrét `Color`/`Typography`/`Shape` érték-javaslatokat ad; a
  tényleges kódolásra **új backlog-tételt** vettem fel (`T-712`, Függ: T-702).
- [x] A dokumentum helye: `docs/DESIGN_NOTES.md`, `docs/CONVENTIONS.md`-ből linkelve.
- [x] `docs/IMPLEMENTATION_PLAN.md` bővítési szakaszának frissítése, commit a taskra
  hivatkozva.
