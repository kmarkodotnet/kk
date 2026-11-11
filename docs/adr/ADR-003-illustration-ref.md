# ADR-003 – `IllustrationRef` sealed típus a nullable képhivatkozás helyett

- **Állapot:** elfogadva (2026-11-11)
- **Kontextus:** CR-05, FR-13; a kép-alapú kvíztípusok (C, D) csak valódi, validált
  illusztrációval aktiválhatók, képhelyőrzővel nem.

## Döntés

```kotlin
sealed interface IllustrationRef {
    data class Asset(val ref: String, val fileName: String) : IllustrationRef
    data object Placeholder : IllustrationRef
}
```

A `Technique.illustration` típusa `IllustrationRef?`, ahol:

| Érték | Jelentés |
|---|---|
| `null` | a rekord nem hivatkozik illusztrációra |
| `Placeholder` | hivatkozik, de nincs valódi kép (szándékos helyőrző, vagy a fájl hiányzik – V005) |
| `Asset` | validált, létező képfájl |

A kép-alapú kvíztípus jogosultsága így **egyetlen típusvizsgálat**:
`technique.illustration is IllustrationRef.Asset`.

## Mérlegelt alternatívák

| Alternatíva | Miért nem |
|---|---|
| `illustration: String?` | A „nincs kép", a „helyőrző" és a „valódi kép" három esete egyetlen nullable stringbe csúszna. A kvízjogosultság rejtett string-összehasonlítás lenne (pl. `!= "placeholder"`) – pontosan az a hiba, amit CR-05 el akar kerülni. |
| Külön `hasRealImage: Boolean` flag | Két mező, amelyek kicsúszhatnak egymásból; a fordító nem véd. |

## Következmények

- A funkcionális spec 2.3 pontjának három megjelenítési esete kimerítő `when`-nel kezelhető,
  beleértve a különböző `contentDescription`-öket (NFR-04).
- A validátor V005 szabálya nem „elveti a rekordot", hanem **lefokozza** `Asset` → `Placeholder`
  átalakítással; a technika a tudástárban látható marad, csak a C/D kvíztípusból esik ki.
- A későbbi képsorozat-támogatás (mesterprompt 10. pont) a sealed hierarchia bővítése
  (`data class Sequence(val frames: List<Asset>)`) – a meglévő kód fordítási hibákkal
  jelzi, hol kell kezelni az új esetet. Az MVP ezt **nem** implementálja.
- Room-ban két oszlopként tárolódik (`illustrationRef`, `illustrationFile`), a mapper
  állítja elő a sealed típust.
