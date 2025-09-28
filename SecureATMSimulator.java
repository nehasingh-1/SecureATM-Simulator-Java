import java.awt.*;
import java.io.*;
import java.util.HashMap;
import javax.swing.*;

class Account implements Serializable {
    private static final long serialVersionUID = 1L;
    String accountNumber;
    String pin;
    double balance;

    Account(String accountNumber, String pin, double balance) {
        this.accountNumber = accountNumber;
        this.pin = pin;
        this.balance = balance;
    }

    boolean validatePin(String inputPin) {
        return pin.equals(inputPin);
    }

    public String toString() {
        return "Acc#: " + accountNumber + ", Balance: $" + String.format("%.2f", balance);
    }
}

public class SecureATMSimulator extends JFrame {
    private final HashMap<String, Account> accounts = new HashMap<>();
    private Account currentAccount = null;

    private CardLayout cardLayout = new CardLayout();

    // Login panel components
    private JTextField accountField;
    private JPasswordField pinField;
    private JButton loginButton, adminButton;

    // Main user panel components
    private JLabel balanceLabel;
    private JButton depositButton, withdrawButton, logoutButton;

    // Admin panel components
    private DefaultListModel<Account> accountListModel;
    private JList<Account> accountJList;
    private JButton addAccountButton, deleteAccountButton, modifyAccountButton, backButton;

    private static final String ACCOUNTS_FILE = "accounts.ser";

    public SecureATMSimulator() {
        loadAccounts();

        if (accounts.isEmpty()) {
            // Add default accounts if none loaded
            accounts.put("12345", new Account("12345", "1111", 1000));
            accounts.put("67890", new Account("67890", "2222", 500));
        }

        setTitle("ATM Simulation Extended with Persistence");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 400);
        setLocationRelativeTo(null);
        setLayout(cardLayout);

        // ----------------- Login Panel -----------------
        JPanel loginPanel = new JPanel(new GridLayout(4, 2, 5, 5));
        loginPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        loginPanel.add(new JLabel("Account Number:"));
        accountField = new JTextField();
        loginPanel.add(accountField);
        loginPanel.add(new JLabel("PIN:"));
        pinField = new JPasswordField();
        loginPanel.add(pinField);
        loginButton = new JButton("Login");
        adminButton = new JButton("Admin Panel");
        loginPanel.add(loginButton);
        loginPanel.add(adminButton);

        add(loginPanel, "login");

        // --------------- User Panel -------------------
        JPanel userPanel = new JPanel();
        userPanel.setLayout(new BoxLayout(userPanel, BoxLayout.Y_AXIS));
        balanceLabel = new JLabel("Balance: ");
        balanceLabel.setFont(new Font("Arial", Font.BOLD, 16));
        balanceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        userPanel.add(Box.createVerticalStrut(20));
        userPanel.add(balanceLabel);
        userPanel.add(Box.createVerticalStrut(20));
        depositButton = new JButton("Deposit");
        withdrawButton = new JButton("Withdraw");
        logoutButton = new JButton("Logout");
        depositButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        withdrawButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        logoutButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        userPanel.add(depositButton);
        userPanel.add(Box.createVerticalStrut(10));
        userPanel.add(withdrawButton);
        userPanel.add(Box.createVerticalStrut(10));
        userPanel.add(logoutButton);

        add(userPanel, "user");

        // --------------- Admin Panel -------------------
        JPanel adminPanel = new JPanel(new BorderLayout(10, 10));
        adminPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        accountListModel = new DefaultListModel<>();
        refreshAccountList();
        accountJList = new JList<>(accountListModel);
        accountJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        adminPanel.add(new JScrollPane(accountJList), BorderLayout.CENTER);

        JPanel adminButtonsPanel = new JPanel();
        addAccountButton = new JButton("Add Account");
        deleteAccountButton = new JButton("Delete Account");
        modifyAccountButton = new JButton("Modify Account");
        backButton = new JButton("Back to Login");

        adminButtonsPanel.add(addAccountButton);
        adminButtonsPanel.add(deleteAccountButton);
        adminButtonsPanel.add(modifyAccountButton);
        adminButtonsPanel.add(backButton);

        adminPanel.add(adminButtonsPanel, BorderLayout.SOUTH);

        add(adminPanel, "admin");

        // ----------- Listeners ------------------

        loginButton.addActionListener(e -> {
            String accNum = accountField.getText().trim();
            String pin = new String(pinField.getPassword()).trim();

            Account acc = accounts.get(accNum);
            if (acc != null && acc.validatePin(pin)) {
                currentAccount = acc;
                updateBalance();
                cardLayout.show(getContentPane(), "user");
                clearLoginFields();
            } else {
                JOptionPane.showMessageDialog(this, "Invalid account number or PIN.");
            }
        });

        adminButton.addActionListener(e -> {
            refreshAccountList();
            cardLayout.show(getContentPane(), "admin");
        });

        depositButton.addActionListener(e -> {
            String input = JOptionPane.showInputDialog(this, "Enter deposit amount:");
            try {
                double amount = Double.parseDouble(input);
                if (amount > 0) {
                    currentAccount.balance += amount;
                    updateBalance();
                    saveAccounts();
                    JOptionPane.showMessageDialog(this, "Deposit successful.");
                } else {
                    JOptionPane.showMessageDialog(this, "Enter a valid amount.");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid number format.");
            }
        });

        withdrawButton.addActionListener(e -> {
            String input = JOptionPane.showInputDialog(this, "Enter withdrawal amount:");
            try {
                double amount = Double.parseDouble(input);
                if (amount > 0 && currentAccount.balance >= amount) {
                    currentAccount.balance -= amount;
                    updateBalance();
                    saveAccounts();
                    JOptionPane.showMessageDialog(this, "Withdrawal successful.");
                } else {
                    JOptionPane.showMessageDialog(this, "Insufficient funds or invalid amount.");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid number format.");
            }
        });

        logoutButton.addActionListener(e -> {
            currentAccount = null;
            cardLayout.show(getContentPane(), "login");
        });

        addAccountButton.addActionListener(e -> {
            JTextField accNumField = new JTextField();
            JTextField pinField = new JTextField();
            JTextField balanceField = new JTextField();

            Object[] message = {
                "Account Number:", accNumField,
                "PIN:", pinField,
                "Initial Balance:", balanceField
            };

            int option = JOptionPane.showConfirmDialog(this, message, "Add New Account", JOptionPane.OK_CANCEL_OPTION);
            if (option == JOptionPane.OK_OPTION) {
                String accNum = accNumField.getText().trim();
                String pin = pinField.getText().trim();
                try {
                    double balance = Double.parseDouble(balanceField.getText().trim());
                    if (!accNum.isEmpty() && !pin.isEmpty() && balance >= 0) {
                        if (accounts.containsKey(accNum)) {
                            JOptionPane.showMessageDialog(this, "Account number already exists.");
                        } else {
                            accounts.put(accNum, new Account(accNum, pin, balance));
                            refreshAccountList();
                            saveAccounts();
                            JOptionPane.showMessageDialog(this, "Account added successfully.");
                        }
                    } else {
                        JOptionPane.showMessageDialog(this, "Please enter valid details.");
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid balance amount.");
                }
            }
        });

        deleteAccountButton.addActionListener(e -> {
            int index = accountJList.getSelectedIndex();
            if (index >= 0) {
                Account acc = accountListModel.getElementAt(index);
                int confirm = JOptionPane.showConfirmDialog(this, "Delete account " + acc.accountNumber + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    accounts.remove(acc.accountNumber);
                    refreshAccountList();
                    saveAccounts();
                }
            } else {
                JOptionPane.showMessageDialog(this, "No account selected.");
            }
        });

        modifyAccountButton.addActionListener(e -> {
            int index = accountJList.getSelectedIndex();
            if (index >= 0) {
                Account acc = accountListModel.getElementAt(index);

                JTextField pinField = new JTextField(acc.pin);
                JTextField balanceField = new JTextField(String.valueOf(acc.balance));

                Object[] message = {
                    "PIN:", pinField,
                    "Balance:", balanceField
                };

                int option = JOptionPane.showConfirmDialog(this, message, "Modify Account " + acc.accountNumber, JOptionPane.OK_CANCEL_OPTION);
                if (option == JOptionPane.OK_OPTION) {
                    String newPin = pinField.getText().trim();
                    try {
                        double newBalance = Double.parseDouble(balanceField.getText().trim());
                        if (!newPin.isEmpty() && newBalance >= 0) {
                            acc.pin = newPin;
                            acc.balance = newBalance;
                            refreshAccountList();
                            saveAccounts();
                        } else {
                            JOptionPane.showMessageDialog(this, "Invalid values entered.");
                        }
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(this, "Invalid balance amount.");
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "No account selected.");
            }
        });

        backButton.addActionListener(e -> {
            cardLayout.show(getContentPane(), "login");
        });
    }

    private void updateBalance() {
        balanceLabel.setText("Balance: $" + String.format("%.2f", currentAccount.balance));
    }

    private void clearLoginFields() {
        accountField.setText("");
        pinField.setText("");
    }

    private void refreshAccountList() {
        accountListModel.clear();
        accounts.values().forEach(accountListModel::addElement);
    }

    private void saveAccounts() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(ACCOUNTS_FILE))) {
            out.writeObject(accounts);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error saving accounts: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void loadAccounts() {
        File f = new File(ACCOUNTS_FILE);
        if (!f.exists()) return;
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(f))) {
            HashMap<String, Account> loaded = (HashMap<String, Account>) in.readObject();
            accounts.clear();
            accounts.putAll(loaded);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading accounts: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SecureATMSimulator app = new SecureATMSimulator();
            app.setVisible(true);
        });
    }
}
