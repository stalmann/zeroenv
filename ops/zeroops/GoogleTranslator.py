from googletrans import Translator

translator = Translator()

#Achtung: must still handle  15k limit of google api for tranlations!

def translateToEn(text):
    return translate(text, 'en')

def translate(text, destLang):
    translated = translator.translate(text, dest=destLang)
    return translated.text
