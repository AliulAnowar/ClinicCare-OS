package com.clinic.controller;

import com.clinic.util.DBConnection;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.Month;

public class AdminDashboardController {

    @FXML private Label lblTodayIncome;
    @FXML private Label lblMonthlyIncome;
    @FXML private Label lblCabinOccupancy;

    @FXML private ComboBox<String> cmbMonth;
    @FXML private ComboBox<Integer> cmbYear;

    @FXML private PieChart chartCabinOccupancy;
    @FXML private BarChart<String, Number> chartRevenue;
    @FXML private BarChart<String, Number> chartMonthlyRevenue;

    @FXML
    public void initialize() {
        setupDateFilters();
        loadOccupancyData();
        loadDailyData();
        
        int initialMonth = (cmbMonth != null && cmbMonth.getSelectionModel().getSelectedIndex() >= 0) 
                           ? cmbMonth.getSelectionModel().getSelectedIndex() + 1 
                           : LocalDate.now().getMonthValue();
        int initialYear = (cmbYear != null && cmbYear.getValue() != null) 
                          ? cmbYear.getValue() 
                          : LocalDate.now().getYear();

        loadMonthlyData(initialMonth, initialYear);
    }

    private void setupDateFilters() {
        if (cmbMonth == null || cmbYear == null) return;

        cmbMonth.getItems().clear();
        for (Month month : Month.values()) {
            cmbMonth.getItems().add(month.name());
        }

        cmbYear.getItems().clear();
        int currentYear = LocalDate.now().getYear();
        for (int i = currentYear; i >= currentYear - 5; i--) {
            cmbYear.getItems().add(i);
        }

        cmbMonth.getSelectionModel().select(LocalDate.now().getMonthValue() - 1);
        cmbYear.getSelectionModel().select(Integer.valueOf(currentYear));
    }

    private void loadOccupancyData() {
        String sql = "SELECT " +
                     "  SUM(CASE WHEN UPPER(status) = 'OCCUPIED' THEN 1 ELSE 0 END) AS occupied, " +
                     "  SUM(CASE WHEN UPPER(status) = 'AVAILABLE' THEN 1 ELSE 0 END) AS available " +
                     "FROM cabins";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                int occupied = rs.getInt("occupied");
                int available = rs.getInt("available");
                int total = occupied + available;

                ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
                    new PieChart.Data("Occupied (" + occupied + ")", occupied),
                    new PieChart.Data("Available (" + available + ")", available)
                );

                if (chartCabinOccupancy != null) {
                    chartCabinOccupancy.setData(pieData);
                }
                if (lblCabinOccupancy != null) {
                    lblCabinOccupancy.setText(occupied + " / " + total);
                }
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Occupancy Query Exception: " + e.getMessage());
        }
    }

    private void loadDailyData() {
        if (chartRevenue == null) return;
        
        chartRevenue.getData().clear();
        XYChart.Series<String, Number> dailySeries = new XYChart.Series<>();
        dailySeries.setName("Today's Revenue");

        double grandTotal = 0;

        // 1. Cabins Today
        String cabinSql = "SELECT COALESCE(SUM(advance_payment), 0) AS total FROM admissions " +
                          "WHERE status = 'ADMITTED' AND DATE(admission_date) = CURRENT_DATE()";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(cabinSql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                double val = rs.getDouble("total");
                grandTotal += val;
                dailySeries.getData().add(new XYChart.Data<>("Cabins", val));
            }
        } catch (Exception e) {
            System.err.println("[WARNING] Daily admissions query: " + e.getMessage());
        }

        // 2. Lab Today
        String labSql = "SELECT COALESCE(SUM(amount), 0) AS total FROM lab_payments " +
                        "WHERE DATE(payment_date) = CURRENT_DATE()";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(labSql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                double val = rs.getDouble("total");
                grandTotal += val;
                dailySeries.getData().add(new XYChart.Data<>("Lab Tests", val));
            }
        } catch (Exception e) {
            dailySeries.getData().add(new XYChart.Data<>("Lab Tests", 0));
        }

        // 3. Pharmacy Today
        String pharmacySql = "SELECT COALESCE(SUM(amount), 0) AS total FROM pharmacy_payments " +
                             "WHERE DATE(payment_date) = CURRENT_DATE()";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(pharmacySql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                double val = rs.getDouble("total");
                grandTotal += val;
                dailySeries.getData().add(new XYChart.Data<>("Pharmacy", val));
            }
        } catch (Exception e) {
            dailySeries.getData().add(new XYChart.Data<>("Pharmacy", 0));
        }

        chartRevenue.getData().add(dailySeries);
        if (lblTodayIncome != null) {
            lblTodayIncome.setText(String.format("৳ %,.2f", grandTotal));
        }
    }

    @FXML
    private void handleFilterMonthlyData() {
        if (cmbMonth == null || cmbYear == null) return;
        int selectedMonth = cmbMonth.getSelectionModel().getSelectedIndex() + 1;
        int selectedYear = cmbYear.getValue();
        loadMonthlyData(selectedMonth, selectedYear);
    }

    @FXML
    private void handleSaveTest() {
        System.out.println("handleSaveTest triggered");
    }

    private void loadMonthlyData(int month, int year) {
        if (chartMonthlyRevenue == null) return;

        chartMonthlyRevenue.getData().clear();
        XYChart.Series<String, Number> monthlySeries = new XYChart.Series<>();
        String monthName = (cmbMonth != null && cmbMonth.getValue() != null) ? cmbMonth.getValue() : "Selected Period";
        monthlySeries.setName("Revenue for " + monthName + " " + year);

        double grandTotal = 0;

        // 1. Monthly Cabins (admissions)
        String cabinSql = "SELECT COALESCE(SUM(advance_payment), 0) AS total FROM admissions " +
                          "WHERE status = 'ADMITTED' AND MONTH(admission_date) = ? AND YEAR(admission_date) = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(cabinSql)) {
            stmt.setInt(1, month);
            stmt.setInt(2, year);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    double cabinTotal = rs.getDouble("total");
                    grandTotal += cabinTotal;
                    monthlySeries.getData().add(new XYChart.Data<>("Cabins", cabinTotal));
                }
            }
        } catch (Exception e) {
            System.err.println("[ERROR] admissions table query: " + e.getMessage());
        }

        // 2. Monthly Lab Payments
        String labSql = "SELECT COALESCE(SUM(amount), 0) AS total FROM lab_payments " +
                        "WHERE MONTH(payment_date) = ? AND YEAR(payment_date) = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(labSql)) {
            stmt.setInt(1, month);
            stmt.setInt(2, year);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    double labTotal = rs.getDouble("total");
                    grandTotal += labTotal;
                    monthlySeries.getData().add(new XYChart.Data<>("Lab Tests", labTotal));
                }
            }
        } catch (Exception e) {
            monthlySeries.getData().add(new XYChart.Data<>("Lab Tests", 0));
        }

        // 3. Monthly Pharmacy Payments
        String pharmacySql = "SELECT COALESCE(SUM(amount), 0) AS total FROM pharmacy_payments " +
                             "WHERE MONTH(payment_date) = ? AND YEAR(payment_date) = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(pharmacySql)) {
            stmt.setInt(1, month);
            stmt.setInt(2, year);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    double pharmacyTotal = rs.getDouble("total");
                    grandTotal += pharmacyTotal;
                    monthlySeries.getData().add(new XYChart.Data<>("Pharmacy", pharmacyTotal));
                }
            }
        } catch (Exception e) {
            monthlySeries.getData().add(new XYChart.Data<>("Pharmacy", 0));
        }

        chartMonthlyRevenue.getData().add(monthlySeries);
        if (lblMonthlyIncome != null) {
            lblMonthlyIncome.setText(String.format("৳ %,.2f", grandTotal));
        }
    }




    @FXML
    private void handleSaveMedicine() {
        // Save medicine logic
    }

    @FXML
    private void handleSaveCabin() {
        // Save cabin logic
    }
}