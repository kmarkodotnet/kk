# ADR-004 – Tartalomtörlés helyett `retired = true`, nincs cascade a felhasználói adatra

- **Állapot:** elfogadva (2026-11-11)
- **Kontextus:** FR-33, DR-06, DR-07, TR-05 – a tudástár frissítése nem veszítheti el a
  felhasználó előrehaladását.

## Döntés

1. A tartalomfrissítés **upsert**, nem `DELETE ALL` + `INSERT`.
2. Ha egy technika kikerül az új `techniques.json`-ból, a `technique` sor **nem törlődik**,
   hanem `retired = true` jelölést kap.
3. A `quiz_attempt` és `study_review` táblák `techniqueId` oszlopára **nincs**
   `onDelete = CASCADE`. Vagy nincs FK kényszer, vagy `NO_ACTION`.
4. A `retired` technika nem jelenik meg a Tudástár listában, a Tanulás kártyái között és a
   kvízjelöltek között, de a Haladás képernyőn a korábbi eredménye látható marad, a
   `progress.retired` üzenettel.
5. A technika `id` **örök**: törlés után sem használható újra más fogalomra (DR-06).

## Mérlegelt alternatívák

| Alternatíva | Miért nem |
|---|---|
| Tartalomtáblák ürítése és újratöltése | A legegyszerűbb import, de minden FK-hivatkozás elszakadna, és a cascade törölné a felhasználói próbálkozásokat. Közvetlenül sérti FR-33-at. |
| Fizikai törlés, de a felhasználói sorok megtartása FK nélkül | A próbálkozás megmaradna, de a Haladás képernyőn feloldhatatlan `techniqueId` jelenne meg („ismeretlen technika"). Rosszabb UX, és a kategóriabontás is elromlana. |
| Soft delete `deletedAt` időbélyeggel | Ugyanaz a hatás, mint a `retired` flag, de több állapotot hordoz, mint amennyire az MVP-nek szüksége van. |

## Következmények

- A `technique` tábla idővel visszavont rekordokat is tárol. Néhány száz rekordnál ez
  elhanyagolható méret.
- Minden tartalomlekérdezésnek szűrnie kell a `retired = false`-ra – ez a repository
  implementáció felelőssége, a hívó nem felejtheti el.
- A **TR-05** teszt ezt az invariánst méri: új `contentVersion` betöltése után a korábbi
  `QuizAttempt` sorok száma és a számolt statisztika változatlan; átnevezett technikánál az
  előrehaladás követi az `id`-t.
- `fallbackToDestructiveMigration()` tiltása ennek a döntésnek a kiegészítése: a séma
  változása sem veszíthet felhasználói adatot (DR-08).
