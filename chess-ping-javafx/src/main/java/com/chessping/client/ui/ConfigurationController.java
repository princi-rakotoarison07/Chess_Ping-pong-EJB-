package com.chessping.client.ui;

import com.chessping.client.MainApp;
import com.chessping.client.chess.model.PieceTypeDTO;
import com.chessping.client.service.PieceTypeService;
import com.chessping.client.session.GameSession;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Stage;
import javafx.util.converter.IntegerStringConverter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class ConfigurationController {

    @FXML
    private ComboBox<Integer> colsCombo;

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
    private RadioButton whiteStartsRadio;

    @FXML
    private RadioButton blackStartsRadio;

    @FXML
    private Label warningLabel;

    private boolean manualWhiteCountsOverride = false;
    private boolean manualBlackCountsOverride = false;

    private final PieceTypeService pieceTypeService =
            new PieceTypeService("http://localhost:8080/chess-ping-ejb/api");

    @FXML
    private void initialize() {
        // 2,4,6,8 colonnes
        colsCombo.setItems(FXCollections.observableArrayList(2, 4, 6, 8));
        colsCombo.getSelectionModel().select(Integer.valueOf(8));

        ToggleGroup serveGroup = new ToggleGroup();
        whiteStartsRadio.setToggleGroup(serveGroup);
        blackStartsRadio.setToggleGroup(serveGroup);
        if ("BLACK".equalsIgnoreCase(GameSession.firstServer)) {
            blackStartsRadio.setSelected(true);
        } else {
            whiteStartsRadio.setSelected(true);
        }

        // Configuration des colonnes : on affiche nom, nombre (modifiable) et vie max (modifiable)
        whiteTypeColumn.setCellValueFactory(new PropertyValueFactory<>("displayName"));
        whiteCountColumn.setCellValueFactory(new PropertyValueFactory<>("count"));
        whiteLifeColumn.setCellValueFactory(new PropertyValueFactory<>("maxHealth"));
        blackTypeColumn.setCellValueFactory(new PropertyValueFactory<>("displayName"));
        blackCountColumn.setCellValueFactory(new PropertyValueFactory<>("count"));
        blackLifeColumn.setCellValueFactory(new PropertyValueFactory<>("maxHealth"));

        whiteTable.setEditable(true);
        blackTable.setEditable(true);
        whiteCountColumn.setEditable(true);
        whiteLifeColumn.setEditable(true);
        blackCountColumn.setEditable(true);
        blackLifeColumn.setEditable(true);

        whiteCountColumn.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        whiteCountColumn.setOnEditCommit(evt -> {
            PieceTypeDTO dto = evt.getRowValue();
            if (dto == null) return;
            Integer v = evt.getNewValue();
            dto.setCount(v != null && v >= 0 ? v : 0);
            manualWhiteCountsOverride = true;
            whiteTable.refresh();
        });

        blackCountColumn.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        blackCountColumn.setOnEditCommit(evt -> {
            PieceTypeDTO dto = evt.getRowValue();
            if (dto == null) return;
            Integer v = evt.getNewValue();
            dto.setCount(v != null && v >= 0 ? v : 0);
            manualBlackCountsOverride = true;
            blackTable.refresh();
        });

        whiteLifeColumn.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        whiteLifeColumn.setOnEditCommit(evt -> {
            PieceTypeDTO dto = evt.getRowValue();
            if (dto == null) return;
            Integer v = evt.getNewValue();
            dto.setMaxHealth(v != null && v > 0 ? v : 1);
            whiteTable.refresh();
        });

        blackLifeColumn.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        blackLifeColumn.setOnEditCommit(evt -> {
            PieceTypeDTO dto = evt.getRowValue();
            if (dto == null) return;
            Integer v = evt.getNewValue();
            dto.setMaxHealth(v != null && v > 0 ? v : 1);
            blackTable.refresh();
        });

        try {
            List<PieceTypeDTO> pieces = pieceTypeService.findAll();
            // Listes indépendantes (sinon l'édition d'un camp modifie l'autre)
            var whiteObs = FXCollections.observableArrayList(clonePieceTypes(pieces));
            var blackObs = FXCollections.observableArrayList(clonePieceTypes(pieces));
            whiteTable.setItems(whiteObs);
            blackTable.setItems(blackObs);

            // Calcul initial des nombres pour 8 colonnes
            updateCountsForCols(8, whiteTable.getItems());
            updateCountsForCols(8, blackTable.getItems());
        } catch (IOException | InterruptedException e) {
            warningLabel.setText("Erreur de chargement des pièces: " + e.getMessage());
            e.printStackTrace();
        }

        // Recalcule les nombres si l'utilisateur change le nombre de colonnes
        colsCombo.getSelectionModel().selectedItemProperty().addListener((obsSel, oldVal, newVal) -> {
            if (newVal != null && whiteTable.getItems() != null) {
                if (!manualWhiteCountsOverride) {
                    updateCountsForCols(newVal, whiteTable.getItems());
                }
                if (blackTable.getItems() != null && !manualBlackCountsOverride) {
                    updateCountsForCols(newVal, blackTable.getItems());
                }
            }
        });
    }

    private List<PieceTypeDTO> clonePieceTypes(List<PieceTypeDTO> pieces) {
        if (pieces == null) return java.util.List.of();
        java.util.List<PieceTypeDTO> out = new java.util.ArrayList<>(pieces.size());
        for (PieceTypeDTO src : pieces) {
            if (src == null) continue;
            PieceTypeDTO dto = new PieceTypeDTO();
            dto.setId(src.getId());
            dto.setName(src.getName());
            dto.setDisplayName(src.getDisplayName());
            dto.setMaxHealth(src.getMaxHealth());
            dto.setAttack(src.getAttack());
            dto.setDefense(src.getDefense());
            dto.setCount(src.getCount());
            out.add(dto);
        }
        return out;
    }

    private void updateCountsForCols(int cols, List<PieceTypeDTO> pieces) {
        // Limite totale de pièces par couleur = 2 lignes * nombre de colonnes
        // (une ligne arrière + une ligne de pions)
        int limit = cols * 2;

        // Comptes standard d'un jeu d'échecs par couleur
        var standardCounts = java.util.Map.of(
                "pawn", 8,
                "rook", 2,
                "knight", 2,
                "bishop", 2,
                "queen", 1,
                "king", 1
        );

        java.util.Map<String, Integer> baseCounts = new java.util.HashMap<>();
        for (String k : standardCounts.keySet()) {
            baseCounts.put(k, 0);
        }

        // Répartition par défaut:
        // - 1ère ligne (arrière) = exactement `cols` cases
        // - 2e ligne = le reste jusqu'à `cols * 2`
        // Ordre de remplissage:
        // - Ligne arrière: Roi, Reine, Tours, Cavaliers, Fous
        // - 2e ligne: Cavaliers restants, puis Pions

        int remainingBackRank = cols;
        if (remainingBackRank > 0) {
            baseCounts.put("king", 1);
            remainingBackRank -= 1;
        }
        if (remainingBackRank > 0) {
            baseCounts.put("queen", 1);
            remainingBackRank -= 1;
        }

        if (remainingBackRank > 0) {
            int take = Math.min(standardCounts.getOrDefault("rook", 0), remainingBackRank);
            baseCounts.put("rook", take);
            remainingBackRank -= take;
        }
        if (remainingBackRank > 0) {
            int take = Math.min(standardCounts.getOrDefault("knight", 0), remainingBackRank);
            baseCounts.put("knight", take);
            remainingBackRank -= take;
        }
        if (remainingBackRank > 0) {
            int take = Math.min(standardCounts.getOrDefault("bishop", 0), remainingBackRank);
            baseCounts.put("bishop", take);
            remainingBackRank -= take;
        }

        int remainingTotal = limit;
        for (Integer v : baseCounts.values()) {
            remainingTotal -= (v != null ? v : 0);
        }

        if (remainingTotal > 0) {
            int currentKnights = baseCounts.getOrDefault("knight", 0);
            int addKnights = Math.min(standardCounts.getOrDefault("knight", 0) - currentKnights, remainingTotal);
            if (addKnights > 0) {
                baseCounts.put("knight", currentKnights + addKnights);
                remainingTotal -= addKnights;
            }
        }

        if (remainingTotal > 0) {
            int addPawns = Math.min(standardCounts.getOrDefault("pawn", 0), remainingTotal);
            baseCounts.put("pawn", addPawns);
            remainingTotal -= addPawns;
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
        Integer cols = colsCombo.getValue();
        if (cols == null) cols = 8;

        GameSession.setBoardCols(cols);

        if (blackStartsRadio != null && blackStartsRadio.isSelected()) {
            GameSession.firstServer = "BLACK";
        } else {
            GameSession.firstServer = "WHITE";
        }

        GameSession.whitePieceCounts = new HashMap<>();
        GameSession.blackPieceCounts = new HashMap<>();
        GameSession.whiteCustomMaxHealth = new HashMap<>();
        GameSession.blackCustomMaxHealth = new HashMap<>();

        if (whiteTable.getItems() != null) {
            for (PieceTypeDTO dto : whiteTable.getItems()) {
                if (dto == null || dto.getName() == null) continue;
                String key = dto.getName().toLowerCase();
                int count = dto.getCount() != null && dto.getCount() >= 0 ? dto.getCount() : 0;
                int hp = dto.getMaxHealth() != null && dto.getMaxHealth() > 0 ? dto.getMaxHealth() : 1;
                GameSession.whitePieceCounts.put(key, count);
                GameSession.whiteCustomMaxHealth.put(key, hp);
            }
        }

        if (blackTable.getItems() != null) {
            for (PieceTypeDTO dto : blackTable.getItems()) {
                if (dto == null || dto.getName() == null) continue;
                String key = dto.getName().toLowerCase();
                int count = dto.getCount() != null && dto.getCount() >= 0 ? dto.getCount() : 0;
                int hp = dto.getMaxHealth() != null && dto.getMaxHealth() > 0 ? dto.getMaxHealth() : 1;
                GameSession.blackPieceCounts.put(key, count);
                GameSession.blackCustomMaxHealth.put(key, hp);
            }
        }

        // Petit résumé textuel de la config auto (comme feedback visuel)
        StringBuilder sb = new StringBuilder();
        sb.append("Configuration pour ").append(cols).append(" colonnes : ");

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

            Stage stage = (Stage) colsCombo.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            warningLabel.setText("Erreur lors du lancement de la partie: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onSaveConfig() {
        try {
            java.util.Map<Integer, Integer> byId = new java.util.HashMap<>();
            if (whiteTable.getItems() != null) {
                for (PieceTypeDTO dto : whiteTable.getItems()) {
                    if (dto == null || dto.getId() == null || dto.getMaxHealth() == null) continue;
                    byId.put(dto.getId(), dto.getMaxHealth());
                }
            }
            if (blackTable.getItems() != null) {
                for (PieceTypeDTO dto : blackTable.getItems()) {
                    if (dto == null || dto.getId() == null || dto.getMaxHealth() == null) continue;
                    byId.put(dto.getId(), dto.getMaxHealth());
                }
            }

            int updated = 0;
            for (java.util.Map.Entry<Integer, Integer> e : byId.entrySet()) {
                pieceTypeService.updateMaxHealth(e.getKey(), e.getValue());
                updated++;
            }
            warningLabel.setText("Sauvegarde OK (" + updated + " types)");
            warningLabel.setStyle("-fx-text-fill: #7CFC00;");
        } catch (IOException | InterruptedException e) {
            warningLabel.setText("Erreur lors de la sauvegarde: " + e.getMessage());
            warningLabel.setStyle("-fx-text-fill: red;");
            e.printStackTrace();
        }
    }
}
