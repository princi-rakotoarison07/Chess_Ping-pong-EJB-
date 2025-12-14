package com.chessping.client.ui;

import com.chessping.client.MainApp;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.io.IOException;

public class MainMenuController {

    @FXML
    private Button localButton;

    @FXML
    private Button serverButton;

    @FXML
    private Button clientButton;

    private Stage getStage() {
        return (Stage) localButton.getScene().getWindow();
    }

    private void loadScene(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/fxml/" + fxml));
            Parent root = loader.load();
            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(MainApp.class.getResource("/css/styles.css").toExternalForm());
            getStage().setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onLocalGame(ActionEvent event) {
        loadScene("Configuration.fxml");
    }

    @FXML
    private void onCreateServer(ActionEvent event) {
        loadScene("Configuration.fxml");
    }

    @FXML
    private void onJoinClient(ActionEvent event) {
        loadScene("NetworkSetup.fxml");
    }
}
