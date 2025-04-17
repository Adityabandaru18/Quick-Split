package com.expensesplitter.model;

/**
 * Model class representing a settlement transaction between users
 */
public class Transaction {
    private int id;
    private int payerId;
    private String payerName;
    private int receiverId;
    private String receiverName;
    private double amount;
    private String settledAt;

    public Transaction() {}

    public Transaction(int payerId, String payerName, int receiverId, String receiverName, double amount) {
        this.payerId = payerId;
        this.payerName = payerName;
        this.receiverId = receiverId;
        this.receiverName = receiverName;
        this.amount = amount;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPayerId() {
        return payerId;
    }

    public void setPayerId(int payerId) {
        this.payerId = payerId;
    }

    public String getPayerName() {
        return payerName;
    }

    public void setPayerName(String payerName) {
        this.payerName = payerName;
    }

    public int getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(int receiverId) {
        this.receiverId = receiverId;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getSettledAt() {
        return settledAt;
    }

    public void setSettledAt(String settledAt) {
        this.settledAt = settledAt;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", payerId=" + payerId +
                ", payerName='" + payerName + '\'' +
                ", receiverId=" + receiverId +
                ", receiverName='" + receiverName + '\'' +
                ", amount=" + amount +
                ", settledAt='" + settledAt + '\'' +
                '}';
    }
}