import json


def entitiesAsList(doc):
    entList = []
    for ent in doc.ents:
        e = {"text": ent.text, "start_char": ent.start_char, "end_char": ent.end_char, "label": ent.label_}
        entList.append(e)
    return entList
