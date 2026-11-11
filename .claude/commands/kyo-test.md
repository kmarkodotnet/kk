---
description: Unit (és ha lehet, instrumentált) tesztek futtatása + TEST_REPORT frissítése.
allowed-tools: Bash, PowerShell, Read, Write, Edit
---

Cél: a `docs/REQUIREMENTS.md` tesztkövetelményeinek (TR-*) tényleges ellenőrzése.

1. Környezetellenőrzés (`java -version`, `./gradlew --version`). Hiány esetén állj meg,
   dokumentálj, javasolj `/kyo-setup-env`-et.
2. `./gradlew testDebugUnitTest --no-daemon`
3. Bukó teszt esetén: a tényleges assertion-hibát idézd, javítsd a KÓDOT (ne a tesztet,
   kivéve ha a teszt bizonyítottan hibás elvárást fogalmaz meg), futtasd újra.
4. Ha van csatlakoztatott eszköz/emulátor (`adb devices`), futtasd:
   `./gradlew connectedDebugAndroidTest`. Ha nincs, írd ki: „nem futott – nincs eszköz".
5. Frissítsd `docs/TEST_REPORT.md`-t: dátum, parancs, teszt/hiba szám, mely TR-* lett
   lefedve, mi NEM futott és miért.
6. Sorold fel, mely TR-* követelmény maradt lefedés nélkül.

Tilos kimenet nélkül zöldre jelenteni tesztet.
