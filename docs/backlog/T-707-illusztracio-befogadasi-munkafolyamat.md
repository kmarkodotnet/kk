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

- [ ] `docs/KNOWLEDGE_BASE.md` kiegészítése egy "Illusztráció utólagos hozzáadása"
  szakasszal: pontos fájlrendszerbeli hely (`app/src/main/assets/knowledge/
  illustrations/`), elnevezési konvenció, a `manifest.json`/`entries.json`
  `illustration_ref` mezőjének bekötése, a `contentVersion`/`dataset_version` léptetés
  szabálya (FR-30).
- [ ] Egyértelműsíteni, hogy egy új kép érkezésekor a hozzá tartozó rekord
  `verified`/`editorial_status` mezője **nem** változik automatikusan "hitelesre" –
  a kép puszta megléte nem azonos a szakmai ellenőrzéssel (CLAUDE.md tartalmi
  integritás, lásd T-708).
- [ ] Mérlegelni és dokumentálni, hogy kelljen-e egy kis ellenőrző szkript/Gradle-feladat,
  ami importálás előtt jelzi, ha egy becsatolt kép formátuma/mérete/elnevezése nem
  felel meg a `DR-05` validációs elvárásnak (pl. hiányzó `illustration_ref`-találat,
  nem támogatott formátum) – **csak ha ez tényleg hiányzik** a meglévő
  `KnowledgeValidator`/`V004` szabályból; ha már lefedi, elég a dokumentáció.
- [ ] A `kyo-knowledge-base` skill (`.claude/skills/kyo-knowledge-base/SKILL.md`)
  hivatkozása az új szakaszra.
- [ ] Ha készül szkript/kód: `./gradlew testDebugUnitTest assembleDebug` zöld.
- [ ] `docs/IMPLEMENTATION_PLAN.md` bővítési szakaszának frissítése, commit a taskra
  hivatkozva.
