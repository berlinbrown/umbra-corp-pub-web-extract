package com.umbra.crawler.driver;

import com.umbra.crawler.driver.model.WordUnigramFreq;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Umbra RSS Crawler
 */
public class UmbraCrawlerDriverApp {

    /**
     * Point for high value words
     */
    private static final long PIVOT_POINT_BY_FREQ = 485544;

    /**
     * Arbitary scaling for top words
     */
    private static final long SCALE_UP_FREQ = 100428495;

    private static final Map<String, WordUnigramFreq> wordFreqMapData = new TreeMap<>();

    /**
     * High value words.
     */
    private static final Map<String, WordUnigramFreq> wordFreqMapHighValData = new TreeMap<>();

    private static final Map<Long, WordUnigramFreq> wordByFreqValueData = new TreeMap<>();

    private static final Map<String, String> wordToPos = new HashMap<>();

    /**
     * All RSS feed titles (or sentence)
     */
    private static final List<String> allDocumentsForIDFCalc = new ArrayList<>();

    public static void loadDocumentsForIDF() {
        final InputStream inputStream = UmbraCrawlerDriverApp.class.getClassLoader()
                .getResourceAsStream("com/umbra/crawler/models/common_phrases_relevant.dat");
        if (inputStream != null) {
            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.length() > 2) {
                        // load documents
                        System.out.println(" <<<" + line);
                        allDocumentsForIDFCalc.add(line.trim().toLowerCase());
                    }
                }

            } catch (final IOException e) {
                System.err.println("Error reading IDF data file: " + e.getMessage());
            }
        } else {
            System.err.println("Error reading IDF dat file: ");
        }
    }
    // Load WordNet data from a specific file
    public static void loadWordNetData(final String file, final String posTag) {
        final InputStream inputStream = UmbraCrawlerDriverApp.class.getClassLoader()
                .getResourceAsStream("com/umbra/crawler/models/wordnet/data/"+file);
        if (inputStream != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("  ")) continue; // Skip metadata or comments
                    String[] parts = line.split("\\s+");
                    if (parts.length > 4) {
                        String word = parts[4]; // Extract the first word in the synset
                        wordToPos.put(normalize(word.toLowerCase()), posTag);
                    }
                }
                System.out.println("Loaded POS - " + posTag + " " + wordToPos.size());

            } catch (final IOException e) {
                System.err.println("Error reading WordNet file: " + e.getMessage());
            }
        } else {
            System.err.println("Error reading WordNet file: ");
        }
    }

    public static void loadWordNet() {
        // Load WordNet files (assuming they are downloaded locally)
        loadWordNetData("data.noun", "noun");
        loadWordNetData("data.verb", "verb");
        loadWordNetData("data.adj", "adjective");
        loadWordNetData("data.adv", "adverb");

    }

    public static void posTest() {
        // Test with a sentence
        final String sentence = "The happy dog run quickly.";
        System.out.println("Classifying sentence: " + sentence);
        classifySentence(sentence);
    }

    // Classify words in a sentence
    public static void classifySentence(String sentence) {
        final String[] words = sentence.split("\\s+");
        for (final String word : words) {
            final String normalizedWord = normalize(word.toLowerCase());
            String pos = wordToPos.getOrDefault(normalizedWord, "unknown");
            System.out.println("'" + normalizedWord + "' is a " + pos);
        }
    }


    private static String normalize(final String term) {
        if (term == null) {
            return "";
        }
        return term.trim().toLowerCase().replaceAll("[^a-z]", "");

    }
    /**
     * test word lookup.
     */
    public static void lookupTest() {
        final String sentence = "Nine Palestinians killed as Israeli forces launch major operation in Jenin";
        lookup(sentence);
    }
    /**
     * test word lookup.
     */
    public static void lookup(final String sentence) {
        final String [] terms = sentence.split("\\s+");
        double pass = 0;
        for (final String term : terms) {
            final String normalizeKey = normalize(term.trim().toLowerCase());
            if (normalizeKey != null && normalizeKey.length() > 2) {
                final boolean lookupFound = wordFreqMapHighValData.containsKey(normalizeKey);
                System.out.println("   >" + normalizeKey + " " + lookupFound);
                if (lookupFound) {
                    pass += 1.0;
                }
            }
        }
        final double perc = (pass / (double)terms.length) * 100.0;
        System.out.println("Percent pass " + perc + " percent");
    }

    /**
     * Calculate Inverse Document Frequency (IDF) for a term.
     */
    private static double calculateIDF(final String term) {
        final long documentCount = allDocumentsForIDFCalc.size();
        final long termOccurrences = allDocumentsForIDFCalc.stream()
                .filter(doc -> Arrays.asList(doc.toLowerCase().split("\\s+")).contains(term))
                .count();
        // If term is not found in any document, return a small value to avoid division by zero
        if (termOccurrences == 0) {
            return Math.log((double) documentCount + 1);
        }
        // IDF formula: log((N + 1) / (df + 1)) + 1
        return Math.log((double) (documentCount + 1) / (termOccurrences + 1)) + 1;
    }

    /**
     * Calculate the TF-IDF relevance score for a sentence.
     */
    private static double calculateTFIDFRelevance(final String sentence) {
        System.out.println("   << " + sentence);
        final String[] terms = sentence.toLowerCase().split("\\s+");
        final Map<String, Integer> termFrequency = new HashMap<>();
        // Calculate term frequency (TF) for the given sentence
        for (String term : terms) {
            term = normalize(term.trim());
            termFrequency.put(term, termFrequency.getOrDefault(term, 0) + 1);
        }
        // Calculate TF-IDF
        double score = 0.0;
        for (final String term : termFrequency.keySet()) {
            double tf = termFrequency.get(term) / (double) terms.length; // Term Frequency
            double idf = calculateIDF(term); // Inverse Document Frequency
            score += tf * idf;
        }
        return score;
    }

    /**
     * loadfile
     */
    public static void loadFile() {
        // Load without slash format for freq file
        final InputStream inputStream = UmbraCrawlerDriverApp.class.getClassLoader().getResourceAsStream("com/umbra/crawler/models/unigram_freq.csv");
        if (inputStream != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                int i = 0;
                while ((line = reader.readLine()) != null) {
                    if (i > 0) {
                        try {
                            if (line.trim().length() > 1) {
                                final String[] wordPair = line.split(",");
                                final String wordKey = normalize(wordPair[0].trim().toLowerCase());
                                final long freq = Long.parseLong(wordPair[1].trim());
                                final WordUnigramFreq word = new WordUnigramFreq(wordKey, freq);
                                wordFreqMapData.put(word.getWord(), word);
                                wordByFreqValueData.put(freq, word);
                            }
                        } catch (final Exception e) {
                            e.printStackTrace();
                            System.err.println("Error processing line " + i + " data - " + line + " message=" + e.getMessage());
                        }
                    }
                    i++;
                }
                System.out.println("Word Map Database Size: " + wordFreqMapData.size());
                System.out.println("Loaded word csv file " + i + " records");
            } catch (final IOException e) {
                e.printStackTrace();
            }

            // Load top freq words
            findTopWords();
        } else {
            System.out.println("Resource not found");
        }
    }

    public static void loadRSS() {
        // Continue load RSS feed
        //final String rssUrl = "https://techcrunch.com/feed/";
        final String rssUrl = "https://feeds.bbci.co.uk/news/world/rss.xml";

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // Step 1: Create and execute an HTTP GET request
            final HttpGet request = new HttpGet(rssUrl);
            final HttpResponse response = httpClient.execute(request);

            // Step 2: Check the response status
            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                System.out.println("Failed to fetch RSS feed. HTTP Status: " + statusCode);
                return;
            }

            // Step 3: Parse the response entity
            final HttpEntity entity = response.getEntity();
            if (entity == null) {
                System.out.println("No content found in RSS feed response.");
                return;
            }

            try (InputStream inputStream = entity.getContent()) {
                // Step 4: Parse the RSS XML
                final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                final DocumentBuilder builder = factory.newDocumentBuilder();
                final Document doc = builder.parse(inputStream);

                // Normalize the XML structure
                doc.getDocumentElement().normalize();

                // Step 5: Extract RSS feed items
                final NodeList itemList = doc.getElementsByTagName("item");
                System.out.println("<Running Report for >> " + itemList.getLength() + "<< number of items");

                for (int i = 0; i < itemList.getLength(); i++) {
                    Node item = itemList.item(i);
                    if (item.getNodeType() == Node.ELEMENT_NODE) {
                        Element element = (Element) item;

                        // Extract title, description, and link
                        final String title = getElementValue(element, "title");
                        final String description = getElementValue(element, "description");
                        final String link = getElementValue(element, "link");
                        final String date = getElementValue(element, "pubDate");

                        System.out.println("----------------------- (" + i + ") date:" + date);
                        System.out.println("Title: " + title);
                        System.out.println("Description: " + description);
                        System.out.println("Link: " + link);

                        lookup(title);
                        lookup(description);
                        classifySentence(title);
                        classifySentence(description);

                        // Test title for IDF
                        double relevanceScore = calculateTFIDFRelevance(description);
                        System.out.println("     >)Relevance Score: " + relevanceScore);

                        System.out.println("-----------------------");
                    }
                }
                System.out.println("<Processed for >> " + itemList.getLength() + "<< number of items");
            } finally {
                // Ensure the entity content is fully consumed
                EntityUtils.consume(entity);
            }

        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    private static String getElementValue(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            Node node = nodeList.item(0);
            return node.getTextContent();
        }
        return null;
    }

    private static void findTopWords() {
        int i = 1;
        for (final Map.Entry<Long, WordUnigramFreq> mapEntry : wordByFreqValueData.entrySet()) {
            if (mapEntry.getValue().getFrequency() > PIVOT_POINT_BY_FREQ) {
                final String newKey = normalize(mapEntry.getValue().getWord());
                final WordUnigramFreq newFreq = new WordUnigramFreq(newKey, mapEntry.getValue().getFrequency() + SCALE_UP_FREQ);
                wordFreqMapHighValData.put(newKey, newFreq);
                i++;
            }
        }
    }

    /**
     * Main entry point into program.
     * @param args
     */
    public static void main( String[] args ) {

        System.out.println("Running Crawler");

        loadFile();
        loadWordNet();
        loadDocumentsForIDF();
        System.out.println(">>>>");

        lookupTest();
        loadRSS();
        System.out.println(">>>> Continue with POS testing (done rss)");

    }

}
