---
description: Debug build futtatása és az eredmény hiteles dokumentálása.
allowed-tools: Bash, PowerShell, Read, Write, Edit
---

1. Ellenőrizd a környezetet: `java -version` és `./gradlew --version`.
   Ha bármelyik hiányzik: **ne futtass buildet**, írd ki pontosan mi hiányzik,
   frissítsd `docs/ENVIRONMENT.md`-t, és javasold a `/kyo-setup-env`-et.
2. `./gradlew assembleDebug --no-daemon`
3. Siker esetén: jelentsd az APK pontos útvonalát (`app/build/outputs/apk/debug/*.apk`)
   és a fájl méretét.
4. Hiba esetén: idézd a tényleges Gradle hibakimenetet (a releváns 20-30 sort),
   diagnosztizáld, javítsd, és futtasd újra. Max 3 javítási kör, utána jelentsd a blokkolót.
5. Írd be `docs/TEST_REPORT.md`-be: dátum, pontos parancs, exit kód, eredmény.

Soha ne írj „build sikeres"-t tényleges, nullás exit kódú futtatás nélkül.
