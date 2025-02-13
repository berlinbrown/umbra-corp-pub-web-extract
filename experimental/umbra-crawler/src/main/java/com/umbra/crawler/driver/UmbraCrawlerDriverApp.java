package com.umbra.crawler.driver;

import com.umbra.crawler.driver.model.WordUnigramFreq;
import com.umbra.crawler.driver.services.utils.Pair;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
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
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.text.DecimalFormat;


/**
 * Umbra RSS Crawler, right now all in one class or one file.
 */
public class UmbraCrawlerDriverApp {

    private static final long FILE_OFFSET_TIMESTAMP = 173760000000L;

    /**
     * Max items, set -1 to load all.
     * Use 20, 30, or -1
     */
    private static final int MAX_FEED_LOAD_ITEMS = 2000;
    private static final int MAX_LEN_TITLE = 100;

    private static final int MAX_LEN_DESCRIPTION = 140;

    private static final int MAX_LINKS_RSS = 8;

    private static final int MIN_WORD_CHECK = 4;

    private static final double MIN_VALUE_PERC_CLASSIFY = 45.0;

    private static final double MIN_VALUE_LOOKUP_CLASSIFY = 45.0;

    /**
     * Point for high value words
     */
    private static final long PIVOT_POINT_BY_FREQ = 485544;

    /**
     * Arbitary scaling for top words
     */
    private static final long SCALE_UP_FREQ = 100428495;

    // Define timeout values in milliseconds
    private static final int connectionTimeout = 12000; // Connection establishment timeout
    private static final int socketTimeout = 12000;     // Data transfer timeout

    private static final Map<String, WordUnigramFreq> wordFreqMapData = new TreeMap<>();

    /**
     * High value words.
     */
    private static final Map<String, WordUnigramFreq> wordFreqMapHighValData = new TreeMap<>();

    private static final Map<Long, WordUnigramFreq> wordByFreqValueData = new TreeMap<>();

    private static final Map<String, String> wordToPos = new HashMap<>();

    /**
     * All IDF feed titles (or sentence)
     */
    private static final List<String> allDocumentsForIDFCalc = new ArrayList<>();

    private static final Set<String> rssFeedList = new TreeSet<>();

    private static final Set<String> rssFeedCleanList = new TreeSet<>();

    private static final Set<String> profanityList = new TreeSet<>();

    public static void printAllClean(final PrintWriter out) {
        for (final String rssUrl : rssFeedCleanList) {
            System.out.println(rssUrl);
        }
        out(out).println("    [ Clean List RSS Feeds, size = " + rssFeedCleanList.size() + "]");
    }

    public static void scanAllRSSCleanValues(final PrintWriter rssOut) {
        int i = 0;
        for (final String rssUrl : rssFeedCleanList) {
            System.out.println(">> Processing " + rssUrl + " : " + i);
            try {
                rssOut.println("  {----}");
                rssOut.println("  { At ROOT RSS Feed, rss.ID=" + i + " : rssURL="+rssUrl);
                loadRSS(rssOut, rssUrl);
            } catch(final Exception e) {
                e.printStackTrace();
            }
            System.out.println("<< Processing " + rssUrl + " : " + i);
            i++;
        }
    }

    /**
     * Preliminary check, are the feeds available
     */
    public static void quickAnalysisAllFeeds() {
        int i = 0;
        // Run custom algorithm to reject items
        for (final String rssUrl : rssFeedList) {
            if (rssUrl.length() > 78) {
                System.out.println(i + " WARN - rejecting URL because of length - " + rssUrl);
            } else {
                System.out.println(i + "@>> ENTERING for RSS " + rssUrl);
                boolean checkFeed = quickAnalysisFeed(rssUrl);
                if (checkFeed) {
                    System.out.println("PASS>" + rssUrl);
                    rssFeedCleanList.add(rssUrl);
                    // TODO remove -- stop at a couple
                    if (i != -1) {
                        if (i > MAX_FEED_LOAD_ITEMS) {
                            break;
                        }
                    }
                } else {
                    System.out.println("FAIL>" + rssUrl);
                }
                System.out.println(i + " @<< LEAVING for RSS " + rssUrl);
            }
            i++;
        }
    }
    public static boolean quickAnalysisFeed(final String rssUrl) {
        try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {

            // Step 1: Configure timeouts using RequestConfig
            final RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(connectionTimeout)
                    .setSocketTimeout(socketTimeout)
                    .build();

            // Step 1: Create and execute an HTTP GET request
            final HttpGet request = new HttpGet(rssUrl);

            // Apply the timeout configuration to the request
            request.setConfig(requestConfig);

            // Step 2: Set a friendly User-Agent header
            request.setHeader("User-Agent", "UmbraCrawlerBot/1.0 (Brown +https://github.com/berlinbrown/umbra-corp-pub-web-extract)");

            final HttpResponse response = httpClient.execute(request);

            // Step 2: Check the response status
            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                System.out.println("Failed to fetch RSS feed. HTTP Status: " + statusCode + " rss=" + rssUrl);
                return false;
            }

            // Step 3: Parse the response entity
            final HttpEntity entity = response.getEntity();
            if (entity == null) {
                System.out.println("No content found in RSS feed response = " + rssUrl);
                return false;
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
                System.out.println("<Processed for >> " + itemList.getLength() + "<< number of items");
                if (itemList.getLength() > 8) {
                    return true;
                }
            } finally {
                // Ensure the entity content is fully consumed
                EntityUtils.consume(entity);
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Load feed file with basic listing of feeds
     */
    public static void loadRSSFeedFile(final PrintWriter out, final String path) {
        try (final BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.length() > 2) {
                    // load documents
                    System.out.println(" ! Loading RSS Feed, line=" + line.trim());
                    rssFeedList.add(line.trim());
                }
            }
            System.out.println("*** RSS Feeds Load, rss items loaded=" + rssFeedList.size());
            out(out).println("    [ RSS Feed File Original, size rss list = "+ rssFeedList.size()+ " ]");
        } catch (final IOException e) {
            System.err.println("Error reading RSS data file: " + e.getMessage());
        }
    }

    public static void loadDocumentForProfanity(final PrintWriter out) {
        final InputStream inputStream = UmbraCrawlerDriverApp.class.getClassLoader()
                .getResourceAsStream("com/umbra/crawler/models/profanity.csv");
        if (inputStream != null) {
            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                int i = 0;
                while ((line = reader.readLine()) != null) {
                    if (line.length() > 2) {
                        profanityList.add(line.trim().toLowerCase());
                        i++;
                    }
                }
                System.out.println("[Load Profanity Check] - profanity words - " + profanityList.size());
                out(out).println("    [ Load Profanity Check, records in profanityList = " +profanityList.size()  + " ]");
            } catch (final IOException e) {
                System.err.println("Error reading profanity data file: " + e.getMessage());
            }
        } else {
            System.err.println("Error reading profanity dat file: ");
        }
    }

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

    // Classify words in a sentence
    public static Pair<Boolean, String> classifySentence(String sentence) {
        final String[] words = sentence.split("\\s+");
        int i = 0;
        int wordct = 0;
        double pass = 0.0;
        System.out.println();
        for (final String word : words) {
            final String normalizedWord = normalize(word.toLowerCase());
            if (normalizedWord.length() > 1) {
                final String pos = wordToPos.getOrDefault(normalizedWord, "unknown");
                System.out.print(" '" + normalizedWord + "' is a " + pos + ", ");
                if (!"unknown".equalsIgnoreCase(pos)) {
                    pass += 1.0;
                }
                wordct++;
            }
            if (i > 16) {
                System.out.println();
                break;
            }
            i++;
        }
        if (wordct > MIN_WORD_CHECK) {
            final double perc = (pass / (double) (wordct - 0)) * 100.0;
            System.out.println();
            System.out.println("[POS Classify] percent pass=" + perc + ", pass=" + pass + ", i=" + (wordct - 0));
            System.out.println();
            final boolean hasClassifyByPOS = perc > MIN_VALUE_PERC_CLASSIFY;
            return new Pair<Boolean, String>(hasClassifyByPOS, "(POSCheck:" + formatNumber(perc) + ";"+wordct+")");
        } else {
            System.out.println();
            System.out.println("[POS Classify] could not POS classify");
            System.out.println();
            return new Pair<Boolean, String>(false, "(POSCheck:0.0)");
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
    public static Pair<Boolean, String> lookup(final String sentence) {
        final String [] terms = sentence.split("\\s+");
        double pass = 0;
        int i = 0;
        int wordct = 0;
        System.out.println();
        for (final String term : terms) {
            final String normalizeKey = normalize(term.trim().toLowerCase());
            if (normalizeKey != null && normalizeKey.length() > 2) {
                final boolean lookupFound = wordFreqMapHighValData.containsKey(normalizeKey);
                // TODO remove System.out.print("   >" + normalizeKey + " " + lookupFound + ", ");
                if (lookupFound) {
                    pass += 1.0;
                }
                wordct++;
            }
            if (i > 20) {
                System.out.println();
                break;
            }
            i++;
        }
        if (wordct > MIN_WORD_CHECK) {
            final double perc = (pass / (double) (wordct - 0)) * 100.0;
            System.out.println();
            System.out.println("[Word Lookup] Percent pass " + perc + " percent (pass=" + pass + ", ct=" + (wordct - 0) + ")");
            final boolean hasLookup = perc > MIN_VALUE_LOOKUP_CLASSIFY;
            return new Pair<Boolean, String>(hasLookup, "(LookupCheck:"+formatNumber(perc)+";"+wordct+")");
        } else {
            return new Pair<Boolean, String>(false, "(LookupCheck:0.0)");
        }
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
    public static void loadFrequencyFile(final PrintWriter out) {
        // Load without slash format for freq file
        final InputStream inputStream = UmbraCrawlerDriverApp.class.getClassLoader()
                .getResourceAsStream("com/umbra/crawler/models/unigram_freq.csv");
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

                out(out).println("    [ Freq Files, records in wordFreqMapData = " +wordFreqMapData.size()  + " ]");
            } catch (final IOException e) {
                e.printStackTrace();
            }

            // Load top freq words
            findTopWords();
        } else {
            System.out.println("Resource not found");
        }
    }

    public static void loadRSS(final PrintWriter rssOut, final String singleLoadRssUrl) {
        // Continue load RSS feed
        final String rssUrl = singleLoadRssUrl;
        final String headerRssPrefix = "    ";
        final String secondRssPrefix = "        ";

        try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // Step 1: Create and execute an HTTP GET request
            final HttpGet request = new HttpGet(rssUrl);
            final HttpResponse response = httpClient.execute(request);

            // Step 2: Check the response status
            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                System.out.println("Failed to fetch RSS feed. HTTP Status: " + statusCode);
                return;
            }
            rssOut.println();
            rssOut.println(headerRssPrefix + "[ Successful (200 HTTP Response) Download for RSS FEED URL = " + singleLoadRssUrl);

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

                // First run through, does it have at least one valid.
                boolean hasOneValidOne = false;
                int checkRssItemData = 0;
                for (int i = 0; i < itemList.getLength(); i++) {
                    final Node item = itemList.item(i);
                    if (item.getNodeType() == Node.ELEMENT_NODE) {
                        final Element element = (Element) item;
                        String title = getElementValue(element, "title");
                        String description = getElementValue(element, "description");
                        final String origTitle = title;
                        final String origDescription = description;
                        final String link = getElementValue(element, "link");
                        final String date = getElementValue(element, "pubDate");
                        System.out.println("----------------------- (" + i + ") date:" + date);
                        // Clip title and description
                        if (title != null && title.length() > MAX_LEN_TITLE) {
                            title = title.substring(0, MAX_LEN_TITLE) + "...";
                        }
                        if (description != null && description.length() > MAX_LEN_DESCRIPTION) {
                            description = description.substring(0, MAX_LEN_DESCRIPTION)  + "...";
                        }
                        System.out.println("(first run) Title: " + title);
                        System.out.println("(first run) Description: " + description);
                        System.out.println("(first run) Link: " + link);

                        // First run
                        Pair<Boolean, String> pass1Obj;
                        Pair<Boolean, String> pass2Obj;
                        Pair<Boolean, String> pass3Obj;
                        Pair<Boolean, String> pass4Obj;

                        System.out.println("[[ Lookup Title ==]]");
                        pass1Obj = lookup(origTitle);
                        System.out.println("[[ Lookup Description ==]]");
                        pass2Obj = lookup(origDescription);
                        pass3Obj = classifySentence(origTitle);
                        pass4Obj = classifySentence(origDescription);
                        double relevanceScore = calculateTFIDFRelevance(description);
                        if (pass1Obj.getFirst() && pass2Obj.getFirst() &&
                                pass3Obj.getFirst() && pass4Obj.getFirst()) {
                            hasOneValidOne = true;
                            checkRssItemData++;
                        }
                        if (i > MAX_LINKS_RSS) {
                            break;
                        }
                    }
                } // End of first run through.

                // Second run through, write data
                if (hasOneValidOne && checkRssItemData >= 2) {
                    for (int i = 0; i < itemList.getLength(); i++) {
                        final Node item = itemList.item(i);
                        if (item.getNodeType() == Node.ELEMENT_NODE) {
                            Element element = (Element) item;

                            // Extract title, description, and link
                            String title = getElementValue(element, "title");
                            String description = getElementValue(element, "description");
                            final String origTitle = title;
                            final String origDescription = description;
                            final String link = getElementValue(element, "link");
                            final String date = getElementValue(element, "pubDate");

                            System.out.println("----------------------- (" + i + ") date:" + date);
                            rssOut.println(secondRssPrefix + ".checking RSS Item Entry for data, id=" + i);

                            // Clip title and description
                            if (title != null && title.length() > MAX_LEN_TITLE) {
                                title = title.substring(0, MAX_LEN_TITLE) + "...";
                            }
                            if (description != null && description.length() > MAX_LEN_DESCRIPTION) {
                                description = description.substring(0, MAX_LEN_DESCRIPTION) + "...";
                            }
                            System.out.println("(second run) Title: " + title);
                            System.out.println("(second run) Description: " + description);
                            System.out.println("(second run) Link: " + link);

                            // Second run
                            Pair<Boolean, String> pass1Obj;
                            Pair<Boolean, String> pass2Obj;
                            Pair<Boolean, String> pass3Obj;
                            Pair<Boolean, String> pass4Obj;

                            System.out.println("[[ Lookup Title ==]]");
                            pass1Obj = lookup(origTitle);
                            System.out.println("[[ Lookup Description ==]]");
                            pass2Obj = lookup(origDescription);

                            System.out.println("[[ POS Classify Title ==]]");
                            pass3Obj = classifySentence(origTitle);
                            System.out.println("[[ POS Classify Description ==]]");
                            pass4Obj = classifySentence(origDescription);

                            // Test title for IDF
                            double relevanceScore = calculateTFIDFRelevance(description);
                            System.out.println("     >)Relevance Score: " + relevanceScore);
                            System.out.println("-----------------------");

                            if (pass1Obj.getFirst() && pass2Obj.getFirst() &&
                                    pass3Obj.getFirst() && pass4Obj.getFirst()) {
                                rssOut.println(secondRssPrefix + "--------");
                                rssOut.println(secondRssPrefix + "Item contains passed data, printing, id=" + i);
                                rssOut.println(secondRssPrefix + " Title=" + title);
                                rssOut.println(secondRssPrefix + " Description=" + description);
                                rssOut.println(secondRssPrefix + " Publication Date=" + date);
                                rssOut.println(secondRssPrefix + " Link=" + link);
                                rssOut.println(secondRssPrefix + " (----)");
                                rssOut.println(secondRssPrefix + " *Lookup Checks title:" + pass1Obj + " - description:" + pass2Obj);
                                rssOut.println(secondRssPrefix + " *POS Checks title:" + pass3Obj + " - description:" + pass4Obj);
                                rssOut.println(secondRssPrefix + " *IDF Relevance Score=" + relevanceScore);
                                rssOut.println(secondRssPrefix + " (----)");
                                // final listing for item
                                rssOut.println();
                            } else {
                                // rssOut.println(secondRssPrefix + "// skipping printing item, id="
                                //         + i + " title:" + pass3Obj + " description:" + pass4Obj);
                                // rssOut.println(secondRssPrefix + "// skipping, id=" + i );
                            }
                            if (i > MAX_LINKS_RSS) {
                                break;
                            }
                        }
                    } // End of second run through.
                } else {
                    rssOut.println(secondRssPrefix + "    << No Data Found for RSS URL listed, rssURL="+rssUrl);
                    rssOut.println();
                    rssOut.println();
                } // End of check has at least one valid
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

    private static PrintWriter out(final PrintWriter out) {
        if (out == null) {
            throw new IllegalStateException("Invalid Writer Object");
        }
        return out;
    }

    private static String formatNumber(final double number) {
        final DecimalFormat df = new DecimalFormat("#.##");
        return df.format(number);
    }

    public static void rssAll() {

        final String formattedDate = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMdd")).toLowerCase();
        final long timestampDir = System.currentTimeMillis() - FILE_OFFSET_TIMESTAMP;
        final String filePath = "./output/samplerun/analytics/" + formattedDate + "/00" + timestampDir
                + "/data/job_analytics.dat";
        final Path path = Paths.get(filePath).getParent();

        // Continue with rss data file
        final String rssDataFile = "./output/samplerun/data/" + formattedDate + "/00" + timestampDir
                + "/data/rss_feed.dat";
        final Path rssDataPath = Paths.get(rssDataFile).getParent();

        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
            if (!Files.exists(rssDataPath)) {
                Files.createDirectories(rssDataPath);
            }
            // Open the file and get access to PrintWriter
            try (final PrintWriter out = new PrintWriter(new FileWriter(filePath))) {

                // Write header for report
                out.println("#======== Umbra Crawler Bot Job System Report ========");
                out.println("[ Job Timestamp = "+ LocalDateTime.now()+ " ]");
                System.out.println("File written successfully to: " + filePath);

                System.out.println("(([Step 1] Entering loading frequency file ))");
                out(out).println(" [ Frequency File Data ]");
                loadFrequencyFile(out);

                System.out.println("(([Step 2] Entering loading word net file ))");
                loadWordNet();

                System.out.println("(([Step 3] Entering loading documents for IDF ))");
                loadDocumentsForIDF();

                System.out.println("(([Step 4] Entering loading documents for profanity ))");
                out(out).println(" [ Profanity Data ]");
                loadDocumentForProfanity(out);
                System.out.println(">>>> Continue with run, word tools loaded");

                System.out.println("(([Step 5] Entering loading documents RSS feed file off file system ))");
                out(out).println(" [ Basic RSS Feeds Listing ]");
                loadRSSFeedFile(out, "./input_feeds/basic-feeds.csv");
                System.out.println("<<<< Done");

                System.out.println("(([Step 6] System Check for RSS Data ))");
                quickAnalysisAllFeeds();

                System.out.println("(([Step 7] List of cleaned RSS feeds ))");
                out(out).println(" [ Verifying RSS Listing ]");
                printAllClean(out);

                //loadRSS("https://feeds.bbci.co.uk/news/world/rss.xml");

                // Continue to write rss data file
                try (final PrintWriter rssOut = new PrintWriter(new FileWriter(rssDataFile))) {
                    // Write header for report
                    rssOut.println("#======== Umbra Crawler Bot RSS File Report ========");
                    rssOut.println("[ Contains RSS Data from sample run including title, description, links and analysis of data ]");
                    rssOut.println("[ RSS File Timestamp = "+ LocalDateTime.now()+ " ]");
                    rssOut.println();
                    rssOut.println();

                    //loadRSS(rssOut, "https://feeds.bbci.co.uk/news/world/rss.xml");
                    scanAllRSSCleanValues(rssOut);

                    rssOut.println();
                    rssOut.println("#======== End File ========");

                }
                out(out).println();
                out(out).println(" [ Job Complete "+ LocalDateTime.now() + " ]");
                out.println("#======== End of Job System Report ========");
            }

        } catch (final IOException e) {
            // Handle exceptions properly
            System.err.println("An error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Main entry point into program.
     * @param args
     */
    public static void main( String[] args ) {
        if (true) {
            throw new RuntimeException();
        }
        System.out.println("==== Running Crawler Bot ====");
        rssAll();
        System.out.println("==== End Running Crawler Bot ====");

    }

}
