from zeroops.GoogleTranslator import translateToEn
from vaderSentiment.vaderSentiment import SentimentIntensityAnalyzer

analyser = SentimentIntensityAnalyzer()

def calcSentiment(text):
    return analyser.polarity_scores(translateToEn(text))

def calcSentimentEN(text):
    return analyser.polarity_scores(text)
