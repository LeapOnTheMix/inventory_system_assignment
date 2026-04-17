package com.inventory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class SalesPOSPanel extends JPanel {

    private Main frame;
    private JTable productTable;
    private JTable cartTable;
    private DefaultTableModel productModel;
    private DefaultTableModel cartModel;
    private JLabel lblTotal;
    private double grandTotal = 0.0;

    public SalesPOSPanel(Main frame) {
        this.frame = frame;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Title
        JLabel title = new JLabel("Sales (POS) - Admin", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 24));
        add(title, BorderLayout.NORTH);

        // Main Split Pane: Products | Cart
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.65);

        // ==================== LEFT: Products ====================
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));

        // Search
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField txtSearch = new JTextField(20);
        JButton btnSearch = new JButton("Search");
        searchPanel.add(new JLabel("Search Product:"));
        searchPanel.add(txtSearch);
        searchPanel.add(btnSearch);

        leftPanel.add(searchPanel, BorderLayout.NORTH);

        // Product Table
        String[] productColumns = {"ID", "Product Name", "Price", "Stock"};
        productModel = new DefaultTableModel(productColumns, 0);
        productTable = new JTable(productModel);
        productTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        productTable.setRowHeight(25);

        JButton btnAddToCart = new JButton("Add to Cart");
        btnAddToCart.addActionListener(e -> addToCart());

        JPanel leftBottom = new JPanel(new FlowLayout());
        leftBottom.add(btnAddToCart);

        leftPanel.add(new JScrollPane(productTable), BorderLayout.CENTER);
        leftPanel.add(leftBottom, BorderLayout.SOUTH);

        // ==================== RIGHT: Cart ====================
        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));

        JLabel cartTitle = new JLabel("Shopping Cart", SwingConstants.CENTER);
        cartTitle.setFont(new Font("Arial", Font.BOLD, 18));
        rightPanel.add(cartTitle, BorderLayout.NORTH);

        String[] cartColumns = {"ID", "Name", "Price", "Qty", "Subtotal"};
        cartModel = new DefaultTableModel(cartColumns, 0);
        cartTable = new JTable(cartModel);

        rightPanel.add(new JScrollPane(cartTable), BorderLayout.CENTER);

        // Cart Controls
        JPanel cartControlPanel = new JPanel(new FlowLayout());
        JButton btnRemove = new JButton("Remove Item");
        JButton btnClear = new JButton("Clear Cart");
        btnRemove.addActionListener(e -> removeFromCart());
        btnClear.addActionListener(e -> clearCart());

        cartControlPanel.add(btnRemove);
        cartControlPanel.add(btnClear);

        // Total & Complete Sale
        JPanel totalPanel = new JPanel(new BorderLayout());
        lblTotal = new JLabel("Total: $0.00", SwingConstants.CENTER);
        lblTotal.setFont(new Font("Arial", Font.BOLD, 20));
        lblTotal.setForeground(new Color(0, 102, 0));

        JButton btnCompleteSale = new JButton("Complete Sale");
        btnCompleteSale.setFont(new Font("Arial", Font.BOLD, 16));
        btnCompleteSale.setBackground(new Color(0, 153, 0));
        btnCompleteSale.setForeground(Color.WHITE);
        btnCompleteSale.addActionListener(e -> completeSale());

        totalPanel.add(lblTotal, BorderLayout.NORTH);
        totalPanel.add(btnCompleteSale, BorderLayout.SOUTH);

        JPanel rightBottom = new JPanel(new BorderLayout());
        rightBottom.add(cartControlPanel, BorderLayout.NORTH);
        rightBottom.add(totalPanel, BorderLayout.CENTER);

        rightPanel.add(rightBottom, BorderLayout.SOUTH);

        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(rightPanel);

        add(splitPane, BorderLayout.CENTER);

        // ==================== Bottom Back Button ====================
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnBack = new JButton("← Back to Dashboard");
        btnBack.setFont(new Font("Arial", Font.BOLD, 15));
        btnBack.setBackground(new Color(220, 53, 69));
        btnBack.setForeground(Color.WHITE);
        btnBack.setPreferredSize(new Dimension(220, 45));
        btnBack.addActionListener(e -> frame.show("admin_dashboard"));
        bottomPanel.add(btnBack);

        add(bottomPanel, BorderLayout.SOUTH);

        // Load products on startup
        loadAllProducts();

        // Search action
        btnSearch.addActionListener(e -> searchProducts(txtSearch.getText().trim()));
    }

    // ==================== LOAD ALL PRODUCTS ====================
    private void loadAllProducts() {
        productModel.setRowCount(0);
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name, price, quantity FROM products")) {

            while (rs.next()) {
                productModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("name"),
                        String.format("%.2f", rs.getDouble("price")),
                        rs.getInt("quantity")
                });
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading products: " + ex.getMessage());
        }
    }

    // ==================== SEARCH PRODUCTS ====================
    private void searchProducts(String keyword) {
        productModel.setRowCount(0);
        if (keyword.isEmpty()) {
            loadAllProducts();
            return;
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, name, price, quantity FROM products WHERE name LIKE ?")) {

            ps.setString(1, "%" + keyword + "%");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                productModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("name"),
                        String.format("%.2f", rs.getDouble("price")),
                        rs.getInt("quantity")
                });
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // ==================== ADD TO CART ====================
    private void addToCart() {
        int selectedRow = productTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a product!");
            return;
        }

        int id = (int) productModel.getValueAt(selectedRow, 0);
        String name = (String) productModel.getValueAt(selectedRow, 1);
        double price = Double.parseDouble(productModel.getValueAt(selectedRow, 2).toString().replace("$", ""));
        int stock = (int) productModel.getValueAt(selectedRow, 3);

        String qtyStr = JOptionPane.showInputDialog(this, 
            "Enter quantity (Available: " + stock + "):", "1");

        if (qtyStr == null || qtyStr.trim().isEmpty()) return;

        try {
            int qty = Integer.parseInt(qtyStr);
            if (qty <= 0 || qty > stock) {
                JOptionPane.showMessageDialog(this, "Invalid quantity! Must be 1 to " + stock);
                return;
            }

            double subtotal = price * qty;
            cartModel.addRow(new Object[]{id, name, String.format("%.2f", price), qty, String.format("%.2f", subtotal)});

            grandTotal += subtotal;
            lblTotal.setText(String.format("Total: $%.2f", grandTotal));

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number!");
        }
    }

    // ==================== REMOVE FROM CART ====================
    private void removeFromCart() {
        int row = cartTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select an item to remove!");
            return;
        }

        double subtotal = Double.parseDouble(cartModel.getValueAt(row, 4).toString());
        grandTotal -= subtotal;
        lblTotal.setText(String.format("Total: $%.2f", grandTotal));

        cartModel.removeRow(row);
    }

    // ==================== CLEAR CART ====================
    private void clearCart() {
        if (cartModel.getRowCount() == 0) return;
        int confirm = JOptionPane.showConfirmDialog(this, "Clear entire cart?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            cartModel.setRowCount(0);
            grandTotal = 0.0;
            lblTotal.setText("Total: $0.00");
        }
    }

    // ==================== COMPLETE SALE ====================
    private void completeSale() {
        if (cartModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Cart is empty!");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, 
            "Complete sale for $" + String.format("%.2f", grandTotal) + "?", 
            "Confirm Sale", JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            for (int i = 0; i < cartModel.getRowCount(); i++) {
                int id = (int) cartModel.getValueAt(i, 0);
                int qty = (int) cartModel.getValueAt(i, 3);

                PreparedStatement ps = conn.prepareStatement(
                    "UPDATE products SET quantity = quantity - ? WHERE id = ?");
                ps.setInt(1, qty);
                ps.setInt(2, id);
                ps.executeUpdate();
            }

            conn.commit();

            JOptionPane.showMessageDialog(this, 
                "✅ Sale Completed Successfully!\nTotal: $" + String.format("%.2f", grandTotal), 
                "Success", JOptionPane.INFORMATION_MESSAGE);

            clearCart();  // Clear after success

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Sale failed: " + ex.getMessage());
        }
    }
}