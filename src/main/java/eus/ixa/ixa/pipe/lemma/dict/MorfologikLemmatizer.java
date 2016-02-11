/*
 * Copyright 2014 Rodrigo Agerri

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package eus.ixa.ixa.pipe.lemma.dict;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import morfologik.stemming.Dictionary;
import morfologik.stemming.DictionaryLookup;
import morfologik.stemming.IStemmer;
import morfologik.stemming.WordData;
import eus.ixa.ixa.pipe.lemma.Lemmatizer;
import eus.ixa.ixa.pipe.pos.Resources;

/**
 * Lemmatizer based on Morfologik Stemming library. It requires a FSA Morfologik
 * dictionary as input.
 * 
 * @author ragerri
 * @version 2014-07-08
 * 
 */
public class MorfologikLemmatizer implements Lemmatizer {

  /**
   * The Morfologik steamer to perform lemmatization with FSA dictionaries.
   */
  private final IStemmer dictLookup;
  /**
   * The class dealing with loading the default dictionaries.
   */
  private final Resources tagRetriever = new Resources();
  /**
   * The language.
   */
  private final String lang;

  /**
   * Reads a dictionary in morfologik FSA format.
   * 
   * @param dictURL
   *          the URL containing the dictionary
   * @param aLang
   *          the language
   * @throws IllegalArgumentException
   *           if an exception is illegal
   * @throws IOException
   *           throws an exception if dictionary path is not correct
   */
  public MorfologikLemmatizer(final URL dictURL, final String aLang)
      throws IOException {
    this.dictLookup = new DictionaryLookup(Dictionary.read(dictURL));
    this.lang = aLang;
  }

  /**
   * Get the lemma for a surface form word and a postag from a FSA morfologik
   * generated dictionary.
   * 
   * @param word
   *          the surface form
   * @return the hashmap with the word and tag as keys and the lemma as value
   */
  private HashMap<List<String>, String> getLemmaTagsDict(final String word) {
    final List<WordData> wdList = this.dictLookup.lookup(word);
    final HashMap<List<String>, String> dictMap = new HashMap<List<String>, String>();
    for (final WordData wd : wdList) {
      final List<String> wordLemmaTags = new ArrayList<String>();
      wordLemmaTags.add(word);
      wordLemmaTags.add(wd.getTag().toString());
      dictMap.put(wordLemmaTags, wd.getStem().toString());
    }
    return dictMap;
  }

  /**
   * Generate the dictionary keys (word, postag).
   * 
   * @param word
   *          the surface form
   * @param postag
   *          the assigned postag
   * @return a list of keys consisting of the word and its postag
   */
  private List<String> getDictKeys(final String word, final String postag) {
    final List<String> keys = new ArrayList<String>();
    final String constantTag = this.tagRetriever.setTagConstant(this.lang,
        postag);
    if (postag.startsWith(String.valueOf(constantTag))) {
      keys.addAll(Arrays.asList(word, postag));
    } else {
      keys.addAll(Arrays.asList(word.toLowerCase(), postag));
    }
    return keys;
  }

  /**
   * Generates the dictionary map.
   * 
   * @param word
   *          the surface form word
   * @param postag
   *          the postag assigned by the pos tagger
   * @return the hash map dictionary
   */
  private HashMap<List<String>, String> getDictMap(final String word,
      final String postag) {
    HashMap<List<String>, String> dictMap = new HashMap<List<String>, String>();
    final String constantTag = this.tagRetriever.setTagConstant(this.lang,
        postag);
    if (postag.startsWith(String.valueOf(constantTag))) {
      dictMap = this.getLemmaTagsDict(word);
    } else {
      dictMap = this.getLemmaTagsDict(word.toLowerCase());
    }
    return dictMap;
  }
  
  /* (non-Javadoc)
   * @see eus.ixa.ixa.pipe.lemma.Lemmatizer#lemmatize(java.lang.String[], java.lang.String[])
   */
  public String[] lemmatize(final String[] tokens, final String[] postags) {
    List<String> lemmas = new ArrayList<String>();
    for (int i = 0; i < tokens.length; i++) {
      lemmas.add(this.apply(tokens[i], postags[i])); 
    }
    return lemmas.toArray(new String[lemmas.size()]);
  }

  public String apply(final String word, final String postag) {
    String lemma = null;
    final List<String> keys = this.getDictKeys(word, postag);
    final HashMap<List<String>, String> dictMap = this.getDictMap(word, postag);
    // lookup lemma as value of the map
    final String keyValue = dictMap.get(keys);
    final String constantTag = this.tagRetriever.setTagConstant(this.lang,
        postag);
    if (keyValue != null) {
      lemma = keyValue;
    } else if (keyValue == null
        && postag.startsWith(String.valueOf(constantTag))) {
      lemma = word;
    } else if (keyValue == null && word.toUpperCase().equals(word)) {
      lemma = word;
    } else {
      lemma = word.toLowerCase();
    }
    return lemma;
  }

}
