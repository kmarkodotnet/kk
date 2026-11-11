#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Kyokushin tudástár konverter: `knowledgebase/` -> `knowledgebase_transfer/`

Bemenet (a tartalom forrása, kézzel szerkesztett):
    knowledgebase/kyokushin_knowledge_hu.csv     <- a hiteles tudástár (161 rekord)
    knowledgebase/kyokushin_knowledge_hu.json    <- ugyanaz JSON-ban (keresztellenőrzésre)
    knowledgebase/pics/manifest.json             <- az illusztrációk nyilvántartása
    knowledgebase/pics/*.jpg                     <- az illusztrációk

Kimenet (az app bemenete, ADR-011 kanonikus séma):
    knowledgebase_transfer/manifest.json
    knowledgebase_transfer/entries.json
    knowledgebase_transfer/illustrations/*.jpg
    knowledgebase_transfer/VALIDATION_REPORT.md

A konverter SOHA nem emel hitelesítési státuszt és nem talál ki tartalmat
(CR-01, CR-02). Csak leképez, normalizál, ellenőriz és jelöl.

Futtatás a projekt gyökeréből:
    python knowledgebase_transfer/tools/convert.py
"""

from __future__ import annotations

import csv
import hashlib
import io
import json
import os
import re
import shutil
import sys
import unicodedata
from collections import Counter, defaultdict, OrderedDict
from datetime import date

# --------------------------------------------------------------------------
# Konfiguráció
# --------------------------------------------------------------------------

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SRC_DIR = os.path.join(ROOT, "knowledgebase")
OUT_DIR = os.path.join(ROOT, "knowledgebase_transfer")

SRC_CSV = os.path.join(SRC_DIR, "kyokushin_knowledge_hu.csv")
SRC_JSON = os.path.join(SRC_DIR, "kyokushin_knowledge_hu.json")
SRC_PICS = os.path.join(SRC_DIR, "pics")
SRC_PICS_MANIFEST = os.path.join(SRC_PICS, "manifest.json")

OUT_MANIFEST = os.path.join(OUT_DIR, "manifest.json")
OUT_ENTRIES = os.path.join(OUT_DIR, "entries.json")
OUT_ILLUSTRATIONS = os.path.join(OUT_DIR, "illustrations")
OUT_REPORT = os.path.join(OUT_DIR, "VALIDATION_REPORT.md")

SCHEMA_VERSION = "1.0.0"

# A kategóriák megjelenítési sorrendje. A sorrend a forrás JSON `categories`
# tömbjének sorrendje; ez a tartalom döntése, nem a kódé (DR-02).
# Ha a forrásban új kategória jelenik meg, a végére kerül, és a riport jelzi.

# --------------------------------------------------------------------------
# KÉZZEL ELLENŐRZÖTT megállapítás az illusztrációkról
# --------------------------------------------------------------------------
# 2026-11-11: mind a 10 kép vizuálisan átnézve. Az alábbi képekbe BELE VAN ÉGETVE
# a technika azonosítója, japán neve és magyar neve. Az ilyen kép:
#   - kvízben NEM használható (felfedi a helyes választ: FR-14, CR-07 szelleme),
#   - tanulókártyán felfedés ELŐTT nem mutatható (FR-07 elrejtés).
# Ez a lista EMBERI megfigyelés eredménye, nem automatikus detektálás.
# Új kép hozzáadásakor a listát felül kell vizsgálni (lásd README.md).
IMAGES_WITH_EMBEDDED_LABEL = {
    "KY-0045",  # "KY-0045 / Heisoku dachi / Zárt lábú állás"
    "KY-0050",  # "KY-0050 / Zenkutsu dachi / Elülső súlypontú hosszú állás"
    "KY-0052",  # "KY-0052 / Kiba dachi / Lovaglóállás"
}

# Az illusztrációk eredete. A pics/manifest.json és a README.txt szerint ezek
# generált, szakmailag NEM ellenőrzött oktatási illusztrációk (CR-01, CR-04).
ILLUSTRATION_PROVENANCE = {
    "origin": "ai_generated",
    "review_status": "human_review_required",
    "license": "project-internal",
    "source": "knowledgebase/pics (a projekt saját, generált készlete)",
    "author": None,
    "warning": (
        "AI által generált illusztráció, szakmailag nem ellenőrzött. "
        "A testhelyzet, a végtagok szöge és a mozgásirány ellenőrzésre vár."
    ),
}

ID_PATTERN = re.compile(r"^KY-\d{4}$")

# --------------------------------------------------------------------------
# Segédfüggvények
# --------------------------------------------------------------------------


def normalize_search(text: str) -> str:
    """Kereséshez normalizált alak: kisbetű, ékezet és írásjel nélkül (FR-03)."""
    if not text:
        return ""
    decomposed = unicodedata.normalize("NFD", text.lower())
    stripped = "".join(ch for ch in decomposed if unicodedata.category(ch) != "Mn")
    collapsed = re.sub(r"[^a-z0-9]+", " ", stripped)
    return collapsed.strip()


def sha256_of(path: str) -> str:
    h = hashlib.sha256()
    with open(path, "rb") as fh:
        for chunk in iter(lambda: fh.read(65536), b""):
            h.update(chunk)
    return h.hexdigest()


def image_size(path: str):
    """Kép méretének kiolvasása Pillow nélkül is (JPEG SOF marker)."""
    try:
        from PIL import Image  # type: ignore

        with Image.open(path) as im:
            return im.size
    except Exception:
        pass
    try:
        with open(path, "rb") as fh:
            data = fh.read()
        i = 2
        while i < len(data) - 9:
            if data[i] != 0xFF:
                i += 1
                continue
            marker = data[i + 1]
            if marker in (0xC0, 0xC1, 0xC2, 0xC3):
                h = int.from_bytes(data[i + 5 : i + 7], "big")
                w = int.from_bytes(data[i + 7 : i + 9], "big")
                return (w, h)
            seg_len = int.from_bytes(data[i + 2 : i + 4], "big")
            i += 2 + seg_len
    except Exception:
        pass
    return (None, None)


class Report:
    """Validációs megállapítások gyűjtője. A konverter nem dob kivételt
    tartalmi hibára: jelent, és a hibás rekordot megjelöli."""

    def __init__(self):
        self.issues = []  # (code, severity, entry_id, field, detail)

    def add(self, code, severity, entry_id, field, detail):
        self.issues.append((code, severity, entry_id, field, detail))

    def by_severity(self, severity):
        return [i for i in self.issues if i[1] == severity]

    @property
    def blocking(self):
        return self.by_severity("BLOKKOLÓ")

    @property
    def warnings(self):
        return self.by_severity("FIGYELM.")


# --------------------------------------------------------------------------
# Beolvasás
# --------------------------------------------------------------------------


def read_csv_rows():
    with io.open(SRC_CSV, encoding="utf-8-sig", newline="") as fh:
        return list(csv.DictReader(fh))


def read_source_json():
    with io.open(SRC_JSON, encoding="utf-8") as fh:
        return json.load(fh)


def read_pics_manifest():
    if not os.path.exists(SRC_PICS_MANIFEST):
        return {"images": []}
    with io.open(SRC_PICS_MANIFEST, encoding="utf-8") as fh:
        return json.load(fh)


# --------------------------------------------------------------------------
# Konverzió
# --------------------------------------------------------------------------


def build_categories(rows, source_json, report):
    """A kategórialista a forrás JSON sorrendjét követi; a CSV-ben megjelenő,
    de a JSON-ban hiányzó kategória a végére kerül és jelzést kap."""
    json_cats = OrderedDict()
    for c in source_json.get("categories", []):
        json_cats[c["id"]] = c.get("name_hu", c["id"])

    csv_cats = OrderedDict()
    for r in rows:
        cid = (r.get("category_id") or "").strip()
        if cid and cid not in csv_cats:
            csv_cats[cid] = (r.get("category_hu") or "").strip()

    ordered = []
    for i, (cid, name) in enumerate(json_cats.items(), start=1):
        csv_name = csv_cats.get(cid)
        if csv_name and csv_name != name:
            report.add(
                "C002", "FIGYELM.", None, "category_hu",
                f"'{cid}': a CSV neve ('{csv_name}') eltér a JSON nevétől ('{name}'). A JSON neve lett használva.",
            )
        ordered.append({"id": cid, "name_hu": name, "sort_order": i})

    extra = [cid for cid in csv_cats if cid not in json_cats]
    for j, cid in enumerate(extra, start=len(ordered) + 1):
        report.add(
            "C001", "FIGYELM.", None, "category_id",
            f"'{cid}' csak a CSV-ben szerepel, a JSON kategórialistájában nem; a lista végére került.",
        )
        ordered.append({"id": cid, "name_hu": csv_cats[cid] or cid, "sort_order": j})

    return ordered


def build_illustrations(pics_manifest, report):
    """Illusztráció-nyilvántartás: minden bejegyzés hordozza a licencet, az
    eredetet, a review státuszt és a kvízhasználhatóságot (CR-04, CR-05)."""
    illustrations = []
    seen_hashes = {}

    manifest_by_id = {im["id"]: im for im in pics_manifest.get("images", [])}
    actual_files = {}
    if os.path.isdir(SRC_PICS):
        for fn in sorted(os.listdir(SRC_PICS)):
            if fn.lower().endswith((".jpg", ".jpeg", ".png", ".webp")):
                actual_files[os.path.splitext(fn)[0]] = fn

    for entry_id in sorted(set(manifest_by_id) | set(actual_files)):
        fn = actual_files.get(entry_id)
        if fn is None:
            report.add(
                "I001", "BLOKKOLÓ", entry_id, "illustration",
                f"A pics/manifest.json hivatkozik rá, de a képfájl hiányzik: {manifest_by_id[entry_id].get('filename')}",
            )
            continue
        if entry_id not in manifest_by_id:
            report.add(
                "I002", "FIGYELM.", entry_id, "illustration",
                f"A '{fn}' fájl nincs benne a pics/manifest.json-ban; licenc- és review-adat nélkül került át.",
            )

        path = os.path.join(SRC_PICS, fn)
        digest = sha256_of(path)
        if digest in seen_hashes:
            report.add(
                "I003", "FIGYELM.", entry_id, "illustration",
                f"Bitre azonos kép mint '{seen_hashes[digest]}' – a (D) kvíztípusban nem lehetnek egy kérdés opciói.",
            )
        else:
            seen_hashes[digest] = entry_id

        width, height = image_size(path)
        has_label = entry_id in IMAGES_WITH_EMBEDDED_LABEL
        if has_label:
            report.add(
                "I004", "FIGYELM.", entry_id, "illustration",
                "A képbe bele van égetve a technika neve: kvízben nem használható, "
                "és tanulókártyán felfedés előtt nem mutatható.",
            )

        illustrations.append(
            {
                "ref": entry_id,
                "file": "illustrations/" + fn,
                "content_type": "image/jpeg" if fn.lower().endswith((".jpg", ".jpeg")) else "image/" + fn.rsplit(".", 1)[1].lower(),
                "width": width,
                "height": height,
                "sha256": digest,
                "origin": ILLUSTRATION_PROVENANCE["origin"],
                "license": ILLUSTRATION_PROVENANCE["license"],
                "source": ILLUSTRATION_PROVENANCE["source"],
                "author": ILLUSTRATION_PROVENANCE["author"],
                "review_status": ILLUSTRATION_PROVENANCE["review_status"],
                # A két kulcsfontosságú megjelenítési kapu:
                "has_embedded_label": has_label,
                "quiz_usable": not has_label,
                "hideable_on_study_card": not has_label,
            }
        )

    return illustrations


def build_entries(rows, categories, illustrations, report):
    category_ids = {c["id"] for c in categories}
    illustration_refs = {i["ref"] for i in illustrations}

    entries = []
    seen_ids = set()
    by_norm_hu = defaultdict(list)
    by_norm_romaji = defaultdict(list)

    for row in rows:
        eid = (row.get("id") or "").strip()
        if not eid:
            report.add("V002", "BLOKKOLÓ", None, "id", "Üres azonosító – a rekord elvetve.")
            continue
        if eid in seen_ids:
            report.add("V003", "BLOKKOLÓ", eid, "id", "Duplikált azonosító – a későbbi rekord elvetve.")
            continue
        if not ID_PATTERN.match(eid):
            report.add("V003b", "FIGYELM.", eid, "id", "Az azonosító nem a 'KY-NNNN' formát követi.")
        seen_ids.add(eid)

        cid = (row.get("category_id") or "").strip()
        romaji = (row.get("japanese_romaji") or "").strip()
        romaji_ascii = (row.get("japanese_romaji_ascii") or "").strip()
        hungarian = (row.get("hungarian_name") or "").strip()
        description = (row.get("description_hu") or "").strip()
        review_status = (row.get("review_status") or "").strip() or "editorial_review_required"

        incomplete_fields = []
        if not cid:
            incomplete_fields.append("category_id")
        elif cid not in category_ids:
            report.add("V004", "BLOKKOLÓ", eid, "category_id", f"Ismeretlen kategória: '{cid}'.")
            incomplete_fields.append("category_id")
        if not romaji:
            incomplete_fields.append("japanese_romaji")
        if not hungarian:
            incomplete_fields.append("hungarian_name")
        if incomplete_fields:
            report.add(
                "V002", "BLOKKOLÓ", eid, ",".join(incomplete_fields),
                "Kötelező mező hiányzik – a rekord INCOMPLETE, kvízből kizárva.",
            )

        # Az ékezetmentes keresési alakot a forrás adja; ha hiányzik, előállítjuk.
        if not romaji_ascii and romaji:
            romaji_ascii = normalize_search(romaji)
            report.add("V009", "FIGYELM.", eid, "japanese_romaji_ascii", "Hiányzott, a romajiból származtatva.")

        illustration_ref = eid if eid in illustration_refs else None

        entry = {
            "id": eid,
            "category_id": cid,
            "japanese_romaji": romaji,
            "japanese_romaji_ascii": romaji_ascii,
            "japanese_script": None,
            "hungarian_name": hungarian,
            "description_hu": description or None,
            "aliases": [],
            "tags": [],
            "illustration_ref": illustration_ref,
            "review_status": review_status,
            "quiz_excluded": False,
            "retired": False,
            # Származtatott, kereséshez előre normalizált mezők (FR-03).
            "search_romaji": normalize_search(romaji),
            "search_hungarian": normalize_search(hungarian),
            # A konverter megállapítása; az app validátora újraszámolja.
            "incomplete": bool(incomplete_fields),
        }
        entries.append(entry)

        if hungarian:
            by_norm_hu[normalize_search(hungarian)].append(eid)
        if romaji:
            by_norm_romaji[normalize_search(romaji)].append(eid)

    # Szinonima-/ütközéscsoportok: ezek kölcsönösen NEM lehetnek distraktorok (CR-07, V006).
    synonym_groups = []
    for norm, ids in list(by_norm_hu.items()) + list(by_norm_romaji.items()):
        if len(ids) > 1:
            group = sorted(set(ids))
            if group not in synonym_groups:
                synonym_groups.append(group)
                report.add(
                    "V006", "FIGYELM.", None, "hungarian_name/japanese_romaji",
                    f"Azonos normalizált alak ('{norm}') több rekordnál: {', '.join(group)}. "
                    "Ezek egy kérdésben nem lehetnek egymás válaszlehetőségei.",
                )

    return entries, synonym_groups


def cross_check_json(rows, source_json, report):
    """A CSV a szerkesztési forrás; a JSON-nak egyeznie kell vele (ADR-011)."""
    json_entries = {e["id"]: e for e in source_json.get("entries", [])}
    csv_ids = {(r.get("id") or "").strip() for r in rows}

    only_csv = csv_ids - set(json_entries)
    only_json = set(json_entries) - csv_ids
    for eid in sorted(only_csv):
        report.add("X001", "FIGYELM.", eid, None, "Csak a CSV-ben szerepel, a forrás JSON-ban nem.")
    for eid in sorted(only_json):
        report.add("X002", "FIGYELM.", eid, None, "Csak a forrás JSON-ban szerepel, a CSV-ben nem.")

    diffs = 0
    for r in rows:
        eid = (r.get("id") or "").strip()
        j = json_entries.get(eid)
        if not j:
            continue
        for csv_key, json_key in (
            ("japanese_romaji", "japanese_romaji"),
            ("hungarian_name", "hungarian_name"),
            ("category_id", "category_id"),
            ("description_hu", "description_hu"),
        ):
            a = (r.get(csv_key) or "").strip()
            b = (j.get(json_key) or "") or ""
            b = b.strip() if isinstance(b, str) else ""
            if a != b:
                diffs += 1
                report.add(
                    "X003", "FIGYELM.", eid, csv_key,
                    f"CSV és JSON eltér: CSV='{a}' vs JSON='{b}'. A CSV lett a hiteles.",
                )
    return len(only_csv), len(only_json), diffs


# --------------------------------------------------------------------------
# Kiírás
# --------------------------------------------------------------------------


def copy_illustrations(illustrations):
    os.makedirs(OUT_ILLUSTRATIONS, exist_ok=True)
    copied = 0
    for ill in illustrations:
        src = os.path.join(SRC_PICS, os.path.basename(ill["file"]))
        dst = os.path.join(OUT_ILLUSTRATIONS, os.path.basename(ill["file"]))
        shutil.copy2(src, dst)
        copied += 1
    return copied


def write_json(path, payload):
    with io.open(path, "w", encoding="utf-8", newline="\n") as fh:
        json.dump(payload, fh, ensure_ascii=False, indent=2)
        fh.write("\n")


def write_report(report, stats):
    lines = []
    lines.append("# Konverziós és validációs jelentés\n")
    lines.append(f"**Generálva:** {stats['generated_at']} · "
                 f"`python knowledgebase_transfer/tools/convert.py`\n")
    lines.append("Ez a fájl **generált** – ne szerkeszd kézzel. A tartalom forrása a "
                 "`knowledgebase/` mappa.\n")

    lines.append("\n## Összegzés\n")
    lines.append("| Mutató | Érték |")
    lines.append("|---|---|")
    lines.append(f"| Beolvasott CSV sor | {stats['csv_rows']} |")
    lines.append(f"| Kiírt rekord | {stats['entries']} |")
    lines.append(f"| Ebből hiányos (INCOMPLETE) | {stats['incomplete']} |")
    lines.append(f"| Kategória | {stats['categories']} |")
    lines.append(f"| Illusztráció összesen | {stats['illustrations']} |")
    lines.append(f"| Ebből kvízben használható | {stats['quiz_usable_images']} |")
    lines.append(f"| Ebből névfelirattal (kvízből kizárva) | {stats['labelled_images']} |")
    lines.append(f"| Szinonima-/ütközéscsoport | {stats['synonym_groups']} |")
    lines.append(f"| BLOKKOLÓ megállapítás | {len(report.blocking)} |")
    lines.append(f"| FIGYELMEZTETÉS | {len(report.warnings)} |")

    lines.append("\n## Kategóriánkénti kvízkészenlét\n")
    lines.append("Egy kérdéstípus akkor indítható egy kategóriában, ha legalább **4** "
                 "alkalmas jelölt van (1 helyes + 3 distraktor).\n")
    lines.append("| Kategória | Rekord | Szöveges kvízre alkalmas | (A)/(B) indítható | Kép | Képes kvízre alkalmas | (C)/(D) indítható |")
    lines.append("|---|---|---|---|---|---|---|")
    for row in stats["category_readiness"]:
        lines.append(
            "| {name} (`{id}`) | {total} | {text_ok} | {ab} | {images} | {img_ok} | {cd} |".format(**row)
        )

    lines.append("\n## Megállapítások\n")
    if not report.issues:
        lines.append("Nincs megállapítás.\n")
    else:
        lines.append("| Kód | Súlyosság | Rekord | Mező | Leírás |")
        lines.append("|---|---|---|---|---|")
        for code, sev, eid, field, detail in report.issues:
            lines.append(f"| `{code}` | {sev} | {eid or '—'} | {field or '—'} | {detail} |")

    lines.append("\n## Kódjegyzék\n")
    lines.append("| Kód | Jelentés |")
    lines.append("|---|---|")
    for code, meaning in [
        ("V002", "kötelező mező hiányzik -> a rekord INCOMPLETE, kvízből kizárva"),
        ("V003", "duplikált azonosító -> a későbbi rekord elvetve"),
        ("V003b", "az azonosító nem a KY-NNNN formát követi"),
        ("V004", "ismeretlen kategóriaazonosító"),
        ("V006", "azonos normalizált név -> kölcsönös distraktor-kizárás"),
        ("V009", "hiányzó ékezetmentes alak -> származtatva"),
        ("I001", "a manifest hivatkozik rá, de a képfájl hiányzik"),
        ("I002", "képfájl manifest-bejegyzés nélkül"),
        ("I003", "bitre azonos kép két rekordnál"),
        ("I004", "a képbe bele van égetve a technika neve"),
        ("C001", "kategória csak a CSV-ben"),
        ("C002", "kategórianév eltér CSV és JSON között"),
        ("X001", "rekord csak a CSV-ben"),
        ("X002", "rekord csak a forrás JSON-ban"),
        ("X003", "CSV és JSON mezőérték eltér"),
    ]:
        lines.append(f"| `{code}` | {meaning} |")

    with io.open(OUT_REPORT, "w", encoding="utf-8", newline="\n") as fh:
        fh.write("\n".join(lines) + "\n")


def category_readiness(entries, illustrations, categories):
    ill_by_ref = {i["ref"]: i for i in illustrations}
    rows = []
    for cat in categories:
        cid = cat["id"]
        in_cat = [e for e in entries if e["category_id"] == cid]
        text_ok = [
            e for e in in_cat
            if not e["incomplete"] and not e["quiz_excluded"] and not e["retired"]
            and e["japanese_romaji"] and e["hungarian_name"]
        ]
        with_image = [e for e in in_cat if e["illustration_ref"]]
        img_ok = [
            e for e in text_ok
            if e["illustration_ref"] and ill_by_ref.get(e["illustration_ref"], {}).get("quiz_usable")
        ]
        rows.append({
            "id": cid,
            "name": cat["name_hu"],
            "total": len(in_cat),
            "text_ok": len(text_ok),
            "ab": "igen" if len(text_ok) >= 4 else "**nem**",
            "images": len(with_image),
            "img_ok": len(img_ok),
            "cd": "igen" if len(img_ok) >= 4 else "**nem**",
        })
    return rows


def main():
    if not os.path.exists(SRC_CSV):
        print(f"HIBA: nincs meg a forrás CSV: {SRC_CSV}", file=sys.stderr)
        return 2

    report = Report()
    rows = read_csv_rows()
    source_json = read_source_json()
    pics_manifest = read_pics_manifest()

    categories = build_categories(rows, source_json, report)
    illustrations = build_illustrations(pics_manifest, report)
    entries, synonym_groups = build_entries(rows, categories, illustrations, report)
    cross_check_json(rows, source_json, report)

    os.makedirs(OUT_DIR, exist_ok=True)
    copied = copy_illustrations(illustrations)

    generated_at = date.today().isoformat()
    dataset_version = source_json.get("dataset_version", "0.1.0")

    manifest = {
        "schema_version": SCHEMA_VERSION,
        "dataset_version": dataset_version,
        "generated_at": generated_at,
        "language": source_json.get("language", "hu-HU"),
        "martial_art": source_json.get("martial_art", "Kyokushin karate"),
        # ADR-010: a bizalmi szint a TARTALOMBÓL jön, nem felhasználói beállításból.
        "editorial_status": source_json.get("editorial_status", "draft_review_required"),
        "dataset_trust": "practice_on_unreviewed",
        "source": {
            "csv": "knowledgebase/kyokushin_knowledge_hu.csv",
            "json": "knowledgebase/kyokushin_knowledge_hu.json",
            "images": "knowledgebase/pics",
            "converter": "knowledgebase_transfer/tools/convert.py",
        },
        "notes": list(source_json.get("notes", [])) + [
            "Ez a mappa GENERÁLT. A tartalmat a knowledgebase/ mappában kell szerkeszteni, "
            "majd a konvertert újra kell futtatni.",
            "Az illusztrációk AI által generáltak és szakmailag nem ellenőrzöttek; "
            "a testhelyzet és a mozgásirány felülvizsgálatra vár.",
            "A névfelirattal ellátott képek kvízben nem használhatók, mert felfedik a választ.",
        ],
        "categories": categories,
        "illustrations": illustrations,
        "synonym_groups": synonym_groups,
    }

    entries_payload = {
        "schema_version": SCHEMA_VERSION,
        "dataset_version": dataset_version,
        "entries": entries,
    }

    write_json(OUT_MANIFEST, manifest)
    write_json(OUT_ENTRIES, entries_payload)

    stats = {
        "generated_at": generated_at,
        "csv_rows": len(rows),
        "entries": len(entries),
        "incomplete": sum(1 for e in entries if e["incomplete"]),
        "categories": len(categories),
        "illustrations": len(illustrations),
        "quiz_usable_images": sum(1 for i in illustrations if i["quiz_usable"]),
        "labelled_images": sum(1 for i in illustrations if i["has_embedded_label"]),
        "synonym_groups": len(synonym_groups),
        "category_readiness": category_readiness(entries, illustrations, categories),
    }
    write_report(report, stats)

    print(f"Rekord:          {stats['entries']} (hiányos: {stats['incomplete']})")
    print(f"Kategória:       {stats['categories']}")
    print(f"Illusztráció:    {stats['illustrations']} átmásolva ({copied} fájl), "
          f"kvízben használható: {stats['quiz_usable_images']}")
    print(f"Megállapítás:    {len(report.blocking)} BLOKKOLÓ, {len(report.warnings)} figyelmeztetés")
    print(f"Kimenet:         {OUT_DIR}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
