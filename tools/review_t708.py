"""T-708: kézzel értékelt forrásösszevetésből teljes, reprodukálható kimutatás.

Nem nyelvi/technikai hitelesítő algoritmus, és nem módosít tananyagot.
"""
from pathlib import Path
import json, hashlib, csv, io
from collections import Counter

root=Path(__file__).resolve().parents[1]
asset=root/'app/src/main/assets/knowledge/entries.json'
entries=json.loads(asset.read_text(encoding='utf-8'))['entries']
issues={}
def note(ids,status,reason,sources):
    for i in ids: issues[i]=(status,reason,sources)
note([5,41],'PONTOSÍTANDÓ','A rangidősség nem kizárólag övfok; a korábban kezdett gyakorlás is számít.','S5: Senpai & Kohai; S1: 3. o.')
note([42],'PONTOSÍTANDÓ','A fiatalabb félreérthető életkorként. Javaslat: később kezdett / kevésbé régi tanítvány.','S5: Senpai & Kohai')
note([43],'PONTOSÍTANDÓ','A megadott alapítói szerep kontextus, nem szó szerinti címfordítás. Az elnöki jelentést is külön kell jelezni.','S1: 3. o.; S4: 14. o.')
note([38,39],'PONTOSÍTANDÓ','A tanuló-/mesterfokozat magyar szokásos címke; nem jelent automatikus oktatói vagy szakértői minősítést.','S4: 12–13. o.; S5: Dan grades & titles')
note([20],'NYITOTT','A teljes kifejezéshez a vizsgált listákban nem találtam közvetlen párt; ez nem bizonyítja, hogy hibás.','S1: 3–7. o.; S3: általános kifejezések (negatív keresés)')
note([48],'NYITOTT','A forrás részletesebb irányjelölést is használ; a jelző nélküli állás és a lábfejirány pontos azonosságát lektorálni kell.','S4: 9. o.; S1: 1. o.')
note([59],'FORRÁSFÜGGŐ','A forrás egyszerűbb felső védésnevet használ. Az age bővítés elfogadott helyi alak-e, bemutatással tisztázandó.','S1: 11. o.; S2: Blocks; S3: védések')
note([60,61],'PONTOSÍTANDÓ','A külső/belső név és a mozgás iránya nem azonos. A magyar név mellé a befelé/kifelé mozgást is egyértelműsíteni kell.','S1: 11. o.; S3: védések')
note([65],'PONTOSÍTANDÓ','A középső kézélvédés nem választja szét a belső és külső változatot. Pontos technikát kell hozzárendelni.','S1: 1. o.; S2: 4th Kyu')
note([67],'PONTOSÍTANDÓ','A kétkezes és a megtámasztott nem automatikusan ugyanaz. A forrás teljesebb technikanevet ad; a fogalmat pontosítani kell.','S1: 11. o.; S4: 11. o.')
note([71],'NYITOTT','Nincs közvetlen találat a megvizsgált technikai listákban. Külön Kyokushin-bemutató/forrás szükséges.','S1: 11. o.; S2: Blocks; S4: 11. o. (negatív keresés)')
note([81,82,83,84,119],'JAVÍTÁS JAVASOLT','Az ökölhát és a nyitott kéz háta megkülönböztetendő. Magyarul ökölhát / ökölhátütés javasolt, a technika ellenőrzésével.','S3: testfegyverek és uraken; S4: 7., 10. o.')
note([82],'JAVÍTÁS JAVASOLT','Ökölhátütés javasolt; a sayū kétoldaliságát az általános oldalirány helyett érdemes kifejteni.','S3: sayu és uraken; S1: 9. o.')
note([89],'FORRÁSFÜGGŐ','A belső névhez az egyik angol leírás kifelé tartó csapást társít. A pálya és a célpont bemutatás alapján tisztázandó.','S1: 10. o.; S3: shuto; S4: 10. o.')
note([90],'PONTOSÍTANDÓ','Az arc mint gyűjtőcél elfogadható, de az egyik leírás konkrétabban halántékot nevez meg. A változatot rögzíteni kell.','S1: 10. o.; S3: shuto')
note([95,100,104,114],'NYITOTT','Az összetett név pontos alakjához és önálló rekordként való elkülönítéséhez nincs elég közvetlen bizonyíték a vizsgált anyagokban.','S1: 1., 10–11. o.; S2: technikai listák')
note([108,109],'FORRÁSFÜGGŐ','A források irányleírása nem egységes: az IFK anyag mozgáspályája a jelenlegi magyar befelé/kifelé értelmezéssel ellentétes. Nem szabad automatikusan felcserélni; bemutatás és szervezeti névhasználat kell.','S1: 11. o.; S3: rúgások; S4: 11. o.')
note([110],'PONTOSÍTANDÓ','A sarokrúgás iránya túl általános; a befelé/kifelé vezetett változat megkülönböztetése szükséges lehet.','S1: 1., 11. o.; S2: 1st Kyu')
note([130],'PONTOSÍTANDÓ','A teljes alsó talpfelület túl tág. A boltozati/belső talpfelület pontos megnevezése javasolt.','S3: teisoku; S4: 7. o.')
note([156],'FORRÁSFÜGGŐ','Eltérő névváltozat szerepel a forrásokban; nem bizonyított elírás. Alias és választott szervezeti szóhasználat rögzítendő.','S1: 12. o.; S2: Shodan; S4: 4–6. o.')
note([161],'PONTOSÍTANDÓ','A név és az 54 lépés jelentés összeillik; a jelenlegi hosszú magánhangzók következetességét japán íráskép nélkül nem igazolja a latin betűs lista.','S4: 6. o.; S2: Sandan')

sources='''## Források és súlyuk

- **S1:** [IFK Norge – Syllabus og Renraku](https://www.ifknorge.org/wp-content/uploads/2024/03/syllabus_hoyopp.pdf). Nemzeti Kyokushin-szervezet saját oldalán elérhető vizsgaanyag és szószedet; az oldalszámok a PDF-olvasó 1-től számozott lapjai. Saját elírásokat is tartalmaz, ezért nem abszolút tekintély.
- **S2:** [Nintai Dojo – BKK/IFK syllabus](https://www.nintaidojo.co.uk/syllabus/). Az 1993-as anyag mai átirata. S1-gyel közös IFK-hagyomány: nem számít teljesen független második iskolának.
- **S3:** [Mészáros Dojo, Vác – Kyokushin szótár](https://kyovac.hu/szotar/). Magyar dojo saját oktatási anyaga, közvetlen magyar jelentésekhez. Független kiadó S1-től; helyi szóhasználat és néhány gépelési hiba van benne. Nem országos normatív jóváhagyás.
- **S4:** [Isshin Dojo – Kyokushin Kata & Terminology](https://www.isshindojo.nl/resource/file/normal/999d1f82647872dc1ef3830c1cdf3f8bb92d03bb_Kyokushin-Kata-Kyokushin-Terminology.pdf). Kiegészítő dojoanyag, különösen katajelentésekhez. Több része közös hagyományból származhat S1-gyel; nem kezeljük független konszenzusszavazatként. Oldalak itt is 1-től.
- **S5:** [Kyokushin-kan Indonesia – Terminology](https://www.kyokushinkan-indonesia.org/pages/terminology.html). Más szervezeti ág saját cím- és rangidősség-magyarázata; közvetlenül a címekhez, nem az összes technikához használható.

Elérés: 2026-11-14. Az ausztrál Kyokushin-kan PDF két lekéréssel 502 hibát adott; egy IKO kata-PDF 404-et. Ezek tartalmára nem alapozunk megállapítást. S1 + S3 külön kiadójú szakmai anyag; S5 külön szervezeti ágat is bevon. A többször átvett szöveg nem több független bizonyíték.
'''
rows=[]
for e in entries:
    i=int(e['id'][3:]); cat=e['category_id']
    if i<=19: src='S1: 3–6. o.; S4: 12–14. o.'
    elif i<=43: src='S1: 5–8. o.; S3: általános kifejezések'
    elif i<=58: src='S4: 9. o.; S2: Stances; S3: állások'
    elif i<=72: src='S1: 1., 11. o.; S2: Blocks'
    elif i<=95: src='S3: alaptechnikák; S1: 9–10. o.'
    elif i<=117: src='S1: 1., 11. o.; S2: Kicks'
    elif i<=131: src='S3: testfegyverek; S4: 7. o.'
    elif i<=141: src='S2: Counting; S3: számok'
    else: src='S2: kata-listák; S4: 4–6. o.'
    status,reason,src=issues.get(i,('MEGFELELTETHETŐ','Nincs feltárt lényegi név-/jelentésellentmondás.',src))
    if cat=='commands' and i>=21:
        reason+=' Kategória: irány/szint/fogalom/cím, nem önálló vezényszó; kategóriabontás javasolt.'
        if status=='MEGFELELTETHETŐ': status='KATEGÓRIA'
    if 142<=i<=149 or i in (151,153,154,156,157):
        reason+=' A magyar mező névcímke, nem teljes etimológiai fordítás; ez önmagában nem hiba.'
    if i in (73,74,75): reason+=' A szintkülönbség megtartandó; a források összevont szintjelölést is alkalmaznak.'
    if i in (54,57): reason+=' A magyar név használható címke, de nem teljes szó szerinti fordítás.'
    action='nincs teendő' if status=='MEGFELELTETHETŐ' else ('javítás javasolt' if status in ('JAVÍTÁS JAVASOLT','KATEGÓRIA') else 'nyitott')
    rows.append(dict(id=e['id'],romaji=e['japanese_romaji'],magyar=e['hungarian_name'],kategoria=cat,status=status,forras=src,eszrevetel=reason,teendo=action))
counts=Counter(r['status'] for r in rows)
head=f'''# T-708 – Fordítások és terminológia forrásösszevetése

Dátum: 2026-11-14. Teljes állomány: **{len(entries)} rekord**. Ez dokumentált tartalmi előellenőrzés, nem szakmai hitelesítés. A romaji, a magyar név és a kategória minden rekordnál áttekintve. A forrás szerinti fogalmi összeillés nem jelent szó szerinti magyar szövegegyezést.

Pillanatkép SHA-256 (`app/src/main/assets/knowledge/entries.json`): `{hashlib.sha256(asset.read_bytes()).hexdigest()}`.
A CSV az elsődleges szerkesztési forrás; az alábbi ellenőrzés külön igazolja a három mező egyezését vele. A párhuzamos Claude-fejlesztés kódjához nem nyúltunk. Tananyag, verzió és review-státusz nem változott.

## Kimutatás

| Jelölés | Rekord |
|---|---:|
'''+''.join(f'| {k} | {v} |\n' for k,v in sorted(counts.items()))+'''
**MEGFELELTETHETŐ:** a megjelölt helyeken lényegi ellentmondás nem merült fel; nem hitelesítési pecsét. **JAVÍTÁS JAVASOLT/PONTOSÍTANDÓ:** problémás megnevezés vagy félreérthető meghatározás. **FORRÁSFÜGGŐ:** dokumentált eltérő használat. **NYITOTT:** elégtelen közvetlen bizonyíték, nem bizonyított hiba. **KATEGÓRIA:** szerkezeti/oktatási csoportosítás, nem feltétlen rossz fordítás.

## Fontos értelmezési határok

A makronok, kötőjelek és szóközök hiánya a forrásban nem automatikus elírás. Japán íráskép és kiejtési forrás nélkül a hosszú magánhangzók teljes ellenőrzése nyitott. A rövidített nevek lehetnek érvényes változatok. A különböző szintű és irányú technikákat nem szabad pusztán szöveghasonlóság alapján összevonni.

Az `Ütések` gyűjtőkategória tartalmazhat csapásokat és könyöktechnikákat is; az angol belső `punches` azonosító nem önmagában tananyaghiba. A `commands` kategóriába tett szótári fogalmaknál ezt külön megjelöltük.

Automatikus adatjavítás nincs: a jelzett eltérések szakmai értelmezést vagy helyi szóhasználati döntést igényelnek. Nincs olyan bizonyított, egyszerű gépelési hiba, amelyet ebben a körben migrálni kellett volna.

'''+sources
def table(rs):
    result='| id | Jelenlegi romaji | Jelenlegi magyar | Kategória | Jelölés | Forráshely | Megállapítás | Javasolt teendő |\n|---|---|---|---|---|---|---|---|\n'
    for r in rs: result+='| '+' | '.join(str(r[k]).replace('|',' / ').replace('\n',' ') for k in ('id','romaji','magyar','kategoria','status','forras','eszrevetel','teendo'))+' |\n'
    return result
flagged=[r for r in rows if r['status']!='MEGFELELTETHETŐ']
report=head+'\n## Kiemelt, átnézendő megnevezések\n\n'+table(flagged)+'\n## Teljes, 161 soros ellenőrzési jegyzék\n\n'+table(rows)
report+='\n## Következő szakmai döntések\n\nElsőként az ökölhát-terminológiát és az irányleírásokat kell a mesterrel végignézni. Utána a túl általános nevek pontosítása, a közvetlenül nem igazolt változatok bemutatása és a kategóriák rendezése következhet. A névcsere előtt az aliasokat és a kvízbeli összetéveszthetőséget is vizsgálni kell. A források nem adnak önmagukban teljes magyar terminológiai szabványt.\n'
(root/'docs/CONTENT_VERIFICATION.md').write_text(report,encoding='utf-8')
with (root/'docs/CONTENT_VERIFICATION.csv').open('w',encoding='utf-8-sig',newline='') as f:
    out=csv.DictWriter(f,fieldnames=list(rows[0]));out.writeheader();out.writerows(rows)
csvrows=list(csv.DictReader((root/'knowledgebase/kyokushin_knowledge_hu.csv').open(encoding='utf-8-sig')))
lookup={r['id']:r for r in csvrows}
assert len(rows)==161 and len(set(r['id'] for r in rows))==161
for e in entries:
    assert all(e[k]==lookup[e['id']][k] for k in ('japanese_romaji','hungarian_name','category_id'))
assert all(e['review_status']=='editorial_review_required' for e in entries)
print(json.dumps(dict(total=len(rows),flagged=len(flagged),counts=counts,csv_matches=True),ensure_ascii=False))
