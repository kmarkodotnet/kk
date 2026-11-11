---
description: A következő implementációs lépés végrehajtása az IMPLEMENTATION_PLAN alapján.
allowed-tools: Bash, Read, Write, Edit, Grep, Glob, PowerShell, Skill
---

Hajtsd végre a **következő egyetlen** nyitott lépést a `docs/IMPLEMENTATION_PLAN.md`-ből.
Argumentum (opcionális): `$ARGUMENTS` – ha meg van adva, azt a lépést/fázist vedd előre.

Kötelező menet:
1. Olvasd be `CLAUDE.md`, `docs/IMPLEMENTATION_PLAN.md` és a lépéshez tartozó spec-szakaszt
   (`docs/REQUIREMENTS.md`, `docs/FUNCTIONAL_SPEC.md`, `docs/ARCHITECTURE.md`).
2. Töltsd be a releváns skillt: tudástár → `kyo-knowledge-base`; kvíz → `kyo-quiz-engine`;
   Compose/modul → `kyo-android-module`; karate tartalom → `kyokushin-content`.
3. Implementáld a lépést. Egy lépés = egy koherens, tesztelhető egység.
4. Írj hozzá unit tesztet (a `docs/REQUIREMENTS.md` tesztkövetelményeihez kötve).
5. Futtasd: `./gradlew testDebugUnitTest`. Ha a környezet nem engedi (nincs JDK/SDK),
   NE hazudj zöld tesztet – rögzítsd `docs/ENVIRONMENT.md`-ben és menj tovább.
6. Frissítsd `docs/IMPLEMENTATION_PLAN.md` állását (kipipálás + 1 soros megjegyzés).
7. Commit: `feat|test|docs(scope): ...` angol nyelven.

Megállási szabály: egy `/kyo-next` hívás egy lépést zár le. Ne fuss előre több fázisra.
