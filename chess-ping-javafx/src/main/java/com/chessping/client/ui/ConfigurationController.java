package com.chessping.client.ui;

import com.chessping.client.chess.model.PieceTypeDTO;
import com.chessping.client.service.PieceTypeService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

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
    private TableColumn<PieceTypeDTO, Integer> whiteLifeColumn;

    @FXML
    private TableColumn<PieceTypeDTO, String> blackTypeColumn;

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

        // Configuration des colonnes : on affiche displayName et maxHealth
        whiteTypeColumn.setCellValueFactory(new PropertyValueFactory<>("displayName"));
        whiteLifeColumn.setCellValueFactory(new PropertyValueFactory<>("maxHealth"));
        blackTypeColumn.setCellValueFactory(new PropertyValueFactory<>("displayName"));
        blackLifeColumn.setCellValueFactory(new PropertyValueFactory<>("maxHealth"));

        try {
            List<PieceTypeDTO> pieces = pieceTypeService.findAll();
            // Pour l'instant, on affiche la même liste pour Blancs et Noirs
            whiteTable.setItems(FXCollections.observableArrayList(pieces));
            blackTable.setItems(FXCollections.observableArrayList(pieces));
        } catch (IOException | InterruptedException e) {
            warningLabel.setText("Erreur de chargement des pièces: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onStartGame() {
        // Pour l'instant : simple message, la logique détaillée sera ajoutée plus tard
        warningLabel.setText("TODO: lancer la partie (implémentation à venir)");
    }
}
