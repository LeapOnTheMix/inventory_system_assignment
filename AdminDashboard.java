package com.inventory;

import javax.swing.*;
import java.awt.*;

public class AdminDashboard extends JPanel {

    Main frame;

    public AdminDashboard(Main frame) {
        this.frame = frame;
        setLayout(new GridLayout(4, 2, 15, 15));

        // Create buttons with action listeners
        add(createButton("Product Management", "product_management"));
        add(createButton("Purchase", "purchase"));
        add(createButton("Sales (POS)", "sales_pos"));
        add(createButton("Suppliers", "suppliers"));
        add(createButton("User Management", "user_management"));
        add(createButton("Reports", "reports"));

        JButton logout = new JButton("Logout");
        logout.setFont(new Font("Arial", Font.BOLD, 14));
        logout.addActionListener(e -> frame.show("login"));
        add(logout);
    }

    private JButton createButton(String text, String panelName) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.BOLD, 14));
        btn.addActionListener(e -> frame.show(panelName));
        return btn;
    }
}