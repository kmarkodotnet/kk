# Könyv és Kyo tartalom összevetése

**Felhasználói döntés: 2026-11-16.** Az E11 könyvfeldolgozására és a hozzá tartozó
T-713…T-730 jegyekre érvényes. Ez felülírja azt a korábbi általános feltételezést,
hogy minden könyvből átvett állításhoz új szakmai lektorálás szükséges.

## Döntési szabály

| Besorolás | Mit jelent | Teendő | Új szakmai lektorálás |
|---|---|---|---|
| `match` – egyezés | Ugyanarról a fogalomról ugyanazt állítja a könyv és a meglévő alkalmazásadat. Eltérő szövegezés önmagában nem eltérés. | A meglévő rekord/id megtartása, könyvforrás hozzárendelése, egyeztetés dokumentálása. | Nem szükséges. |
| `minor_difference` – kisebb eltérés | A jelentés és a tanított tény lényegében azonos; az eltérés megfogalmazás, átírás, terminológiai alak vagy az alapállítást nem módosító pontosítás. | A könyv változatát kell átvezetni, a meglévő id megtartásával és szükség szerinti revízióemeléssel. | Nem szükséges; a könyv az irányadó. |
| `conflict` – lényegi ellentmondás | Ugyanarról a fogalomról vagy követelményről érdemben mást állít a két forrás. | Mindkét változat és a konkrét eltérés megőrzése; célzott szakmai lektorálási tétel létrehozása. A vitát nem oldja fel automatikus könyvfelülírás. | Szükséges, a vitatott állításra. |

Az egyező és kisebb eltérésű állítás **a felhasználó egyeztetési szabálya alapján
elfogadott**. Ezt nem szabad új szakértői lektorálásként, kitalált reviewerrel vagy
lektorálási dátummal feltüntetni. Az elfogadás jogcíme és bizonyítéka külön adat.
Korábban lektorálatlan alkalmazásrekord egyezése is e szabály szerint kezelendő;
nem kell csak a korábbi státusza miatt új szakmai lektorálási körre küldeni.

## Az összevetés határai

- A könyvet a **feldolgozás előtti, verzióval azonosított Kyo-adatkészlethez** kell
  hasonlítani. A már könyv alapján átírt rekord nem lehet önmaga független megerősítése.
- A besorolás állításonként/mezőnként történik. Egy egyező név nem fogadja el
  automatikusan a hozzá másolt teljes életrajzot, leírást vagy további követelményt.
- Először azonosítani kell, hogy ugyanarról a fogalomról, technikaváltozatról,
  szervezetről és tananyagverzióról van-e szó. Két külön technikaváltozat vagy eltérő
  szervezet követelménye nem válik pusztán szövegkülönbség miatt ellentmondássá.
- A különbség súlyát a **jelentés**, nem a karaktertávolság dönti el. Negáció,
  találati magasság, ütőfelület, személyazonosság, évszám vagy vizsgakövetelmény
  változása akár egyetlen eltérő karakter esetén is érdemi eltérés lehet.
- Valóban kisebb eltérésnél nem kell újabb felhasználói jóváhagyást vagy szakértői
  ellenőrzést kérni. Ha nem dönthető el, hogy az eltérés kisebb vagy lényegi,
  a konkrét ügy az indoklásával együtt tisztázandó; nem kell az egész fejezetet blokkolni.
- A könyvben szereplő, az alkalmazásban **előzmény nélküli új állítás** külön
  `new_content` besorolást kap. Nem minősül ellentmondásnak, és egyezés alapján
  elfogadottnak sem. Az új tartalom korábban előírt szakmai állapota marad érvényes;
  a mostani döntés nem rendeli automatikusan a három összehasonlítási eset egyikébe.
- Az OCR-bizonytalanságot előbb a JPG alapján kell rendezni; a hibás átiratból nem
  szabad látszólagos szakmai ellentmondást gyártani. Ismeretlen bibliográfia külön
  jelölendő, de nem teszi lektorálandóvá az egyébként egyező állítást.

## Példák a besorolásra

| Eset | Besorolás és eredmény |
|---|---|
| Mindkét forrás: `Mae geri` – elülső/előre irányuló rúgás. | `match`; nincs új lektorálás. |
| Az alkalmazás „elülső rúgás”, a könyv ugyanarra a fogalomra „előre rúgás” megfogalmazást használ. | `minor_difference`; a könyv megfogalmazása az irányadó. |
| Ugyanazt a konkrét technikát az egyik forrás fejre, a másik középre irányulóként írja le, és nem két eltérő változatról van szó. | `conflict`; célzott lektorálás. |
| A könyv új személyről közöl életrajzot, akiről a Kyo adatbázisában nincs adat. | `new_content`; nincs összevethető korábbi állítás. |

## Nyomkövetés és adatmodell

T-713 rögzíti a konkrét mezőneveket és a jelenlegi `review_status`/`VerificationStatus`
mezőkkel való leképezést. Legalább az alábbi adatokat kell megőrizni:

- érintett stabil rekordazonosító és állítás/mező;
- a kiinduló Kyo-adatkészlet verziója és az összevetett érték;
- könyvforrás, fejezet és kép/forráshely;
- besorolás: `match`, `minor_difference`, `conflict` vagy `new_content`;
- rövid indoklás és a kiválasztott eredményérték;
- elfogadás jogcíme: felhasználói egyeztetési szabály vagy tényleges szakmai lektorálás;
- szükség esetén nyitott lektorálási ügy és érintett revízió.

Az import nem minősíthet minden könyvrekordot feltétel nélkül hitelesnek, de nem is
küldheti új lektorálásra a szabály szerint már elfogadott egyezéseket/kisebb eltéréseket.
A kvízjogosultság továbbra is levezetett, a rögzített elfogadási jogcímet is figyelembe
vevő döntés. Egy vitatott mellékállítás ne szüntesse meg az attól független, elfogadott
név–jelentés pár használhatóságát.

## Szerkesztett kérdések

Az elfogadott háttérállítást nem kell a kérdés kedvéért ismét szakmailag lektoráltatni.
A kérdés saját szerkesztési ellenőrzése ettől megmarad: pontos megfogalmazás,
egyértelmű kontextus, egyetlen helyes válasz, nem félreérthető opciók és megfelelő
magyarázat. Új tényt tartalmazó vagy lényegi ellentmondásra épülő kérdés nem örökölhet
elfogadást a háttérrekord más, egyező mezőjétől.
