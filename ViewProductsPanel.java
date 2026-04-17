package com.inventory;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class ViewProductsPanel extends JPanel {

    private Main frame;

    public ViewProductsPanel(Main frame) {
        this.frame = frame;
        setLayout(new BorderLayout());

        JLabel title = new JLabel("View Products (Staff)", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 18));
        add(title, BorderLayout.NORTH);

        // Table
        String[] columns = {"ID", "Product Name", "Price", "Stock Quantity"};
        javax.swing.table.DefaultTableModel model = new javax.swing.table.DefaultTableModel(columns, 0);
        JTable productTable = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(productTable);
        add(scrollPane, BorderLayout.CENTER);

        // Bottom buttons
        JPanel bottomPanel = new JPanel(new FlowLayout());
        JButton refreshBtn = new JButton("Refresh");
        JButton backBtn = new JButton("Back to Dashboard");

        refreshBtn.addActionListener(e -> loadProducts(model));
        backBtn.addActionListener(e -> frame.show("staff_dashboard"));

        bottomPanel.add(refreshBtn);
        bottomPanel.add(backBtn);
        add(bottomPanel, BorderLayout.SOUTH);

        // Load data on startup
        loadProducts(model);
    }

    private void loadProducts(javax.swing.table.DefaultTableModel model) {
        model.setRowCount(0); // Clear previous rows

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT id, name, price, quantity FROM products");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getDouble("price"),
                    rs.getInt("quantity")
                });
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "Error loading products:\n" + e.getMessage(), 
                "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}