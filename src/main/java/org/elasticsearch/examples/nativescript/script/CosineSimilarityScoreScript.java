package org.elasticsearch.examples.nativescript.script;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Collections;

import org.elasticsearch.script.ScriptException;

import org.elasticsearch.common.Nullable;
import org.elasticsearch.script.AbstractSearchScript;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.NativeScriptFactory;
import org.elasticsearch.search.lookup.IndexField;
import org.elasticsearch.search.lookup.IndexFieldTerm;

/**
 * Script that scores documents with cosine similarity, see Manning et al.,
 * "Information Retrieval", Chapter 6, Eq. 6.12 (link:
 * http://nlp.stanford.edu/IR-book/). This implementation only scores a list of
 * terms on one field.
 */
public class CosineSimilarityScoreScript extends AbstractSearchScript {

    //Il campo su cui deve essere effettuato il calcolo della cosine similarity
    String field = null;
    //Termini della query per effettuare lo score (rappresentano il vettore query)
    ArrayList<String> terms = null;
    //Pesi dei singoli termini (si possono usare le rispettive frequenze nel testo da cui è stata estratta la query o usare valori a 1)
    ArrayList<Double> weights = null;

    final static public String SCRIPT_NAME = "cosine_sim_script_score";

    /**
     * Factory that is registered in
     * {@link org.elasticsearch.examples.nativescript.plugin.NativeScriptExamplesPlugin#onModule(org.elasticsearch.script.ScriptModule)}
     * method when the plugin is loaded.
     */
    public static class Factory implements NativeScriptFactory {

        /**
         * This method is called for every search on every shard.
         * 
         * @param params
         *            list of script parameters passed with the query
         * @return new native script
         */
        @Override
        public ExecutableScript newScript(@Nullable Map<String, Object> params) throws ScriptException {
            return new CosineSimilarityScoreScript(params);
        }
    }

    /**
     * @param params
     *            terms that a scored are placed in this parameter. Initialize
     *            them here.
     * @throws ScriptException 
     */
    private CosineSimilarityScoreScript(Map<String, Object> params) throws ScriptException {
        params.entrySet();
        // Recupero i termini
        terms = (ArrayList<String>) params.get("terms");
        // Recupero i pesi
        weights = (ArrayList<Double>) params.get("weights");
        // Recupero il campo su cui fare il confronto
        field = (String) params.get("field");
        if (field == null || terms == null || weights == null) {
            throw new ScriptException("cannot initialize " + SCRIPT_NAME + ": field, terms or weights parameter missing!");
        }
       if (weights.size() != terms.size()) {
          weights = new ArrayList<Double>(Collections.nCopies(terms.size(), (double) (double) 1));
       }
    }

    @Override
    public Object run() {
        try {
            float score = 0;
            // first, get the ShardTerms object for the field.
            IndexField indexField = this.indexLookup().get(field);
            double queryWeightSum = 0.0f;
            double docWeightSum = 0.0f;
            for (int i = 0; i < terms.size(); i++) {
                //Recupero le statistiche di ogni termine
                IndexFieldTerm indexTermField = indexField.get(terms.get(i));
                //calcolo df, tf ed idf
                int df = (int) indexTermField.df();
                int tf = indexTermField.tf();
                double idf = Math.log(((float) indexField.docCount() + 2.0) / ((float) df + 1.0));
                if (df != 0 && tf != 0) {
                    double tfidf = (double) tf * idf;
                    //weight * idf è il valore del termine del documento query
                    double termscore = (double) tfidf * weights.get(i) * idf;
                    score += termscore;
                    docWeightSum += Math.pow(tfidf, 2.0);
                }
                queryWeightSum += Math.pow(weights.get(i) * idf, 2.0);
            }
            return score / (Math.sqrt(docWeightSum) * Math.sqrt(queryWeightSum));
        } catch (IOException ex) {
            throw new ScriptException("Could not compute cosine similarity: ", ex);
        }
    }

}
