package com.expensesplitter.service;

import com.expensesplitter.db.DatabaseHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service class for balance-related operations
 */
public class BalanceService {
    private Connection connection;

    public BalanceService() {
        this.connection = DatabaseHelper.getConnection();
    }

    /**
     * Get the balances for a user
     * @param userId User ID
     * @return Map of username to balance amount (positive: others owe user, negative: user owes others)
     */
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

    /**
     * Settle a debt between two users
     * @param payerId User ID who is paying
     * @param receiverId User ID who is receiving
     * @param amount Amount being settled
     * @return true if successful, false otherwise
     */
    public boolean settleDebt(int payerId, int receiverId, double amount) {
        // Start a transaction
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

    /**
     * Get all settlement history for a user
     * @param userId User ID
     * @return Map with two lists: "paid" (settlements where user paid) and "received" (settlements where user received)
     */
    public Map<String, List<Map<String, Object>>> getSettlementHistory(int userId) {
        Map<String, List<Map<String, Object>>> history = new HashMap<>();
        history.put("paid", new ArrayList<>());
        history.put("received", new ArrayList<>());
        
        // Get settlements where user paid
        String sql1 = "SELECT s.id, s.amount, s.settled_at, u.username as receiver " +
                     "FROM settlements s " +
                     "JOIN users u ON s.receiver_id = u.id " +
                     "WHERE s.payer_id = ? " +
                     "ORDER BY s.settled_at DESC";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql1)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> settlement = new HashMap<>();
                settlement.put("id", rs.getInt("id"));
                settlement.put("amount", rs.getDouble("amount"));
                settlement.put("settled_at", rs.getString("settled_at"));
                settlement.put("other_user", rs.getString("receiver"));
                history.get("paid").add(settlement);
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving paid settlements: " + e.getMessage());
        }
        
        // Get settlements where user received
        String sql2 = "SELECT s.id, s.amount, s.settled_at, u.username as payer " +
                     "FROM settlements s " +
                     "JOIN users u ON s.payer_id = u.id " +
                     "WHERE s.receiver_id = ? " +
                     "ORDER BY s.settled_at DESC";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql2)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> settlement = new HashMap<>();
                settlement.put("id", rs.getInt("id"));
                settlement.put("amount", rs.getDouble("amount"));
                settlement.put("settled_at", rs.getString("settled_at"));
                settlement.put("other_user", rs.getString("payer"));
                history.get("received").add(settlement);
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving received settlements: " + e.getMessage());
        }
        
        return history;
    }
}