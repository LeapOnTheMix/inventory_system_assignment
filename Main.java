package com.inventory;

import javax.swing.*;
import java.awt.*;

public class Main extends JFrame {

    CardLayout cardLayout;
    JPanel container;

    public Main() {
        cardLayout = new CardLayout();
        container = new JPanel(cardLayout);

        // Add all panels
        container.add(new LoginPanel(this), "login");
        container.add(new SignupPanel(this), "signup");
        container.add(new AdminDashboard(this), "admin_dashboard");
        container.add(new StaffDashboard(this), "staff_dashboard");

        // Admin Panels
        container.add(new ProductManagementPanel(this), "product_management");
        container.add(new PurchasePanel(this), "purchase");
        container.add(new SalesPOSPanel(this), "sales_pos");
        container.add(new SupplierPanel(this), "suppliers");
        container.add(new UserManagementPanel(this), "user_management");
        container.add(new ReportsPanel(this), "reports");
        // Staff Panels
        container.add(new ViewProductsPanel(this), "view_products");
        container.add(new StaffSalesPOSPanel(this), "staff_sales_pos");

        add(container);

        setTitle("Inventory System");
        setSize(800, 600);           // Increased size
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public void show(String name) {
        cardLayout.show(container, name);
    }

    public static void main(String[] args) {
        new Main();
    }
}