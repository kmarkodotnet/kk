# ADR-001 – Több Gradle modul egyetlen monolit modul helyett

- **Állapot:** elfogadva (2026-11-11, felhasználói jóváhagyással)
- **Kontextus:** NFR-03 előírja, hogy a UI ne férjen hozzá közvetlenül Room DAO-hoz, és a
  kvízlogika tiszta Kotlin legyen, Android-függőség nélkül tesztelhetően.

## Döntés

A projekt teljes modulbontást kap:

```
app, core/model, core/quiz, core/data, core/ui,
feature/library, feature/learn, feature/quiz, feature/progress,
navigation, di
```

A `core/model` és `core/quiz` **Kotlin JVM** modul (nem Android library) – így az
`android.*` import nem is fordulna le bennük. A `core/data` Android library, de a Room
entitásokat és DAO-kat `internal` láthatósággal zárja; kifelé csak repository interfészt
és domain modellt publikál.

## Mérlegelt alternatívák

| Alternatíva | Miért nem |
|---|---|
| Egyetlen modul, csomagokkal | A rétegszabály csak konvenció maradna, és az auditor utólagos ellenőrzésére épülne. A kvízmotor Android-függetlensége első óvatlan importnál elromlik. |
| Közepes bontás (feature-ök egy modulban) | A lényeg megmaradna, de a feature-ök közti szivárgást nem akadályozná meg, és a későbbi bővítéskor (kyu-tanterv, SR) újra szét kellene bontani. |

## Következmények

**Pozitív**
- A rétegsértés **fordítási hiba**, nem code review kérdés (NFR-03, TR-07).
- A `core/quiz` és `core/model` sima JVM unit teszttel futtatható, emulátor és Android SDK
  nélkül – ez a jelen környezetben különösen értékes (`docs/ENVIRONMENT.md`).
- A modulok külön fordulnak, az inkrementális build gyorsabb lesz a projekt növekedésével.

**Negatív / költség**
- Több `build.gradle.kts` fájl, nagyobb kezdeti beállítási munka.
- Az első (cold) build lassabb.
- A Hilt kötéseknek a modulhatárokon át kell dolgozniuk – ezt a `di` modul központosítja.
