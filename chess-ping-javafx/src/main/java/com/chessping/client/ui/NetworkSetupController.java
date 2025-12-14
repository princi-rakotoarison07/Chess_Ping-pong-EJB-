package com.chessping.client.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class NetworkSetupController {

    @FXML
    private TextField ipField;

    @FXML
    private TextField portField;

    @FXML
    private Label errorLabel;

    @FXML
    private void onConnect() {
        // TODO: implémenter la connexion réseau; pour l'instant, juste afficher les valeurs
        String ip = ipField.getText();
        String port = portField.getText();
        errorLabel.setText("Connexion non implémentée (" + ip + ":" + port + ")");
    }
}
