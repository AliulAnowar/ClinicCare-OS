package com.clinic.controller;

import com.clinic.util.DBConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.sql.*;
import java.time.LocalDate;

public class ReceptionController {

    // Tab 1 Fields
    @FXML private TilePane cabinTilePane;
    @FXML private TextField txtSelectedCabin;
    @FXML private TextField txtPatientName;
    @FXML private TextField txtPhone;
    @FXML private DatePicker dpDob;
    @FXML private ComboBox<String> comboGender;
    @FXML private TextField txtDoctor;
    @FXML private TextField txtAdvancePayment;
    @FXML private TextField txtAdmissionDiscount;

    // Tab 2 Fields (Lab)
    @FXML private TextField txtLabAdmissionId;
    @FXML private ComboBox<String> comboLabTests;
    @FXML private TextField txtLabPrice;
    @FXML private TextField txtLabDiscount;

    // Tab 3 Fields (Pharmacy)
    @FXML private TextField txtPharmaAdmissionId;
    @FXML private ComboBox<String> comboMedicines;
    @FXML private TextField txtPharmaQty;
    @FXML private TextField txtPharmaPrice;
    @FXML private TextField txtPharmaDiscount;

    // Tab 4 Fields (Billing)
    @FXML private TextField txtBillAdmissionId;
    @FXML private TextArea txtBillSummary;
    @FXML private TextField txtFinalBillDiscount;

    private int selectedCabinId = -1;

    @FXML
    public void initialize() {
        comboGender.setItems(FXCollections.observableArrayList("Male", "Female", "Other"));
        loadCabinGrid();
        loadLabTests();
        loadMedicines();
    }

    @FXML
    private void handleRefresh() {
        loadCabinGrid();
    }

    @FXML
    private void handleClear() {
        txtSelectedCabin.clear();
        txtPatientName.clear();
        txtPhone.clear();
        dpDob.setValue(null);
        comboGender.setValue(null);
        txtDoctor.clear();
        txtAdvancePayment.clear();
        txtAdmissionDiscount.clear();
        selectedCabinId = -1;
    }

    // TAB 1: ADMIT PATIENT & PRINT ADMISSION SLIP
    @FXML
    private void handleAdmit() {
        if (selectedCabinId == -1 || txtPatientName.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Input Error", "Please select a cabin and enter patient name.");
            return;
        }

        double advance = parseDouble(txtAdvancePayment.getText());
        double discount = parseDouble(txtAdmissionDiscount.getText());

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            String patientCode = "PAT-" + (System.currentTimeMillis() % 1000000);
            String admissionCode = "ADM-" + (System.currentTimeMillis() % 1000000);

            // 1. Insert Patient
            int patientId = -1;
            String sqlPatient = "INSERT INTO patients (patient_code, full_name, date_of_birth, gender, phone) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sqlPatient, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, patientCode);
                ps.setString(2, txtPatientName.getText().trim());
                ps.setDate(3, dpDob.getValue() != null ? Date.valueOf(dpDob.getValue()) : Date.valueOf(LocalDate.now()));
                ps.setString(4, comboGender.getValue() != null ? comboGender.getValue() : "Other");
                ps.setString(5, txtPhone.getText().trim());
                ps.executeUpdate();
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) patientId = rs.getInt(1);
            }

            // 2. Insert Admission
            long admissionId = -1;
            String sqlAdmission = "INSERT INTO admissions (admission_code, patient_id, doctor_id, cabin_id, admission_date, status, advance_payment) VALUES (?, ?, 1, ?, NOW(), 'ADMITTED', ?)";
            try (PreparedStatement ps = conn.prepareStatement(sqlAdmission, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, admissionCode);
                ps.setInt(2, patientId);
                ps.setInt(3, selectedCabinId);
                ps.setDouble(4, advance);
                ps.executeUpdate();
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) admissionId = rs.getLong(1);
            }

            // 3. Update Cabin Status
            try (PreparedStatement ps = conn.prepareStatement("UPDATE cabins SET status = 'OCCUPIED' WHERE cabin_id = ?")) {
                ps.setInt(1, selectedCabinId);
                ps.executeUpdate();
            }

            conn.commit();

            String printSlip = "=== CLINIC ADMISSION SLIP ===\n" +
                    "Admission ID: " + admissionId + "\n" +
                    "Admission Code: " + admissionCode + "\n" +
                    "Patient Name: " + txtPatientName.getText() + "\n" +
                    "Cabin: " + txtSelectedCabin.getText() + "\n" +
                    "Advance Payment: Tk " + advance + "\n" +
                    "Discount Applied: Tk " + discount + "\n" +
                    "Date: " + LocalDate.now() + "\n" +
                    "=============================";

            printDocument(printSlip);
            handleClear();
            loadCabinGrid();

        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
        }
    }

    // TAB 2: DISPATCH LAB TEST ORDER
    @FXML
    private void handleSendLabOrder() {
        int admissionId = Integer.parseInt(txtLabAdmissionId.getText().trim());
        String testName = comboLabTests.getValue();
        double price = parseDouble(txtLabPrice.getText());
        double discount = parseDouble(txtLabDiscount.getText());

        String sql = "INSERT INTO diagnostic_orders (admission_id, test_name, price, discount, status) VALUES (?, ?, ?, ?, 'PENDING')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, admissionId);
            ps.setString(2, testName);
            ps.setDouble(3, price);
            ps.setDouble(4, discount);
            ps.executeUpdate();

            // WebSocket broadcast trigger point to PC 2
            showAlert(Alert.AlertType.INFORMATION, "Socket Dispatched", "Lab Test order sent to Diagnostic Lab PC (PC 2)!");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
        }
    }

    // TAB 3: DISPATCH PHARMACY ORDER
    @FXML
    private void handleSendPharmaOrder() {
        int admissionId = Integer.parseInt(txtPharmaAdmissionId.getText().trim());
        String medName = comboMedicines.getValue();
        int qty = Integer.parseInt(txtPharmaQty.getText().trim());
        double unitPrice = parseDouble(txtPharmaPrice.getText());
        double discount = parseDouble(txtPharmaDiscount.getText());

        String sql = "INSERT INTO pharmacy_orders (admission_id, medicine_name, quantity, unit_price, discount, status) VALUES (?, ?, ?, ?, ?, 'PENDING')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, admissionId);
            ps.setString(2, medName);
            ps.setInt(3, qty);
            ps.setDouble(4, unitPrice);
            ps.setDouble(5, discount);
            ps.executeUpdate();

            // WebSocket broadcast trigger point to PC 3
            showAlert(Alert.AlertType.INFORMATION, "Socket Dispatched", "Medicine order sent to Pharmacy PC (PC 3)!");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
        }
    }

    // TAB 4: CALCULATE FINAL BILL
    @FXML
    private void handleFetchBill() {
        int admissionId = Integer.parseInt(txtBillAdmissionId.getText().trim());
        StringBuilder sb = new StringBuilder();
        double totalBill = 0.0;

        try (Connection conn = DBConnection.getConnection()) {
            sb.append("=== ITEMIZATION INVOICE FOR ADMISSION #").append(admissionId).append(" ===\n\n");

            // 1. Lab Tests
            sb.append("--- Diagnostic Lab Charges ---\n");
            String sqlLab = "SELECT test_name, price, discount FROM diagnostic_orders WHERE admission_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlLab)) {
                ps.setInt(1, admissionId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    double itemTotal = rs.getDouble("price") - rs.getDouble("discount");
                    totalBill += itemTotal;
                    sb.append(rs.getString("test_name")).append(" - Tk ").append(itemTotal).append("\n");
                }
            }

            // 2. Pharmacy Medicines
            sb.append("\n--- Pharmacy Charges ---\n");
            String sqlPharma = "SELECT medicine_name, quantity, unit_price, discount FROM pharmacy_orders WHERE admission_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlPharma)) {
                ps.setInt(1, admissionId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    double itemTotal = (rs.getInt("quantity") * rs.getDouble("unit_price")) - rs.getDouble("discount");
                    totalBill += itemTotal;
                    sb.append(rs.getString("medicine_name")).append(" (x").append(rs.getInt("quantity")).append(") - Tk ").append(itemTotal).append("\n");
                }
            }

            sb.append("\n=============================\n");
            sb.append("TOTAL CALCULATED AMOUNT: Tk ").append(totalBill);
            txtBillSummary.setText(sb.toString());

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
        }
    }

    // PRINT DOCUMENT
    @FXML
    private void handlePrintFinalBill() {
        double discount = parseDouble(txtFinalBillDiscount.getText());
        String textToPrint = txtBillSummary.getText() + "\nFinal Discount: Tk " + discount + "\n=== THANK YOU ===";
        printDocument(textToPrint);
    }

    private void printDocument(String content) {
        TextFlow printNode = new TextFlow(new Text(content));
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(null)) {
            boolean success = job.printPage(printNode);
            if (success) {
                job.endJob();
                showAlert(Alert.AlertType.INFORMATION, "Print Success", "Document sent to printer successfully.");
            }
        }
    }

    private void loadLabTests() {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT test_name, price FROM lab_tests");
             ResultSet rs = ps.executeQuery()) {
            ObservableList<String> list = FXCollections.observableArrayList();
            while (rs.next()) list.add(rs.getString("test_name"));
            comboLabTests.setItems(list);
            comboLabTests.setOnAction(e -> {
                try (PreparedStatement ps2 = conn.prepareStatement("SELECT price FROM lab_tests WHERE test_name = ?")) {
                    ps2.setString(1, comboLabTests.getValue());
                    ResultSet rs2 = ps2.executeQuery();
                    if (rs2.next()) txtLabPrice.setText(String.valueOf(rs2.getDouble("price")));
                } catch (Exception ex) {}
            });
        } catch (Exception e) {}
    }

    private void loadMedicines() {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT medicine_name, unit_price FROM medicines");
             ResultSet rs = ps.executeQuery()) {
            ObservableList<String> list = FXCollections.observableArrayList();
            while (rs.next()) list.add(rs.getString("medicine_name"));
            comboMedicines.setItems(list);
            comboMedicines.setOnAction(e -> {
                try (PreparedStatement ps2 = conn.prepareStatement("SELECT unit_price FROM medicines WHERE medicine_name = ?")) {
                    ps2.setString(1, comboMedicines.getValue());
                    ResultSet rs2 = ps2.executeQuery();
                    if (rs2.next()) txtPharmaPrice.setText(String.valueOf(rs2.getDouble("unit_price")));
                } catch (Exception ex) {}
            });
        } catch (Exception e) {}
    }

    public void loadCabinGrid() {
        cabinTilePane.getChildren().clear();
        String sql = "SELECT cabin_id, cabin_number, cabin_type, daily_rate, status FROM cabins ORDER BY cabin_id ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int cabinId = rs.getInt("cabin_id");
                String cabinNumber = rs.getString("cabin_number");
                String cabinType = rs.getString("cabin_type");
                double dailyRate = rs.getDouble("daily_rate");
                boolean isOccupied = "OCCUPIED".equalsIgnoreCase(rs.getString("status"));

                VBox card = new VBox(4);
                card.getStyleClass().addAll("cabin-card", isOccupied ? "cabin-occupied" : "cabin-available");
                card.getChildren().addAll(
                        new Label(cabinNumber),
                        new Label(cabinType + " (Tk " + dailyRate + "/day)"),
                        new Label(isOccupied ? "Occupied" : "Available")
                );
                if (!isOccupied) {
                    card.setOnMouseClicked(e -> {
                        selectedCabinId = cabinId;
                        txtSelectedCabin.setText(cabinNumber + " (" + cabinType + ")");
                    });
                }
                cabinTilePane.getChildren().add(card);
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private double parseDouble(String str) {
        try { return Double.parseDouble(str.trim()); } catch (Exception e) { return 0.0; }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}