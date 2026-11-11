# Dokumentációs index

| Dokumentum | Mit tartalmaz | Állapot |
|---|---|---|
| [REQUIREMENTS.md](REQUIREMENTS.md) | Követelményspecifikáció: FR, NFR, CR, DR, TR, AC azonosítókkal, nyitott kérdésekkel | v1.0 – jóváhagyásra vár |
| [FUNCTIONAL_SPEC.md](FUNCTIONAL_SPEC.md) | Funkcionális specifikáció: képernyők, állapotok, folyamatok, UI szövegek, hibakezelés | v1.0 – jóváhagyásra vár |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Architektúraterv: modultérkép, rétegszabályok, domain modell, Room séma, DI, tesztarchitektúra, ADR index | v1.0 – jóváhagyásra vár |
| [BACKLOG.md](BACKLOG.md) | 6 epik, 21 user story elfogadási kritériumokkal, 71 dev task függőségekkel és Definition of Done-nal | v1.0 |
| [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md) | Fázisok, tényleges állás, blokkolók, következő lépések | élő dokumentum |
| [ENVIRONMENT.md](ENVIRONMENT.md) | A build környezet tényleges állapota és a blokkolók (B-01…B-04) | élő dokumentum |
| [TEST_REPORT.md](TEST_REPORT.md) | **Csak tényleges** build- és tesztfuttatások naplója | append-only |
| [KNOWLEDGE_BASE.md](KNOWLEDGE_BASE.md) | Tudástár séma, bővítés, hitelesítés, licenc, migráció | vázlat (Fázis 3.3) |
| [QUIZ_RULES.md](QUIZ_RULES.md) | Kvíz kérdéstípusok, distraktorszabályok, fallback | vázlat (Fázis 8.5) |
| [adr/](adr/) | Architekturális döntések (ADR-001…) | Fázis 0.9 |

## Normatív fejlesztői szabályok

A napi fejlesztést vezérlő, gépi olvasásra szánt szabályok a Claude környezetben élnek:

| Hely | Mit tartalmaz |
|---|---|
| `../CLAUDE.md` | vasszabályok, dokumentumhierarchia, munkamód |
| `../.claude/skills/kyokushin-content/` | tartalmi integritás: mit nem szabad kitalálni, hitelesítési státuszok |
| `../.claude/skills/kyo-knowledge-base/` | JSON séma, V001…V010 validációs szabályok, import, migráció |
| `../.claude/skills/kyo-quiz-engine/` | kvízmotor szerződése, invariánsai, distraktorszabályai |
| `../.claude/skills/kyo-android-module/` | modulhatárok, Compose/Room/Hilt konvenciók, akadálymentesség |
| `../.claude/skills/kyo-build-env/` | környezet felmérése, telepítés, hiteles jelentés |
| `../.claude/commands/` | `/kyo-status`, `/kyo-next`, `/kyo-setup-env`, `/kyo-build`, `/kyo-test`, `/kyo-validate-kb`, `/kyo-doc-sync` |
| `../.claude/agents/kyo-spec-auditor.md` | csak-olvasó auditor a spec-megfelelés ellenőrzéséhez |

Ha a specifikáció és egy skill eltér, a hierarchia:
mesterprompt → REQUIREMENTS → FUNCTIONAL_SPEC → ARCHITECTURE → skill.
