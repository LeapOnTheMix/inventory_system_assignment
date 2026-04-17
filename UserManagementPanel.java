package com.inventory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class UserManagementPanel extends JPanel {

    private Main frame;
    private JTable userTable;
    private DefaultTableModel tableModel;

    public UserManagementPanel(Main frame) {
        this.frame = frame;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Title
        JLabel title = new JLabel("User Management", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 24));
        add(title, BorderLayout.NORTH);

        // Table - Removed created_at column
        String[] columns = {"ID", "Username", "Role"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        userTable = new JTable(tableModel);
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userTable.setRowHeight(28);

        add(new JScrollPane(userTable), BorderLayout.CENTER);

        // Button Panel
        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 5, 0));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        JButton btnAdd = new JButton("Add New User");
        JButton btnUpdate = new JButton("Update Selected");
        JButton btnDelete = new JButton("Delete Selected");
        JButton btnRefresh = new JButton("Refresh");

        leftPanel.add(btnAdd);
        leftPanel.add(btnUpdate);
        leftPanel.add(btnDelete);
        leftPanel.add(btnRefresh);

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
        btnAdd.addActionListener(e -> openAddUserDialog());
        btnUpdate.addActionListener(e -> openUpdateUserDialog());
        btnDelete.addActionListener(e -> deleteUser());
        btnRefresh.addActionListener(e -> loadUsers());
        btnBack.addActionListener(e -> frame.show("admin_dashboard"));

        loadUsers();
    }

    // ==================== LOAD USERS (Fixed) ====================
    private void loadUsers() {
        tableModel.setRowCount(0);

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, username, role FROM users ORDER BY id")) {

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("role")
                });
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading users:\n" + ex.getMessage());
        }
    }

    // ==================== ADD USER DIALOG ====================
    private void openAddUserDialog() {
        JDialog dialog = createUserDialog("Add New User", null);
        dialog.setVisible(true);
    }

    // ==================== UPDATE USER DIALOG ====================
    private void openUpdateUserDialog() {
        int row = userTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a user to update!");
            return;
        }

        int id = (int) tableModel.getValueAt(row, 0);
        String username = (String) tableModel.getValueAt(row, 1);
        String role = (String) tableModel.getValueAt(row, 2);

        JDialog dialog = createUserDialog("Update User", new UserData(id, username, role));
        dialog.setVisible(true);
    }

    // ==================== DIALOG CREATOR ====================
    private JDialog createUserDialog(String title, UserData data) {
        JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parentFrame, title, true);
        dialog.setSize(450, 320);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridLayout(4, 2, 15, 20));
        panel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        JTextField txtUsername = new JTextField(20);
        JPasswordField txtPassword = new JPasswordField(20);
        JComboBox<String> roleBox = new JComboBox<>(new String[]{"Admin", "Staff"});

        if (data != null) {
            txtUsername.setText(data.username);
            roleBox.setSelectedItem(data.role);
        }

        panel.add(new JLabel("Username:"));
        panel.add(txtUsername);
        panel.add(new JLabel("Password:"));
        panel.add(txtPassword);
        panel.add(new JLabel("Role:"));
        panel.add(roleBox);

        JButton btnSave = new JButton(data == null ? "Add User" : "Update User");
        JButton btnCancel = new JButton("Cancel");

        btnSave.addActionListener(e -> {
            saveUser(txtUsername.getText().trim(), 
                     new String(txtPassword.getPassword()).trim(),
                     roleBox.getSelectedItem().toString(),
                     data != null ? data.id : -1);
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

    // ==================== SAVE USER ====================
    private void saveUser(String username, String password, String role, int updateId) {
        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username is required!");
            return;
        }
        if (updateId == -1 && password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Password is required for new user!");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            String sql;
            if (updateId == -1) {
                sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, username);
                    ps.setString(2, password);
                    ps.setString(3, role);
                    ps.executeUpdate();
                }
                JOptionPane.showMessageDialog(this, "✅ User added successfully!");
            } else {
                if (password.isEmpty()) {
                    sql = "UPDATE users SET username=?, role=? WHERE id=?";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, username);
                        ps.setString(2, role);
                        ps.setInt(3, updateId);
                        ps.executeUpdate();
                    }
                } else {
                    sql = "UPDATE users SET username=?, password=?, role=? WHERE id=?";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, username);
                        ps.setString(2, password);
                        ps.setString(3, role);
                        ps.setInt(4, updateId);
                        ps.executeUpdate();
                    }
                }
                JOptionPane.showMessageDialog(this, "✅ User updated successfully!");
            }
            loadUsers();
        } catch (SQLException ex) {
            ex.printStackTrace();
            if (ex.getMessage().contains("Duplicate")) {
                JOptionPane.showMessageDialog(this, "Username already exists!");
            } else {
                JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage());
            }
        }
    }

    // ==================== DELETE USER ====================
    private void deleteUser() {
        int row = userTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a user to delete!");
            return;
        }

        int id = (int) tableModel.getValueAt(row, 0);
        String username = (String) tableModel.getValueAt(row, 1);
        String role = (String) tableModel.getValueAt(row, 2);

        // Prevent deleting last Admin
        if (role.equals("Admin")) {
            try (Connection conn = DBConnection.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users WHERE role = 'Admin'")) {
                if (rs.next() && rs.getInt(1) <= 1) {
                    JOptionPane.showMessageDialog(this, "Cannot delete the last Admin user!");
                    return;
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }

        int confirm = JOptionPane.showConfirmDialog(this, 
                "Delete user: " + username + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "User deleted successfully!");
            loadUsers();
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Delete failed: " + ex.getMessage());
        }
    }

    private static class UserData {
        int id;
        String username;
        String role;

        UserData(int id, String username, String role) {
            this.id = id;
            this.username = username;
            this.role = role;
        }
    }
}