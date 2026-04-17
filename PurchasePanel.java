package com.inventory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class PurchasePanel extends JPanel {

    private Main frame;
    private JTable productTable;
    private DefaultTableModel tableModel;

    public PurchasePanel(Main frame) {
        this.frame = frame;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Title
        JLabel title = new JLabel("Purchase / Restock Management", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 24));
        add(title, BorderLayout.NORTH);

        // Table
        String[] columns = {"ID", "Product Name", "Current Price", "Current Stock"};
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

        // Left: Action Buttons
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        JButton btnPurchase = new JButton("Add Stock (Purchase)");
        JButton btnRefresh = new JButton("Refresh List");

        leftPanel.add(btnPurchase);
        leftPanel.add(btnRefresh);

        // Right: Back Button (Prominent)
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
        btnPurchase.addActionListener(e -> openPurchaseDialog());
        btnRefresh.addActionListener(e -> loadProducts());
        btnBack.addActionListener(e -> frame.show("admin_dashboard"));

        // Load data on start
        loadProducts();
    }

    // ==================== LOAD PRODUCTS ====================
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

    // ==================== OPEN PURCHASE DIALOG ====================
    private void openPurchaseDialog() {
        int row = productTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a product to add stock!", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int id = (int) tableModel.getValueAt(row, 0);
        String name = (String) tableModel.getValueAt(row, 1);
        int currentStock = (int) tableModel.getValueAt(row, 3);

        JDialog dialog = createPurchaseDialog(id, name, currentStock);
        dialog.setVisible(true);
    }

    // ==================== PURCHASE DIALOG ====================
    private JDialog createPurchaseDialog(int productId, String productName, int currentStock) {
        JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parentFrame, "Add Stock - " + productName, true);
        dialog.setSize(450, 280);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridLayout(4, 2, 15, 20));
        panel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        JLabel lblName = new JLabel("Product: " + productName);
        lblName.setFont(new Font("Arial", Font.BOLD, 14));

        JTextField txtQuantity = new JTextField(15);
        JTextField txtSupplier = new JTextField(15);   // Optional: supplier name

        panel.add(new JLabel("Product Name:"));
        panel.add(lblName);
        panel.add(new JLabel("Current Stock:"));
        panel.add(new JLabel(String.valueOf(currentStock)));
        panel.add(new JLabel("Add Quantity:"));
        panel.add(txtQuantity);
        panel.add(new JLabel("Supplier (Optional):"));
        panel.add(txtSupplier);

        JButton btnAddStock = new JButton("Add Stock");
        JButton btnCancel = new JButton("Cancel");

        btnAddStock.addActionListener(e -> {
            addStock(productId, txtQuantity.getText().trim(), txtSupplier.getText().trim());
            dialog.dispose();
        });

        btnCancel.addActionListener(e -> dialog.dispose());

        JPanel btnPanel = new JPanel(new FlowLayout());
        btnPanel.add(btnAddStock);
        btnPanel.add(btnCancel);

        panel.add(new JLabel(""));
        panel.add(btnPanel);

        dialog.add(panel);
        return dialog;
    }

    // ==================== ADD STOCK TO DATABASE ====================
    private void addStock(int productId, String qtyStr, String supplier) {
        if (qtyStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter quantity to add!");
            return;
        }

        try {
            int quantityToAdd = Integer.parseInt(qtyStr);
            if (quantityToAdd <= 0) {
                JOptionPane.showMessageDialog(this, "Quantity must be greater than 0!");
                return;
            }

            String sql = "UPDATE products SET quantity = quantity + ? WHERE id = ?";
            
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setInt(1, quantityToAdd);
                ps.setInt(2, productId);
                int rows = ps.executeUpdate();

                if (rows > 0) {
                    JOptionPane.showMessageDialog(this, 
                        "✅ Successfully added " + quantityToAdd + " units to stock!", 
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                    loadProducts(); // Refresh table
                }
            }

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Quantity must be a valid number!");
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage());
        }
    }
}