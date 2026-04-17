package com.inventory;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class SignupPanel extends JPanel {

    JTextField txtUser;
    JPasswordField txtPass;
    JComboBox<String> roleBox;
    Main frame;

    public SignupPanel(Main frame) {
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

        JButton btnRegister = new JButton("Register");
        JButton btnBack = new JButton("Back");

        add(btnRegister);
        add(btnBack);

        btnRegister.addActionListener(e -> register());
        btnBack.addActionListener(e -> frame.show("login"));
    }

    private void register() {
        try {
            Connection conn = DBConnection.getConnection();

            String sql = "INSERT INTO users(username, password, role) VALUES (?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, txtUser.getText());
            ps.setString(2, String.valueOf(txtPass.getPassword()));
            ps.setString(3, roleBox.getSelectedItem().toString());

            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Registered Successfully!");
            frame.show("login");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
