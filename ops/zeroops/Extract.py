from zeroops.Files import fread
import numpy as np
import pandas as pd
from zeroops.GoogleTranslator import translateToEn
import nltk
from nltk.tokenize import sent_tokenize
import networkx as nx
from nltk.corpus import stopwords
from sklearn.metrics.pairwise import cosine_similarity
from nltk.tokenize import sent_tokenize
nltk.download('punkt')  # one time execution

# ! wget http://nlp.stanford.edu/data/glove.6B.zip
# ! unzip glove*.zip

stop_words = stopwords.words('english')


def remove_stopwords(sen):
  sen_new = " ".join([i for i in sen if i not in stop_words])
  return sen_new

#expects english text as input!
def summarize(text, max_sentences):
  sentences = sent_tokenize(text)
  # remove punctuations, numbers and special characters
  clean_sentences = pd.Series(sentences).str.replace("[^a-zA-Z]", " ")

  # make alphabets lowercase
  clean_sentences = [s.lower() for s in clean_sentences]

  nltk.download('stopwords')

  clean_sentences = [remove_stopwords(r.split()) for r in clean_sentences]

  word_embeddings = {}
  f = open('../data/glove/glove.6B.100d.txt', encoding='utf-8')
  for line in f:
      values = line.split()
      word = values[0]
      coefs = np.asarray(values[1:], dtype='float32')
      word_embeddings[word] = coefs
  f.close()

  sentence_vectors = []
  for i in clean_sentences:
      if len(i) != 0:
          v = sum([word_embeddings.get(w, np.zeros((100,))) for w in i.split()]) / (len(i.split()) + 0.001)
      else:
          v = np.zeros((100,))
      sentence_vectors.append(v)

  len(sentence_vectors)

  """
  The next step is to find similarities among the sentences. We will use cosine similarity to find similarity between a pair of sentences. Let's create an empty similarity matrix for this task and populate it with cosine similarities of the sentences.
  """
  sim_mat = np.zeros([len(sentences), len(sentences)])

  for i in range(len(sentences)):
      for j in range(len(sentences)):
          if i != j:
              sim_mat[i][j] = cosine_similarity(sentence_vectors[i].reshape(1, 100), sentence_vectors[j].reshape(1, 100))[
                  0, 0]


  nx_graph = nx.from_numpy_array(sim_mat)
  scores = nx.pagerank(nx_graph)

  ranked_sentences = sorted(((scores[i],s) for i,s in enumerate(sentences)), reverse=True)

  sn = len(ranked_sentences)
  if sn > max_sentences:
    sn = max_sentences

  # Generate summary
  summary = []
  for i in range(sn):
    print(ranked_sentences[i][1])
    summary.append(ranked_sentences[i][1])

  return summary

#text = translateToEn(fread("../data/gbrot.txt"))
#print(summarize(text, 3))