package com.inventory;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class LoginPanel extends JPanel {

    JTextField txtUser;
    JPasswordField txtPass;
    JComboBox<String> roleBox;
    Main frame;

    public LoginPanel(Main frame) {
        this.frame = frame;
        setLayout(new GridLayout(5, 2, 10, 10));

        add(new JLabel("Username:"));
        txtUser = new JTextField();
        add(txtUser);

        add(new JLabel("Password:"));
        txtPass = new JPasswordField();
        add(txtPass);

        add(new JLabel("Role:"));
        roleBox = new JComboBox<>(new String[]{"Admin", "Staff"});
        add(roleBox);

        JButton btnLogin = new JButton("Login");
        JButton btnSignup = new JButton("Go Signup");

        add(btnLogin);
        add(btnSignup);

        btnLogin.addActionListener(e -> login());
        btnSignup.addActionListener(e -> frame.show("signup"));
    }

    private void login() {
        try {
            Connection conn = DBConnection.getConnection();

            String sql = "SELECT * FROM users WHERE username=? AND password=? AND role=?";
            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, txtUser.getText());
            ps.setString(2, String.valueOf(txtPass.getPassword()));
            ps.setString(3, roleBox.getSelectedItem().toString());

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {

                String role = rs.getString("role");

                JOptionPane.showMessageDialog(this, "Login Success as " + role);

                // 🔥 ROLE CONTROL
                if (role.equals("Admin")) {
                    frame.show("admin_dashboard");
                } else {
                    frame.show("staff_dashboard");
                }

            } else {
                JOptionPane.showMessageDialog(this, "Invalid Login!");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
