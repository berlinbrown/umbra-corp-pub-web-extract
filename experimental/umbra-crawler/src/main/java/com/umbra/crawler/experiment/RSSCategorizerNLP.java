package com.umbra.crawler.experiment;

import edu.stanford.nlp.simple.Document;
import edu.stanford.nlp.simple.Sentence;

import java.util.*;
import java.util.stream.Collectors;

public class RSSCategorizerNLP {

    public static void main(String[] args) {
        // Example RSS feed phrases
        final List<String> phrases = Arrays.asList(
            "Rocket launch challenges Elon Musk's space dominance",
            "Two women killed in knife attack at Slovak school"
        );

        // Define training examples for categories
        final Map<String, List<String>> trainingData = new HashMap<>();
        trainingData.put("Space", Arrays.asList("rocket", "launch", "space", "NASA", "Elon Musk"));
        trainingData.put("Global News", Arrays.asList("attack", "killed", "school", "global", "violence"));

        // Categorize each phrase
        phrases.forEach(phrase -> {
            String category = categorizePhrase(phrase, trainingData);
            System.out.println("\"" + phrase + "\"");
            System.out.println("Goes into: " + category + "\n");
        });
    }

    /**
     * Categorizes a phrase using NLP and cosine similarity.
     *
     * @param phrase       The phrase to categorize.
     * @param trainingData A map of category names to their associated keywords/examples.
     * @return The name of the best matching category.
     */
    private static String categorizePhrase(String phrase, Map<String, List<String>> trainingData) {
        // Preprocess the phrase (lemmatization)
        List<String> phraseTokens = preprocess(phrase);

        // Calculate similarity with each category
        String bestCategory = "Uncategorized";
        double bestScore = 0;

        for (Map.Entry<String, List<String>> entry : trainingData.entrySet()) {
            String category = entry.getKey();
            List<String> categoryKeywords = entry.getValue();

            // Compute cosine similarity between phrase tokens and category keywords
            double score = cosineSimilarity(phraseTokens, categoryKeywords);
            if (score > bestScore) {
                bestScore = score;
                bestCategory = category;
            }
        }

        return bestCategory;
    }

    /**
     * Preprocesses a phrase by tokenizing and lemmatizing it.
     *
     * @param phrase The input phrase.
     * @return A list of lemmatized tokens.
     */
    private static List<String> preprocess(String phrase) {
        Document doc = new Document(phrase);
        return doc.sentences().stream()
            .flatMap(sentence -> sentence.lemmas().stream())
            .map(String::toLowerCase)
            .collect(Collectors.toList());
    }

    /**
     * Computes the cosine similarity between two lists of words.
     *
     * @param tokens1 The first list of words.
     * @param tokens2 The second list of words.
     * @return The cosine similarity score.
     */
    private static double cosineSimilarity(List<String> tokens1, List<String> tokens2) {
        // Count term frequencies
        final Map<String, Integer> freq1 = termFrequency(tokens1);
        final Map<String, Integer> freq2 = termFrequency(tokens2);

        // Compute dot product and magnitudes
        final Set<String> uniqueTerms = new HashSet<>(freq1.keySet());
        uniqueTerms.addAll(freq2.keySet());

        int dotProduct = 0;
        int magnitude1 = 0;
        int magnitude2 = 0;

        for (String term : uniqueTerms) {
            int count1 = freq1.getOrDefault(term, 0);
            int count2 = freq2.getOrDefault(term, 0);

            dotProduct += count1 * count2;
            magnitude1 += count1 * count1;
            magnitude2 += count2 * count2;
        }

        // Avoid division by zero
        if (magnitude1 == 0 || magnitude2 == 0) return 0;

        return dotProduct / (Math.sqrt(magnitude1) * Math.sqrt(magnitude2));
    }

    /**
     * Computes term frequencies for a list of tokens.
     *
     * @param tokens The list of tokens.
     * @return A map of terms to their frequencies.
     */
    private static Map<String, Integer> termFrequency(List<String> tokens) {
        Map<String, Integer> freqMap = new HashMap<>();
        for (String token : tokens) {
            freqMap.put(token, freqMap.getOrDefault(token, 0) + 1);
        }
        return freqMap;
    }
}
