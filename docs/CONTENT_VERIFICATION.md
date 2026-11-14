# T-708 – Fordítások és terminológia forrásösszevetése

Dátum: 2026-11-14. Teljes állomány: **161 rekord**. Ez dokumentált tartalmi előellenőrzés, nem szakmai hitelesítés. A romaji, a magyar név és a kategória minden rekordnál áttekintve. A forrás szerinti fogalmi összeillés nem jelent szó szerinti magyar szövegegyezést.

Pillanatkép SHA-256 (`app/src/main/assets/knowledge/entries.json`): `3029d1031555333cf5ab236ef9b30e39fb60d7a91f23f1ed664d2514c3694aa9`.
A CSV az elsődleges szerkesztési forrás; az alábbi ellenőrzés külön igazolja a három mező egyezését vele. A párhuzamos Claude-fejlesztés kódjához nem nyúltunk. Tananyag, verzió és review-státusz nem változott.

## Kimutatás

| Jelölés | Rekord |
|---|---:|
| FORRÁSFÜGGŐ | 5 |
| JAVÍTÁS JAVASOLT | 5 |
| KATEGÓRIA | 18 |
| MEGFELELTETHETŐ | 112 |
| NYITOTT | 7 |
| PONTOSÍTANDÓ | 14 |

**MEGFELELTETHETŐ:** a megjelölt helyeken lényegi ellentmondás nem merült fel; nem hitelesítési pecsét. **JAVÍTÁS JAVASOLT/PONTOSÍTANDÓ:** problémás megnevezés vagy félreérthető meghatározás. **FORRÁSFÜGGŐ:** dokumentált eltérő használat. **NYITOTT:** elégtelen közvetlen bizonyíték, nem bizonyított hiba. **KATEGÓRIA:** szerkezeti/oktatási csoportosítás, nem feltétlen rossz fordítás.

## Fontos értelmezési határok

A makronok, kötőjelek és szóközök hiánya a forrásban nem automatikus elírás. Japán íráskép és kiejtési forrás nélkül a hosszú magánhangzók teljes ellenőrzése nyitott. A rövidített nevek lehetnek érvényes változatok. A különböző szintű és irányú technikákat nem szabad pusztán szöveghasonlóság alapján összevonni.

Az `Ütések` gyűjtőkategória tartalmazhat csapásokat és könyöktechnikákat is; az angol belső `punches` azonosító nem önmagában tananyaghiba. A `commands` kategóriába tett szótári fogalmaknál ezt külön megjelöltük.

Automatikus adatjavítás nincs: a jelzett eltérések szakmai értelmezést vagy helyi szóhasználati döntést igényelnek. Nincs olyan bizonyított, egyszerű gépelési hiba, amelyet ebben a körben migrálni kellett volna.

## Források és súlyuk

- **S1:** [IFK Norge – Syllabus og Renraku](https://www.ifknorge.org/wp-content/uploads/2024/03/syllabus_hoyopp.pdf). Nemzeti Kyokushin-szervezet saját oldalán elérhető vizsgaanyag és szószedet; az oldalszámok a PDF-olvasó 1-től számozott lapjai. Saját elírásokat is tartalmaz, ezért nem abszolút tekintély.
- **S2:** [Nintai Dojo – BKK/IFK syllabus](https://www.nintaidojo.co.uk/syllabus/). Az 1993-as anyag mai átirata. S1-gyel közös IFK-hagyomány: nem számít teljesen független második iskolának.
- **S3:** [Mészáros Dojo, Vác – Kyokushin szótár](https://kyovac.hu/szotar/). Magyar dojo saját oktatási anyaga, közvetlen magyar jelentésekhez. Független kiadó S1-től; helyi szóhasználat és néhány gépelési hiba van benne. Nem országos normatív jóváhagyás.
- **S4:** [Isshin Dojo – Kyokushin Kata & Terminology](https://www.isshindojo.nl/resource/file/normal/999d1f82647872dc1ef3830c1cdf3f8bb92d03bb_Kyokushin-Kata-Kyokushin-Terminology.pdf). Kiegészítő dojoanyag, különösen katajelentésekhez. Több része közös hagyományból származhat S1-gyel; nem kezeljük független konszenzusszavazatként. Oldalak itt is 1-től.
- **S5:** [Kyokushin-kan Indonesia – Terminology](https://www.kyokushinkan-indonesia.org/pages/terminology.html). Más szervezeti ág saját cím- és rangidősség-magyarázata; közvetlenül a címekhez, nem az összes technikához használható.

Elérés: 2026-11-14. Az ausztrál Kyokushin-kan PDF két lekéréssel 502 hibát adott; egy IKO kata-PDF 404-et. Ezek tartalmára nem alapozunk megállapítást. S1 + S3 külön kiadójú szakmai anyag; S5 külön szervezeti ágat is bevon. A többször átvett szöveg nem több független bizonyíték.

## Kiemelt, átnézendő megnevezések

| id | Jelenlegi romaji | Jelenlegi magyar | Kategória | Jelölés | Forráshely | Megállapítás | Javasolt teendő |
|---|---|---|---|---|---|---|---|
| KY-0005 | Senpai ni rei | Meghajlás a rangidős tanítvány felé | etiquette | PONTOSÍTANDÓ | S5: Senpai & Kohai; S1: 3. o. | A rangidősség nem kizárólag övfok; a korábban kezdett gyakorlás is számít. | nyitott |
| KY-0020 | Ki o tsuke | Vigyázz! | commands | NYITOTT | S1: 3–7. o.; S3: általános kifejezések (negatív keresés) | A teljes kifejezéshez a vizsgált listákban nem találtam közvetlen párt; ez nem bizonyítja, hogy hibás. | nyitott |
| KY-0021 | Migi | Jobb | commands | KATEGÓRIA | S1: 5–8. o.; S3: általános kifejezések | Nincs feltárt lényegi név-/jelentésellentmondás. Kategória: irány/szint/fogalom/cím, nem önálló vezényszó; kategóriabontás javasolt. | javítás javasolt |
| KY-0022 | Hidari | Bal | commands | KATEGÓRIA | S1: 5–8. o.; S3: általános kifejezések | Nincs feltárt lényegi név-/jelentésellentmondás. Kategória: irány/szint/fogalom/cím, nem önálló vezényszó; kategóriabontás javasolt. | javítás javasolt |
| KY-0023 | Mae | Előre, elöl | commands | KATEGÓRIA | S1: 5–8. o.; S3: általános kifejezések | Nincs feltárt lényegi név-/jelentésellentmondás. Kategória: irány/szint/fogalom/cím, nem önálló vezényszó; kategóriabontás javasolt. | javítás javasolt |
| KY-0024 | Ushiro | Hátra, hátul | commands | KATEGÓRIA | S1: 5–8. o.; S3: általános kifejezések | Nincs feltárt lényegi név-/jelentésellentmondás. Kategória: irány/szint/fogalom/cím, nem önálló vezényszó; kategóriabontás javasolt. | javítás javasolt |
| KY-0025 | Yoko | Oldalra | commands | KATEGÓRIA | S1: 5–8. o.; S3: általános kifejezések | Nincs feltárt lényegi név-/jelentésellentmondás. Kategória: irány/szint/fogalom/cím, nem önálló vezényszó; kategóriabontás javasolt. | javítás javasolt |
| KY-0026 | Mawashi | Köríves | commands | KATEGÓRIA | S1: 5–8. o.; S3: általános kifejezések | Nincs feltárt lényegi név-/jelentésellentmondás. Kategória: irány/szint/fogalom/cím, nem önálló vezényszó; kategóriabontás javasolt. | javítás javasolt |
| KY-0027 | Jōdan | Felső szint | commands | KATEGÓRIA | S1: 5–8. o.; S3: általános kifejezések | Nincs feltárt lényegi név-/jelentésellentmondás. Kategória: irány/szint/fogalom/cím, nem önálló vezényszó; kategóriabontás javasolt. | javítás javasolt |
| KY-0028 | Chūdan | Középső szint | commands | KATEGÓRIA | S1: 5–8. o.; S3: általános kifejezések | Nincs feltárt lényegi név-/jelentésellentmondás. Kategória: irány/szint/fogalom/cím, nem önálló vezényszó; kategóriabontás javasolt. | javítás javasolt |
| KY-0029 | Gedan | Alsó szint | commands | KATEGÓRIA | S1: 5–8. o.; S3: általános kifejezések | Nincs feltárt lényegi név-/jelentésellentmondás. Kategória: irány/szint/fogalom/cím, nem önálló vezényszó; kategóriabontás javasolt. | javítás javasolt |
| KY-0030 | Kihon | Alaptechnikák | commands | KATEGÓRIA | S1: 5–8. o.; S3: általános kifejezések | Nincs feltárt lényegi név-/jelentésellentmondás. Kategória: irány/szint/fogalom/cím, nem önálló vezényszó; kategóriabontás javasolt. | javítás javasolt |
| KY-0031 | Idō geiko | Mozgásos alaptechnikai gyakorlás | commands | KATEGÓRIA | S1: 5–8. o.; S3: általános kifejezések | Nincs feltárt lényegi név-/jelentésellentmondás. Kategória: irány/szint/fogalom/cím, nem önálló vezényszó; kategóriabontás javasolt. | javítás javasolt |
| KY-0032 | Kata | Formagyakorlat | commands | KATEGÓRIA | S1: 5–8. o.; S3: általános kifejezések | Nincs feltárt lényegi név-/jelentésellentmondás. Kategória: irány/szint/fogalom/cím, nem önálló vezényszó; kategóriabontás javasolt. | javítás javasolt |
| KY-0033 | Kumite | Küzdelem | commands | KATEGÓRIA | S1: 5–8. o.; S3: általános kifejezések | Nincs feltárt lényegi név-/jelentésellentmondás. Kategória: irány/szint/fogalom/cím, nem önálló vezényszó; kategóriabontás javasolt. | javítás javasolt |
| KY-0034 | Tameshiwari | Töréstechnika | commands | KATEGÓRIA | S1: 5–8. o.; S3: általános kifejezések | Nincs feltárt lényegi név-/jelentésellentmondás. Kategória: irány/szint/fogalom/cím, nem önálló vezényszó; kategóriabontás javasolt. | javítás javasolt |
| KY-0035 | Dōjō | Edzőterem | commands | KATEGÓRIA | S1: 5–8. o.; S3: általános kifejezések | Nincs feltárt lényegi név-/jelentésellentmondás. Kategória: irány/szint/fogalom/cím, nem önálló vezényszó; kategóriabontás javasolt. | javítás javasolt |
| KY-0036 | Gi | Karateruha | commands | KATEGÓRIA | S1: 5–8. o.; S3: általános kifejezések | Nincs feltárt lényegi név-/jelentésellentmondás. Kategória: irány/szint/fogalom/cím, nem önálló vezényszó; kategóriabontás javasolt. | javítás javasolt |
| KY-0037 | Obi | Öv | commands | KATEGÓRIA | S1: 5–8. o.; S3: általános kifejezések | Nincs feltárt lényegi név-/jelentésellentmondás. Kategória: irány/szint/fogalom/cím, nem önálló vezényszó; kategóriabontás javasolt. | javítás javasolt |
| KY-0038 | Kyū | Tanulófokozat | commands | PONTOSÍTANDÓ | S4: 12–13. o.; S5: Dan grades & titles | A tanuló-/mesterfokozat magyar szokásos címke; nem jelent automatikus oktatói vagy szakértői minősítést. Kategória: irány/szint/fogalom/cím, nem önálló vezényszó; kategóriabontás javasolt. | nyitott |
| KY-0039 | Dan | Mesterfokozat | commands | PONTOSÍTANDÓ | S4: 12–13. o.; S5: Dan grades & titles | A tanuló-/mesterfokozat magyar szokásos címke; nem jelent automatikus oktatói vagy szakértői minősítést. Kategória: irány/szint/fogalom/cím, nem önálló vezényszó; kategóriabontás javasolt. | nyitott |
| KY-0040 | Sensei | Mester, tanár | commands | KATEGÓRIA | S1: 5–8. o.; S3: általános kifejezések | Nincs feltárt lényegi név-/jelentésellentmondás. Kategória: irány/szint/fogalom/cím, nem önálló vezényszó; kategóriabontás javasolt. | javítás javasolt |
| KY-0041 | Senpai | Rangidős tanítvány | commands | PONTOSÍTANDÓ | S5: Senpai & Kohai; S1: 3. o. | A rangidősség nem kizárólag övfok; a korábban kezdett gyakorlás is számít. Kategória: irány/szint/fogalom/cím, nem önálló vezényszó; kategóriabontás javasolt. | nyitott |
| KY-0042 | Kōhai | Fiatalabb tanítvány | commands | PONTOSÍTANDÓ | S5: Senpai & Kohai | A fiatalabb félreérthető életkorként. Javaslat: később kezdett / kevésbé régi tanítvány. Kategória: irány/szint/fogalom/cím, nem önálló vezényszó; kategóriabontás javasolt. | nyitott |
| KY-0043 | Sōsai | Alapító vezető címe | commands | PONTOSÍTANDÓ | S1: 3. o.; S4: 14. o. | A megadott alapítói szerep kontextus, nem szó szerinti címfordítás. Az elnöki jelentést is külön kell jelezni. Kategória: irány/szint/fogalom/cím, nem önálló vezényszó; kategóriabontás javasolt. | nyitott |
| KY-0048 | Hachiji dachi | Nyitott lábfejű természetes állás | stances | NYITOTT | S4: 9. o.; S1: 1. o. | A forrás részletesebb irányjelölést is használ; a jelző nélküli állás és a lábfejirány pontos azonosságát lektorálni kell. | nyitott |
| KY-0059 | Jōdan age uke | Felső emelkedő védés | blocks | FORRÁSFÜGGŐ | S1: 11. o.; S2: Blocks; S3: védések | A forrás egyszerűbb felső védésnevet használ. Az age bővítés elfogadott helyi alak-e, bemutatással tisztázandó. | nyitott |
| KY-0060 | Chūdan soto uke | Középső külső védés | blocks | PONTOSÍTANDÓ | S1: 11. o.; S3: védések | A külső/belső név és a mozgás iránya nem azonos. A magyar név mellé a befelé/kifelé mozgást is egyértelműsíteni kell. | nyitott |
| KY-0061 | Chūdan uchi uke | Középső belső védés | blocks | PONTOSÍTANDÓ | S1: 11. o.; S3: védések | A külső/belső név és a mozgás iránya nem azonos. A magyar név mellé a befelé/kifelé mozgást is egyértelműsíteni kell. | nyitott |
| KY-0065 | Shutō chūdan uke | Középső kézélvédés | blocks | PONTOSÍTANDÓ | S1: 1. o.; S2: 4th Kyu | A középső kézélvédés nem választja szét a belső és külső változatot. Pontos technikát kell hozzárendelni. | nyitott |
| KY-0067 | Morote uke | Kétkezes, megtámasztott védés | blocks | PONTOSÍTANDÓ | S1: 11. o.; S4: 11. o. | A kétkezes és a megtámasztott nem automatikusan ugyanaz. A forrás teljesebb technikanevet ad; a fogalmat pontosítani kell. | nyitott |
| KY-0071 | Sukui uke | Merítő védés | blocks | NYITOTT | S1: 11. o.; S2: Blocks; S4: 11. o. (negatív keresés) | Nincs közvetlen találat a megvizsgált technikai listákban. Külön Kyokushin-bemutató/forrás szükséges. | nyitott |
| KY-0081 | Uraken shōmen uchi | Elülső kézhátütés | punches | JAVÍTÁS JAVASOLT | S3: testfegyverek és uraken; S4: 7., 10. o. | Az ökölhát és a nyitott kéz háta megkülönböztetendő. Magyarul ökölhát / ökölhátütés javasolt, a technika ellenőrzésével. | javítás javasolt |
| KY-0082 | Uraken sayū uchi | Oldalirányú kézhátütés | punches | JAVÍTÁS JAVASOLT | S3: sayu és uraken; S1: 9. o. | Ökölhátütés javasolt; a sayū kétoldaliságát az általános oldalirány helyett érdemes kifejteni. | javítás javasolt |
| KY-0083 | Uraken hizō uchi | Kézhátütés borda/törzs irányába | punches | JAVÍTÁS JAVASOLT | S3: testfegyverek és uraken; S4: 7., 10. o. | Az ökölhát és a nyitott kéz háta megkülönböztetendő. Magyarul ökölhát / ökölhátütés javasolt, a technika ellenőrzésével. | javítás javasolt |
| KY-0084 | Uraken mawashi uchi | Köríves kézhátütés | punches | JAVÍTÁS JAVASOLT | S3: testfegyverek és uraken; S4: 7., 10. o. | Az ökölhát és a nyitott kéz háta megkülönböztetendő. Magyarul ökölhát / ökölhátütés javasolt, a technika ellenőrzésével. | javítás javasolt |
| KY-0089 | Shutō uchi uchi | Belső irányú kézélütés | punches | FORRÁSFÜGGŐ | S1: 10. o.; S3: shuto; S4: 10. o. | A belső névhez az egyik angol leírás kifelé tartó csapást társít. A pálya és a célpont bemutatás alapján tisztázandó. | nyitott |
| KY-0090 | Shutō ganmen uchi | Kézélütés arcra | punches | PONTOSÍTANDÓ | S1: 10. o.; S3: shuto | Az arc mint gyűjtőcél elfogadható, de az egyik leírás konkrétabban halántékot nevez meg. A változatot rögzíteni kell. | nyitott |
| KY-0095 | Hiji mawashi uchi | Köríves könyökütés | punches | NYITOTT | S1: 1., 10–11. o.; S2: technikai listák | Az összetett név pontos alakjához és önálló rekordként való elkülönítéséhez nincs elég közvetlen bizonyíték a vizsgált anyagokban. | nyitott |
| KY-0100 | Yoko kekomi | Oldalsó tolórúgás | kicks | NYITOTT | S1: 1., 10–11. o.; S2: technikai listák | Az összetett név pontos alakjához és önálló rekordként való elkülönítéséhez nincs elég közvetlen bizonyíték a vizsgált anyagokban. | nyitott |
| KY-0104 | Ura mawashi geri | Fordított köríves rúgás | kicks | NYITOTT | S1: 1., 10–11. o.; S2: technikai listák | Az összetett név pontos alakjához és önálló rekordként való elkülönítéséhez nincs elég közvetlen bizonyíték a vizsgált anyagokban. | nyitott |
| KY-0108 | Uchi mawashi geri | Befelé ívelő köríves rúgás | kicks | FORRÁSFÜGGŐ | S1: 11. o.; S3: rúgások; S4: 11. o. | A források irányleírása nem egységes: az IFK anyag mozgáspályája a jelenlegi magyar befelé/kifelé értelmezéssel ellentétes. Nem szabad automatikusan felcserélni; bemutatás és szervezeti névhasználat kell. | nyitott |
| KY-0109 | Soto mawashi geri | Kifelé ívelő köríves rúgás | kicks | FORRÁSFÜGGŐ | S1: 11. o.; S3: rúgások; S4: 11. o. | A források irányleírása nem egységes: az IFK anyag mozgáspályája a jelenlegi magyar befelé/kifelé értelmezéssel ellentétes. Nem szabad automatikusan felcserélni; bemutatás és szervezeti névhasználat kell. | nyitott |
| KY-0110 | Oroshi kakato geri | Lefelé csapó sarokrúgás | kicks | PONTOSÍTANDÓ | S1: 1., 11. o.; S2: 1st Kyu | A sarokrúgás iránya túl általános; a befelé/kifelé vezetett változat megkülönböztetése szükséges lehet. | nyitott |
| KY-0114 | Mae hiza geri | Elülső térdrúgás | kicks | NYITOTT | S1: 1., 10–11. o.; S2: technikai listák | Az összetett név pontos alakjához és önálló rekordként való elkülönítéséhez nincs elég közvetlen bizonyíték a vizsgált anyagokban. | nyitott |
| KY-0119 | Uraken | Kézhát / kézhátbütykök | body_parts | JAVÍTÁS JAVASOLT | S3: testfegyverek és uraken; S4: 7., 10. o. | Az ökölhát és a nyitott kéz háta megkülönböztetendő. Magyarul ökölhát / ökölhátütés javasolt, a technika ellenőrzésével. | javítás javasolt |
| KY-0130 | Teisoku | Talp belső/alsó felülete | body_parts | PONTOSÍTANDÓ | S3: teisoku; S4: 7. o. | A teljes alsó talpfelület túl tág. A boltozati/belső talpfelület pontos megnevezése javasolt. | nyitott |
| KY-0156 | Saiha | Saiha kata | kata | FORRÁSFÜGGŐ | S1: 12. o.; S2: Shodan; S4: 4–6. o. | Eltérő névváltozat szerepel a forrásokban; nem bizonyított elírás. Alias és választott szervezeti szóhasználat rögzítendő. A magyar mező névcímke, nem teljes etimológiai fordítás; ez önmagában nem hiba. | nyitott |
| KY-0161 | Sūshiho | Ötvennégy lépés | kata | PONTOSÍTANDÓ | S4: 6. o.; S2: Sandan | A név és az 54 lépés jelentés összeillik; a jelenlegi hosszú magánhangzók következetességét japán íráskép nélkül nem igazolja a latin betűs lista. | nyitott |

## Teljes, 161 soros ellenőrzési jegyzék

| id | Jelenlegi romaji | Jelenlegi magyar | Kategória | Jelölés | Forráshely | Megállapítás | Javasolt teendő |
|---|---|---|---|---|---|---|---|
| KY-0001 | Osu | Tiszteletteljes köszönés és válasz | etiquette | MEGFELELTETHETŐ | S1: 3–6. o.; S4: 12–14. o. | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0002 | Rei | Meghajlás | etiquette | MEGFELELTETHETŐ | S1: 3–6. o.; S4: 12–14. o. | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0003 | Shōmen ni rei | Meghajlás az edzőterem fő iránya felé | etiquette | MEGFELELTETHETŐ | S1: 3–6. o.; S4: 12–14. o. | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0004 | Sensei ni rei | Meghajlás a mester felé | etiquette | MEGFELELTETHETŐ | S1: 3–6. o.; S4: 12–14. o. | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0005 | Senpai ni rei | Meghajlás a rangidős tanítvány felé | etiquette | PONTOSÍTANDÓ | S5: Senpai & Kohai; S1: 3. o. | A rangidősség nem kizárólag övfok; a korábban kezdett gyakorlás is számít. | nyitott |
| KY-0006 | Mokusō | Meditáció, összpontosítás | etiquette | MEGFELELTETHETŐ | S1: 3–6. o.; S4: 12–14. o. | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0007 | Mokusō yame | Meditáció vége | etiquette | MEGFELELTETHETŐ | S1: 3–6. o.; S4: 12–14. o. | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0008 | Onegaishimasu | Kérem, tanítson / kérem az együttműködést | etiquette | MEGFELELTETHETŐ | S1: 3–6. o.; S4: 12–14. o. | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0009 | Arigatō gozaimashita | Köszönöm szépen (a megtörténtért) | etiquette | MEGFELELTETHETŐ | S1: 3–6. o.; S4: 12–14. o. | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0010 | Yōi | Készülj! | commands | MEGFELELTETHETŐ | S1: 3–6. o.; S4: 12–14. o. | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0011 | Hajime | Kezdd! | commands | MEGFELELTETHETŐ | S1: 3–6. o.; S4: 12–14. o. | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0012 | Yame | Állj! | commands | MEGFELELTETHETŐ | S1: 3–6. o.; S4: 12–14. o. | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0013 | Naore | Vissza alaphelyzetbe! | commands | MEGFELELTETHETŐ | S1: 3–6. o.; S4: 12–14. o. | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0014 | Mawatte | Fordulj meg! | commands | MEGFELELTETHETŐ | S1: 3–6. o.; S4: 12–14. o. | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0015 | Yasume | Pihenj! | commands | MEGFELELTETHETŐ | S1: 3–6. o.; S4: 12–14. o. | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0016 | Kamaete | Vedd fel a tartást! | commands | MEGFELELTETHETŐ | S1: 3–6. o.; S4: 12–14. o. | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0017 | Kiai | Harci kiáltás | commands | MEGFELELTETHETŐ | S1: 3–6. o.; S4: 12–14. o. | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0018 | Seiza | Térdelő ülés | commands | MEGFELELTETHETŐ | S1: 3–6. o.; S4: 12–14. o. | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0019 | Tatte | Állj fel! | commands | MEGFELELTETHETŐ | S1: 3–6. o.; S4: 12–14. o. | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0020 | Ki o tsuke | Vigyázz! | commands | NYITOTT | S1: 3–7. o.; S3: általános kifejezések (negatív keresés) | A teljes kifejezéshez a vizsgált listákban nem találtam közvetlen párt; ez nem bizonyítja, hogy hibás. | nyitott |
| KY-0021 | Migi | Jobb | commands | KATEGÓRIA | S1: 5–8. o.; S3: általános kifejezések | Nincs feltárt lényegi név-/jelentésellentmondás. Kategória: irány/szint/fogalom/cím, nem önálló vezényszó; kategóriabontás javasolt. | javítás javasolt |
| KY-0022 | Hidari | Bal | commands | KATEGÓRIA | S1: 5–8. o.; S3: általános kifejezések | Nincs feltárt lényegi név-/jelentésellentmondás. Kategória: irány/szint/fogalom/cím, nem önálló vezényszó; kategóriabontás javasolt. | javítás javasolt |
| KY-0023 | Mae | Előre, elöl | commands | KATEGÓRIA | S1: 5–8. o.; S3: általános kifejezések | Nincs feltárt lényegi név-/jelentésellentmondás. Kategória: irány/szint/fogalom/cím, nem önálló vezényszó; kategóriabontás javasolt. | javítás javasolt |
| KY-0024 | Ushiro | Hátra, hátul | commands | KATEGÓRIA | S1: 5–8. o.; S3: általános kifejezések | Nincs feltárt lényegi név-/jelentésellentmondás. Kategória: irány/szint/fogalom/cím, nem önálló vezényszó; kategóriabontás javasolt. | javítás javasolt |
| KY-0025 | Yoko | Oldalra | commands | KATEGÓRIA | S1: 5–8. o.; S3: általános kifejezések | Nincs feltárt lényegi név-/jelentésellentmondás. Kategória: irány/szint/fogalom/cím, nem önálló vezényszó; kategóriabontás javasolt. | javítás javasolt |
| KY-0026 | Mawashi | Köríves | commands | KATEGÓRIA | S1: 5–8. o.; S3: általános kifejezések | Nincs feltárt lényegi név-/jelentésellentmondás. Kategória: irány/szint/fogalom/cím, nem önálló vezényszó; kategóriabontás javasolt. | javítás javasolt |
| KY-0027 | Jōdan | Felső szint | commands | KATEGÓRIA | S1: 5–8. o.; S3: általános kifejezések | Nincs feltárt lényegi név-/jelentésellentmondás. Kategória: irány/szint/fogalom/cím, nem önálló vezényszó; kategóriabontás javasolt. | javítás javasolt |
| KY-0028 | Chūdan | Középső szint | commands | KATEGÓRIA | S1: 5–8. o.; S3: általános kifejezések | Nincs feltárt lényegi név-/jelentésellentmondás. Kategória: irány/szint/fogalom/cím, nem önálló vezényszó; kategóriabontás javasolt. | javítás javasolt |
| KY-0029 | Gedan | Alsó szint | commands | KATEGÓRIA | S1: 5–8. o.; S3: általános kifejezések | Nincs feltárt lényegi név-/jelentésellentmondás. Kategória: irány/szint/fogalom/cím, nem önálló vezényszó; kategóriabontás javasolt. | javítás javasolt |
| KY-0030 | Kihon | Alaptechnikák | commands | KATEGÓRIA | S1: 5–8. o.; S3: általános kifejezések | Nincs feltárt lényegi név-/jelentésellentmondás. Kategória: irány/szint/fogalom/cím, nem önálló vezényszó; kategóriabontás javasolt. | javítás javasolt |
| KY-0031 | Idō geiko | Mozgásos alaptechnikai gyakorlás | commands | KATEGÓRIA | S1: 5–8. o.; S3: általános kifejezések | Nincs feltárt lényegi név-/jelentésellentmondás. Kategória: irány/szint/fogalom/cím, nem önálló vezényszó; kategóriabontás javasolt. | javítás javasolt |
| KY-0032 | Kata | Formagyakorlat | commands | KATEGÓRIA | S1: 5–8. o.; S3: általános kifejezések | Nincs feltárt lényegi név-/jelentésellentmondás. Kategória: irány/szint/fogalom/cím, nem önálló vezényszó; kategóriabontás javasolt. | javítás javasolt |
| KY-0033 | Kumite | Küzdelem | commands | KATEGÓRIA | S1: 5–8. o.; S3: általános kifejezések | Nincs feltárt lényegi név-/jelentésellentmondás. Kategória: irány/szint/fogalom/cím, nem önálló vezényszó; kategóriabontás javasolt. | javítás javasolt |
| KY-0034 | Tameshiwari | Töréstechnika | commands | KATEGÓRIA | S1: 5–8. o.; S3: általános kifejezések | Nincs feltárt lényegi név-/jelentésellentmondás. Kategória: irány/szint/fogalom/cím, nem önálló vezényszó; kategóriabontás javasolt. | javítás javasolt |
| KY-0035 | Dōjō | Edzőterem | commands | KATEGÓRIA | S1: 5–8. o.; S3: általános kifejezések | Nincs feltárt lényegi név-/jelentésellentmondás. Kategória: irány/szint/fogalom/cím, nem önálló vezényszó; kategóriabontás javasolt. | javítás javasolt |
| KY-0036 | Gi | Karateruha | commands | KATEGÓRIA | S1: 5–8. o.; S3: általános kifejezések | Nincs feltárt lényegi név-/jelentésellentmondás. Kategória: irány/szint/fogalom/cím, nem önálló vezényszó; kategóriabontás javasolt. | javítás javasolt |
| KY-0037 | Obi | Öv | commands | KATEGÓRIA | S1: 5–8. o.; S3: általános kifejezések | Nincs feltárt lényegi név-/jelentésellentmondás. Kategória: irány/szint/fogalom/cím, nem önálló vezényszó; kategóriabontás javasolt. | javítás javasolt |
| KY-0038 | Kyū | Tanulófokozat | commands | PONTOSÍTANDÓ | S4: 12–13. o.; S5: Dan grades & titles | A tanuló-/mesterfokozat magyar szokásos címke; nem jelent automatikus oktatói vagy szakértői minősítést. Kategória: irány/szint/fogalom/cím, nem önálló vezényszó; kategóriabontás javasolt. | nyitott |
| KY-0039 | Dan | Mesterfokozat | commands | PONTOSÍTANDÓ | S4: 12–13. o.; S5: Dan grades & titles | A tanuló-/mesterfokozat magyar szokásos címke; nem jelent automatikus oktatói vagy szakértői minősítést. Kategória: irány/szint/fogalom/cím, nem önálló vezényszó; kategóriabontás javasolt. | nyitott |
| KY-0040 | Sensei | Mester, tanár | commands | KATEGÓRIA | S1: 5–8. o.; S3: általános kifejezések | Nincs feltárt lényegi név-/jelentésellentmondás. Kategória: irány/szint/fogalom/cím, nem önálló vezényszó; kategóriabontás javasolt. | javítás javasolt |
| KY-0041 | Senpai | Rangidős tanítvány | commands | PONTOSÍTANDÓ | S5: Senpai & Kohai; S1: 3. o. | A rangidősség nem kizárólag övfok; a korábban kezdett gyakorlás is számít. Kategória: irány/szint/fogalom/cím, nem önálló vezényszó; kategóriabontás javasolt. | nyitott |
| KY-0042 | Kōhai | Fiatalabb tanítvány | commands | PONTOSÍTANDÓ | S5: Senpai & Kohai | A fiatalabb félreérthető életkorként. Javaslat: később kezdett / kevésbé régi tanítvány. Kategória: irány/szint/fogalom/cím, nem önálló vezényszó; kategóriabontás javasolt. | nyitott |
| KY-0043 | Sōsai | Alapító vezető címe | commands | PONTOSÍTANDÓ | S1: 3. o.; S4: 14. o. | A megadott alapítói szerep kontextus, nem szó szerinti címfordítás. Az elnöki jelentést is külön kell jelezni. Kategória: irány/szint/fogalom/cím, nem önálló vezényszó; kategóriabontás javasolt. | nyitott |
| KY-0044 | Fudō dachi | Természetes, stabil állás | stances | MEGFELELTETHETŐ | S4: 9. o.; S2: Stances; S3: állások | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0045 | Heisoku dachi | Zárt lábú állás | stances | MEGFELELTETHETŐ | S4: 9. o.; S2: Stances; S3: állások | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0046 | Musubi dachi | V alakú zárt állás | stances | MEGFELELTETHETŐ | S4: 9. o.; S2: Stances; S3: állások | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0047 | Heikō dachi | Párhuzamos állás | stances | MEGFELELTETHETŐ | S4: 9. o.; S2: Stances; S3: állások | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0048 | Hachiji dachi | Nyitott lábfejű természetes állás | stances | NYITOTT | S4: 9. o.; S1: 1. o. | A forrás részletesebb irányjelölést is használ; a jelző nélküli állás és a lábfejirány pontos azonosságát lektorálni kell. | nyitott |
| KY-0049 | Uchi hachiji dachi | Befelé fordított lábfejű állás | stances | MEGFELELTETHETŐ | S4: 9. o.; S2: Stances; S3: állások | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0050 | Zenkutsu dachi | Elülső súlypontú hosszú állás | stances | MEGFELELTETHETŐ | S4: 9. o.; S2: Stances; S3: állások | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0051 | Kōkutsu dachi | Hátsó súlypontú állás | stances | MEGFELELTETHETŐ | S4: 9. o.; S2: Stances; S3: állások | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0052 | Kiba dachi | Lovaglóállás | stances | MEGFELELTETHETŐ | S4: 9. o.; S2: Stances; S3: állások | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0053 | Shiko dachi | Szumóállás | stances | MEGFELELTETHETŐ | S4: 9. o.; S2: Stances; S3: állások | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0054 | Sanchin dachi | Sanchin állás | stances | MEGFELELTETHETŐ | S4: 9. o.; S2: Stances; S3: állások | Nincs feltárt lényegi név-/jelentésellentmondás. A magyar név használható címke, de nem teljes szó szerinti fordítás. | nincs teendő |
| KY-0055 | Neko ashi dachi | Macskaállás | stances | MEGFELELTETHETŐ | S4: 9. o.; S2: Stances; S3: állások | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0056 | Tsuru ashi dachi | Darulábállás | stances | MEGFELELTETHETŐ | S4: 9. o.; S2: Stances; S3: állások | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0057 | Kake dachi | Keresztezett lábú állás | stances | MEGFELELTETHETŐ | S4: 9. o.; S2: Stances; S3: állások | Nincs feltárt lényegi név-/jelentésellentmondás. A magyar név használható címke, de nem teljes szó szerinti fordítás. | nincs teendő |
| KY-0058 | Kumite dachi | Küzdőállás | stances | MEGFELELTETHETŐ | S4: 9. o.; S2: Stances; S3: állások | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0059 | Jōdan age uke | Felső emelkedő védés | blocks | FORRÁSFÜGGŐ | S1: 11. o.; S2: Blocks; S3: védések | A forrás egyszerűbb felső védésnevet használ. Az age bővítés elfogadott helyi alak-e, bemutatással tisztázandó. | nyitott |
| KY-0060 | Chūdan soto uke | Középső külső védés | blocks | PONTOSÍTANDÓ | S1: 11. o.; S3: védések | A külső/belső név és a mozgás iránya nem azonos. A magyar név mellé a befelé/kifelé mozgást is egyértelműsíteni kell. | nyitott |
| KY-0061 | Chūdan uchi uke | Középső belső védés | blocks | PONTOSÍTANDÓ | S1: 11. o.; S3: védések | A külső/belső név és a mozgás iránya nem azonos. A magyar név mellé a befelé/kifelé mozgást is egyértelműsíteni kell. | nyitott |
| KY-0062 | Gedan barai | Alsó söprő védés | blocks | MEGFELELTETHETŐ | S1: 1., 11. o.; S2: Blocks | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0063 | Shutō mawashi uke | Köríves kézélvédés | blocks | MEGFELELTETHETŐ | S1: 1., 11. o.; S2: Blocks | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0064 | Shutō jōdan uke | Felső kézélvédés | blocks | MEGFELELTETHETŐ | S1: 1., 11. o.; S2: Blocks | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0065 | Shutō chūdan uke | Középső kézélvédés | blocks | PONTOSÍTANDÓ | S1: 1. o.; S2: 4th Kyu | A középső kézélvédés nem választja szét a belső és külső változatot. Pontos technikát kell hozzárendelni. | nyitott |
| KY-0066 | Shutō gedan barai | Alsó kézél-söprés | blocks | MEGFELELTETHETŐ | S1: 1., 11. o.; S2: Blocks | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0067 | Morote uke | Kétkezes, megtámasztott védés | blocks | PONTOSÍTANDÓ | S1: 11. o.; S4: 11. o. | A kétkezes és a megtámasztott nem automatikusan ugyanaz. A forrás teljesebb technikanevet ad; a fogalmat pontosítani kell. | nyitott |
| KY-0068 | Jūji uke | Keresztvédés | blocks | MEGFELELTETHETŐ | S1: 1., 11. o.; S2: Blocks | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0069 | Kake uke | Kampózó védés | blocks | MEGFELELTETHETŐ | S1: 1., 11. o.; S2: Blocks | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0070 | Osae uke | Lenyomó védés | blocks | MEGFELELTETHETŐ | S1: 1., 11. o.; S2: Blocks | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0071 | Sukui uke | Merítő védés | blocks | NYITOTT | S1: 11. o.; S2: Blocks; S4: 11. o. (negatív keresés) | Nincs közvetlen találat a megvizsgált technikai listákban. Külön Kyokushin-bemutató/forrás szükséges. | nyitott |
| KY-0072 | Hiji uke | Könyökvédés | blocks | MEGFELELTETHETŐ | S1: 1., 11. o.; S2: Blocks | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0073 | Seiken chūdan tsuki | Egyenes ökölütés középre | punches | MEGFELELTETHETŐ | S3: alaptechnikák; S1: 9–10. o. | Nincs feltárt lényegi név-/jelentésellentmondás. A szintkülönbség megtartandó; a források összevont szintjelölést is alkalmaznak. | nincs teendő |
| KY-0074 | Seiken jōdan tsuki | Egyenes ökölütés felső szintre | punches | MEGFELELTETHETŐ | S3: alaptechnikák; S1: 9–10. o. | Nincs feltárt lényegi név-/jelentésellentmondás. A szintkülönbség megtartandó; a források összevont szintjelölést is alkalmaznak. | nincs teendő |
| KY-0075 | Seiken gedan tsuki | Egyenes ökölütés alsó szintre | punches | MEGFELELTETHETŐ | S3: alaptechnikák; S1: 9–10. o. | Nincs feltárt lényegi név-/jelentésellentmondás. A szintkülönbség megtartandó; a források összevont szintjelölést is alkalmaznak. | nincs teendő |
| KY-0076 | Oi tsuki | Előrelépő, azonos oldali ütés | punches | MEGFELELTETHETŐ | S3: alaptechnikák; S1: 9–10. o. | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0077 | Gyaku tsuki | Ellentétes oldali egyenes ütés | punches | MEGFELELTETHETŐ | S3: alaptechnikák; S1: 9–10. o. | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0078 | Seiken ago uchi | Állra irányuló ökölütés | punches | MEGFELELTETHETŐ | S3: alaptechnikák; S1: 9–10. o. | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0079 | Seiken mawashi uchi | Köríves ökölütés | punches | MEGFELELTETHETŐ | S3: alaptechnikák; S1: 9–10. o. | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0080 | Seiken shita tsuki | Alulról felfelé vezetett ökölütés | punches | MEGFELELTETHETŐ | S3: alaptechnikák; S1: 9–10. o. | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0081 | Uraken shōmen uchi | Elülső kézhátütés | punches | JAVÍTÁS JAVASOLT | S3: testfegyverek és uraken; S4: 7., 10. o. | Az ökölhát és a nyitott kéz háta megkülönböztetendő. Magyarul ökölhát / ökölhátütés javasolt, a technika ellenőrzésével. | javítás javasolt |
| KY-0082 | Uraken sayū uchi | Oldalirányú kézhátütés | punches | JAVÍTÁS JAVASOLT | S3: sayu és uraken; S1: 9. o. | Ökölhátütés javasolt; a sayū kétoldaliságát az általános oldalirány helyett érdemes kifejteni. | javítás javasolt |
| KY-0083 | Uraken hizō uchi | Kézhátütés borda/törzs irányába | punches | JAVÍTÁS JAVASOLT | S3: testfegyverek és uraken; S4: 7., 10. o. | Az ökölhát és a nyitott kéz háta megkülönböztetendő. Magyarul ökölhát / ökölhátütés javasolt, a technika ellenőrzésével. | javítás javasolt |
| KY-0084 | Uraken mawashi uchi | Köríves kézhátütés | punches | JAVÍTÁS JAVASOLT | S3: testfegyverek és uraken; S4: 7., 10. o. | Az ökölhát és a nyitott kéz háta megkülönböztetendő. Magyarul ökölhát / ökölhátütés javasolt, a technika ellenőrzésével. | javítás javasolt |
| KY-0085 | Tettsui oroshi uchi | Lefelé csapó kalapácsököl | punches | MEGFELELTETHETŐ | S3: alaptechnikák; S1: 9–10. o. | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0086 | Tettsui yoko uchi | Oldalsó kalapácsököl | punches | MEGFELELTETHETŐ | S3: alaptechnikák; S1: 9–10. o. | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0087 | Shutō sakotsu uchi | Kézélütés kulcscsontra | punches | MEGFELELTETHETŐ | S3: alaptechnikák; S1: 9–10. o. | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0088 | Shutō sakotsu uchikomi | Behatoló kézélütés kulcscsontra | punches | MEGFELELTETHETŐ | S3: alaptechnikák; S1: 9–10. o. | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0089 | Shutō uchi uchi | Belső irányú kézélütés | punches | FORRÁSFÜGGŐ | S1: 10. o.; S3: shuto; S4: 10. o. | A belső névhez az egyik angol leírás kifelé tartó csapást társít. A pálya és a célpont bemutatás alapján tisztázandó. | nyitott |
| KY-0090 | Shutō ganmen uchi | Kézélütés arcra | punches | PONTOSÍTANDÓ | S1: 10. o.; S3: shuto | Az arc mint gyűjtőcél elfogadható, de az egyik leírás konkrétabban halántékot nevez meg. A változatot rögzíteni kell. | nyitott |
| KY-0091 | Nukite | Ujjhegyes szúrás | punches | MEGFELELTETHETŐ | S3: alaptechnikák; S1: 9–10. o. | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0092 | Hiji ate | Könyökütés | punches | MEGFELELTETHETŐ | S3: alaptechnikák; S1: 9–10. o. | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0093 | Hiji age uchi | Emelkedő könyökütés | punches | MEGFELELTETHETŐ | S3: alaptechnikák; S1: 9–10. o. | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0094 | Hiji oroshi uchi | Lefelé csapó könyökütés | punches | MEGFELELTETHETŐ | S3: alaptechnikák; S1: 9–10. o. | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0095 | Hiji mawashi uchi | Köríves könyökütés | punches | NYITOTT | S1: 1., 10–11. o.; S2: technikai listák | Az összetett név pontos alakjához és önálló rekordként való elkülönítéséhez nincs elég közvetlen bizonyíték a vizsgált anyagokban. | nyitott |
| KY-0096 | Mae geri | Elülső rúgás | kicks | MEGFELELTETHETŐ | S1: 1., 11. o.; S2: Kicks | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0097 | Mae keage | Elülső lendítő rúgás | kicks | MEGFELELTETHETŐ | S1: 1., 11. o.; S2: Kicks | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0098 | Yoko geri | Oldalrúgás | kicks | MEGFELELTETHETŐ | S1: 1., 11. o.; S2: Kicks | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0099 | Yoko keage | Oldalsó lendítő rúgás | kicks | MEGFELELTETHETŐ | S1: 1., 11. o.; S2: Kicks | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0100 | Yoko kekomi | Oldalsó tolórúgás | kicks | NYITOTT | S1: 1., 10–11. o.; S2: technikai listák | Az összetett név pontos alakjához és önálló rekordként való elkülönítéséhez nincs elég közvetlen bizonyíték a vizsgált anyagokban. | nyitott |
| KY-0101 | Mawashi geri | Köríves rúgás | kicks | MEGFELELTETHETŐ | S1: 1., 11. o.; S2: Kicks | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0102 | Ushiro geri | Hátrarúgás | kicks | MEGFELELTETHETŐ | S1: 1., 11. o.; S2: Kicks | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0103 | Ushiro mawashi geri | Hátsó köríves rúgás | kicks | MEGFELELTETHETŐ | S1: 1., 11. o.; S2: Kicks | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0104 | Ura mawashi geri | Fordított köríves rúgás | kicks | NYITOTT | S1: 1., 10–11. o.; S2: technikai listák | Az összetett név pontos alakjához és önálló rekordként való elkülönítéséhez nincs elég közvetlen bizonyíték a vizsgált anyagokban. | nyitott |
| KY-0105 | Hiza geri | Térdrúgás | kicks | MEGFELELTETHETŐ | S1: 1., 11. o.; S2: Kicks | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0106 | Kin geri | Ágyékrúgás | kicks | MEGFELELTETHETŐ | S1: 1., 11. o.; S2: Kicks | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0107 | Kansetsu geri | Ízületre irányuló rúgás | kicks | MEGFELELTETHETŐ | S1: 1., 11. o.; S2: Kicks | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0108 | Uchi mawashi geri | Befelé ívelő köríves rúgás | kicks | FORRÁSFÜGGŐ | S1: 11. o.; S3: rúgások; S4: 11. o. | A források irányleírása nem egységes: az IFK anyag mozgáspályája a jelenlegi magyar befelé/kifelé értelmezéssel ellentétes. Nem szabad automatikusan felcserélni; bemutatás és szervezeti névhasználat kell. | nyitott |
| KY-0109 | Soto mawashi geri | Kifelé ívelő köríves rúgás | kicks | FORRÁSFÜGGŐ | S1: 11. o.; S3: rúgások; S4: 11. o. | A források irányleírása nem egységes: az IFK anyag mozgáspályája a jelenlegi magyar befelé/kifelé értelmezéssel ellentétes. Nem szabad automatikusan felcserélni; bemutatás és szervezeti névhasználat kell. | nyitott |
| KY-0110 | Oroshi kakato geri | Lefelé csapó sarokrúgás | kicks | PONTOSÍTANDÓ | S1: 1., 11. o.; S2: 1st Kyu | A sarokrúgás iránya túl általános; a befelé/kifelé vezetett változat megkülönböztetése szükséges lehet. | nyitott |
| KY-0111 | Tobi mae geri | Ugró elülső rúgás | kicks | MEGFELELTETHETŐ | S1: 1., 11. o.; S2: Kicks | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0112 | Tobi yoko geri | Ugró oldalrúgás | kicks | MEGFELELTETHETŐ | S1: 1., 11. o.; S2: Kicks | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0113 | Tobi mawashi geri | Ugró köríves rúgás | kicks | MEGFELELTETHETŐ | S1: 1., 11. o.; S2: Kicks | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0114 | Mae hiza geri | Elülső térdrúgás | kicks | NYITOTT | S1: 1., 10–11. o.; S2: technikai listák | Az összetett név pontos alakjához és önálló rekordként való elkülönítéséhez nincs elég közvetlen bizonyíték a vizsgált anyagokban. | nyitott |
| KY-0115 | Gedan mawashi geri | Alsó köríves rúgás | kicks | MEGFELELTETHETŐ | S1: 1., 11. o.; S2: Kicks | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0116 | Chūdan mawashi geri | Középső köríves rúgás | kicks | MEGFELELTETHETŐ | S1: 1., 11. o.; S2: Kicks | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0117 | Jōdan mawashi geri | Felső köríves rúgás | kicks | MEGFELELTETHETŐ | S1: 1., 11. o.; S2: Kicks | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0118 | Seiken | Első két ökölbütyök | body_parts | MEGFELELTETHETŐ | S3: testfegyverek; S4: 7. o. | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0119 | Uraken | Kézhát / kézhátbütykök | body_parts | JAVÍTÁS JAVASOLT | S3: testfegyverek és uraken; S4: 7., 10. o. | Az ökölhát és a nyitott kéz háta megkülönböztetendő. Magyarul ökölhát / ökölhátütés javasolt, a technika ellenőrzésével. | javítás javasolt |
| KY-0120 | Tettsui | Kalapácsököl | body_parts | MEGFELELTETHETŐ | S3: testfegyverek; S4: 7. o. | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0121 | Shutō | Kézél | body_parts | MEGFELELTETHETŐ | S3: testfegyverek; S4: 7. o. | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0122 | Haitō | Belső kézél | body_parts | MEGFELELTETHETŐ | S3: testfegyverek; S4: 7. o. | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0123 | Shōtei | Tenyértő | body_parts | MEGFELELTETHETŐ | S3: testfegyverek; S4: 7. o. | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0124 | Hiji | Könyök | body_parts | MEGFELELTETHETŐ | S3: testfegyverek; S4: 7. o. | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0125 | Hiza | Térd | body_parts | MEGFELELTETHETŐ | S3: testfegyverek; S4: 7. o. | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0126 | Chūsoku | Talppárna | body_parts | MEGFELELTETHETŐ | S3: testfegyverek; S4: 7. o. | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0127 | Sokutō | Talp külső éle | body_parts | MEGFELELTETHETŐ | S3: testfegyverek; S4: 7. o. | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0128 | Kakato | Sarok | body_parts | MEGFELELTETHETŐ | S3: testfegyverek; S4: 7. o. | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0129 | Haisoku | Lábfej felső része | body_parts | MEGFELELTETHETŐ | S3: testfegyverek; S4: 7. o. | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0130 | Teisoku | Talp belső/alsó felülete | body_parts | PONTOSÍTANDÓ | S3: teisoku; S4: 7. o. | A teljes alsó talpfelület túl tág. A boltozati/belső talpfelület pontos megnevezése javasolt. | nyitott |
| KY-0131 | Sune | Sípcsont | body_parts | MEGFELELTETHETŐ | S3: testfegyverek; S4: 7. o. | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0132 | Ichi | Egy | numbers | MEGFELELTETHETŐ | S2: Counting; S3: számok | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0133 | Ni | Kettő | numbers | MEGFELELTETHETŐ | S2: Counting; S3: számok | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0134 | San | Három | numbers | MEGFELELTETHETŐ | S2: Counting; S3: számok | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0135 | Shi | Négy | numbers | MEGFELELTETHETŐ | S2: Counting; S3: számok | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0136 | Go | Öt | numbers | MEGFELELTETHETŐ | S2: Counting; S3: számok | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0137 | Roku | Hat | numbers | MEGFELELTETHETŐ | S2: Counting; S3: számok | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0138 | Shichi | Hét | numbers | MEGFELELTETHETŐ | S2: Counting; S3: számok | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0139 | Hachi | Nyolc | numbers | MEGFELELTETHETŐ | S2: Counting; S3: számok | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0140 | Ku | Kilenc | numbers | MEGFELELTETHETŐ | S2: Counting; S3: számok | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0141 | Jū | Tíz | numbers | MEGFELELTETHETŐ | S2: Counting; S3: számok | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0142 | Taikyoku sono ichi | Taikyoku első forma | kata | MEGFELELTETHETŐ | S2: kata-listák; S4: 4–6. o. | Nincs feltárt lényegi név-/jelentésellentmondás. A magyar mező névcímke, nem teljes etimológiai fordítás; ez önmagában nem hiba. | nincs teendő |
| KY-0143 | Taikyoku sono ni | Taikyoku második forma | kata | MEGFELELTETHETŐ | S2: kata-listák; S4: 4–6. o. | Nincs feltárt lényegi név-/jelentésellentmondás. A magyar mező névcímke, nem teljes etimológiai fordítás; ez önmagában nem hiba. | nincs teendő |
| KY-0144 | Taikyoku sono san | Taikyoku harmadik forma | kata | MEGFELELTETHETŐ | S2: kata-listák; S4: 4–6. o. | Nincs feltárt lényegi név-/jelentésellentmondás. A magyar mező névcímke, nem teljes etimológiai fordítás; ez önmagában nem hiba. | nincs teendő |
| KY-0145 | Pinan sono ichi | Pinan első forma | kata | MEGFELELTETHETŐ | S2: kata-listák; S4: 4–6. o. | Nincs feltárt lényegi név-/jelentésellentmondás. A magyar mező névcímke, nem teljes etimológiai fordítás; ez önmagában nem hiba. | nincs teendő |
| KY-0146 | Pinan sono ni | Pinan második forma | kata | MEGFELELTETHETŐ | S2: kata-listák; S4: 4–6. o. | Nincs feltárt lényegi név-/jelentésellentmondás. A magyar mező névcímke, nem teljes etimológiai fordítás; ez önmagában nem hiba. | nincs teendő |
| KY-0147 | Pinan sono san | Pinan harmadik forma | kata | MEGFELELTETHETŐ | S2: kata-listák; S4: 4–6. o. | Nincs feltárt lényegi név-/jelentésellentmondás. A magyar mező névcímke, nem teljes etimológiai fordítás; ez önmagában nem hiba. | nincs teendő |
| KY-0148 | Pinan sono yon | Pinan negyedik forma | kata | MEGFELELTETHETŐ | S2: kata-listák; S4: 4–6. o. | Nincs feltárt lényegi név-/jelentésellentmondás. A magyar mező névcímke, nem teljes etimológiai fordítás; ez önmagában nem hiba. | nincs teendő |
| KY-0149 | Pinan sono go | Pinan ötödik forma | kata | MEGFELELTETHETŐ | S2: kata-listák; S4: 4–6. o. | Nincs feltárt lényegi név-/jelentésellentmondás. A magyar mező névcímke, nem teljes etimológiai fordítás; ez önmagában nem hiba. | nincs teendő |
| KY-0150 | Sanchin | Három csata | kata | MEGFELELTETHETŐ | S2: kata-listák; S4: 4–6. o. | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0151 | Yantsu | Yantsu kata | kata | MEGFELELTETHETŐ | S2: kata-listák; S4: 4–6. o. | Nincs feltárt lényegi név-/jelentésellentmondás. A magyar mező névcímke, nem teljes etimológiai fordítás; ez önmagában nem hiba. | nincs teendő |
| KY-0152 | Tsuki no kata | Ütések formagyakorlata | kata | MEGFELELTETHETŐ | S2: kata-listák; S4: 4–6. o. | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0153 | Gekisai dai | Gekisai nagy forma | kata | MEGFELELTETHETŐ | S2: kata-listák; S4: 4–6. o. | Nincs feltárt lényegi név-/jelentésellentmondás. A magyar mező névcímke, nem teljes etimológiai fordítás; ez önmagában nem hiba. | nincs teendő |
| KY-0154 | Gekisai shō | Gekisai kis forma | kata | MEGFELELTETHETŐ | S2: kata-listák; S4: 4–6. o. | Nincs feltárt lényegi név-/jelentésellentmondás. A magyar mező névcímke, nem teljes etimológiai fordítás; ez önmagában nem hiba. | nincs teendő |
| KY-0155 | Tenshō | Forgó tenyerek | kata | MEGFELELTETHETŐ | S2: kata-listák; S4: 4–6. o. | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0156 | Saiha | Saiha kata | kata | FORRÁSFÜGGŐ | S1: 12. o.; S2: Shodan; S4: 4–6. o. | Eltérő névváltozat szerepel a forrásokban; nem bizonyított elírás. Alias és választott szervezeti szóhasználat rögzítendő. A magyar mező névcímke, nem teljes etimológiai fordítás; ez önmagában nem hiba. | nyitott |
| KY-0157 | Seienchin | Seienchin kata | kata | MEGFELELTETHETŐ | S2: kata-listák; S4: 4–6. o. | Nincs feltárt lényegi név-/jelentésellentmondás. A magyar mező névcímke, nem teljes etimológiai fordítás; ez önmagában nem hiba. | nincs teendő |
| KY-0158 | Garyū | Fekvő sárkány | kata | MEGFELELTETHETŐ | S2: kata-listák; S4: 4–6. o. | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0159 | Seipai | Tizennyolc | kata | MEGFELELTETHETŐ | S2: kata-listák; S4: 4–6. o. | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0160 | Kankū dai | Égre tekintés (nagy forma) | kata | MEGFELELTETHETŐ | S2: kata-listák; S4: 4–6. o. | Nincs feltárt lényegi név-/jelentésellentmondás. | nincs teendő |
| KY-0161 | Sūshiho | Ötvennégy lépés | kata | PONTOSÍTANDÓ | S4: 6. o.; S2: Sandan | A név és az 54 lépés jelentés összeillik; a jelenlegi hosszú magánhangzók következetességét japán íráskép nélkül nem igazolja a latin betűs lista. | nyitott |

## Következő szakmai döntések

Elsőként az ökölhát-terminológiát és az irányleírásokat kell a mesterrel végignézni. Utána a túl általános nevek pontosítása, a közvetlenül nem igazolt változatok bemutatása és a kategóriák rendezése következhet. A névcsere előtt az aliasokat és a kvízbeli összetéveszthetőséget is vizsgálni kell. A források nem adnak önmagukban teljes magyar terminológiai szabványt.
