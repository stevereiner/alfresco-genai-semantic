import json
import spacy
from fastapi import UploadFile
from PyPDF2 import PdfReader
import os
from dotenv import load_dotenv

load_dotenv(".env")

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

    nlp.add_pipe("entityLinker", last=True)

    doc = nlp(text)

    labels = []
    links = []
    type_lists = []
    separator = ","

    for ent in doc._.linkedEntities:
        labels.append(ent.get_label())
        links.append(ent.get_url())
        
        super_list = ""
        super_entities =  ent.get_super_entities()

        for  i, super_ent in enumerate(super_entities):
            super_list += "Wikidata:Q" + str(super_ent.get_id())  
            if i < len(super_entities) - 1:
                super_list += separator
                
        type_lists.append(super_list)

    return {"labels": json.dumps(labels), "links": json.dumps(links), "type_lists": json.dumps(type_lists)}


def getEntityLinksDBpedia(file: UploadFile):

    dbpedia_spotlight_url  = os.getenv("DBPEDIA_SPOTLIGHT_URL")

    #print(dbpedia_spotlight_url)    
    
    pdf_reader = PdfReader(file.file)
    text = ""
    for page in pdf_reader.pages:
        text += page.extract_text()

    # load your model as usual
    nlp = spacy.load('en_core_web_trf')

    # add the pipeline stage
    # nlp.add_pipe('dbpedia_spotlight')
    # use local dbpedia spotlight server
    nlp.add_pipe('dbpedia_spotlight', config={'dbpedia_rest_endpoint':   dbpedia_spotlight_url + '/rest'})
    
    # get the document
    doc = nlp(text)

    labels = []
    links = []
    type_lists = []
    
    for ent in doc.ents:
        labels.append(ent.text)
        links.append(ent.kb_id_)
        type_lists.append(ent._.dbpedia_raw_result['@types'])

    return {"labels": json.dumps(labels), "links": json.dumps(links), "type_lists": json.dumps(type_lists)}
