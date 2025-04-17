package com.expensesplitter.model;

import java.util.ArrayList;
import java.util.List;


public class Expense {
    private int id;
    private String description;
    private double amount;
    private String createdAt;
    private String createdBy;
    private int createdById;
    private List<Split> splits;

    public Expense() {
        this.splits = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public int getCreatedById() {
        return createdById;
    }

    public void setCreatedById(int createdById) {
        this.createdById = createdById;
    }

    public List<Split> getSplits() {
        return splits;
    }

    public void setSplits(List<Split> splits) {
        this.splits = splits;
    }

    public void addSplit(Split split) {
        this.splits.add(split);
    }

    /**
     * Calculate the total of all splits
     * @return Sum of all split amounts
     */
    public double getTotalSplitAmount() {
        double total = 0;
        for (Split split : splits) {
            total += split.getAmount();
        }
        return total;
    }

    @Override
    public String toString() {
        return "Expense{" +
                "id=" + id +
                ", description='" + description + '\'' +
                ", amount=" + amount +
                ", createdAt='" + createdAt + '\'' +
                ", createdBy='" + createdBy + '\'' +
                ", splits=" + splits +
                '}';
    }
}