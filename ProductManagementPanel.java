package com.inventory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class ProductManagementPanel extends JPanel {

    private Main frame;
    private JTable productTable;
    private DefaultTableModel tableModel;

    public ProductManagementPanel(Main frame) {
        this.frame = frame;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Title
        JLabel title = new JLabel("Product Management", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 24));
        add(title, BorderLayout.NORTH);

        // Table
        String[] columns = {"ID", "Product Name", "Price ($)", "Stock Quantity"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        productTable = new JTable(tableModel);
        productTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        productTable.setRowHeight(28);

        add(new JScrollPane(productTable), BorderLayout.CENTER);

        // Button Panel
        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 5, 0));

        // Left buttons
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        JButton btnAdd = new JButton("Add New Product");
        JButton btnUpdate = new JButton("Update Selected");
        JButton btnDelete = new JButton("Delete Selected");
        JButton btnRefresh = new JButton("Refresh");

        leftPanel.add(btnAdd);
        leftPanel.add(btnUpdate);
        leftPanel.add(btnDelete);
        leftPanel.add(btnRefresh);

        // Right: Back Button (Clear & Visible)
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnBack = new JButton("← Back to Dashboard");
        btnBack.setFont(new Font("Arial", Font.BOLD, 15));
        btnBack.setBackground(new Color(220, 53, 69));
        btnBack.setForeground(Color.WHITE);
        btnBack.setPreferredSize(new Dimension(220, 45));
        rightPanel.add(btnBack);

        buttonPanel.add(leftPanel, BorderLayout.CENTER);
        buttonPanel.add(rightPanel, BorderLayout.EAST);

        add(buttonPanel, BorderLayout.SOUTH);

        // Action Listeners
        btnAdd.addActionListener(e -> openAddProductDialog());
        btnUpdate.addActionListener(e -> openUpdateProductDialog());
        btnDelete.addActionListener(e -> deleteProduct());
        btnRefresh.addActionListener(e -> loadProducts());
        btnBack.addActionListener(e -> frame.show("admin_dashboard"));

        loadProducts();
    }

    private void loadProducts() {
        tableModel.setRowCount(0);

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name, price, quantity FROM products ORDER BY id")) {

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("name"),
                        String.format("%.2f", rs.getDouble("price")),
                        rs.getInt("quantity")
                });
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading products:\n" + ex.getMessage());
        }
    }

    private void openAddProductDialog() {
        JDialog dialog = createProductDialog("Add New Product", null);
        dialog.setVisible(true);
    }

    private void openUpdateProductDialog() {
        int row = productTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a product to update!");
            return;
        }

        try {
            int id = (int) tableModel.getValueAt(row, 0);
            String name = (String) tableModel.getValueAt(row, 1);
            String priceStr = tableModel.getValueAt(row, 2).toString()
                                .replace("$", "").replace(",", "").trim();

            double price = Double.parseDouble(priceStr);
            int qty = (int) tableModel.getValueAt(row, 3);

            JDialog dialog = createProductDialog("Update Product", 
                               new ProductData(id, name, price, qty));
            dialog.setVisible(true);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error reading product data. Please try again.");
        }
    }

    // ==================== FIXED DIALOG CREATOR ====================
    private JDialog createProductDialog(String title, ProductData data) {
        // Fixed: Use JFrame as parent (safer)
        JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        
        JDialog dialog = new JDialog(parentFrame, title, true);   // ← Fixed line
        dialog.setSize(450, 300);
        dialog.setLocationRelativeTo(this);

        JPanel mainPanel = new JPanel(new GridLayout(4, 2, 15, 20));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        JTextField txtName = new JTextField(20);
        JTextField txtPrice = new JTextField(20);
        JTextField txtQty = new JTextField(20);

        if (data != null) {
            txtName.setText(data.name);
            txtPrice.setText(String.valueOf(data.price));
            txtQty.setText(String.valueOf(data.quantity));
        }

        mainPanel.add(new JLabel("Product Name:"));
        mainPanel.add(txtName);
        mainPanel.add(new JLabel("Price ($):"));
        mainPanel.add(txtPrice);
        mainPanel.add(new JLabel("Quantity:"));
        mainPanel.add(txtQty);

        JButton btnSave = new JButton(data == null ? "Add Product" : "Update Product");
        JButton btnCancel = new JButton("Cancel");

        btnSave.addActionListener(e -> {
            saveProduct(txtName.getText().trim(), txtPrice.getText().trim(), 
                       txtQty.getText().trim(), data != null ? data.id : -1);
            dialog.dispose();
        });

        btnCancel.addActionListener(e -> dialog.dispose());

        JPanel btnPanel = new JPanel(new FlowLayout());
        btnPanel.add(btnSave);
        btnPanel.add(btnCancel);

        mainPanel.add(new JLabel(""));
        mainPanel.add(btnPanel);

        dialog.add(mainPanel);
        return dialog;
    }

    private void saveProduct(String name, String priceStr, String qtyStr, int updateId) {
        if (name.isEmpty() || priceStr.isEmpty() || qtyStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required!");
            return;
        }

        try {
            double price = Double.parseDouble(priceStr);
            int qty = Integer.parseInt(qtyStr);

            String sql = (updateId == -1) ?
                    "INSERT INTO products(name, price, quantity) VALUES(?, ?, ?)" :
                    "UPDATE products SET name=?, price=?, quantity=? WHERE id=?";

            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, name);
                ps.setDouble(2, price);
                ps.setInt(3, qty);
                if (updateId != -1) ps.setInt(4, updateId);

                ps.executeUpdate();

                String msg = (updateId == -1) ? "✅ Product Added Successfully!" : "✅ Product Updated Successfully!";
                JOptionPane.showMessageDialog(this, msg);
                loadProducts();
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Price and Quantity must be valid numbers!");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage());
        }
    }

    private void deleteProduct() {
        int row = productTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a product to delete!");
            return;
        }

        int id = (int) tableModel.getValueAt(row, 0);
        String name = (String) tableModel.getValueAt(row, 1);

        int confirm = JOptionPane.showConfirmDialog(this, "Delete " + name + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM products WHERE id=?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
                JOptionPane.showMessageDialog(this, "Product Deleted Successfully!");
                loadProducts();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    private static class ProductData {
        int id;
        String name;
        double price;
        int quantity;

        ProductData(int id, String name, double price, int quantity) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.quantity = quantity;
        }
    }
}
