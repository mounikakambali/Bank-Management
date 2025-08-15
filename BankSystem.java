import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

class Account implements Serializable {
    String accountNumber;
    String name;
    double balance;
    String pinHash; // Hashed PIN for security

    public Account(String accountNumber, String name, double balance, String pin) {
        this.accountNumber = accountNumber;
        this.name = name;
        this.balance = balance;
        this.pinHash = hashPin(pin);
    }

    public boolean checkPin(String pin) {
        return pinHash.equals(hashPin(pin));
    }

    private String hashPin(String pin) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(pin.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}

public class BankSystem {
    static final String FILE_NAME = "accounts.dat";
    static final String TRANSACTION_FILE = "transactions.txt";
    static List<Account> accounts = new ArrayList<>();

    public static void main(String[] args) {
        loadAccounts();
        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.println("\n--- BANK MANAGEMENT SYSTEM ---");
            System.out.println("1. Create Account");
            System.out.println("2. Deposit");
            System.out.println("3. Withdraw");
            System.out.println("4. Check Balance");
            System.out.println("5. Fund Transfer");
            System.out.println("6. View Transaction History");
            System.out.println("7. Exit");
            System.out.print("Choose option: ");
            int choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {
                case 1: createAccount(sc); break;
                case 2: deposit(sc); break;
                case 3: withdraw(sc); break;
                case 4: checkBalance(sc); break;
                case 5: fundTransfer(sc); break;
                case 6: viewTransactionHistory(sc); break;
                case 7: saveAccounts(); System.exit(0);
                default: System.out.println("Invalid choice.");
            }
        }
    }

    static void createAccount(Scanner sc) {
        System.out.print("Enter account number: ");
        String accNo = sc.nextLine();
        System.out.print("Enter name: ");
        String name = sc.nextLine();
        System.out.print("Enter initial balance: ");
        double bal = sc.nextDouble();
        sc.nextLine();
        System.out.print("Set 4-digit PIN: ");
        String pin = sc.nextLine();

        if (pin.length() != 4 || !pin.matches("\\d+")) {
            System.out.println("PIN must be exactly 4 digits.");
            return;
        }

        accounts.add(new Account(accNo, name, bal, pin));
        saveAccounts();
        logTransaction(accNo, "Account Created", bal);
        System.out.println("Account created successfully!");
    }

    static Account authenticate(Scanner sc, String accNo) {
        for (Account acc : accounts) {
            if (acc.accountNumber.equals(accNo)) {
                System.out.print("Enter PIN: ");
                String pin = sc.nextLine();
                if (acc.checkPin(pin)) {
                    return acc;
                } else {
                    System.out.println("Incorrect PIN.");
                    return null;
                }
            }
        }
        System.out.println("Account not found.");
        return null;
    }

    static void deposit(Scanner sc) {
        System.out.print("Enter account number: ");
        String accNo = sc.nextLine();
        Account acc = authenticate(sc, accNo);
        if (acc != null) {
            System.out.print("Enter amount to deposit: ");
            double amt = sc.nextDouble();
            acc.balance += amt;
            saveAccounts();
            logTransaction(accNo, "Deposit", amt);
            System.out.println("Deposit successful.");
        }
    }

    static void withdraw(Scanner sc) {
        System.out.print("Enter account number: ");
        String accNo = sc.nextLine();
        Account acc = authenticate(sc, accNo);
        if (acc != null) {
            System.out.print("Enter amount to withdraw: ");
            double amt = sc.nextDouble();
            if (amt <= acc.balance) {
                acc.balance -= amt;
                saveAccounts();
                logTransaction(accNo, "Withdraw", amt);
                System.out.println("Withdrawal successful.");
            } else {
                System.out.println("Insufficient balance.");
            }
        }
    }

    static void checkBalance(Scanner sc) {
        System.out.print("Enter account number: ");
        String accNo = sc.nextLine();
        Account acc = authenticate(sc, accNo);
        if (acc != null) {
            System.out.println("Account Holder: " + acc.name);
            System.out.println("Balance: " + acc.balance);
        }
    }

    static void fundTransfer(Scanner sc) {
        System.out.print("Enter sender account number: ");
        String senderNo = sc.nextLine();
        Account sender = authenticate(sc, senderNo);
        if (sender == null) return;

        System.out.print("Enter receiver account number: ");
        String receiverNo = sc.nextLine();
        Account receiver = null;
        for (Account acc : accounts) {
            if (acc.accountNumber.equals(receiverNo)) {
                receiver = acc;
                break;
            }
        }
        if (receiver == null) {
            System.out.println("Receiver account not found.");
            return;
        }

        System.out.print("Enter amount to transfer: ");
        double amt = sc.nextDouble();
        if (amt > sender.balance) {
            System.out.println("Insufficient funds in sender account.");
            return;
        }

        sender.balance -= amt;
        receiver.balance += amt;
        saveAccounts();
        logTransaction(senderNo, "Transfer to " + receiverNo, amt);
        logTransaction(receiverNo, "Received from " + senderNo, amt);
        System.out.println("Transfer successful.");
    }

    static void viewTransactionHistory(Scanner sc) {
        System.out.print("Enter account number: ");
        String accNo = sc.nextLine();
        Account acc = authenticate(sc, accNo);
        if (acc != null) {
            try (BufferedReader br = new BufferedReader(new FileReader(TRANSACTION_FILE))) {
                String line;
                boolean found = false;
                while ((line = br.readLine()) != null) {
                    if (line.contains(accNo)) {
                        System.out.println(line);
                        found = true;
                    }
                }
                if (!found) System.out.println("No transactions found for this account.");
            } catch (IOException e) {
                System.out.println("Error reading transaction file.");
            }
        }
    }

    static void saveAccounts() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
            oos.writeObject(accounts);
        } catch (IOException e) {
            System.out.println("Error saving accounts.");
        }
    }

    static void loadAccounts() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(FILE_NAME))) {
            accounts = (List<Account>) ois.readObject();
        } catch (Exception e) {
            accounts = new ArrayList<>();
        }
    }

    static void logTransaction(String accNo, String type, double amount) {
        try (FileWriter fw = new FileWriter(TRANSACTION_FILE, true)) {
            fw.write(new Date() + " | " + accNo + " | " + type + " | " + amount + "\n");
        } catch (IOException e) {
            System.out.println("Error logging transaction.");
        }
    }
}
