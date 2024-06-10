import json
import spacy
from fastapi import UploadFile
from PyPDF2 import PdfReader

test_text = '''The International Space Station (ISS) is an awe-inspiring feat 
of human engineering and collaboration. Orbiting the Earth at an 
altitude of approximately 408 kilometers, the ISS serves as a research 
laboratory and living space for astronauts from around the world. 
NASA, in partnership with other space agencies such as Roscosmos, 
ESA, JAXA, and CSA, has been instrumental in the construction and 
operation of this remarkable structure. The ISS has facilitated 
groundbreaking scientific experiments and discoveries in various fields, 
including physics, biology, and astronomy. Notable astronauts like 
Chris Hadfield and Scott Kelly have spent significant time aboard 
the ISS, conducting experiments and gathering data to expand our 
knowledge of space exploration. However, with the advancement of 
commercial space travel, companies like SpaceX and Blue Origin are 
aiming to make space more accessible and potentially challenge the 
ISS's monopoly on human space presence.'''


def getEntityLinksWikidata(file: UploadFile):

    pdf_reader = PdfReader(file.file)
    text = ""
    for page in pdf_reader.pages:
        text += page.extract_text()

    nlp = spacy.load('en_core_web_trf')

    nlp.add_pipe('opentapioca')

    doc = nlp(text)

    '''
    #links =[{"text": ent.text, 
            "description": ent._.description,
            "score:": ent._.score,
            "start": ent.start_char,
            "end": ent.end_char,
            "label": ent.label_,
            "kb_id": ent.kb_id_,
            "kb_url": "https://www.wikidata.org/entity/" + ent.kb_id_}
            for ent in doc.ents]

    #links.append(f'<a href="https://www.wikidata.org/entity/{ent.kb_id_}">{ent.text}</a>')            
    '''    
    
    links = []
    for ent in doc.ents:
        links.append(ent.text + " " + "https://www.wikidata.org/entity/" + ent.kb_id_ + " " + ent.label_)

    links_json = json.dumps(links)

    print(links_json)

    return links_json


def getEntityLinksDBpedia(file: UploadFile):

    pdf_reader = PdfReader(file.file)
    text = ""
    for page in pdf_reader.pages:
        text += page.extract_text()

    # load your model as usual
    nlp = spacy.load('en_core_web_trf')

    # add the pipeline stage
    nlp.add_pipe('dbpedia_spotlight')

    # get the document
    doc = nlp(text)

    #links =  [(ent.text, ent.label_, ent.kb_id_, ent._.dbpedia_raw_result) for ent in doc.ents]
    #links.append(f'<a href="{ent.kb_id_}">{ent.text}</a>')

    links = []
    for ent in doc.ents:
        #types = ent._.dbpedia_raw_result['@types']
        links.append(ent.text +  " " + ent.kb_id_ +  " " + ent.label_)

    links_json = json.dumps(links)

    print(links_json)

    return links_json
