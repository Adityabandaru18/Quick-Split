package com.expensesplitter.model;

/**
 * Model class representing a split of an expense between users
 */
public class Split {
    private int id;
    private int expenseId;
    private int userId;
    private String username;
    private double amount;
    private boolean isPaid;

    public Split() {}

    public Split(int userId, String username, double amount) {
        this.userId = userId;
        this.username = username;
        this.amount = amount;
        this.isPaid = false;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getExpenseId() {
        return expenseId;
    }

    public void setExpenseId(int expenseId) {
        this.expenseId = expenseId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public boolean isPaid() {
        return isPaid;
    }

    public void setPaid(boolean paid) {
        isPaid = paid;
    }

    @Override
    public String toString() {
        return "Split{" +
                "id=" + id +
                ", expenseId=" + expenseId +
                ", userId=" + userId +
                ", username='" + username + '\'' +
                ", amount=" + amount +
                ", isPaid=" + isPaid +
                '}';
    }
}