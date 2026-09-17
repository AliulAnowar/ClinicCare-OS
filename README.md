# ClinicCare OS - Admin Station

A JavaFX and MySQL-based desktop application designed for managing medical clinic operations, financial analytics, diagnostic cataloging, inventory tracking, and cabin admissions.

---

## Features

* **Executive Analytics**: Real-time daily and monthly revenue breakdowns across departments (Cabins, Lab Tests, Pharmacy) using interactive JavaFX charts.
* **Cabin Occupancy Tracking**: Visual pie charts and status metrics for real-time cabin availability and occupancy.
* **Diagnostic Lab Catalog**: Management of lab tests, pricing, and category structures.
* **Pharmacy Inventory**: Tracking of stock levels and pharmaceutical sales.
* **Role-Based Access**: Secure login routing for clinic administration and staff.

---

## Prerequisites

Ensure you have the following installed before setting up the project:

* **Java Development Kit (JDK)**: Version 21 or higher
* **JavaFX SDK**: Version 21 or higher
* **MySQL Server**: Version 8.0 or higher
* **MySQL Connector/J**: JDBC driver `mysql-connector-j-8.3.0.jar`

---

## Project Structure

```text
C:\MyClinicProject
├── com
│   └── clinic
│       ├── MainApp.java
│       ├── controller
│       │   ├── AdminDashboardController.java
│       │   └── LoginController.java
│       ├── util
│       │   └── DBConnection.java
│       └── view
│           └── admin_view.fxml
├── lib
│   └── mysql-connector-j-8.3.0.jar
├── README.md
├── SECURITY.md
└── .gitignore