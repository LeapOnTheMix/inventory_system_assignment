package com.inventory;

import javax.swing.*;
import java.awt.*;

public class StaffDashboard extends JPanel {

    private Main frame;

    public StaffDashboard(Main frame) {
        this.frame = frame;
        setLayout(new GridLayout(3, 1, 15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Title
        JLabel title = new JLabel("Staff Dashboard", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        add(title);

        // Buttons
        add(createButton("View Products", "view_products"));
        add(createButton("Sales (POS)", "staff_sales_pos"));

        // Logout Button
        JButton logout = new JButton("Logout");
        logout.setFont(new Font("Arial", Font.BOLD, 14));
        logout.addActionListener(e -> frame.show("login"));
        add(logout);
    }

    private JButton createButton(String text, String panelName) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.BOLD, 14));
        btn.setPreferredSize(new Dimension(200, 50));
        btn.addActionListener(e -> frame.show(panelName));
        return btn;
    }
}