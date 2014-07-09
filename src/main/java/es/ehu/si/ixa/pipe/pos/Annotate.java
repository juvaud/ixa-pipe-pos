/*
 * Copyright 2013 Rodrigo Agerri

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

package es.ehu.si.ixa.pipe.pos;

import ixa.kaflib.KAFDocument;
import ixa.kaflib.WF;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import es.ehu.si.ixa.pipe.lemmatize.DictionaryLemmatizer;

/**
 * @author ragerri
 * 
 */
public class Annotate {

  private MorphoTagger posTagger;
  private String lang;
  private MorphoFactory morphoFactory;

  public Annotate(String aLang, String model, int beamsize)
      throws IOException {
    if (model.equalsIgnoreCase("baseline")) {
      System.err.println("No POS model chosen, reverting to baseline model!");
    }
    this.lang = aLang;
    morphoFactory = new MorphoFactory();
    posTagger = new MorphoTagger(lang, model, beamsize, morphoFactory);
  }

  /**
   * 
   * Mapping between Penn Treebank tagset and KAF tagset
   * 
   * @param penn
   *          treebank postag
   * @return kaf POS tag
   */
  private String mapEnglishTagSetToKaf(String postag) {
    if (postag.startsWith("RB")) {
      return "A"; // adverb
    } else if (postag.equalsIgnoreCase("CC")) {
      return "C"; // conjunction
    } else if (postag.startsWith("D") || postag.equalsIgnoreCase("PDT")) {
      return "D"; // determiner and predeterminer
    } else if (postag.startsWith("J")) {
      return "G"; // adjective
    } else if (postag.equalsIgnoreCase("NN") || postag.equalsIgnoreCase("NNS")) {
      return "N"; // common noun
    } else if (postag.startsWith("NNP")) {
      return "R"; // proper noun
    } else if (postag.equalsIgnoreCase("TO") || postag.equalsIgnoreCase("IN")) {
      return "P"; // preposition
    } else if (postag.startsWith("PRP") || postag.startsWith("WP")) {
      return "Q"; // pronoun
    } else if (postag.startsWith("V")) {
      return "V"; // verb
    } else {
      return "O"; // other
    }
  }

  private String mapSpanishTagSetToKaf(String postag) {
    if (postag.equalsIgnoreCase("RB") || postag.equalsIgnoreCase("RN")) {
      return "A"; // adverb
    } else if (postag.equalsIgnoreCase("CC") || postag.equalsIgnoreCase("CS")) {
      return "C"; // conjunction
    } else if (postag.startsWith("D")) {
      return "D"; // determiner and predeterminer
    } else if (postag.startsWith("A")) {
      return "G"; // adjective
    } else if (postag.startsWith("NC")) {
      return "N"; // common noun
    } else if (postag.startsWith("NP")) {
      return "R"; // proper noun
    } else if (postag.startsWith("SP")) {
      return "P"; // preposition
    } else if (postag.startsWith("P")) {
      return "Q"; // pronoun
    } else if (postag.startsWith("V")) {
      return "V"; // verb
    } else {
      return "O"; // other
    }
  }

  private String getKafTagSet(String lang, String postag) {
    String tag = null;
    if (lang.equalsIgnoreCase("en")) {
      tag = this.mapEnglishTagSetToKaf(postag);
    }
    if (lang.equalsIgnoreCase("es")) {
      tag = this.mapSpanishTagSetToKaf(postag);
    }
    return tag;
  }

  /**
   * Set the term type attribute based on the pos value
   * 
   * @param kaf
   *          postag
   * @return type
   */
  private String setTermType(String postag) {
    if (postag.startsWith("N") || postag.startsWith("V")
        || postag.startsWith("G") || postag.startsWith("A")) {
      return "open";
    } else {
      return "close";
    }
  }

  public void annotatePOSToKAF(KAFDocument kaf, DictionaryLemmatizer dictLemmatizer) throws IOException {

    List<List<WF>> sentences = kaf.getSentences();
    for (List<WF> sentence : sentences) {

      String tokens[] = new String[sentence.size()];
      for (int i = 0; i < sentence.size(); i++) {
        tokens[i] = sentence.get(i).getForm();
      }

      List<Morpheme> morphemes = posTagger.getMorphemes(tokens);
      for (int i = 0; i < morphemes.size(); i++) {
        List<WF> wfs = new ArrayList<WF>();
        wfs.add(sentence.get(i));
        String posId = this.getKafTagSet(lang, morphemes.get(i).getTag());
        String type = this.setTermType(posId);
        String lemma = dictLemmatizer.lemmatize(lang, morphemes.get(i).getWord(), morphemes.get(i).getTag());
        morphemes.get(i).setLemma(lemma);
        kaf.createTermOptions(type, morphemes.get(i).getLemma(), posId, morphemes.get(i).getTag(), wfs);
      }
    }
  }
  

  public String annotatePOSToCoNLL(KAFDocument kaf,
      DictionaryLemmatizer dictLemmatizer) throws IOException {
    StringBuilder sb = new StringBuilder();
    List<List<WF>> sentences = kaf.getSentences();
    for (List<WF> sentence : sentences) {
      //Get an array of token forms from a list of WF objects.
      String tokens[] = new String[sentence.size()];
      for (int i = 0; i < sentence.size(); i++) {
        tokens[i] = sentence.get(i).getForm();
      }
      List<String> posTagged = posTagger.posAnnotate(tokens);
      for (int i = 0; i < posTagged.size(); i++) {
        String posTag = posTagged.get(i);
        String lemma = dictLemmatizer.lemmatize(lang, tokens[i], posTag); // lemma
        sb.append(tokens[i]).append("\t").append(lemma).append("\t").append(posTag).append("\n");
      }
      sb.append("\n");
    }
    return sb.toString();
  }

}
