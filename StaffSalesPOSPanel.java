package com.inventory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class StaffSalesPOSPanel extends JPanel {

    private Main frame;
    private JTextField txtSearch;
    private JTable productTable;
    private JTable cartTable;
    private JLabel lblTotal;
    private DefaultTableModel cartModel;
    private double grandTotal = 0.0;

    public StaffSalesPOSPanel(Main frame) {
        this.frame = frame;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Title
        JLabel title = new JLabel("Sales (POS) - Staff", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        add(title, BorderLayout.NORTH);

        // Main Split Pane: Left = Products, Right = Cart
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.6);

        // === LEFT PANEL: Product Search & List ===
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Search Product:"));
        txtSearch = new JTextField(20);
        JButton btnSearch = new JButton("Search");
        btnSearch.addActionListener(e -> searchProducts());

        searchPanel.add(txtSearch);
        searchPanel.add(btnSearch);

        leftPanel.add(searchPanel, BorderLayout.NORTH);

        // Product Table
        String[] productColumns = {"ID", "Name", "Price", "Stock"};
        DefaultTableModel productModel = new DefaultTableModel(productColumns, 0);
        productTable = new JTable(productModel);
        productTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JButton btnAddToCart = new JButton("Add to Cart");
        btnAddToCart.addActionListener(e -> addToCart());

        JPanel leftBottom = new JPanel();
        leftBottom.add(btnAddToCart);

        leftPanel.add(new JScrollPane(productTable), BorderLayout.CENTER);
        leftPanel.add(leftBottom, BorderLayout.SOUTH);

        // === RIGHT PANEL: Cart ===
        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));

        JLabel cartLabel = new JLabel("Shopping Cart", SwingConstants.CENTER);
        cartLabel.setFont(new Font("Arial", Font.BOLD, 16));
        rightPanel.add(cartLabel, BorderLayout.NORTH);

        // Cart Table
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

        // Total and Complete Sale
        JPanel totalPanel = new JPanel(new BorderLayout());
        lblTotal = new JLabel("Total: $0.00", SwingConstants.CENTER);
        lblTotal.setFont(new Font("Arial", Font.BOLD, 18));
        lblTotal.setForeground(new Color(0, 102, 0));

        JButton btnCompleteSale = new JButton("Complete Sale");
        btnCompleteSale.setFont(new Font("Arial", Font.BOLD, 14));
        btnCompleteSale.setBackground(new Color(0, 153, 0));
        btnCompleteSale.setForeground(Color.WHITE);
        btnCompleteSale.addActionListener(e -> completeSale());

        totalPanel.add(lblTotal, BorderLayout.NORTH);
        totalPanel.add(btnCompleteSale, BorderLayout.SOUTH);

        rightPanel.add(cartControlPanel, BorderLayout.SOUTH);
        rightPanel.add(totalPanel, BorderLayout.EAST);  // Wait, better placement below

        // Fix layout for right panel
        JPanel rightBottom = new JPanel(new BorderLayout());
        rightBottom.add(cartControlPanel, BorderLayout.NORTH);
        rightBottom.add(totalPanel, BorderLayout.CENTER);

        rightPanel.add(rightBottom, BorderLayout.SOUTH);

        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(rightPanel);

        add(splitPane, BorderLayout.CENTER);

        // Bottom Back Button
        JButton backBtn = new JButton("Back to Staff Dashboard");
        backBtn.addActionListener(e -> frame.show("staff_dashboard"));
        JPanel bottomPanel = new JPanel();
        bottomPanel.add(backBtn);
        add(bottomPanel, BorderLayout.SOUTH);

        // Load all products on start
        loadAllProducts(productModel);
    }

    private void loadAllProducts(DefaultTableModel model) {
        model.setRowCount(0);
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name, price, quantity FROM products")) {

            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDouble("price"),
                        rs.getInt("quantity")
                });
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading products: " + ex.getMessage());
        }
    }

    private void searchProducts() {
        String keyword = txtSearch.getText().trim();
        if (keyword.isEmpty()) {
            loadAllProducts((DefaultTableModel) productTable.getModel());
            return;
        }

        DefaultTableModel model = (DefaultTableModel) productTable.getModel();
        model.setRowCount(0);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, name, price, quantity FROM products WHERE name LIKE ?")) {

            ps.setString(1, "%" + keyword + "%");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDouble("price"),
                        rs.getInt("quantity")
                });
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void addToCart() {
        int selectedRow = productTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a product!");
            return;
        }

        DefaultTableModel prodModel = (DefaultTableModel) productTable.getModel();
        int id = (int) prodModel.getValueAt(selectedRow, 0);
        String name = (String) prodModel.getValueAt(selectedRow, 1);
        double price = (double) prodModel.getValueAt(selectedRow, 2);
        int stock = (int) prodModel.getValueAt(selectedRow, 3);

        String qtyStr = JOptionPane.showInputDialog(this, "Enter quantity (Max: " + stock + "):", "1");
        if (qtyStr == null || qtyStr.trim().isEmpty()) return;

        try {
            int qty = Integer.parseInt(qtyStr);
            if (qty <= 0 || qty > stock) {
                JOptionPane.showMessageDialog(this, "Invalid quantity! Must be between 1 and " + stock);
                return;
            }

            double subtotal = price * qty;

            // Add to cart
            cartModel.addRow(new Object[]{id, name, price, qty, subtotal});

            // Update grand total
            grandTotal += subtotal;
            lblTotal.setText(String.format("Total: $%.2f", grandTotal));

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number!");
        }
    }

    private void removeFromCart() {
        int row = cartTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select an item to remove!");
            return;
        }

        double subtotal = (double) cartModel.getValueAt(row, 4);
        grandTotal -= subtotal;
        lblTotal.setText(String.format("Total: $%.2f", grandTotal));

        cartModel.removeRow(row);
    }

    private void clearCart() {
        if (cartModel.getRowCount() == 0) return;
        int confirm = JOptionPane.showConfirmDialog(this, "Clear entire cart?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            cartModel.setRowCount(0);
            grandTotal = 0.0;
            lblTotal.setText("Total: $0.00");
        }
    }

    private void completeSale() {
        if (cartModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Cart is empty!");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, 
            "Complete this sale for $" + String.format("%.2f", grandTotal) + "?", 
            "Confirm Sale", JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            // Reduce stock for each item
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
                "Sale Completed Successfully!\nTotal: $" + String.format("%.2f", grandTotal), 
                "Success", JOptionPane.INFORMATION_MESSAGE);

            // Clear cart after successful sale
            clearCart();

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Sale failed: " + ex.getMessage());
        }
    }
}