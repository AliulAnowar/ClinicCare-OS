package com.clinic;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Load the Login screen FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/clinic/view/login_view.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("ClinicCare OS - Station Login");
        primaryStage.setScene(new Scene(root));
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}