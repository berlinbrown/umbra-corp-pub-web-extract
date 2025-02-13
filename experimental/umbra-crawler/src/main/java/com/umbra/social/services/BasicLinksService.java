package com.umbra.social.services;

import org.apache.lucene.search.Sort;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import java.util.List;
import java.util.ArrayList;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class BasicLinksService {

    private static Set<String> links = new HashSet<>();

    private static Set<String> randomizedSet = new HashSet<>();

    public static void dbtest() {
        // HSQLDB JDBC URL for in-memory database
        final String url = "jdbc:hsqldb:mem:mydb";
        final String user = "SA"; // Default username
        final String password = ""; // Default password is empty

        try (Connection connection = DriverManager.getConnection(url, user, password);
             final Statement statement = connection.createStatement()) {

            // Step 1: Create a table
            final String createTableSQL = "CREATE TABLE employees (id INT PRIMARY KEY, name VARCHAR(50), salary DECIMAL(10, 2))";
            statement.execute(createTableSQL);
            System.out.println("Table created successfully.");

            // Step 2: Insert data into the table
            final String insertDataSQL = "INSERT INTO employees (id, name, salary) VALUES (1, 'John Doe', 50000.00), " +
                    "(2, 'Jane Smith', 60000.00), (3, 'Mike Brown', 55000.00)";
            statement.executeUpdate(insertDataSQL);
            System.out.println("Data inserted successfully.");

            // Step 3: Query the table
            String querySQL = "SELECT * FROM employees";
            ResultSet resultSet = statement.executeQuery(querySQL);

            // Print the query results
            System.out.println("Employees:");
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String name = resultSet.getString("name");
                double salary = resultSet.getDouble("salary");
                System.out.printf("ID: %d, Name: %s, Salary: %.2f%n", id, name, salary);
            }

            // Step 4: Drop the table (optional cleanup)
            String dropTableSQL = "DROP TABLE employees";
            statement.execute(dropTableSQL);
            System.out.println("Table dropped successfully.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<String> data() {
        final List<String> stringList = new ArrayList<>(links);
        return stringList;
    }
    public static void load() {
        System.out.println("Starting to load links");
        final String filePath = "/Users/berlinbrown/projects2/umbra-corp-pub-web-extract/experimental/umbra-crawler/output/samplerun/data/static/rss_feed.dat";
        int i = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            System.out.println("Entering file load by line");
            // Read the link database.
            while ((line = br.readLine()) != null) {
                if (line.trim().length() > 10) {
                    final String ln = line.trim();
                    if (ln.startsWith("Link=")) {
                        final String url = ln.substring(5);
                        System.out.println(url);
                        links.add(url);
                        i++;
                    }
                }
            }
            System.out.println("Number of links loaded: " + i);
            final List<String> stringList = new ArrayList<>(links);
            Collections.shuffle(stringList);

            // Optionally convert back to a set (if needed, but note sets don't maintain order)
            randomizedSet = new LinkedHashSet<>(stringList);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String version() {
        System.out.println("Printing version...1.0.1");
        return "1.0.1";
    }
}
