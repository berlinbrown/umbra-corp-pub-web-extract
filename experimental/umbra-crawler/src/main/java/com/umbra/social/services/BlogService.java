package com.umbra.social.services;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

public class BlogService {

    private static Set<String> blogdata = new HashSet<>();

    public static void blogdb() {
        // HSQLDB JDBC URL for in-memory database
        final String url = "jdbc:hsqldb:mem:mydb";
        final String user = "SA"; // Default username
        final String password = ""; // Default password is empty

        try (Connection connection = DriverManager.getConnection(url, user, password);
             final Statement statement = connection.createStatement()) {

            // Step 1: Create a table
            final String createTableSQL = "CREATE TABLE blog (id INT PRIMARY KEY, blogentry VARCHAR(800), salary DECIMAL(10, 2))";
            statement.execute(createTableSQL);
            System.out.println("Table created successfully.");

            // Step 2: Insert data into the table
            final String insertDataSQL = "INSERT INTO blog (id, blogentry, salary) VALUES (1, 'Do it, go Atlanta', 50000.00), " +
                    "(2, 'More Atl #Atlanta', 60000.00), (3, 'Running it away, no snow', 55000.00)";
            statement.executeUpdate(insertDataSQL);
            System.out.println("Data inserted successfully.");

            // Step 3: Query the table
            String querySQL = "SELECT * FROM blog ";
            ResultSet resultSet = statement.executeQuery(querySQL);

            blogdata.clear();

            // Print the query results
            System.out.println("Employees:");
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String blogentry = resultSet.getString("blogentry");
                double salary = resultSet.getDouble("salary");

                blogdata.add(blogentry);
                System.out.printf("ID: %d, Name: %s, Salary: %.2f%n", id, blogentry, salary);
            }

            // Step 4: Drop the table (optional cleanup)
            String dropTableSQL = "DROP TABLE blog";
            statement.execute(dropTableSQL);
            System.out.println("Table dropped successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<String> data() {
        final List<String> stringList = new ArrayList<>(blogdata);
        return stringList;
    }

    public static String version() {
        System.out.println("Printing version...1.0.1");
        return "1.0.2";
    }
}
