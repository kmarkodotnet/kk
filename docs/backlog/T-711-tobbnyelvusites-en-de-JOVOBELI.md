# T-711 · Többnyelvűsítés (angol, német) — JÖVŐBELI, JELENLEG NEM INDÍTHATÓ

| | |
|---|---|
| Modul | összes `feature/*`, `core/model` |
| Függ | **OQ-07 (nyitott döntés, `docs/REQUIREMENTS.md` 10. szakasz) — szándékosan nem elvégezhető task-függőség** |
| Követelmény | igény rögzítve, **nincs** hozzárendelt FR; a mesterprompt 9. pontja ("többnyelvűség") explicit kizárja MOST-ra |
| Teszt | — |

> **Ezt a jegyet a `/dev` és a `/devall` NEM veheti fel automatikusan** – lásd T-709
> figyelmeztetését, ugyanúgy érvényes.

A felhasználó a magyar mellett angol és német felhasználói szöveget kér. A CLAUDE.md
vasszabálya jelenleg ezt mondja ki: *"minden felhasználói szöveg és dokumentáció
magyar"*. A mesterprompt 9. pontja a többnyelvűséget explicit a "később, MOST ne
implementáld" listán tartja.

## Mit kell eldönteni, mielőtt ez a task elindítható (OQ-07)

- A `CLAUDE.md` idézett vasszabályának **explicit felülírása/módosítása** szükséges a
  felhasználó részéről, mielőtt bármilyen angol/német `strings.xml` erőforrás
  bekerülhetne – enélkül a jelenlegi vasszabály és a többnyelvűsítési igény
  egymásnak ellentmond, és a vasszabály győz (`CLAUDE.md`: *"Ha bármi ütközik, a
  mesterprompt győz"*).
- Terjed-e a többnyelvűsítés a **tartalomra** (romaji változatlan marad, de a magyar
  jelentés/leírás angol-német fordítása is hiteles forrásból kell, hogy jöjjön – ez
  önmagában egy T-708-hoz hasonló tartalmi ellenőrzési munka **három** nyelven) vagy
  **csak a UI-kezelőfelület szövegeire** (gombfeliratok, képernyőcímek), a tartalmi
  leírások maradnak magyarul?
- Nyelvválasztás: rendszerbeállítás követése vagy explicit in-app választó?

## A döntés után elvégzendő (vázlat, nem végleges Teendők lista)

- `strings.xml` → `values-en/strings.xml`, `values-de/strings.xml` minden modulban.
- Ha a tartalomra is kiterjed: a kanonikus séma bővítése (`hungarian_name` mintájára
  `english_name`/`german_name`), import/validátor bővítés, migráció (ADR szükséges).
- A kvízmotor normalizálása (`NameNormalizer`) és a disztraktor-/szinonima-kizárás
  nyelvenkénti konzisztenciájának átgondolása.
