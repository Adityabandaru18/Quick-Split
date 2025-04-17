package com.expensesplitter.service;

import com.expensesplitter.db.DatabaseHelper;
import com.expensesplitter.model.Expense;
import com.expensesplitter.model.Split;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class ExpenseService {
    private Connection connection;

    public ExpenseService() {
        this.connection = DatabaseHelper.getConnection();
    }


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


    public List<Split> getExpenseSplits(int expenseId) {
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


    public Expense getExpenseById(int expenseId) {
        String sql = "SELECT e.id, e.description, e.amount, e.created_at, u.username " +
                     "FROM expenses e JOIN users u ON e.created_by = u.id " +
                     "WHERE e.id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, expenseId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                Expense expense = new Expense();
                expense.setId(rs.getInt("id"));
                expense.setDescription(rs.getString("description"));
                expense.setAmount(rs.getDouble("amount"));
                expense.setCreatedAt(rs.getString("created_at"));
                expense.setCreatedBy(rs.getString("username"));
                
                // Get splits for this expense
                expense.setSplits(getExpenseSplits(expense.getId()));
                
                return expense;
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving expense: " + e.getMessage());
        }
        
        return null;
    }

    public boolean deleteExpense(int expenseId) {
        // Start a transaction
        try {
            connection.setAutoCommit(false);
            
            // First delete all splits for this expense
            String deleteSplits = "DELETE FROM splits WHERE expense_id = ?";
            PreparedStatement pstmt1 = connection.prepareStatement(deleteSplits);
            pstmt1.setInt(1, expenseId);
            pstmt1.executeUpdate();
            
            // Then delete the expense
            String deleteExpense = "DELETE FROM expenses WHERE id = ?";
            PreparedStatement pstmt2 = connection.prepareStatement(deleteExpense);
            pstmt2.setInt(1, expenseId);
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
            System.out.println("Error deleting expense: " + e.getMessage());
            return false;
        }
    }
}