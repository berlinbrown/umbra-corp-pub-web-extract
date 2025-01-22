package com.umbra.crawler;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class UmbraCrawlerAppAtom {

    public static void main(String[] args) {

        final String feedUrl = "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/all_day.atom";
        try {
            // Connect to the remote URL
            HttpURLConnection connection = (HttpURLConnection) new URL(feedUrl).openConnection();
            connection.setRequestMethod("GET");

            // Check response code
            if (connection.getResponseCode() != 200) {
                System.out.println("Failed to fetch feed. HTTP response code: " + connection.getResponseCode());
                return;
            }

            // Read the response stream
            InputStream inputStream = connection.getInputStream();

            // Parse the XML input stream
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true); // Handle Atom namespaces
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document document = builder.parse(inputStream);

            // Normalize the document
            document.getDocumentElement().normalize();

            // Get the root element (feed)
            Element root = document.getDocumentElement();

            // Read the title of the feed
            NodeList titleNodes = root.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "title");
            if (titleNodes.getLength() > 0) {
                Node titleNode = titleNodes.item(0);
                System.out.println("Feed Title: " + titleNode.getTextContent());
            }

            // Read entries in the feed
            NodeList entryNodes = root.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "entry");
            for (int i = 0; i < entryNodes.getLength(); i++) {
                Node entryNode = entryNodes.item(i);

                if (entryNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element entryElement = (Element) entryNode;

                    // Get the title of the entry
                    NodeList entryTitleNodes = entryElement.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "title");
                    if (entryTitleNodes.getLength() > 0) {
                        System.out.println("Entry Title: " + entryTitleNodes.item(0).getTextContent());
                    }

                    // Get the summary of the entry (optional)
                    NodeList summaryNodes = entryElement.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "summary");
                    if (summaryNodes.getLength() > 0) {
                        System.out.println("Entry Summary: " + summaryNodes.item(0).getTextContent());
                    }
                }
            }

            // Close the connection
            inputStream.close();
            connection.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
