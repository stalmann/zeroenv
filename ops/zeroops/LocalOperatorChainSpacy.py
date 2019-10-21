import json

def processDocument(jsonInput):
    jsonObject = json.loads(jsonInput)
    text = jsonObject["document"]
    text = 'Hallo Welt'
    jsonObject["document"] = text

    return json.dumps(jsonObject)