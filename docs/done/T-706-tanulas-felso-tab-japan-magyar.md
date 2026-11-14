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

---

## Elkészült

| | |
|---|---|
| Dátum | 2026-11-14 |
| Commit | lásd a következő commit üzenetét (`feat(learn): …[T-706]`) |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew.bat testDebugUnitTest assembleDebug --console=plain` | 0 | `BUILD SUCCESSFUL`, 659 teszt összesen (656 → +3: 5 új `LearnCardsViewModelTest`, 2 törölve `LearnSetupViewModelTest`-ből), 0 hiba |
| `./gradlew.bat ktlintCheck lint --console=plain` | 0 | `BUILD SUCCESSFUL`; `feature/learn`, `navigation` és `core/ui` lint-jelentése is üres |

**Megjegyzés:** lásd a fenti "Tervezési döntés" és "Szándékos alapértelmezés-váltás"
bekezdéseket, és a `docs/CONVENTIONS.md` 8. szakaszának új sorát a
`FUNCTIONAL_SPEC.md` 4.1/3. és 4.2 elmaradt frissítéséről (nem volt a task
modulhatárán belül). A `navigation` modul route-ja (`KyokushinScreen.LEARN_CARDS`,
`KyokushinRoutes.learnCards`) is módosult a `hideMode` argumentum eltávolításával –
mechanikus, a `learn/setup` szerződésváltásának közvetlen következménye.

## Teendők és készültségi feltételek

- [x] A kártya képernyő tetején `SecondaryTabRow` két füllel: "Japán nevek" és "Magyar
  nevek" – a kiválasztott fül határozza meg, melyik oldal **indul felfedve** (a fül
  neve a KEZDŐ, látható oldalt nevezi meg, nem a rejtettet – lásd `HideModeTabRow`
  KDoc-ja). FR-07 logikája megtartva, csak a vezérlő felülete cserélődött rádiógombról
  fülekre, **és** a munkamenet szintjéről a kártya szintjére költözött.
- [x] Fülváltáskor az aktuális kártya **visszaáll** elrejtett állapotba
  (`LearnCardsViewModel.onSelectHideMode`), de a kártyapozíció **nem** változik, és a
  már megtekintett, más pozíción álló kártyák felfedettsége érintetlen marad – csak az
  éppen látott kártyáé törlődik.
- [x] A fülállapot `SavedStateHandle`-ben tárolva (`KEY_HIDE_MODE`, most már
  **reaktívan**, `getStateFlow`-val, nem egyszer kiolvasott `val`-ként), túléli a
  forgatást és a folyamat-újraindítást.
- [x] A korábbi, önálló "Mit rejtsünk el?" rádiógombos választó **eltávolítva** a
  `learn/setup`-ból (`HideModeSelector`/`HideModeOption` törölve, `LearnSetupUiState`/
  `ViewModel`/`Screen` már nem ismeri a `HideMode`-ot) – nem maradt két egymásnak
  ellentmondó vezérlő. Ez a `LearnSetupScreen`/`LearnSetupRoute`/`onStartCards`
  szerződését is érintette (`(Set<CategoryId>, HideMode) -> Unit` →
  `(Set<CategoryId>) -> Unit`), ami a deklarált modulhatáron (`feature/learn`) túl a
  `navigation` modul route-ját (`KyokushinScreen.LEARN_CARDS`, `KyokushinRoutes.learnCards`,
  `KyokushinNavHost`) is érintette – mechanikus, a `hideMode` route-argumentum puszta
  törlése, dokumentálva a KDoc-okban.
- [x] Akadálymentesség: a Material3 `Tab` már önmagában `Role.Tab` + `selected`
  szemantikát ad, amit a TalkBack fülváltás után automatikusan bejelent – nem kellett
  külön `stateDescription`.
- [x] `@Preview` mindkét fülállapotra, világos és sötét témában – a meglévő
  `LearnCardsScreen` előnézetek változatlanok (a fülváltó mindegyikben megjelenik),
  az `onSelectHideMode` paraméter mind a hat előnézetbe bekötve.
- [x] ViewModel teszt: 5 új teszt (`LearnCardsViewModelTest`) – alapértelmezett irány,
  fülváltás hatása az állapotra, aktuális kártya visszaállása pozícióváltás nélkül,
  más kártyák felfedettségének érintetlensége, forgatás utáni megőrzés.
- [x] `./gradlew testDebugUnitTest assembleDebug` zöld (659 teszt, 0 hiba).
- [x] `docs/IMPLEMENTATION_PLAN.md` bővítési szakaszának frissítése, commit a taskra
  hivatkozva.

**Tervezési döntés (a fül neve ↔ `HideMode` leképezés):** a felhasználó megfogalmazása
("Kiindulás: Japán nevek, Magyar nevek") a **kezdő, látható** oldalt nevezi meg, nem a
rejtettet – ezért "Japán nevek" fül = `HideMode.HIDE_HUNGARIAN` (a romaji látszik
elsőként), "Magyar nevek" fül = `HideMode.HIDE_JAPANESE` – ez a `HideMode` enumnevekhez
képest fordított leképezés, dokumentálva a `HideModeTabRow` KDoc-jában.

**Szándékos alapértelmezés-váltás:** a korábbi `learn/setup` alapértelmezése
`HIDE_JAPANESE` volt (a magyar név látszott elsőként). Az új alapértelmezés
`HIDE_HUNGARIAN` ("Japán nevek" fül aktív először), mert a felhasználó kérésének
szövege a japán nevet említi elsőként ("Kiindulás: Japán nevek, Magyar nevek") – ez egy
tudatos, dokumentált viselkedésváltás, nem hiba.
