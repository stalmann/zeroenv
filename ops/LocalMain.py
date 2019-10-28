import json
from spacy.pipeline import TextCategorizer
from spacy.symbols import nsubj, VERB, dobj
from spacy_langdetect import LanguageDetector
from spacy import displacy
import textacy
import textacy.ke

from zeroops.GoogleTranslator import translateToEn
from zeroops.SentimentScore import calcSentiment, calcSentimentEN
from zeroops.SpacyLoader import NLP
from zeroops.Files import fread
from zeroops.JsonHelper import entitiesAsList
import spacy
import base64
from zeroops.Extract import summarize


nlp = NLP()
nlp.add_pipe(LanguageDetector(), name="language_detector", last=True)

message = fread("../data/testdoc.json")
print(message)

jsondoc = json.loads(message)
text = jsondoc["document"]

doc = nlp(text)
if doc._.language["language"] != "en":
    jsondoc["language"] = doc._.language["language"]
    text = translateToEn(text)
    jsondoc["document.en"] = text
    doc = nlp(text)
jsondoc["entitites.markup.b64"] = str(base64.b64encode(displacy.render(doc,  style="ent").encode("utf-8")))

jsondoc["sentiment"] = calcSentimentEN(text)

jsondoc["entitites"] = entitiesAsList(doc)

keywords = textacy.ke.yake(doc,  normalize="lemma", ngrams=(1, 2, 3), include_pos=("NOUN", "PROPN", "ADJ"), window_size=2, topn=0.15)
jsondoc["keywords"] = sorted(keywords, key=lambda x: x[1], reverse=True)

jsondoc["summary"] = summarize(text, 6)

print(json.dumps(jsondoc, indent=4, separators=(",", ": ")))
print("\n")

