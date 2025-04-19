package com.expensesplitter.ui;

import com.expensesplitter.db.DatabaseManager;
import com.expensesplitter.model.Expense;
import com.expensesplitter.model.Split;
import com.expensesplitter.model.User;

import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class ConsoleUI {
    private final DatabaseManager dbManager;
    private final Scanner scanner;
    private User currentUser;
    private static final int CONSOLE_WIDTH = 80; // Assumed console width

    public ConsoleUI(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        boolean running = true;
        while (running) {
            if (currentUser == null) {
                showAuthMenu();
            } else {
                showMainMenu();
            }
        }
    }

    private void showAuthMenu() {
        clearScreen();
        displayMainTitle();
        displaySubtitle("AUTHENTICATION");
        System.out.println();
        System.out.println("                              1. Login");
        System.out.println("                              2. Register");
        System.out.println("                              3. Exit");
        System.out.println();
        System.out.print("                         Choose an option: ");
        
        int choice = getIntInput();
        switch (choice) {
            case 1:
                login();
                break;
            case 2:
                register();
                break;
            case 3:
                exit();
                break;
            default:
                System.out.println("                    Invalid option. Please try again.");
                sleep(1);
        }
    }

    private void showMainMenu() {
        clearScreen();
        displayMainTitle();
        displaySubtitle("MAIN MENU");
        System.out.println();
        System.out.println("                     Welcome, " + currentUser.getUsername() + "!");
        System.out.println();
        System.out.println("                         1. Add Expense");
        System.out.println("                         2. View Balances");
        System.out.println("                         3. Settle Debts");
        System.out.println("                         4. View Expense History");
        System.out.println("                         5. Logout");
        System.out.println("                         6. Exit");
        System.out.println();
        System.out.print("                    Choose an option: ");
        
        int choice = getIntInput();
        switch (choice) {
            case 1:
                addExpense();
                break;
            case 2:
                viewBalances();
                break;
            case 3:
                settleDebts();
                break;
            case 4:
                viewExpenseHistory();
                break;
            case 5:
                logout();
                break;
            case 6:
                exit();
                break;
            default:
                System.out.println("              Invalid option. Please try again.");
                sleep(1);
        }
    }

    private void login() {
        clearScreen();
        displayMainTitle();
        displaySubtitle("LOGIN");
        System.out.println();
        System.out.print("                         Username: ");
        String username = scanner.nextLine();
        System.out.print("                         Password: ");
        String password = scanner.nextLine();
        System.out.println();
        
        User user = dbManager.authenticateUser(username, password);
        if (user != null) {
            currentUser = user;
            System.out.println("                      Login successful!");
            sleep(1);
        } else {
            System.out.println("             Invalid username or password. Please try again.");
            sleep(2);
        }
    }

    private void register() {
        clearScreen();
        displayMainTitle();
        displaySubtitle("REGISTER");
        System.out.println();
        System.out.print("                         Username: ");
        String username = scanner.nextLine();
        System.out.print("                         Password: ");
        String password = scanner.nextLine();
        System.out.print("                         Email: ");
        String email = scanner.nextLine();
        System.out.println();
        
        boolean success = dbManager.registerUser(username, password, email);
        if (success) {
            System.out.println("               Registration successful! Please login.");
            sleep(2);
        } else {
            System.out.println("      Registration failed. Username or email may already be in use.");
            sleep(2);
        }
    }

    private void addExpense() {
        clearScreen();
        displayMainTitle();
        displaySubtitle("ADD EXPENSE");
        
        // Get expense details
        System.out.print("                       Expense description: ");
        String description = scanner.nextLine();
        
        System.out.print("                       Amount: $");
        double amount = getDoubleInput();
        
        // Split type
        System.out.println("                       Split type:");
        System.out.println("                       1. Equal Split");
        System.out.println("                       2. Custom Split");
        System.out.print("                       Choose an option: ");
        int splitType = getIntInput();
        
        System.out.print("                       Number of users involved (including you): ");
        int numUsers = getIntInput();
        
        if (numUsers < 2) {
            System.out.println("                       At least 2 users are required for splitting expenses.");
            sleep(2);
            return;
        }
        
        // Add splits
        double[] splits = new double[numUsers];
        String[] usernames = new String[numUsers];
        int[] userIds = new int[numUsers];
        
        // First user is always the current user
        usernames[0] = currentUser.getUsername();
        userIds[0] = currentUser.getId();
        
        if (splitType == 1) {
            // Equal split
            double splitAmount = amount / numUsers;
            for (int i = 0; i < numUsers; i++) {
                splits[i] = splitAmount;
            }
            
            // Get other users
            for (int i = 1; i < numUsers; i++) {
                boolean validUser = false;
                while (!validUser) {
                    System.out.print("                       Enter username " + (i + 1) + ": ");
                    String username = scanner.nextLine();
                    
                    User user = dbManager.getUserByUsername(username);
                    if (user != null) {
                        usernames[i] = username;
                        userIds[i] = user.getId();
                        validUser = true;
                    } else {
                        System.out.println("                       User not found. Please try again.");
                    }
                }
            }
        } else if (splitType == 2) {
            // Custom split
            // Get other users and their splits
            double totalSplit = 0;
            
            for (int i = 0; i < numUsers; i++) {
                if (i == 0) {
                    System.out.print("                       Amount for " + currentUser.getUsername() + ": $");
                } else {
                    boolean validUser = false;
                    while (!validUser) {
                        System.out.print("                       Enter username " + (i + 1) + ": ");
                        String username = scanner.nextLine();
                        
                        User user = dbManager.getUserByUsername(username);
                        if (user != null) {
                            usernames[i] = username;
                            userIds[i] = user.getId();
                            validUser = true;
                        } else {
                            System.out.println("                       User not found. Please try again.");
                        }
                    }
                    
                    System.out.print("                       Amount for " + usernames[i] + ": $");
                }
                
                splits[i] = getDoubleInput();
                totalSplit += splits[i];
            }
            
            // Validate total split with a small tolerance for floating-point errors
            if (Math.abs(totalSplit - amount) > 0.01) {
                System.out.println("\n                       Error: Total split ($" + totalSplit + ") does not match expense amount ($" + amount + ")");
                
                // Give user options to fix the issue
                System.out.println("\n                       Options:");
                System.out.println("                       1. Adjust the split automatically");
                System.out.println("                       2. Re-enter expense details");
                System.out.println("                       3. Cancel expense");
                System.out.print("                       Choose an option: ");
                int choice = getIntInput();
                
                if (choice == 1) {
                    // Adjust the split proportionally
                    double adjustment = amount / totalSplit;
                    for (int i = 0; i < numUsers; i++) {
                        splits[i] = Math.round(splits[i] * adjustment * 100) / 100.0; // Round to 2 decimal places
                    }
                    
                    // Display adjusted splits
                    System.out.println("\n                       Adjusted splits:");
                    double adjustedTotal = 0;
                    for (int i = 0; i < numUsers; i++) {
                        System.out.printf("                       %s: $%.2f\n", usernames[i], splits[i]);
                        adjustedTotal += splits[i];
                    }
                    
                    // Handle any remaining cents due to rounding
                    if (Math.abs(adjustedTotal - amount) > 0.01) {
                        double diff = amount - adjustedTotal;
                        splits[0] += diff;
                        System.out.printf("                       Added $%.2f to %s's share to fix rounding\n", diff, usernames[0]);
                    }
                    
                    System.out.printf("                       Total: $%.2f\n", amount);
                } else if (choice == 2) {
                    addExpense(); // Restart expense entry
                    return;
                } else {
                    System.out.println("                       Expense cancelled.");
                    sleep(1);
                    return;
                }
            }
        } else {
            System.out.println("                       Invalid split type selected.");
            sleep(1);
            return;
        }
        
        // Now create the expense in the database
        int expenseId = dbManager.addExpense(description, amount, currentUser.getId());
        if (expenseId == -1) {
            System.out.println("                       Failed to create expense. Please try again.");
            sleep(2);
            return;
        }
        
        // Save splits to database
        boolean allSplitsAdded = true;
        for (int i = 0; i < numUsers; i++) {
            // Skip adding a split for the expense creator for equal splits
            if (splitType == 1 && i == 0) {
                continue;
            }
            
            boolean success = dbManager.addSplit(expenseId, userIds[i], splits[i]);
            if (!success) {
                allSplitsAdded = false;
            }
        }
        
        if (allSplitsAdded) {
            System.out.println("                       Expense added successfully!");
        } else {
            System.out.println("                       Some splits could not be added. Please check your expense history.");
        }
        sleep(2);
    }
    
    private void viewBalances() {
        clearScreen();
        displayMainTitle();
        displaySubtitle("BALANCES");
        
        Map<String, Double> balances = dbManager.getUserBalances(currentUser.getId());
        
        if (balances.isEmpty()) {
            System.out.println("                       You don't have any outstanding balances.");
            System.out.println("\n                       Press Enter to continue...");
            scanner.nextLine();
            return;
        }
        
        System.out.println("                       Your current balances:");
        System.out.println();
        for (Map.Entry<String, Double> entry : balances.entrySet()) {
            String username = entry.getKey();
            double amount = entry.getValue();
            
            if (amount > 0) {
                System.out.printf("                       %s owes you $%.2f\n", username, amount);
            } else if (amount < 0) {
                System.out.printf("                       You owe %s $%.2f\n", username, Math.abs(amount));
            }
        }
        
        System.out.println("\n                       Press Enter to continue...");
        scanner.nextLine();
    }

    private void settleDebts() {
        clearScreen();
        displayMainTitle();
        displaySubtitle("SETTLE DEBTS");
        
        Map<String, Double> balances = dbManager.getUserBalances(currentUser.getId());
        
        if (balances.isEmpty()) {
            System.out.println("                       You don't have any outstanding balances to settle.");
            sleep(2);
            return;
        }
        
        // Show debts that the current user owes to others
        boolean hasDebts = false;
        System.out.println("                       Your debts:");
        System.out.println();
        int index = 1;
        String[] usernames = new String[balances.size()];
        double[] amounts = new double[balances.size()];
        int count = 0;
        
        for (Map.Entry<String, Double> entry : balances.entrySet()) {
            String username = entry.getKey();
            double amount = entry.getValue();
            
            if (amount < 0) {
                usernames[count] = username;
                amounts[count] = Math.abs(amount);
                System.out.printf("                       %d. You owe %s $%.2f\n", index++, username, Math.abs(amount));
                hasDebts = true;
                count++;
            }
        }
        
        if (!hasDebts) {
            System.out.println("                       You don't have any debts to settle.");
            sleep(2);
            return;
        }
        
        System.out.println();
        System.out.print("                       Select a debt to settle (0 to cancel): ");
        int choice = getIntInput();
        
        if (choice == 0 || choice > count) {
            System.out.println("                       Operation cancelled or invalid selection.");
            sleep(1);
            return;
        }
        
        String selectedUsername = usernames[choice - 1];
        double selectedAmount = amounts[choice - 1];
        
        System.out.printf("                       You are about to settle $%.2f with %s\n", selectedAmount, selectedUsername);
        System.out.print("                       Confirm? (Y/N): ");
        String confirm = scanner.nextLine();
        
        if (confirm.equalsIgnoreCase("Y")) {
            User receiver = dbManager.getUserByUsername(selectedUsername);
            boolean success = dbManager.settleDebt(currentUser.getId(), receiver.getId(), selectedAmount);
            
            if (success) {
                System.out.println("                       Debt settled successfully!");
            } else {
                System.out.println("                       Failed to settle debt. Please try again.");
            }
            sleep(2);
        } else {
            System.out.println("                       Operation cancelled.");
            sleep(1);
        }
    }

    private void viewExpenseHistory() {
        clearScreen();
        displayMainTitle();
        displaySubtitle("EXPENSE HISTORY");
        
        List<Expense> expenses = dbManager.getUserExpenses(currentUser.getId());
        
        if (expenses.isEmpty()) {
            System.out.println("                       You don't have any expenses yet.");
            System.out.println("\n                       Press Enter to continue...");
            scanner.nextLine();
            return;
        }
        
        for (Expense expense : expenses) {
            System.out.println("\n                       Expense: " + expense.getDescription());
            System.out.printf("                       Amount: $%.2f\n", expense.getAmount());
            System.out.println("                       Date: " + expense.getCreatedAt());
            System.out.println("                       Created by: " + expense.getCreatedBy());
            
            System.out.println("                       Splits:");
            for (Split split : expense.getSplits()) {
                System.out.printf("                       - %s: $%.2f (%s)\n", 
                    split.getUsername(), 
                    split.getAmount(), 
                    split.isPaid() ? "Paid" : "Unpaid");
            }
            System.out.println("                       ----------------------------------------");
        }
        
        System.out.println("\n                       Press Enter to continue...");
        scanner.nextLine();
    }

    private void logout() {
        currentUser = null;
        System.out.println("Logged out successfully.");
        sleep(1);
    }

    private void exit() {
        clearScreen();
        displayMainTitle();
        System.out.println();
        System.out.println();
        System.out.println("           Thank you for using Expense Splitter. Goodbye!");
        System.out.println();
        dbManager.closeConnection();
        sleep(2);
        System.exit(0);
    }

    private int getIntInput() {
        while (true) {
            try {
                String input = scanner.nextLine();
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.print("Please enter a valid number: ");
            }
        }
    }

    private double getDoubleInput() {
        while (true) {
            try {
                String input = scanner.nextLine();
                return Double.parseDouble(input);
            } catch (NumberFormatException e) {
                System.out.print("Please enter a valid number: ");
            }
        }
    }
    
    private void clearScreen() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            // If clearing doesn't work, add some newlines as a fallback
            System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
        }
    }
    
    private void sleep(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void displayMainTitle() {
        System.out.println();
        System.out.println();
        System.out.println("       ██████╗ ██╗   ██╗██╗ ██████╗██╗  ██╗");
        System.out.println("      ██╔═══██╗██║   ██║██║██╔════╝██║ ██╔╝");
        System.out.println("      ██║   ██║██║   ██║██║██║     █████╔╝ ");
        System.out.println("      ██║▄▄ ██║██║   ██║██║██║     ██╔═██╗ ");
        System.out.println("      ╚██████╔╝╚██████╔╝██║╚██████╗██║  ██╗");
        System.out.println("       ╚══▀▀═╝  ╚═════╝ ╚═╝ ╚═════╝╚═╝  ╚═╝");
        System.out.println("                ███████╗██████╗ ██╗     ██╗████████╗");
        System.out.println("                ██╔════╝██╔══██╗██║     ██║╚══██╔══╝");
        System.out.println("                ███████╗██████╔╝██║     ██║   ██║   ");
        System.out.println("                ╚════██║██╔═══╝ ██║     ██║   ██║   ");
        System.out.println("                ███████║██║     ███████╗██║   ██║   ");
        System.out.println("                ╚══════╝╚═╝     ╚══════╝╚═╝   ╚═╝   ");
        System.out.println();
    }
    private void displaySubtitle(String subtitle) {
        String formattedSubtitle = "★ " + subtitle + " ★";
        int padding = (CONSOLE_WIDTH - formattedSubtitle.length()) / 2;
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < padding; i++) {
            sb.append(" ");
        }
        
        sb.append(formattedSubtitle);
        System.out.println(sb.toString());
        System.out.println();
    }
}