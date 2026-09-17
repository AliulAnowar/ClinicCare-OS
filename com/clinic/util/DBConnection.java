package com.clinic.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/clinic_db";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    static {
        try {
            // Register driver instance directly without depending on Class.forName reflection
            DriverManager.registerDriver(new com.mysql.cj.jdbc.Driver());
        } catch (SQLException e) {
            System.err.println("Database Driver Registration Failed: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }
}