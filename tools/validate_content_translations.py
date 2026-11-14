"""Fordításfedettség, CSV/JSON-egyezés és a régi konverter kompatibilitásának ellenőrzése."""
from pathlib import Path
import csv, json, subprocess, importlib.util, hashlib

root=Path(__file__).resolve().parents[1]
cp=root/'knowledgebase/kyokushin_knowledge_hu.csv'
jp=root/'knowledgebase/kyokushin_knowledge_hu.json'
rows=list(csv.DictReader(cp.open(encoding='utf-8-sig')))
data=json.loads(jp.read_text(encoding='utf-8'))
entries={e['id']:e for e in data['entries']}
assert len(rows)==len(entries)==161
fields=('english_name','german_name','description_en','description_de','category_en','category_de')
for r in rows:
    assert all(r[f].strip() and r[f]==entries[r['id']][f] for f in fields),r['id']
    assert r['translation_review_status']==entries[r['id']]['translation_review_status']=='draft_review_required'
for c in data['categories']:assert c['name_en'].strip() and c['name_de'].strip()

# A HEAD régi mezőit mind megtartottuk, nem csupán a magyar neveket.
old=json.loads(subprocess.check_output(['git','show','HEAD:knowledgebase/kyokushin_knowledge_hu.json']).decode('utf-8'))
for e in old['entries']:
    assert all(entries[e['id']][k]==v for k,v in e.items() if k not in fields and not k.startswith('translation_'))
old_csv=list(csv.DictReader(subprocess.check_output(['git','show','HEAD:knowledgebase/kyokushin_knowledge_hu.csv']).decode('utf-8-sig').splitlines()))
by_id={r['id']:r for r in rows}
for r in old_csv:
    assert all(by_id[r['id']][k]==v for k,v in r.items() if k not in fields and not k.startswith('translation_'))
assert data['dataset_version']==old['dataset_version']

# A meglévő konvertert csak memóriában hívjuk; nem írja felül az app assetjeit.
spec=importlib.util.spec_from_file_location('converter',root/'knowledgebase_transfer/tools/convert.py')
converter=importlib.util.module_from_spec(spec);spec.loader.exec_module(converter)
report=converter.Report()
assert converter.cross_check_json(rows,data,report)==(0,0,0)
categories=converter.build_categories(rows,data,report)
illustrations=converter.build_illustrations(converter.read_pics_manifest(),report)
converted=converter.build_entries(rows,categories,illustrations,report)
baseline=converter.build_entries(old_csv,converter.build_categories(old_csv,old,converter.Report()),illustrations,converter.Report())
assert converted==baseline,'A HU konverter kimenete megváltozott!'
assert len(converted[0])==161 and not report.blocking
print('PASS: 161/161 EN+DE names and descriptions; 9/9 categories; CSV/JSON match; all original fields preserved.')
print('PASS: existing converter cross-check (0,0,0); HU conversion output unchanged; 0 blocking issues.')
