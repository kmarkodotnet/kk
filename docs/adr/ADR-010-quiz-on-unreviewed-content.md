# ADR-010 – Kvíz engedélyezése lektorálatlan tartalmon, egyértelmű jelöléssel

- **Állapot:** elfogadva (2026-11-11, felhasználói döntés)
- **Felülírja:** a korábbi szabály azon olvasatát, hogy `quizEligible` kizárólag
  `verified = true` rekordon lehet igaz (`kyo-knowledge-base` skill V007).
- **Kontextus:** megérkezett a tényleges tudástár (`knowledgebase/`, 161 rekord,
  9 kategória, `dataset_version 0.1.0`), amelyben **minden** rekord
  `review_status = editorial_review_required`.

## A probléma

A szigorú szabály mellett a 161 rekordból **nulla** lett volna kvízképes, tehát a kvíz –
a termék fő funkciója – egyáltalán nem indult volna el, amíg egy szakértő mind a 161 tételt
végig nem nézi. Az app addig csak böngésző lenne; a kvízmotor, a statisztika és az
elfogadási kritériumok nagy része nem lenne kipróbálható és demonstrálható.

## Döntés

A kvíz **futhat lektorálatlan tartalommal**, de a lektorálatlan állapotot az alkalmazás
végig, feltűnően és félreérthetetlenül jelzi:

| Hol | Mit jelez |
|---|---|
| minden képernyő teteje | nem elutasítható szalag: „Lektorálatlan gyakorlóanyag – a tartalom szakmai ellenőrzésre vár." |
| Tudástár lista és részlet | rekordonkénti „Ellenőrzésre vár" jelölő (változatlan) |
| Kvíz konfiguráció | a kvíz indítása előtt kiírja, hogy lektorálatlan anyagon gyakorolsz |
| Kvíz eredmény | az összegzés mellett: „Az eredmény lektorálatlan tananyagon alapul." |
| Haladás | a statisztika fejlécében ugyanez a megjegyzés |

### Miért nem sérti ez a tartalmi integritást

A CR-01/CR-02 szabály célja, hogy **semmi ne jelenjen meg hitelesként**, ami nem az.
Egy végig megjelölt, „lektorálatlan gyakorlóanyag" címkével futó kvíz nem állít hitelességet;
a felhasználó pontosan tudja, mit gyakorol. A tiltás továbbra is fenntartás nélkül érvényes:

- a `review_status`/`verified` státuszt **kód, import vagy migráció nem emelheti** (CR-02);
- **generált** (modell által kitalált) technikanév vagy magyar jelentés továbbra sem
  kerülhet a tudástárba, sem lektorálatlanként;
- a jelölés nem elrejthető és nem kikapcsolható a felhasználó által.

## Megvalósítási következmény

A `quizEligible` számítása kétlépcsős lesz:

```
quizEligible(technique, datasetTrust) =
    technique.status != INCOMPLETE                       // a hiányos rekord MINDIG kizárva
 && technique.hasFieldsFor(questionType)                 // típusonkénti mezőkövetelmény
 && (datasetTrust == REVIEWED || datasetTrust == PRACTICE_ON_UNREVIEWED)
```

- Hiányos (`INCOMPLETE`) rekord **soha** nem kvízképes – ez nem változott.
- A `datasetTrust` a manifest `editorial_status` mezőjéből származik, nem felhasználói
  beállításból: a tartalom dönti el, nem a felhasználó.
- Ha a tartalom `draft_review_required`, az app `PRACTICE_ON_UNREVIEWED` módban fut, és a
  fenti jelöléseket mindenhol megjeleníti.
- Ha később `reviewed` tartalom érkezik, a jelölések automatikusan eltűnnek – kódmódosítás
  nélkül (AC-08).

## Mérlegelt alternatívák

| Alternatíva | Miért nem |
|---|---|
| Szigorú marad: nincs kvíz lektorálásig | Az MVP fő funkciója használhatatlan és tesztelhetetlen lenne egy olyan okból, amit a felhasználó egyértelműen vállal. |
| Csak „alacsony kockázatú" kategóriák (számok, vezényszavak, etikett) | Önkényes határ, és épp a technikai kategóriák a termék lényege. Félmegoldás, több kóddal. |
| Felhasználói beállítás: „engedd a lektorálatlan tartalmat" | Elrejthető figyelmeztetés = elrejtett kockázat. A tartalom bizalmi szintje nem lehet kapcsoló. |

## Következmény a tesztekre

- Új teszt: `PRACTICE_ON_UNREVIEWED` módban a `quizEligible` igaz az ép rekordokra, de
  **hamis** minden `INCOMPLETE` rekordra.
- Új teszt: a `datasetTrust` kizárólag a manifestből származik; nincs olyan kódút, amely
  felhasználói bemenetből állítaná.
- UI teszt (TR-06): a jelölés jelen van a kvíz- és az eredményképernyőn.
