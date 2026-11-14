# T-707 · Illusztráció-befogadási munkafolyamat (felhasználó által utólag adott képek)

| | |
|---|---|
| Modul | docs, core/data |
| Függ | — |
| Követelmény | FR-41 (`docs/REQUIREMENTS.md` 3.6) |
| Teszt | ha készül ellenőrző szkript/Gradle-feladat: ahhoz tartozó unit teszt |

A felhasználó a jövőben további illusztrációkat ad majd az alkalmazáshoz. Jelenleg a
folyamat (kép elhelyezése, `illustration_ref` bekötése, verziólépés, validáció) csak a
kezdeti `knowledgebase_transfer/` átvétel (T-601) kontextusában van dokumentálva. Ez a
task egy önállóan követhető, ismételhető munkafolyamatot ad.

## Teendők és készültségi feltételek

- [x] `docs/KNOWLEDGE_BASE.md` kiegészítése egy "Illusztráció utólagos hozzáadása"
  szakasszal: pontos fájlrendszerbeli hely (`app/src/main/assets/knowledge/
  illustrations/`), elnevezési konvenció, a `manifest.json`/`entries.json`
  `illustration_ref` mezőjének bekötése, a `contentVersion`/`dataset_version` léptetés
  szabálya (FR-30).
- [x] Egyértelműsíteni, hogy egy új kép érkezésekor a hozzá tartozó rekord
  `verified`/`editorial_status` mezője **nem** változik automatikusan "hitelesre" –
  a kép puszta megléte nem azonos a szakmai ellenőrzéssel (CLAUDE.md tartalmi
  integritás, lásd T-708).
- [x] Mérlegelni és dokumentálni, hogy kelljen-e egy kis ellenőrző szkript/Gradle-feladat,
  ami importálás előtt jelzi, ha egy becsatolt kép formátuma/mérete/elnevezése nem
  felel meg a `DR-05` validációs elvárásnak (pl. hiányzó `illustration_ref`-találat,
  nem támogatott formátum) – **csak ha ez tényleg hiányzik** a meglévő
  `KnowledgeValidator`/`V004` szabályból; ha már lefedi, elég a dokumentáció.
- [x] A `kyo-knowledge-base` skill (`.claude/skills/kyo-knowledge-base/SKILL.md`)
  hivatkozása az új szakaszra.
- [x] Ha készül szkript/kód: `./gradlew testDebugUnitTest assembleDebug` zöld.
- [x] `docs/IMPLEMENTATION_PLAN.md` bővítési szakaszának frissítése, commit a taskra
  hivatkozva.

---

## Elkészült

| | |
|---|---|
| Dátum | 2026-11-14 |
| Commit | (lásd a következő commit) |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| — | — | Nem futott: tisztán dokumentációs task, nincs kódváltozás. A task fejlécének `Teszt` mezője is feltételes volt ("ha készül ellenőrző szkript/Gradle-feladat"); a Teendők 3. pontjának áttekintése után megállapítottam, hogy **nem** kell új szkript/Gradle-feladat, mert a meglévő `core/quiz` `DefaultKnowledgeValidator` V004/V005 szabálya már gracefully kezeli a hiányzó/lógó illusztráció-hivatkozást. |

**Megjegyzés:** Kódból feltárt, új, dokumentált (nem javított) ismert ügy: a
`knowledgebase_transfer/tools/convert.py` `ILLUSTRATION_PROVENANCE` konstansa
**globális**, minden illusztrációra ugyanazt az `"ai_generated"`/
`"project-internal"` eredetet írja, függetlenül a kép tényleges forrásától – egy
felhasználó-adta (nem AI-generált) kép esetén ez téves eredetet eredményezne a
kiadott `manifest.json`-ban, amíg kézzel nem korrigálják. Részletek és a kézi
javítás menete: `docs/KNOWLEDGE_BASE.md` 6a. pont; a tétel rögzítve
`docs/CONVENTIONS.md` 8. szakaszában is. **Javasolt új task (nincs felvéve):**
`convert.py` bővítése, hogy per-kép `origin`/`license`/`source`/`author` adatot
olvasson be (pl. a `knowledgebase/pics/manifest.json`-ból vagy egy új mezőből), és
csak hiányzó adat esetén essen vissza a globális konstansra.
