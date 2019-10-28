import spacy
model = 'en_core_web_lg'
print ('using', model)
nlp = spacy.load(model)
def NLP():
    return nlp

