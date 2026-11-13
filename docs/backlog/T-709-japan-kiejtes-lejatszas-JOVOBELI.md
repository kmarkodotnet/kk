# T-709 · Japán kiejtés lejátszása — JÖVŐBELI, JELENLEG NEM INDÍTHATÓ

| | |
|---|---|
| Modul | feature/learn, core/ui |
| Függ | **OQ-05 (nyitott döntés, `docs/REQUIREMENTS.md` 10. szakasz) — ez szándékosan nem egy elvégezhető task** |
| Követelmény | igény rögzítve, **nincs** hozzárendelt FR; a mesterprompt 9. pontja ("hangos japán kiejtés") explicit kizárja MOST-ra |
| Teszt | — |

> **Ezt a jegyet a `/dev` és a `/devall` NEM veheti fel automatikusan.** A "Függ" mező
> szándékosan egy nyitott kérdésre (OQ-05) mutat, nem egy másik task azonosítójára – ez
> sosem "teljesül" automatikusan a `docs/done/` mappán keresztül. A felvétel oka az,
> hogy a felhasználó igénye dokumentálva legyen, nem az, hogy a munka megkezdhető.

A felhasználó azt kéri, hogy tanulás közben egy hangikonra koppintva meg lehessen
hallgatni egy technika japán kiejtését. Ez a `CLAUDE_KYOKUSHIN_MASTER_PROMPT.md` **10.
pontja** alatt explicit szerepel ("hangos japán kiejtés") mint later felvehető, de MOST
nem implementálandó elem – ezt a `CLAUDE.md` vasszabálya ("Nincs korai bővítés") is
megerősíti.

## Mit kell eldönteni, mielőtt ez a task elindítható (OQ-05)

- Gépi TTS (Android `TextToSpeech`, japán locale) vagy a felhasználó által biztosított,
  valódi kiejtést tartalmazó hangfájlok? Ez eldönti, kell-e új, verziókatalógusba
  felvett függőség, és kell-e tárolási/licenc kérdést tisztázni.
- Ha hangfájl: honnan, milyen licenccel – a CLAUDE.md "harmadik fél appjából kinyert…
  tilos" szabálya ugyanúgy vonatkozik hangra, mint képre.
- Ha TTS: a japán kiejtés gépi minősége elfogadható-e oktatási célra (ezt a felhasználó
  dönti el egy próba meghallgatás után).

## A döntés után elvégzendő (vázlat, nem végleges Teendők lista)

- `core/ui` hangikon komponens + lejátszási állapot (lejátszás/betöltés/hiba).
- `feature/learn` (és később a Tudástár részletező) bekötése.
- Ha TTS: `INTERNET` engedély **nem** kérhető – ellenőrizni, hogy az Android TTS motor
  teljesen helyben fut-e az adott nyelvi csomaggal, vagy hálózatot igényel (ha igen,
  ez önmagában blokkoló, mert a manifestben nem engedélyezett).
- Ha hangfájl: tárolási hely, méretbecslés az APK méretére, `illustration_ref`-hez
  hasonló `audio_ref` séma bővítés (ADR szükséges).
