# T-705 · Tanulás – kategória-ikonok checkbox helyett, technikalista drill-down

| | |
|---|---|
| Modul | feature/learn, core/ui |
| Függ | — |
| Követelmény | FR-39 (`docs/REQUIREMENTS.md` 3.6) |
| Teszt | ViewModel teszt a kategória-kiválasztásra/drill-down állapotra, `@Preview` |

A jelenlegi `learn/setup` képernyő checkbox-okkal választatja ki a kategóriákat
(US-09/US-10 eredeti elfogadási köre). A felhasználó ehelyett ikonnal jelölt
kategóriakártyákat kér, amire koppintva a kategória technikái **egyenként**, böngészhető
listában jelennek meg – azaz a kategóriaválasztás és a konkrét technika-kiválasztás két
külön lépés lesz checkbox-tömb helyett.

## Teendők és készültségi feltételek

- [ ] `core/ui`-ban kategóriánkénti ikonkészlet (vektoros, semleges, **nem** kitalált
  karate-szimbólum – pl. Material ikonok karate-releváns absztrakcióval: ököl, rúgás
  irányú vonal, állás-sziluett; ha egy kategóriához nincs egyértelmű ikon, egységes
  helyőrző ikon jár neki, a kyokushin-content skill tartalmi integritás szabálya
  szerint nem gyártunk hamis vizuális jelentést).
- [ ] A meglévő `learn/setup` ViewModel kategória-kiválasztási állapotának
  átalakítása: checkbox-tömb helyett kategóriakártya-lista + "kiválasztott kategória"
  állapot, ami megnyitja a technikalistát.
- [ ] Új alképernyő/állapot: a kiválasztott kategórián belüli technikák listája,
  amelyből a felhasználó elindíthatja a tanulókártyákat (akár egy technikával is,
  akár többet kijelölve – döntsd el és dokumentáld, hogy az eredeti "több kategória
  egyszerre" lehetőség (US-09) hogyan él tovább ebben az új folyamatban).
- [ ] Forgatás: a kiválasztott kategória és az esetleges technika-kijelölés túléli a
  forgatást (`SavedStateHandle`, a meglévő mintát követve).
- [ ] `contentDescription` minden kategória-ikonra (a kategória magyar nevével), érintési
  célpont ≥ 48 dp.
- [ ] `@Preview` világos és sötét témára, üres/hiba állapotokra is.
- [ ] ViewModel tesztek a kategória-kiválasztás és a drill-down állapotváltásra.
- [ ] `./gradlew testDebugUnitTest assembleDebug` zöld.
- [ ] `docs/IMPLEMENTATION_PLAN.md` bővítési szakaszának frissítése, commit a taskra
  hivatkozva.
