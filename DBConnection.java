package com.inventory;

import java.sql.Connection;
import java.sql.DriverManager;



public class DBConnection {
	public static Connection getConnection() {
        Connection conn = null;

        try {
            String url = "jdbc:mysql://localhost:3306/inventory_db";
            String user = "root";
            String password = "";

            conn = DriverManager.getConnection(url, user, password);
            System.out.println("Connected!");

        } catch (Exception e) {
            e.printStackTrace();
        }

        return conn;
    }

}
