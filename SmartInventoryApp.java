import java.sql.*;
import java.util.*;

public class SmartInventoryApp {
    static final String URL = "jdbc:mysql://localhost:3306/smart_inventory";
    static final String USER = "root";
    static final String PASSWORD = "root"; // change if needed

    static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // Show all available products
    static void showProducts() {
        try (Connection con = getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM products")) {

            System.out.println("\n Available Products:");
            System.out.println("-------------------------------------------");
            while (rs.next()) {
                System.out.printf("[%d] %-20s ₹%.2f (Stock: %d)\n",
                        rs.getInt("product_id"),
                        rs.getString("product_name"),
                        rs.getDouble("price"),
                        rs.getInt("quantity"));
            }
            System.out.println("-------------------------------------------");

        } catch (Exception e) {
            System.out.println("Error loading products: " + e.getMessage());
        }
    }

    // Add or fetch a customer automatically
    static int getOrAddCustomer(String name, String phone) throws SQLException {
        try (Connection con = getConnection()) {
            PreparedStatement ps = con.prepareStatement(
                    "SELECT customer_id FROM customers WHERE phone = ?");
            ps.setString(1, phone);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) return rs.getInt(1); // existing customer

            // new customer
            PreparedStatement ps2 = con.prepareStatement(
                    "INSERT INTO customers (customer_name, phone) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps2.setString(1, name);
            ps2.setString(2, phone);
            ps2.executeUpdate();

            ResultSet gen = ps2.getGeneratedKeys();
            gen.next();
            return gen.getInt(1);
        }
    }

    // Create Bill
    static void createBill() {
        Scanner sc = new Scanner(System.in);
        showProducts();

        try (Connection con = getConnection()) {
            System.out.print("\nEnter Customer Name: ");
            String name = sc.nextLine();
            System.out.print("Enter Phone: ");
            String phone = sc.nextLine();

            int custId = getOrAddCustomer(name, phone);

            PreparedStatement psBill = con.prepareStatement(
                    "INSERT INTO bills (customer_id, total) VALUES (?, 0)",
                    Statement.RETURN_GENERATED_KEYS);
            psBill.setInt(1, custId);
            psBill.executeUpdate();

            ResultSet billKeys = psBill.getGeneratedKeys();
            billKeys.next();
            int billId = billKeys.getInt(1);

            double total = 0;
            while (true) {
                System.out.print("\nEnter Product ID (0 to finish): ");
                int pid = sc.nextInt();
                if (pid == 0) break;

                System.out.print("Quantity: ");
                int qty = sc.nextInt();

                // Fetch product info
                PreparedStatement psProd = con.prepareStatement("SELECT * FROM products WHERE product_id=?");
                psProd.setInt(1, pid);
                ResultSet rs = psProd.executeQuery();

                if (rs.next()) {
                    int stock = rs.getInt("quantity");
                    double price = rs.getDouble("price");

                    if (qty > stock) {
                        System.out.println("Not enough stock!");
                        continue;
                    }

                    double subtotal = qty * price;
                    total += subtotal;

                    // Insert into bill_items
                    PreparedStatement psItem = con.prepareStatement(
                            "INSERT INTO bill_items (bill_id, product_id, quantity, subtotal) VALUES (?, ?, ?, ?)");
                    psItem.setInt(1, billId);
                    psItem.setInt(2, pid);
                    psItem.setInt(3, qty);
                    psItem.setDouble(4, subtotal);
                    psItem.executeUpdate();

                    // Update stock
                    PreparedStatement psUpdate = con.prepareStatement(
                            "UPDATE products SET quantity = quantity - ? WHERE product_id = ?");
                    psUpdate.setInt(1, qty);
                    psUpdate.setInt(2, pid);
                    psUpdate.executeUpdate();

                    System.out.printf("Added %d x %s | ₹%.2f\n", qty, rs.getString("product_name"), subtotal);
                } else {
                    System.out.println("Invalid product!");
                }
            }

            // update total
            PreparedStatement psTotal = con.prepareStatement("UPDATE bills SET total=? WHERE bill_id=?");
            psTotal.setDouble(1, total);
            psTotal.setInt(2, billId);
            psTotal.executeUpdate();

            System.out.println("\n BILL SUMMARY");
            System.out.println("-------------------------------");
            System.out.printf("Bill ID: %d\nCustomer: %s\nTotal Amount: ₹%.2f\n", billId, name, total);
            System.out.println("-------------------------------");

        } catch (Exception e) {
            System.out.println("Error while billing: " + e.getMessage());
        }
    }

    // Show all bills
    static void showBills() {
        try (Connection con = getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT b.bill_id, c.customer_name, b.total, b.bill_date " +
                             "FROM bills b JOIN customers c ON b.customer_id = c.customer_id ORDER BY b.bill_id DESC")) {

            System.out.println("\n All Bills:");
            System.out.println("--------------------------------------------");
            while (rs.next()) {
                System.out.printf("Bill #%d | %s | ₹%.2f | %s\n",
                        rs.getInt("bill_id"),
                        rs.getString("customer_name"),
                        rs.getDouble("total"),
                        rs.getTimestamp("bill_date"));
            }
            System.out.println("--------------------------------------------");

        } catch (Exception e) {
            System.out.println("Error fetching bills: " + e.getMessage());
        }
    }

    // Main Menu
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("\n=== SMART INVENTORY & BILLING SYSTEM ===");
            System.out.println("1- View Products");
            System.out.println("2- Create New Bill");
            System.out.println("3- View All Bills");
            System.out.println("4- Exit");
            System.out.print("Enter your choice: ");
            int ch = sc.nextInt();
            sc.nextLine(); // flush newline

            switch (ch) {
                case 1 -> showProducts();
                case 2 -> createBill();
                case 3 -> showBills();
                case 4 -> {
                    System.out.println("Thank you! Visit again.");
                    return;
                }
                default -> System.out.println("Invalid option!");
            }
        }
    }
}
