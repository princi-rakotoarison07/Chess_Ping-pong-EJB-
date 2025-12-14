package com.chessping.client.ui;

import com.chessping.client.MainApp;
import com.chessping.client.chess.model.PieceTypeDTO;
import com.chessping.client.service.PieceTypeService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class ConfigurationController {

    @FXML
    private ComboBox<Integer> rowsCombo;

    @FXML
    private TableView<PieceTypeDTO> whiteTable;

    @FXML
    private TableView<PieceTypeDTO> blackTable;

    @FXML
    private TableColumn<PieceTypeDTO, String> whiteTypeColumn;

    @FXML
    private TableColumn<PieceTypeDTO, Integer> whiteCountColumn;

    @FXML
    private TableColumn<PieceTypeDTO, Integer> whiteLifeColumn;

    @FXML
    private TableColumn<PieceTypeDTO, String> blackTypeColumn;

    @FXML
    private TableColumn<PieceTypeDTO, Integer> blackCountColumn;

    @FXML
    private TableColumn<PieceTypeDTO, Integer> blackLifeColumn;

    @FXML
    private Label warningLabel;

    private final PieceTypeService pieceTypeService =
            new PieceTypeService("http://localhost:8080/chess-ping-ejb/api");

    @FXML
    private void initialize() {
        // 2,4,6,8 lignes comme dans la version Python
        rowsCombo.setItems(FXCollections.observableArrayList(2, 4, 6, 8));
        rowsCombo.getSelectionModel().select(Integer.valueOf(2));

        // Configuration des colonnes : on affiche nom, nombre (calculé) et vie max
        whiteTypeColumn.setCellValueFactory(new PropertyValueFactory<>("displayName"));
        whiteCountColumn.setCellValueFactory(new PropertyValueFactory<>("count"));
        whiteLifeColumn.setCellValueFactory(new PropertyValueFactory<>("maxHealth"));
        blackTypeColumn.setCellValueFactory(new PropertyValueFactory<>("displayName"));
        blackCountColumn.setCellValueFactory(new PropertyValueFactory<>("count"));
        blackLifeColumn.setCellValueFactory(new PropertyValueFactory<>("maxHealth"));

        try {
            List<PieceTypeDTO> pieces = pieceTypeService.findAll();
            // même liste de types pour Blancs et Noirs
            var obs = FXCollections.observableArrayList(pieces);
            whiteTable.setItems(obs);
            blackTable.setItems(FXCollections.observableArrayList(pieces));

            // Calcul initial des nombres pour 2 lignes
            updateCountsForRows(2, pieces);
        } catch (IOException | InterruptedException e) {
            warningLabel.setText("Erreur de chargement des pièces: " + e.getMessage());
            e.printStackTrace();
        }

        // Recalcule les nombres si l'utilisateur change le nombre de lignes
        rowsCombo.getSelectionModel().selectedItemProperty().addListener((obsSel, oldVal, newVal) -> {
            if (newVal != null && whiteTable.getItems() != null) {
                updateCountsForRows(newVal, whiteTable.getItems());
                // reflète aussi sur la table noire
                if (blackTable.getItems() != null) {
                    updateCountsForRows(newVal, blackTable.getItems());
                }
            }
        });
    }

    private void updateCountsForRows(int rows, List<PieceTypeDTO> pieces) {
        // Reprise de la logique Python _reset_defaults_for_rows
        int limit = 2 * rows;

        // Comptes standard d'un jeu d'échecs par couleur
        var standardCounts = java.util.Map.of(
                "pawn", 8,
                "rook", 2,
                "knight", 2,
                "bishop", 2,
                "queen", 1,
                "king", 1
        );

        String[] priorityOrder = {"rook", "queen", "king", "bishop", "knight", "pawn"};

        java.util.Map<String, Integer> baseCounts = new java.util.HashMap<>();
        for (String k : standardCounts.keySet()) {
            baseCounts.put(k, 0);
        }

        int remaining = limit;
        for (String kind : priorityOrder) {
            if (remaining <= 0) break;
            int std = standardCounts.getOrDefault(kind, 0);
            int take = Math.min(std, remaining);
            baseCounts.put(kind, take);
            remaining -= take;
        }

        // Appliquer ces comptes aux PieceTypeDTO selon leur name (en minuscule)
        for (PieceTypeDTO dto : pieces) {
            if (dto.getName() == null) {
                dto.setCount(0);
                continue;
            }
            String key = dto.getName().toLowerCase();
            Integer c = baseCounts.getOrDefault(key, 0);
            dto.setCount(c);
        }

        whiteTable.refresh();
        if (blackTable != null) {
            blackTable.refresh();
        }
    }

    @FXML
    private void onStartGame() {
        Integer rows = rowsCombo.getValue();
        if (rows == null) rows = 2;

        // Petit résumé textuel de la config auto (comme feedback visuel)
        StringBuilder sb = new StringBuilder();
        sb.append("Configuration pour ").append(rows).append(" lignes : ");

        if (whiteTable.getItems() != null) {
            for (PieceTypeDTO dto : whiteTable.getItems()) {
                if (dto.getCount() != null && dto.getCount() > 0) {
                    sb.append(dto.getCount())
                      .append(" x ")
                      .append(dto.getDisplayName() != null ? dto.getDisplayName() : dto.getName())
                      .append("; ");
                }
            }
        }

        warningLabel.setText(sb.toString());

        // Charger l'écran de jeu
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/fxml/GameView.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(MainApp.class.getResource("/css/styles.css").toExternalForm());

            Stage stage = (Stage) rowsCombo.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            warningLabel.setText("Erreur lors du lancement de la partie: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
