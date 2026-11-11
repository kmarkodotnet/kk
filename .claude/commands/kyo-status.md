---
description: Projekt állapotjelentés – fázis, blokkolók, build/teszt státusz, következő lépés.
allowed-tools: Bash, Read, Grep, Glob
---

Készíts rövid, tényszerű állapotjelentést a Kyokushin Karate Trainer projektről.

1. Olvasd be: `docs/IMPLEMENTATION_PLAN.md`, `docs/ENVIRONMENT.md`, `docs/TEST_REPORT.md`.
2. Futtasd: `git -C . status --short` és `git -C . log --oneline -10`.
3. Számold meg a tényleges forrásfájlokat: `find . -name "*.kt" -not -path "*/build/*" | wc -l`.
4. Ellenőrizd, létezik-e `gradlew`, `settings.gradle.kts`, `app/build.gradle.kts`.

A jelentés formája (max 20 sor, magyarul):
- **Fázis:** melyik végrehajtási fázisnál állunk (mesterprompt 11. pont).
- **Elkészült:** konkrét, ellenőrizhető tételek.
- **Build/teszt:** CSAK tényleges, a TEST_REPORT-ban dokumentált futtatások. Ha nem futott,
  írd ki, hogy nem futott, és miért.
- **Blokkolók:** pontos környezeti vagy tartalmi blokkoló.
- **Következő 3 lépés.**

Ne becsülj, ne kerekíts felfelé, ne állítsd, hogy valami működik, ha nem futtattad.
