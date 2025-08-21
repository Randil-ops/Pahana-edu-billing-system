import java.text.DecimalFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Pahana Edu - Menu-driven Billing System (Console MVP)
 * Features implemented:
 * 1) Login
 * 2) Add Customer
 * 3) Edit Customer
 * 4) Manage Items (Add/Update/Delete/List)
 * 5) Display Account
 * 6) Calculate & Print Bill (itemized OR quick bill via units × UNIT rate)
 * 7) Help
 * 8) Exit
 */
public class PahanaEduApp {

    private static final Scanner SC = new Scanner(System.in);
    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final AuthService authService = new AuthService();
    private static final CustomerRepo customerRepo = new CustomerRepo();
    private static final ItemRepo itemRepo = new ItemRepo();
    private static final BillRepo billRepo = new BillRepo();
    private static final BillingService billingService = new BillingService();

    public static void main(String[] args) {
        seedDemoData();

        if (!doLogin()) {
            System.out.println("\nToo many failed attempts. Exiting. Goodbye!");
            return;
        }

        int choice;
        do {
            showMainMenu();
            choice = readInt("Choose an option (1-8): ", 1, 8);
            switch (choice) {
                case 1 -> showAuthenticatedUser();
                case 2 -> addCustomer();
                case 3 -> editCustomer();
                case 4 -> manageItemsMenu();
                case 5 -> displayAccountDetails();
                case 6 -> createAndPrintBill();
                case 7 -> showHelp();
                case 8 -> System.out.println("\nThank you for using Pahana Edu Billing System. Goodbye!");
            }
            if (choice != 8) pause("\nPress Enter to continue...");
        } while (choice != 8);
    }

    // ======== Login ========
    private static boolean doLogin() {
        System.out.println("Login Required");
        for (int i = 3; i >= 1; i--) {
            String u = readNonEmpty("Username: ");
            String p = readNonEmpty("Password: ");
            if (authService.login(u, p)) {
                System.out.println("\nLogin successful. Welcome, " + u + "!");
                return true;
            } else {
                System.out.println("Invalid credentials. Attempts left: " + (i - 1));
            }
        }
        return false;
    }

    // ======== Menu ========
    private static void showMainMenu() {
        System.out.println("\n======================================================");
        System.out.println("        Pahana Edu — Billing & Account System         ");
        System.out.println("======================================================");
        System.out.println("1) User Authentication (info)");
        System.out.println("2) Add New Customer Account");
        System.out.println("3) Edit Customer Information");
        System.out.println("4) Manage Item Information");
        System.out.println("5) Display Account Details");
        System.out.println("6) Calculate and Print Bill");
        System.out.println("7) Help");
        System.out.println("8) Exit");
    }

    private static void showAuthenticatedUser() {
        System.out.println("\nLogged in as: " + authService.getCurrentUser());
    }

    // ======== Customers ========
    private static void addCustomer() {
        System.out.println("\nAdd New Customer");
        String accountNo;
        while (true) {
            accountNo = readNonEmpty("Account Number: ");
            if (customerRepo.exists(accountNo)) {
                System.out.println("Account number already exists. Try again.");
            } else break;
        }
        String name = readNonEmpty("Full Name: ");
        String address = readNonEmpty("Address: ");
        String phone = readNonEmpty("Telephone: ");
        int units = readInt("Units Consumed: ", 0, Integer.MAX_VALUE);

        Customer c = new Customer(accountNo, name, address, phone, units);
        customerRepo.add(c);
        System.out.println("\nCustomer added successfully:\n" + c);
    }

    private static void editCustomer() {
        System.out.println("\nEdit Customer Information");
        String accountNo = readNonEmpty("Enter Account Number: ");
        Customer c = customerRepo.find(accountNo);
        if (c == null) {
            System.out.println("No customer found for account: " + accountNo);
            return;
        }
        System.out.println("\nCurrent details:\n" + c);

        String name = readLine("Full Name [" + c.name + "]: ");
        String address = readLine("Address [" + c.address + "]: ");
        String phone = readLine("Telephone [" + c.phone + "]: ");
        String unitsStr = readLine("Units Consumed [" + c.unitsConsumed + "]: ");

        if (!name.isBlank()) c.name = name;
        if (!address.isBlank()) c.address = address;
        if (!phone.isBlank()) c.phone = phone;
        if (!unitsStr.isBlank()) {
            try {
                int u = Integer.parseInt(unitsStr.trim());
                if (u >= 0) c.unitsConsumed = u;
            } catch (NumberFormatException ignored) { }
        }

        customerRepo.update(c);
        System.out.println("\nCustomer updated:\n" + c);
    }

    private static void displayAccountDetails() {
        System.out.println("\nDisplay Account Details");
        String accountNo = readNonEmpty("Enter Account Number: ");
        Customer c = customerRepo.find(accountNo);
        if (c == null) System.out.println("No customer found for account: " + accountNo);
        else System.out.println("\n" + c);
    }

    // ======== Items ========
    private static void manageItemsMenu() {
        int choice;
        do {
            System.out.println("\nItem Management");
            System.out.println("1) List Items");
            System.out.println("2) Add Item");
            System.out.println("3) Update Item");
            System.out.println("4) Delete Item");
            System.out.println("5) Back");
            choice = readInt("Choose (1-5): ", 1, 5);

            switch (choice) {
                case 1 -> listItems();
                case 2 -> addItem();
                case 3 -> updateItem();
                case 4 -> deleteItem();
            }
        } while (choice != 5);
    }

    private static void listItems() {
        List<Item> items = itemRepo.listAll();
        if (items.isEmpty()) {
            System.out.println("\nNo items available.");
            return;
        }
        System.out.println("\nItems:");
        for (Item it : items) System.out.println(it);
    }

    private static void addItem() {
        String code;
        while (true) {
            code = readNonEmpty("Item Code: ");
            if (itemRepo.exists(code)) System.out.println("Code already exists.");
            else break;
        }
        String name = readNonEmpty("Item Name: ");
        double price = readDouble("Unit Price: ", 0, Double.MAX_VALUE);
        itemRepo.add(new Item(code, name, price));
        System.out.println("Item added successfully.");
    }

    private static void updateItem() {
        String code = readNonEmpty("Enter Item Code: ");
        Item it = itemRepo.find(code);
        if (it == null) {
            System.out.println("No item found.");
            return;
        }
        String name = readLine("Name [" + it.name + "]: ");
        String priceStr = readLine("Unit Price [" + MONEY.format(it.unitPrice) + "]: ");

        if (!name.isBlank()) it.name = name;
        if (!priceStr.isBlank()) {
            try {
                double p = Double.parseDouble(priceStr.trim());
                if (p >= 0) it.unitPrice = p;
            } catch (NumberFormatException ignored) { }
        }
        itemRepo.update(it);
        System.out.println("Item updated.");
    }

    private static void deleteItem() {
        String code = readNonEmpty("Enter Item Code: ");
        if (!itemRepo.exists(code)) System.out.println("No item found.");
        else {
            itemRepo.delete(code);
            System.out.println("Item deleted.");
        }
    }

    // ======== Billing (Itemized OR Quick bill) ========
    private static void createAndPrintBill() {
        System.out.println("\nCreate & Print Bill");

        String accountNo = readNonEmpty("Enter Account Number: ");
        Customer c = customerRepo.find(accountNo);
        if (c == null) {
            System.out.println("No customer found. Add the customer first.");
            return;
        }

        System.out.println("\n1) Itemized bill (choose items + quantities)");
        System.out.println("2) Quick bill from unitsConsumed × UNIT item price");
        int mode = readInt("Choose (1-2): ", 1, 2);

        List<BillItem> lines = new ArrayList<>();

        if (mode == 1) {
            // Itemized path
            if (itemRepo.listAll().isEmpty()) {
                System.out.println("No items available. Add items first in 'Manage Item Information'.");
                return;
            }
            boolean addMore;
            do {
                listItems();
                String code = readNonEmpty("\nEnter Item Code to add: ");
                Item it = itemRepo.find(code);
                if (it == null) {
                    System.out.println("Invalid item code. Try again.");
                } else {
                    int qty = readInt("Quantity (>=1): ", 1, Integer.MAX_VALUE);
                    lines.add(new BillItem(it, qty));
                    System.out.println("Added: " + it.name + " × " + qty);
                }
                addMore = readYesNo("Add another item? (y/n): ");
            } while (addMore);

            if (lines.isEmpty()) {
                System.out.println("No items added. Bill canceled.");
                return;
            }
        } else {
            // Quick bill path (units × UNIT price)
            Item unitItem = itemRepo.find("UNIT");
            if (unitItem == null) {
                System.out.println("UNIT item not found. Please add an item with code 'UNIT' (per-unit rate).");
                return;
            }
            if (c.unitsConsumed <= 0) {
                System.out.println("Customer's unitsConsumed is 0. Edit the customer to set units first.");
                return;
            }
            lines.add(new BillItem(unitItem, c.unitsConsumed));
            System.out.println("Quick bill created using " + c.unitsConsumed + " × " + unitItem.name +
                    " @ " + MONEY.format(unitItem.unitPrice));
        }

        double taxRate = 0.15; // e.g., 15% VAT
        Bill bill = billingService.generateBill(c, lines, taxRate);
        billRepo.save(bill);
        printReceipt(bill, c);
    }

    private static void printReceipt(Bill bill, Customer c) {
        System.out.println("\n=============== Pahana Edu Receipt ===============");
        System.out.println("Bill No   : " + bill.billNo);
        System.out.println("Date/Time : " + bill.issuedAt.format(TS));
        System.out.println("Customer  : " + c.name + " (Acc#: " + c.accountNo + ")");
        System.out.println("-------------------------------------------------");
        for (BillItem bi : bill.items) {
            System.out.printf("%s x %d = %s%n", bi.item.name, bi.quantity, MONEY.format(bi.lineTotal()));
        }
        System.out.println("-------------------------------------------------");
        System.out.println("Subtotal: " + MONEY.format(bill.subtotal));
        System.out.println("Tax:      " + MONEY.format(bill.tax));
        System.out.println("Total:    " + MONEY.format(bill.total));
        System.out.println("=================================================\n");
    }

    private static void showHelp() {
        System.out.println("\nHelp:");
        System.out.println("Login with admin/admin123 or cashier/cashier123");
        System.out.println("Add/Edit customers, manage items, and generate itemized or quick bills.");
        System.out.println("- Quick bill uses customer's Units Consumed × price of item with code 'UNIT'.");
    }

    // ======== Utilities ========
    private static String readNonEmpty(String prompt) {
        String s;
        do {
            System.out.print(prompt);
            s = SC.nextLine().trim();
        } while (s.isEmpty());
        return s;
    }

    private static String readLine(String prompt) {
        System.out.print(prompt);
        return SC.nextLine();
    }

    private static int readInt(String prompt, int minInclusive, int maxInclusive) {
        while (true) {
            try {
                System.out.print(prompt);
                int v = Integer.parseInt(SC.nextLine().trim());
                if (v >= minInclusive && v <= maxInclusive) return v;
            } catch (Exception ignored) { }
            System.out.println("Invalid input, try again.");
        }
    }

    private static double readDouble(String prompt, double minInclusive, double maxInclusive) {
        while (true) {
            try {
                System.out.print(prompt);
                double v = Double.parseDouble(SC.nextLine().trim());
                if (v >= minInclusive && v <= maxInclusive) return v;
            } catch (Exception ignored) { }
            System.out.println("Invalid input, try again.");
        }
    }

    private static boolean readYesNo(String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = SC.nextLine().trim().toLowerCase();
            if (s.equals("y") || s.equals("yes")) return true;
            if (s.equals("n") || s.equals("no")) return false;
            System.out.println("Please enter y/n.");
        }
    }

    private static void pause(String msg) {
        System.out.print(msg);
        SC.nextLine();
    }

    private static void seedDemoData() {
        authService.addUser("admin", "admin123");
        authService.addUser("cashier", "cashier123");

        itemRepo.add(new Item("UNIT", "Standard Unit", 10.0));
        itemRepo.add(new Item("BK101", "Intro to Java", 2500.00));
        itemRepo.add(new Item("BK202", "Data Structures", 3200.00));

        customerRepo.add(new Customer("ACC1001", "N. Perera", "Colombo", "0771234567", 15));
    }

    // ======== Inner Classes ========
    static class AuthService {
        private final Map<String, String> users = new HashMap<>();
        private String currentUser;
        void addUser(String u, String p) { users.put(u, p); }
        boolean login(String u, String p) {
            if (users.containsKey(u) && users.get(u).equals(p)) { currentUser = u; return true; }
            return false;
        }
        String getCurrentUser() { return currentUser; }
    }

    static class Customer {
        String accountNo, name, address, phone;
        int unitsConsumed;
        Customer(String a, String n, String ad, String ph, int u) { accountNo=a;name=n;address=ad;phone=ph;unitsConsumed=u; }
        public String toString() {
            return "Account: " + accountNo + "\nName: " + name + "\nAddress: " + address +
                   "\nPhone: " + phone + "\nUnits: " + unitsConsumed;
        }
    }

    static class CustomerRepo {
        private final Map<String, Customer> data = new HashMap<>();
        void add(Customer c){data.put(c.accountNo,c);}
        void update(Customer c){data.put(c.accountNo,c);}
        Customer find(String a){return data.get(a);}
        boolean exists(String a){return data.containsKey(a);}
    }

    static class Item {
        String code, name; double unitPrice;
        Item(String c, String n, double p){code=c;name=n;unitPrice=p;}
        public String toString(){return code + " - " + name + " @ " + MONEY.format(unitPrice);}
    }

    static class ItemRepo {
        private final Map<String, Item> data = new LinkedHashMap<>();
        void add(Item i){data.put(i.code,i);}
        void update(Item i){data.put(i.code,i);}
        Item find(String c){return data.get(c);}
        boolean exists(String c){return data.containsKey(c);}
        void delete(String c){data.remove(c);}
        List<Item> listAll(){return new ArrayList<>(data.values());}
    }

    static class BillItem {
        Item item; int quantity;
        BillItem(Item i, int q){item=i;quantity=q;}
        double lineTotal(){return item.unitPrice * quantity;}
    }

    static class Bill {
        String billNo; LocalDateTime issuedAt; List<BillItem> items;
        double subtotal, tax, total;
        Bill(String no, LocalDateTime t, List<BillItem> it, double s, double tax, double tot){
            billNo=no;issuedAt=t;items=it;subtotal=s;this.tax=tax;total=tot;
        }
    }

    static class BillingService {
        Bill generateBill(Customer c, List<BillItem> items, double taxRate){
            String no = "BILL-" + UUID.randomUUID().toString().substring(0,8).toUpperCase();
            double subtotal = 0;
            for (BillItem bi : items) subtotal += bi.lineTotal();
            double tax   = Math.round(subtotal * taxRate * 100) / 100.0;
            double total = Math.round((subtotal + tax) * 100) / 100.0;
            return new Bill(no, LocalDateTime.now(), items, subtotal, tax, total);
        }
    }

    static class BillRepo {
        private final List<Bill> bills = new ArrayList<>();
        void save(Bill b){bills.add(b);}
    }
}
