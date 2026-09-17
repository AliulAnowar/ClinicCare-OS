package com.clinic.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.sql.*;

public class LoginController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<String> comboRole;
    @FXML private Label lblError;
	@FXML private Button btnLogin;

    private final String DB_URL = "jdbc:mysql://localhost:3306/clinic_db";
    private final String DB_USER = "root";
    private final String DB_PASS = ""; 

    @FXML
    public void initialize() {
        comboRole.getItems().addAll("ADMIN", "TECHNICAL", "RECEPTIONIST");
    }

    @FXML
    private void handleLogin() {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();
        String role = comboRole.getValue();

        if (username.isEmpty() || password.isEmpty() || role == null) {
            showError("Please enter username, password, and select a role.");
            return;
        }

        String query = "SELECT user_id, full_name, password_hash, role FROM users WHERE username = ? AND role = ? AND is_active = TRUE";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setString(1, username);
            ps.setString(2, role);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String storedPassword = rs.getString("password_hash");

                if (password.equals(storedPassword)) {
                    int userId = rs.getInt("user_id");
                    String fullName = rs.getString("full_name");
                    navigateToDashboard(role, userId, fullName);
                } else {
                    showError("Invalid username or password.");
                }
            } else {
                showError("User record not found or inactive.");
            }

        } catch (SQLException e) {
            showError("Database Connection Error: " + e.getMessage());
        }
    }

    private void navigateToDashboard(String role, int userId, String fullName) {
    try {
        String normalizedRole = (role != null) ? role.trim().toUpperCase() : "";

        String fxmlPath = switch (normalizedRole) {
            case "RECEPTIONIST" -> "/com/clinic/view/reception_view.fxml";
            case "TECHNICAL", "LAB_TECH", "PHARMACIST" -> "/com/clinic/view/technical_view.fxml";
            case "ADMIN" -> "/com/clinic/view/admin_view.fxml";
            default -> throw new IllegalArgumentException("Unknown role: " + role);
        };

        java.net.URL fxmlUrl = getClass().getResource(fxmlPath);
        if (fxmlUrl == null) {
            java.io.File file = new java.io.File("com/clinic/view/" + fxmlPath.substring(fxmlPath.lastIndexOf('/') + 1));
            if (file.exists()) {
                fxmlUrl = file.toURI().toURL();
            } else {
                throw new java.io.FileNotFoundException("Could not find FXML file: " + fxmlPath);
            }
        }

        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Parent root = loader.load();

        Stage stage = (Stage) txtUsername.getScene().getWindow();
        stage.setScene(new Scene(root));
		stage.setMaximized(true);
        stage.setTitle("ClinicCare OS - " + normalizedRole + " Station");
        stage.show();

    } catch (Throwable e) {
        // THIS PRINTS THE REAL FXML LOAD ERROR TO YOUR CMD TERMINAL:
        System.err.println("=== FXML LOADING ERROR ===");
        e.printStackTrace();
        
        lblError.setText("Failed to load dashboard screen: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
        lblError.setVisible(true);
    }
}

    private void showError(String message) {
        lblError.setText(message);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }
}