package com.expensesplitter.db;

import java.sql.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.expensesplitter.model.Expense;
import com.expensesplitter.model.Split;
import com.expensesplitter.model.User;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:data/expensesplitter.db";
    private Connection connection;

    public DatabaseManager() {
        try {

            connection = DriverManager.getConnection(DB_URL);
            System.out.println("Connected to the database successfully.");
        } catch (SQLException e) {
            System.out.println("Failed to connect to the database: " + e.getMessage());
        }
    }

    public void initializeDatabase() {
        try {

            InputStream inputStream = getClass().getResourceAsStream("/schema.sql");
            if (inputStream == null) {
                createTablesDirectly();
                return;
            }
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String schema = reader.lines().collect(Collectors.joining("\n"));
                

                Statement statement = connection.createStatement();
                statement.executeUpdate(schema);
                statement.close();
                System.out.println("Database schema initialized successfully.");
            } catch (IOException e) {
                System.out.println("Error reading schema file: " + e.getMessage());
                createTablesDirectly();
            }
        } catch (SQLException e) {
            System.out.println("Failed to initialize database schema: " + e.getMessage());
        }
    }
    
    private void createTablesDirectly() throws SQLException {
        Statement statement = connection.createStatement();
        
        // Users table
        statement.executeUpdate(
            "CREATE TABLE IF NOT EXISTS users (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "username TEXT UNIQUE NOT NULL," +
            "password TEXT NOT NULL," +
            "email TEXT UNIQUE," +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")"
        );
        
        // Expenses table
        statement.executeUpdate(
            "CREATE TABLE IF NOT EXISTS expenses (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "description TEXT NOT NULL," +
            "amount REAL NOT NULL," +
            "created_by INTEGER NOT NULL," +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "FOREIGN KEY (created_by) REFERENCES users(id)" +
            ")"
        );
        
        // Splits table
        statement.executeUpdate(
            "CREATE TABLE IF NOT EXISTS splits (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "expense_id INTEGER NOT NULL," +
            "user_id INTEGER NOT NULL," +
            "amount REAL NOT NULL," +
            "is_paid BOOLEAN DEFAULT 0," +
            "FOREIGN KEY (expense_id) REFERENCES expenses(id)," +
            "FOREIGN KEY (user_id) REFERENCES users(id)" +
            ")"
        );
        
        // Settlements table
        statement.executeUpdate(
            "CREATE TABLE IF NOT EXISTS settlements (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "payer_id INTEGER NOT NULL," +
            "receiver_id INTEGER NOT NULL," +
            "amount REAL NOT NULL," +
            "settled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "FOREIGN KEY (payer_id) REFERENCES users(id)," +
            "FOREIGN KEY (receiver_id) REFERENCES users(id)" +
            ")"
        );
        
        statement.close();
        System.out.println("Database schema created directly successfully.");
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            System.out.println("Error closing database connection: " + e.getMessage());
        }
    }

    // User Operations
    public boolean registerUser(String username, String password, String email) {
        String sql = "INSERT INTO users (username, password, email) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password); // In a real app, hash this password
            pstmt.setString(3, email);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Registration failed: " + e.getMessage());
            return false;
        }
    }

    public User authenticateUser(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password); // In a real app, verify against hashed password
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setEmail(rs.getString("email"));
                return user;
            }
        } catch (SQLException e) {
            System.out.println("Authentication error: " + e.getMessage());
        }
        return null;
    }

    public User getUserByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setEmail(rs.getString("email"));
                return user;
            }
        } catch (SQLException e) {
            System.out.println("Error finding user: " + e.getMessage());
        }
        return null;
    }

    // Expense Operations
    public int addExpense(String description, double amount, int createdById) {
        String sql = "INSERT INTO expenses (description, amount, created_by) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, description);
            pstmt.setDouble(2, amount);
            pstmt.setInt(3, createdById);
            pstmt.executeUpdate();
            
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.out.println("Failed to add expense: " + e.getMessage());
        }
        return -1;
    }

    public boolean addSplit(int expenseId, int userId, double amount) {
        String sql = "INSERT INTO splits (expense_id, user_id, amount) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, expenseId);
            pstmt.setInt(2, userId);
            pstmt.setDouble(3, amount);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Failed to add split: " + e.getMessage());
            return false;
        }
    }

    public List<Expense> getUserExpenses(int userId) {
        String sql = "SELECT e.id, e.description, e.amount, e.created_at, u.username " +
                     "FROM expenses e JOIN users u ON e.created_by = u.id " +
                     "WHERE e.created_by = ? ORDER BY e.created_at DESC";
        List<Expense> expenses = new ArrayList<>();
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Expense expense = new Expense();
                expense.setId(rs.getInt("id"));
                expense.setDescription(rs.getString("description"));
                expense.setAmount(rs.getDouble("amount"));
                expense.setCreatedAt(rs.getString("created_at"));
                expense.setCreatedBy(rs.getString("username"));
                
                // Get splits for this expense
                expense.setSplits(getExpenseSplits(expense.getId()));
                
                expenses.add(expense);
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving expenses: " + e.getMessage());
        }
        
        return expenses;
    }

    private List<Split> getExpenseSplits(int expenseId) {
        String sql = "SELECT s.id, s.user_id, s.amount, s.is_paid, u.username " +
                     "FROM splits s JOIN users u ON s.user_id = u.id " +
                     "WHERE s.expense_id = ?";
        List<Split> splits = new ArrayList<>();
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, expenseId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Split split = new Split();
                split.setId(rs.getInt("id"));
                split.setUserId(rs.getInt("user_id"));
                split.setUsername(rs.getString("username"));
                split.setAmount(rs.getDouble("amount"));
                split.setPaid(rs.getBoolean("is_paid"));
                splits.add(split);
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving splits: " + e.getMessage());
        }
        
        return splits;
    }

    // Balance Operations
    public Map<String, Double> getUserBalances(int userId) {
        Map<String, Double> balances = new HashMap<>();
        
        // Get what others owe the user
        String sql1 = "SELECT u.username, SUM(s.amount) as amount " +
                     "FROM splits s " +
                     "JOIN expenses e ON s.expense_id = e.id " +
                     "JOIN users u ON s.user_id = u.id " +
                     "WHERE e.created_by = ? AND s.user_id != ? AND s.is_paid = 0 " +
                     "GROUP BY s.user_id";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql1)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                balances.put(rs.getString("username"), rs.getDouble("amount"));
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving balances (owed to user): " + e.getMessage());
        }
        
        // Get what the user owes to others
        String sql2 = "SELECT u.username, SUM(s.amount) as amount " +
                     "FROM splits s " +
                     "JOIN expenses e ON s.expense_id = e.id " +
                     "JOIN users u ON e.created_by = u.id " +
                     "WHERE s.user_id = ? AND e.created_by != ? AND s.is_paid = 0 " +
                     "GROUP BY e.created_by";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql2)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                String username = rs.getString("username");
                double amount = rs.getDouble("amount");
                
                if (balances.containsKey(username)) {
                    balances.put(username, balances.get(username) - amount);
                } else {
                    balances.put(username, -amount);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving balances (user owes): " + e.getMessage());
        }
        
        return balances;
    }

    // Settlement Operations
    public boolean settleDebt(int payerId, int receiverId, double amount) {
        try {
            connection.setAutoCommit(false);
            
            // Record the settlement
            String sql1 = "INSERT INTO settlements (payer_id, receiver_id, amount) VALUES (?, ?, ?)";
            PreparedStatement pstmt1 = connection.prepareStatement(sql1);
            pstmt1.setInt(1, payerId);
            pstmt1.setInt(2, receiverId);
            pstmt1.setDouble(3, amount);
            pstmt1.executeUpdate();
            
            // Update splits where user owes to the receiver
            String sql2 = "UPDATE splits SET is_paid = 1 " +
                         "WHERE user_id = ? AND expense_id IN (SELECT id FROM expenses WHERE created_by = ?) " +
                         "AND is_paid = 0";
            PreparedStatement pstmt2 = connection.prepareStatement(sql2);
            pstmt2.setInt(1, payerId);
            pstmt2.setInt(2, receiverId);
            pstmt2.executeUpdate();
            
            connection.commit();
            connection.setAutoCommit(true);
            return true;
        } catch (SQLException e) {
            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (SQLException ex) {
                System.out.println("Error during rollback: " + ex.getMessage());
            }
            System.out.println("Error settling debt: " + e.getMessage());
            return false;
        }
    }
}