package com.inventory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ReportsPanel extends JPanel {

    private Main frame;
    private JTable reportTable;
    private DefaultTableModel tableModel;
    private JLabel lblTotalSales;
    private JLabel lblTotalRevenue;

    public ReportsPanel(Main frame) {
        this.frame = frame;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Title
        JLabel title = new JLabel("Reports Dashboard", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 24));
        add(title, BorderLayout.NORTH);

        // Summary Panel (Top)
        JPanel summaryPanel = new JPanel(new GridLayout(1, 2, 20, 10));
        summaryPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 15, 0));

        lblTotalSales = new JLabel("Total Sales: 0", SwingConstants.CENTER);
        lblTotalRevenue = new JLabel("Total Revenue: $0.00", SwingConstants.CENTER);

        lblTotalSales.setFont(new Font("Arial", Font.BOLD, 18));
        lblTotalRevenue.setFont(new Font("Arial", Font.BOLD, 18));
        lblTotalRevenue.setForeground(new Color(0, 102, 0));

        summaryPanel.add(lblTotalSales);
        summaryPanel.add(lblTotalRevenue);

        add(summaryPanel, BorderLayout.NORTH);

        // Report Table
        String[] columns = {"Date", "Product Name", "Type", "Quantity", "Amount"};
        tableModel = new DefaultTableModel(columns, 0);
        reportTable = new JTable(tableModel);
        reportTable.setRowHeight(26);

        add(new JScrollPane(reportTable), BorderLayout.CENTER);

        // Button Panel
        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 5, 0));

        // Left: Report Buttons
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        JButton btnSalesToday = new JButton("Today's Sales");
        JButton btnAllSales = new JButton("All Sales");
        JButton btnStockSummary = new JButton("Stock Summary");
        JButton btnLowStock = new JButton("Low Stock Alert");

        leftPanel.add(btnSalesToday);
        leftPanel.add(btnAllSales);
        leftPanel.add(btnStockSummary);
        leftPanel.add(btnLowStock);

        // Right: Back Button
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
        btnSalesToday.addActionListener(e -> showTodaySales());
        btnAllSales.addActionListener(e -> showAllSales());
        btnStockSummary.addActionListener(e -> showStockSummary());
        btnLowStock.addActionListener(e -> showLowStock());
        btnBack.addActionListener(e -> frame.show("admin_dashboard"));

        // Show All Sales by default when panel loads
        showAllSales();
    }

    // ==================== TODAY'S SALES ====================
    private void showTodaySales() {
        tableModel.setRowCount(0);
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        String sql = "SELECT t.date, p.name, t.type, t.quantity, (t.quantity * p.price) as amount " +
                     "FROM transactions t JOIN products p ON t.product_id = p.id " +
                     "WHERE DATE(t.date) = ? ORDER BY t.date DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, today);
            ResultSet rs = ps.executeQuery();

            double totalRevenue = 0.0;
            int totalSales = 0;

            while (rs.next()) {
                double amount = rs.getDouble("amount");
                tableModel.addRow(new Object[]{
                        rs.getString("date"),
                        rs.getString("name"),
                        rs.getString("type"),
                        rs.getInt("quantity"),
                        String.format("$%.2f", amount)
                });
                totalRevenue += amount;
                totalSales++;
            }

            updateSummary(totalSales, totalRevenue);

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading today's sales.");
        }
    }

    // ==================== ALL SALES ====================
    private void showAllSales() {
        tableModel.setRowCount(0);

        String sql = "SELECT t.date, p.name, t.type, t.quantity, (t.quantity * p.price) as amount " +
                     "FROM transactions t JOIN products p ON t.product_id = p.id " +
                     "ORDER BY t.date DESC";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            double totalRevenue = 0.0;
            int totalSales = 0;

            while (rs.next()) {
                double amount = rs.getDouble("amount");
                tableModel.addRow(new Object[]{
                        rs.getString("date"),
                        rs.getString("name"),
                        rs.getString("type"),
                        rs.getInt("quantity"),
                        String.format("$%.2f", amount)
                });
                totalRevenue += amount;
                totalSales++;
            }

            updateSummary(totalSales, totalRevenue);

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading all sales.");
        }
    }

    // ==================== STOCK SUMMARY ====================
    private void showStockSummary() {
        tableModel.setRowCount(0);
        tableModel.setColumnIdentifiers(new String[]{"Product ID", "Product Name", "Current Stock", "Price"});

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name, quantity, price FROM products ORDER BY quantity")) {

            int totalProducts = 0;
            double totalStockValue = 0.0;

            while (rs.next()) {
                double stockValue = rs.getInt("quantity") * rs.getDouble("price");
                tableModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getInt("quantity"),
                        String.format("$%.2f", rs.getDouble("price"))
                });
                totalStockValue += stockValue;
                totalProducts++;
            }

            lblTotalSales.setText("Total Products: " + totalProducts);
            lblTotalRevenue.setText("Total Stock Value: $" + String.format("%.2f", totalStockValue));

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // ==================== LOW STOCK ALERT ====================
    private void showLowStock() {
        tableModel.setRowCount(0);
        tableModel.setColumnIdentifiers(new String[]{"ID", "Product Name", "Current Stock", "Status"});

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT id, name, quantity FROM products WHERE quantity <= 10 ORDER BY quantity")) {

            while (rs.next()) {
                String status = rs.getInt("quantity") <= 5 ? "CRITICAL" : "LOW";
                tableModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getInt("quantity"),
                        status
                });
            }

            lblTotalSales.setText("Low Stock Items");
            lblTotalRevenue.setText("Alert: Restock Needed");

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // ==================== UPDATE SUMMARY ====================
    private void updateSummary(int totalSales, double totalRevenue) {
        lblTotalSales.setText("Total Transactions: " + totalSales);
        lblTotalRevenue.setText("Total Revenue: $" + String.format("%.2f", totalRevenue));
    }
}