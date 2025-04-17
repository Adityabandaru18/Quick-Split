package com.expensesplitter;

import com.expensesplitter.db.DatabaseManager;
import com.expensesplitter.ui.ConsoleUI;

public class Main {
    public static void main(String[] args) {

        DatabaseManager dbManager = new DatabaseManager();
        dbManager.initializeDatabase();
        
        ConsoleUI ui = new ConsoleUI(dbManager);
        ui.start();
    }
}