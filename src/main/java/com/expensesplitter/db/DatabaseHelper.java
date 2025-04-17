package com.expensesplitter.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.File;


public class DatabaseHelper {
    private static final String DB_URL = "jdbc:sqlite:data/expensesplitter.db";
    private static Connection connection;

    public static Connection getConnection() {
        if (connection == null) {
            try {
                // Ensure the data directory exists
                File dataDir = new File("data");
                if (!dataDir.exists()) {
                    dataDir.mkdirs();
                }
                
                connection = DriverManager.getConnection(DB_URL);
                System.out.println("Connection to SQLite has been established.");
                
                Statement stmt = connection.createStatement();
                stmt.execute("PRAGMA foreign_keys = ON");
                stmt.execute("PRAGMA journal_mode = WAL");
                stmt.close();
                
                return connection;
            } catch (SQLException e) {
                System.out.println("Error connecting to database: " + e.getMessage());
                return null;
            }
        }
        return connection;
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                connection = null;
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            System.out.println("Error closing database connection: " + e.getMessage());
        }
    }
}