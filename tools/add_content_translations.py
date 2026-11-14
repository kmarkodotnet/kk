"""Lektorálandó EN/DE fordítások hozzáadása a szerkesztési CSV/JSON magyar mezői mellé.

Az app csomagolt adatait, importját és a magyar alapmezőket nem módosítja.
"""
from pathlib import Path
import csv, io, json, hashlib
from datetime import date

ROOT=Path(__file__).resolve().parents[1]
CSV=ROOT/'knowledgebase/kyokushin_knowledge_hu.csv'
JSON=ROOT/'knowledgebase/kyokushin_knowledge_hu.json'
REGISTRY=ROOT/'knowledgebase/translations_en_de.tsv'
CATEGORIES={
 'etiquette':('Etiquette and greetings','Etikette und Begrüßungen'),
 'commands':('Commands and basic concepts','Kommandos und Grundbegriffe'),
 'stances':('Stances','Stellungen'),
 'blocks':('Blocks','Abwehrtechniken'),
 'punches':('Hand and elbow techniques','Hand- und Ellenbogentechniken'),
 'kicks':('Kicks and knee techniques','Fuß- und Knietechniken'),
 'body_parts':('Striking surfaces and body parts','Trefferflächen und Körperteile'),
 'numbers':('Japanese numbers','Japanische Zahlen'),
 'kata':('Kata','Kata'),
}
FIELDS=('english_name','german_name','description_en','description_de')

def main():
    original_csv,original_json=CSV.read_bytes(),JSON.read_bytes()
    reader=csv.DictReader(io.StringIO(original_csv.decode('utf-8-sig')))
    columns=list(reader.fieldnames);rows=list(reader)
    data=json.loads(original_json.decode('utf-8-sig'))
    # Az első, kézzel írt fordítási lista pipe-jait valódi TSV-re normalizáljuk.
    text=REGISTRY.read_text(encoding='utf-8')
    if text.startswith('id|'):text=text.replace('|','\t')
    translations=list(csv.DictReader(io.StringIO(text),delimiter='\t'))
    by_id={r['id']:r for r in translations}
    originals={r['id']:dict(r) for r in rows}
    csv_live={r['id']:r for r in rows}
    json_originals={r['id']:dict(r) for r in data['entries']}
    assert len(rows)==len(data['entries'])==len(by_id)==len(translations)==161
    assert set(originals)==set(json_originals)==set(by_id)
    for r in translations:
        assert set(r)=={'id',*FIELDS} and all(r[f] and r[f].strip()==r[f] for f in FIELDS),r['id']
    audit_path=ROOT/'docs/CONTENT_VERIFICATION.csv'
    audit={r['id']:r for r in csv.DictReader(audit_path.open(encoding='utf-8-sig'))}
    for e in data['entries']:
        row=csv_live[e['id']]
        for f in ('japanese_romaji','hungarian_name','description_hu','category_id'):
            assert row[f]==e[f],(e['id'],f)
        tr=by_id[e['id']]
        extras={f:tr[f] for f in FIELDS}
        extras.update(category_en=CATEGORIES[e['category_id']][0],category_de=CATEGORIES[e['category_id']][1],translation_review_status='draft_review_required')
        a=audit[e['id']]
        extras['translation_notes_hu']=('T-708: '+a['status']+' – '+a['eszrevetel']) if a['status']!='MEGFELELTETHETŐ' else ''
        if e['id'] in ('KY-0081','KY-0082','KY-0083','KY-0084','KY-0119'):
            extras['translation_notes_hu']+=' Az EN/DE a romaji technikához illő ökölhátat nevezi meg; a magyar kézhát javítását külön kell jóváhagyni.'
        if e['id']=='KY-0042':
            extras['translation_notes_hu']+=' Az EN junior és DE rangjünger rangidősséget, nem életkort jelöl.'
        row.update(extras);e.update(extras)
        ordered={}
        adjacent={'hungarian_name':('english_name','german_name'),'description_hu':('description_en','description_de'),'category_id':('category_en','category_de')}
        for k,v in e.items():
            if k not in extras:ordered[k]=v
            for extra in adjacent.get(k,()):ordered[extra]=extras[extra]
        for k in ('translation_review_status','translation_notes_hu'):ordered[k]=extras[k]
        e.clear();e.update(ordered)
    for c in data['categories']:
        c['name_en'],c['name_de']=CATEGORIES[c['id']]
    data['translation_metadata']={
      'source_language':'hu','target_languages':['en','de'],
      'review_status':'draft_review_required','method':'assistant_translation_with_terminology_cross_checks',
      'date':str(date.today()),
      'scope':'Names, descriptions and category labels; not application UI',
      'terminology_references':[
        'https://www.ifknorge.org/wp-content/uploads/2024/03/syllabus_hoyopp.pdf',
        'https://kyokushin-muenchen.de/karate-begriffe',
        'https://www.kyokushinkai.de/html/umfang/karate/karateLexikon.html'],
      'note_hu':'Saját, lektorálandó fordítás. A webes szószedetek terminológiai támpontok, nem minden mondat forrásai. A T-708 bizonytalanságai külön jelölve. A magyar szöveg és a tartalom hitelesítési státusza változatlan.'}
    for anchor,new in [('category_hu',['category_en','category_de']),('hungarian_name',['english_name','german_name']),('description_hu',['description_en','description_de'])]:
        for f in new:
            if f in columns:columns.remove(f)
        pos=columns.index(anchor)+1;columns[pos:pos]=new
    for f in ('translation_review_status','translation_notes_hu'):
        if f not in columns:columns.append(f)
    output=io.StringIO(newline='');writer=csv.DictWriter(output,fieldnames=columns);writer.writeheader();writer.writerows(rows)
    # Írás előtt a párhuzamos szerkesztés észlelése; csak saját fordítási mezők újítása.
    assert CSV.read_bytes()==original_csv and JSON.read_bytes()==original_json,'A forrás közben változott; újraolvasás szükséges.'
    for e in data['entries']:
        for k,v in json_originals[e['id']].items():
            if k not in {*FIELDS,'category_en','category_de','translation_review_status','translation_notes_hu'}:assert e[k]==v
        assert all(csv_live[e['id']][f]==e[f] and e[f].strip() for f in FIELDS)
        assert all(csv_live[e['id']][k]==v for k,v in originals[e['id']].items() if k not in {*FIELDS,'category_en','category_de','translation_review_status','translation_notes_hu'})
    CSV.write_text(output.getvalue(),encoding='utf-8',newline='')
    JSON.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    REGISTRY.write_text(text,encoding='utf-8')
    print('161 records x 2 names + 2 descriptions = 644 translated fields; 9 x 2 category labels; HU/romaji/status preserved.')
    print('Stored side by side in',CSV.relative_to(ROOT),'and',JSON.relative_to(ROOT))

if __name__=='__main__':main()
