package com.umbra.crawler;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;

import java.io.InputStream;


/**
 * Umbra RSS Crawler
 */
public class UmbraCrawlerApp  {

    public static void main( String[] args ) {

        if (true) {
            throw new RuntimeException();
        }

        System.out.println("Running Crawler");
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
                        //System.out.println("Description: " + description);
                        System.out.println("Link: " + link);
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

}
