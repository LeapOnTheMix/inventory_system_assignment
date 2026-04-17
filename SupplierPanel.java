package com.inventory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class SupplierPanel extends JPanel {

    private Main frame;
    private JTable supplierTable;
    private DefaultTableModel tableModel;

    public SupplierPanel(Main frame) {
        this.frame = frame;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Title
        JLabel title = new JLabel("Suppliers Management", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 24));
        add(title, BorderLayout.NORTH);

        // Table
        String[] columns = {"ID", "Supplier Name", "Contact Person", "Phone", "Email", "Address"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        supplierTable = new JTable(tableModel);
        supplierTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        supplierTable.setRowHeight(28);

        add(new JScrollPane(supplierTable), BorderLayout.CENTER);

        // Button Panel
        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 5, 0));

        // Left: Action Buttons
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        JButton btnAdd = new JButton("Add New Supplier");
        JButton btnUpdate = new JButton("Update Selected");
        JButton btnDelete = new JButton("Delete Selected");
        JButton btnRefresh = new JButton("Refresh");

        leftPanel.add(btnAdd);
        leftPanel.add(btnUpdate);
        leftPanel.add(btnDelete);
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
        btnAdd.addActionListener(e -> openAddSupplierDialog());
        btnUpdate.addActionListener(e -> openUpdateSupplierDialog());
        btnDelete.addActionListener(e -> deleteSupplier());
        btnRefresh.addActionListener(e -> loadSuppliers());
        btnBack.addActionListener(e -> frame.show("admin_dashboard"));

        // Load data when panel opens
        loadSuppliers();
    }

    // ==================== LOAD SUPPLIERS ====================
    private void loadSuppliers() {
        tableModel.setRowCount(0);

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM suppliers ORDER BY id")) {

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("contact_person"),
                        rs.getString("phone"),
                        rs.getString("email"),
                        rs.getString("address")
                });
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading suppliers:\n" + ex.getMessage());
        }
    }

    // ==================== ADD SUPPLIER DIALOG ====================
    private void openAddSupplierDialog() {
        JDialog dialog = createSupplierDialog("Add New Supplier", null);
        dialog.setVisible(true);
    }

    // ==================== UPDATE SUPPLIER DIALOG ====================
    private void openUpdateSupplierDialog() {
        int row = supplierTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a supplier to update!");
            return;
        }

        int id = (int) tableModel.getValueAt(row, 0);
        String name = (String) tableModel.getValueAt(row, 1);
        String contact = (String) tableModel.getValueAt(row, 2);
        String phone = (String) tableModel.getValueAt(row, 3);
        String email = (String) tableModel.getValueAt(row, 4);
        String address = (String) tableModel.getValueAt(row, 5);

        JDialog dialog = createSupplierDialog("Update Supplier", 
            new SupplierData(id, name, contact, phone, email, address));
        dialog.setVisible(true);
    }

    // ==================== DIALOG CREATOR (Reusable) ====================
    private JDialog createSupplierDialog(String title, SupplierData data) {
        JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parentFrame, title, true);
        dialog.setSize(500, 380);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridLayout(6, 2, 15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        JTextField txtName = new JTextField(20);
        JTextField txtContact = new JTextField(20);
        JTextField txtPhone = new JTextField(20);
        JTextField txtEmail = new JTextField(20);
        JTextArea txtAddress = new JTextArea(3, 20);
        txtAddress.setLineWrap(true);

        if (data != null) {
            txtName.setText(data.name);
            txtContact.setText(data.contactPerson);
            txtPhone.setText(data.phone);
            txtEmail.setText(data.email);
            txtAddress.setText(data.address);
        }

        panel.add(new JLabel("Supplier Name:"));
        panel.add(txtName);
        panel.add(new JLabel("Contact Person:"));
        panel.add(txtContact);
        panel.add(new JLabel("Phone:"));
        panel.add(txtPhone);
        panel.add(new JLabel("Email:"));
        panel.add(txtEmail);
        panel.add(new JLabel("Address:"));
        panel.add(new JScrollPane(txtAddress));

        JButton btnSave = new JButton(data == null ? "Add Supplier" : "Update Supplier");
        JButton btnCancel = new JButton("Cancel");

        btnSave.addActionListener(e -> {
            saveSupplier(txtName.getText().trim(), txtContact.getText().trim(),
                        txtPhone.getText().trim(), txtEmail.getText().trim(),
                        txtAddress.getText().trim(), data != null ? data.id : -1);
            dialog.dispose();
        });

        btnCancel.addActionListener(e -> dialog.dispose());

        JPanel btnPanel = new JPanel(new FlowLayout());
        btnPanel.add(btnSave);
        btnPanel.add(btnCancel);

        panel.add(new JLabel(""));
        panel.add(btnPanel);

        dialog.add(panel);
        return dialog;
    }

    // ==================== SAVE SUPPLIER ====================
    private void saveSupplier(String name, String contact, String phone, String email, String address, int updateId) {
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Supplier Name is required!");
            return;
        }

        String sql = (updateId == -1) ?
                "INSERT INTO suppliers (name, contact_person, phone, email, address) VALUES (?, ?, ?, ?, ?)" :
                "UPDATE suppliers SET name=?, contact_person=?, phone=?, email=?, address=? WHERE id=?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, contact);
            ps.setString(3, phone);
            ps.setString(4, email);
            ps.setString(5, address);
            if (updateId != -1) ps.setInt(6, updateId);

            ps.executeUpdate();

            String msg = (updateId == -1) ? "✅ Supplier Added Successfully!" : "✅ Supplier Updated Successfully!";
            JOptionPane.showMessageDialog(this, msg);
            loadSuppliers();

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage());
        }
    }

    // ==================== DELETE SUPPLIER ====================
    private void deleteSupplier() {
        int row = supplierTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a supplier to delete!");
            return;
        }

        int id = (int) tableModel.getValueAt(row, 0);
        String name = (String) tableModel.getValueAt(row, 1);

        int confirm = JOptionPane.showConfirmDialog(this, 
                "Delete supplier: " + name + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM suppliers WHERE id = ?")) {

            ps.setInt(1, id);
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Supplier deleted successfully!");
            loadSuppliers();

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Delete failed: " + ex.getMessage());
        }
    }

    // ==================== INNER CLASS ====================
    private static class SupplierData {
        int id;
        String name;
        String contactPerson;
        String phone;
        String email;
        String address;

        SupplierData(int id, String name, String contactPerson, String phone, String email, String address) {
            this.id = id;
            this.name = name;
            this.contactPerson = contactPerson;
            this.phone = phone;
            this.email = email;
            this.address = address;
        }
    }
}