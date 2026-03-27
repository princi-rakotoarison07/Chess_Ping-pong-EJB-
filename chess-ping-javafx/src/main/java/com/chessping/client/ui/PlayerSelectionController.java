package com.chessping.client.ui;

import com.chessping.client.MainApp;
import com.chessping.client.model.Player;
import com.chessping.client.service.PlayerService;
import com.chessping.client.session.GameSession;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class PlayerSelectionController {

    @FXML
    private ComboBox<Player> whitePlayerCombo;

    @FXML
    private ComboBox<Player> blackPlayerCombo;

    @FXML
    private TextField whiteNewNameField;

    @FXML
    private TextField blackNewNameField;

    private final PlayerService playerService =
            new PlayerService("http://localhost:8080/chess-ping-ejb/api");

    @FXML
    private void initialize() {
        GameSession.reset();

        StringConverter<Player> converter = new StringConverter<>() {
            @Override
            public String toString(Player player) {
                return player == null ? "" : player.getName();
            }

            @Override
            public Player fromString(String s) {
                return null; // non utilisé
            }
        };

        whitePlayerCombo.setConverter(converter);
        blackPlayerCombo.setConverter(converter);

        reloadPlayers();
    }

    private void reloadPlayers() {
        try {
            List<Player> players = playerService.findAll();
            whitePlayerCombo.setItems(FXCollections.observableArrayList(players));
            blackPlayerCombo.setItems(FXCollections.observableArrayList(players));
        } catch (IOException | InterruptedException e) {
            showError("Erreur lors du chargement des joueurs: " + e.getMessage());
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onCreateWhitePlayer() {
        String name = whiteNewNameField.getText();
        if (name == null || name.isBlank()) {
            showError("Le nom du joueur blanc ne peut pas être vide.");
            return;
        }
        try {
            Player created = playerService.create(name.trim());
            reloadPlayers();
            whitePlayerCombo.getSelectionModel().select(created);
            whiteNewNameField.clear();
        } catch (IOException | InterruptedException e) {
            showError("Erreur lors de la création du joueur blanc: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onCreateBlackPlayer() {
        String name = blackNewNameField.getText();
        if (name == null || name.isBlank()) {
            showError("Le nom du joueur noir ne peut pas être vide.");
            return;
        }
        try {
            Player created = playerService.create(name.trim());
            reloadPlayers();
            blackPlayerCombo.getSelectionModel().select(created);
            blackNewNameField.clear();
        } catch (IOException | InterruptedException e) {
            showError("Erreur lors de la création du joueur noir: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onStartGame() {
        Player white = whitePlayerCombo.getValue();
        Player black = blackPlayerCombo.getValue();

        if (white == null || black == null) {
            showError("Veuillez sélectionner un joueur blanc et un joueur noir.");
            return;
        }
        if (white.getId().equals(black.getId())) {
            showError("Les deux joueurs doivent être différents.");
            return;
        }

        GameSession.playerWhite = white;
        GameSession.playerBlack = black;

        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/fxml/Configuration.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(MainApp.class.getResource("/css/styles.css").toExternalForm());

            Stage stage = (Stage) whitePlayerCombo.getScene().getWindow();
            stage.setScene(scene);
        } catch (IOException e) {
            showError("Erreur lors de l'ouverture de l'écran de configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
