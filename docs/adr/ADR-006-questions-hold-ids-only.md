# ADR-006 – A kvízkérdés csak technikaazonosítókat tárol

- **Állapot:** elfogadva (2026-11-11)
- **Kontextus:** DR-03 – a helyes válasz összehasonlítása technikaazonosítóval történjen,
  soha ne szabad szöveges egyezéssel. CR-07 – a szinonimák nem lehetnek distraktorok.

## Döntés

```kotlin
data class QuizQuestion(
    val id: QuestionId,
    val type: QuestionType,
    val correctTechniqueId: TechniqueId,
    val optionTechniqueIds: List<TechniqueId>,  // pontosan 4, tartalmazza a helyeset
)
```

A kérdés **nem** tárol megjelenítendő szöveget, képet vagy `contentDescription`-t.
A UI a megjelenítéskor oldja fel az azonosítókat a tudástárból.

A válasz kiértékelése ezért mindig:
`selectedTechniqueId == question.correctTechniqueId` – azonosító-összehasonlítás.

## Mérlegelt alternatívák

| Alternatíva | Miért nem |
|---|---|
| A kérdés tárolja a kész opciószövegeket | Ha két technikának azonos a magyar neve vagy az egyik aliasa egyezik a másik nevével, a szöveges összehasonlítás hamis találatot adna – és a felhasználó jogosan tartaná helyesnek a „rossz" választ. Pontosan ezt a hibaosztályt zárja ki a DR-03. |
| Hibrid: ID + előre renderelt szöveg gyorsítótárként | A két forrás kicsúszhat egymásból tartalomfrissítéskor; a mentett kvízállapot elavult szöveget hozna vissza forgatás után. |

## Következmények

- A `QuizPlan` szerializálható a `SavedStateHandle`-be anélkül, hogy a tartalom másolatát
  cipelné (ADR-007) – kicsi és stabil.
- Tartalomfrissítés után egy mentett, félbehagyott kvíz a **friss** szövegekkel jelenik meg;
  az átnevezett technika új neve látszik, a helyesség változatlan.
- A kép-alapú kérdéseknél a UI dönti el a `contentDescription` szövegét, így a
  `JP_TO_IMAGE` opcióinál a leírás nem fedi fel a technika nevét (NFR-04) – ez megjelenítési
  szabály, nem a motor dolga.
- A kvízmotor így teljesen szövegfüggetlen, és a tesztjei nem a tartalomtól, hanem a
  struktúrától függnek (TR-02).
