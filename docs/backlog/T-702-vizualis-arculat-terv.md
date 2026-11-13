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

## Teendők és készültségi feltételek

- [ ] A jelenlegi Material 3 téma (szín, tipográfia, komponensalapértelmezések)
  áttekintése – melyik elem adja a "generikus form" hatást (pl. szabványos
  `OutlinedTextField`-ek, alapértelmezett `Card` árnyékok, egyenletes rácsos
  elrendezés, hiányzó egyedi ikonográfia).
- [ ] Konkrét javaslatok írása (nem kódként, hanem döntési lista formájában), legalább:
  - egyedi, a T-701 kandzsi logóból levezetett szín-paletta (sötétkék elsődleges +
    kiegészítő tónusok, a gi fehér/kék kontrasztjára építve);
  - tipográfiai hangsúly japán szövegre (pl. nagyobb, vastagabb romaji cím a
    kártyákon, a magyar jelentés alárendelt másodlagos szövegként);
  - kategória-ikonográfia iránya (lásd T-705) és hogy ez hogyan jelenik meg
    listákon/kártyákon is, nem csak a Tanulásban;
  - kártya-/felület-mélység (árnyék, lekerekítés, szegély) egy letisztultabb,
    "dojo"-jellegű nyugodtabb felület irányába, a Material 3 vendégkomponensek
    alapértelmezett kinézete helyett.
- [ ] A javaslatokat **nem szabad** kitalált kyokushin-szimbólumokkal vagy
  hitelesítetlen vizuális motívummal alátámasztani (CLAUDE.md tartalmi integritás) –
  csak a T-701 logóból levezetett, semleges grafikai elemek.
- [ ] A dokumentum konkrét `Color`/`Typography`/`Shape` érték-javaslatokat ad, amiket egy
  későbbi taskban (nem ebben) a `core/ui` téma-modul tényleges kódja követ.
- [ ] A dokumentum helye: `docs/DESIGN_NOTES.md` (új fájl), `docs/CONVENTIONS.md`-ből
  linkelve.
- [ ] `docs/IMPLEMENTATION_PLAN.md` bővítési szakaszának frissítése, commit a taskra
  hivatkozva.
