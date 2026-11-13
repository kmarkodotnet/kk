# T-706 · Tanulókártya – felső tab "Japán nevek" / "Magyar nevek"

| | |
|---|---|
| Modul | feature/learn |
| Függ | T-705 |
| Követelmény | FR-40 (`docs/REQUIREMENTS.md` 3.6) |
| Teszt | ViewModel teszt a fülváltásra, `@Preview` |

A jelenlegi elrejtési irány (US-10: "japán nevet rejtse el" vagy a magyart) egy
beállításként él. A felhasználó ehelyett egy közvetlenül a kártya felett lévő
fülváltót kér: "Kiindulás: Japán nevek" / "Magyar nevek".

## Teendők és készültségi feltételek

- [ ] A kártya képernyő tetején `TabRow` (vagy `SecondaryTabRow`) két füllel: "Japán
  nevek" és "Magyar nevek" – a kiválasztott fül határozza meg, melyik oldal **indul
  felfedve**, és melyik van elrejtve (FR-07 logika megtartva, csak a vezérlő felület
  cserélődik checkboxról/switch-ről fülekre).
- [ ] Fülváltáskor az aktuális kártya **visszaáll** elrejtett állapotba (konzisztens a
  US-10 3. elfogadási kritériumával: a felirat elrejtése nem sérülhet), de a
  kártyapozíció (hányadik technikánál tartunk) **nem** változik.
- [ ] A fülállapot `SavedStateHandle`-ben tárolva, túléli a forgatást.
- [ ] A korábbi, önálló "elrejtési irány" beállítás UI-eleme (ha külön képernyőn/
  kapcsolóként élt) eltávolítása vagy a fülekre való explicit átvezetése – nem
  maradhat két egymásnak ellentmondó vezérlő ugyanarra az állapotra.
- [ ] Akadálymentesség: a fülek `selected` szemantikával, váltás után
  `stateDescription`/bejelentés.
- [ ] `@Preview` mindkét fülállapotra, világos és sötét témában.
- [ ] ViewModel teszt: fülváltás hatása az elrejtett/felfedett állapotra, pozíció
  megtartása, forgatás utáni visszaállás.
- [ ] `./gradlew testDebugUnitTest assembleDebug` zöld.
- [ ] `docs/IMPLEMENTATION_PLAN.md` bővítési szakaszának frissítése, commit a taskra
  hivatkozva.
