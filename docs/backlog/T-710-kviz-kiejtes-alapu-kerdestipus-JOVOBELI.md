# T-710 · Kvízkérdés-típus lejátszott kiejtés alapján — JÖVŐBELI, JELENLEG NEM INDÍTHATÓ

| | |
|---|---|
| Modul | core/quiz, feature/quiz |
| Függ | **OQ-05, OQ-06 (nyitott döntés, `docs/REQUIREMENTS.md` 10. szakasz)**, T-709 — szándékosan nem elvégezhető task-függőség |
| Követelmény | igény rögzítve, **nincs** hozzárendelt FR; a mesterprompt 9. pontja miatt a kiejtéslejátszás (T-709) előfeltétele sincs meg |
| Teszt | — |

> **Ezt a jegyet a `/dev` és a `/devall` NEM veheti fel automatikusan** – lásd T-709
> figyelmeztetését, ugyanúgy érvényes. Ez a task **T-709-től is függ**, ami maga sem
> indítható, így kétszeresen blokkolt.

A felhasználó egy ötödik kvízkérdés-típust kér: lejátszott japán kiejtés alapján kell a
helyes magyar nevet kiválasztani a jelöltek közül. Ez értelemszerűen a T-709 kiejtés-
lejátszás funkcióra épül, aminek bevezetése önmagában is nyitott (OQ-05).

## Mit kell eldönteni, mielőtt ez a task elindítható (OQ-06)

- Új kérdéstípus-kód bevezetése (jelenleg (A)–(D) van, lásd FR-12) – ez érinti a
  `core/quiz` `QuestionType` enumot, a jelöltszűrést (`QuizEligibility`,
  ADR-010/ADR-011 mintájára egy új "audio_usable" kapu kellene), és a kvízbeállító
  UI típusválasztóját (T-404).
- A meglévő, `quiz_usable` képkapuhoz hasonló jogosultsági szabály kellene a hangra is
  (pl. csak akkor aktív az új típus, ha a technikának van kiejtés-forrása) – ez csak
  a T-709 döntése (TTS vs. hangfájl) után tervezhető meg érdemben.

## A döntés után elvégzendő (vázlat, nem végleges Teendők lista)

- `core/quiz`: új `QuestionType`, jelöltszűrési kapu, disztraktorválasztás ellenőrzése
  (a meglévő szinonima-/kép-kizárási logika mintájára).
- `feature/quiz`: kérdés UI a hanglejátszó vezérlővel, válasz-visszajelzés a meglévő
  mintát követve (FR-17).
- TR-02/TR-03 jellegű invariáns-tesztek bővítése az új típusra.
