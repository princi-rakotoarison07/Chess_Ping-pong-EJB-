package com.chessping.client.ui;

import com.chessping.client.MainApp;
import com.chessping.client.model.GamePieceStateDTO;
import com.chessping.client.service.GameStateService;
import com.chessping.client.session.GameSession;
import javafx.application.Platform;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Label;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameViewController {

    private static final int CELL_SIZE = 60;
    private static final int BOARD_COLS = 8; // valeur max, boardCols dans GameSession peut être < 8

    @FXML
    private Canvas gameCanvas;

    @FXML
    private Label scoreLabel;

    @FXML
    private Button powerMinusButton;

    @FXML
    private Label powerThresholdLabel;

    @FXML
    private Button powerPlusButton;

    @FXML
    private Button powerDamageMinusButton;

    @FXML
    private Label powerDamageLabel;

    @FXML
    private Button powerDamagePlusButton;

    @FXML
    private ProgressBar powerProgressBar;

    @FXML
    private Label speedLabel;

    @FXML
    private Button speedMinusButton;

    @FXML
    private Button speedPlusButton;

    @FXML
    private Button resetGameButton;

    @FXML
    private Button resetConfigButton;

    @FXML
    private Label statusLabel;

    private final GameStateService gameStateService =
            new GameStateService("http://localhost:8080/chess-ping-ejb/api");

    private final Map<String, Image> pieceImages = new HashMap<>();

    private static class GamePiece {
        Image image;
        double x;
        double y;
        int currentHealth;
        int maxHealth;
        String type; // KING, QUEEN...
        String color; // WHITE, BLACK
        int stateId; // >0 si la pièce vient du backend
    }

    private final List<GamePiece> pieces = new ArrayList<>();

    // paddles (horizontal, rouge en haut, bleu en bas, à l'intérieur du plateau)
    private double paddleRedX;
    private double paddleRedY;
    private double paddleBlueX;
    private double paddleBlueY;
    private static final double PADDLE_WIDTH = 100;
    private static final double PADDLE_HEIGHT = 20;
    private static final double PADDLE_SPEED = 5;

    // ball
    private double ballX;
    private double ballY;
    private double ballVX;
    private double ballVY;
    private static final double BALL_RADIUS = 10;
    private static final double BALL_SPEED = 220;
    private Color ballColor = Color.WHITE;

    private boolean ballPowerMode = false;
    private int powerDamageRemaining = 0;
    private Color normalBallColor = Color.WHITE;

    private enum PaddleSide {
        NONE,
        RED,
        BLUE
    }

    private boolean powerAvailable = false;
    private PaddleSide activePowerPaddle = PaddleSide.NONE;

    private static final double POWER_BAR_HEIGHT = 14;

    private String powerMessage = null;
    private long powerMessageUntilNanos = 0L;

    private enum GamePhase {
        WAITING_SERVE,
        PLAYING,
        PAUSED,
        GAME_OVER
    }

    private GamePhase gamePhase = GamePhase.WAITING_SERVE;
    private double aimAngle = 0.0;
    private String currentServer = "WHITE";

    private AnimationTimer timer;

    private static final double SPEED_STEP = 0.1;
    private static final double SPEED_MIN = 0.2;
    private static final double SPEED_MAX = 3.0;

    // scoring et remise en jeu
    private int scoreRed = 0;
    private int scoreBlue = 0;
    private long resumeTimeNanos = 0L; // si now < resumeTimeNanos, la balle est en pause

    private static final long PIECE_HIT_COOLDOWN_NANOS = 250_000_000L;
    private final Map<Integer, Long> lastHitByPiece = new HashMap<>();

    @FXML
    private void initialize() {
        int cols = GameSession.boardCols > 0 ? GameSession.boardCols : BOARD_COLS;
        int rows = GameSession.BOARD_ROWS;
        System.out.println("[GameView] initialize, boardCols=" + GameSession.boardCols + " cols utilisés=" + cols + ", rows fixés=" + rows);

        double width = cols * CELL_SIZE;
        double height = rows * CELL_SIZE;
        gameCanvas.setWidth(width);
        gameCanvas.setHeight(height);

        scoreLabel.setText("Score: 0 - 0");
        statusLabel.setText("Jeu en cours");
        updateSpeedLabel();

        initPowerUi();

        loadImages();
        initPiecesLocally(cols, rows);
        initPaddlesAndBall(width, height);

        currentServer = ("BLACK".equalsIgnoreCase(GameSession.firstServer)) ? "BLACK" : "WHITE";
        gamePhase = GamePhase.WAITING_SERVE;
        attachBallToPaddle(currentServer);

        GraphicsContext gc = gameCanvas.getGraphicsContext2D();

        timer = new AnimationTimer() {
            private long lastTime = -1;

            @Override
            public void handle(long now) {
                if (lastTime < 0) {
                    lastTime = now;
                    return;
                }
                double deltaSeconds = (now - lastTime) / 1_000_000_000.0;
                lastTime = now;

                update(deltaSeconds, width, height);
                render(gc, width, height);
            }
        };

        // gestion des touches pour les paddles (Q/D pour paddle rouge, flèches gauche/droite pour paddle bleu)
        gameCanvas.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                    if (e.getCode() == KeyCode.SPACE) {
                        tryStartServe();
                        return;
                    }
                    if (e.getCode() == KeyCode.Q) {
                        paddleRedX -= PADDLE_SPEED;
                    } else if (e.getCode() == KeyCode.D) {
                        paddleRedX += PADDLE_SPEED;
                    } else if (e.getCode() == KeyCode.LEFT) {
                        paddleBlueX -= PADDLE_SPEED;
                    } else if (e.getCode() == KeyCode.RIGHT) {
                        paddleBlueX += PADDLE_SPEED;
                    }
                });

                // Assurer le focus clavier sur le canvas une fois la Scene attachée
                Platform.runLater(() -> gameCanvas.requestFocus());
            }
        });

        gameCanvas.setOnMouseMoved(e -> {
            if (gamePhase == GamePhase.WAITING_SERVE) {
                aimAngle = Math.atan2(e.getY() - ballY, e.getX() - ballX);
            }
        });

        gameCanvas.setOnMouseClicked(e -> {
            gameCanvas.requestFocus();
            tryStartServe();
        });

        gameCanvas.setFocusTraversable(true);
        Platform.runLater(() -> gameCanvas.requestFocus());

        timer.start();
    }

    private void initPowerUi() {
        updatePowerUi();
    }

    private void updatePowerUi() {
        if (powerProgressBar == null) return;

        int threshold = Math.max(1, GameSession.powerThreshold);
        if (powerThresholdLabel != null) {
            powerThresholdLabel.setText(String.valueOf(threshold));
        }
        if (powerDamageLabel != null) {
            powerDamageLabel.setText(String.valueOf(Math.max(1, GameSession.powerDamage)));
        }
        double ratio = Math.max(0, Math.min(1.0, (double) GameSession.powerProgress / (double) threshold));
        powerProgressBar.setProgress(ratio);

        boolean ready = powerAvailable || ratio >= 1.0;
        if (ready) {
            long phase = (System.nanoTime() / 200_000_000L) % 2;
            boolean blinkOn = phase == 0;
            String accent = blinkOn ? "#FFD700" : "#FFECB3";
            powerProgressBar.setStyle("-fx-accent: " + accent + ";");
        } else {
            powerProgressBar.setStyle("-fx-accent: #00BCD4;");
        }
    }

    private void applyPowerThreshold(int newThreshold) {
        GameSession.powerThreshold = Math.max(1, newThreshold);
        updatePowerUi();
    }

    @FXML
    private void onPowerMinus() {
        applyPowerThreshold(GameSession.powerThreshold - 1);
        if (gameCanvas != null) {
            gameCanvas.requestFocus();
        }
    }

    @FXML
    private void onPowerPlus() {
        applyPowerThreshold(GameSession.powerThreshold + 1);
        if (gameCanvas != null) {
            gameCanvas.requestFocus();
        }
    }

    private void applyPowerDamage(int newDamage) {
        GameSession.powerDamage = Math.max(1, newDamage);
        updatePowerUi();
    }

    @FXML
    private void onPowerDamageMinus() {
        applyPowerDamage(GameSession.powerDamage - 1);
        if (gameCanvas != null) {
            gameCanvas.requestFocus();
        }
    }

    @FXML
    private void onPowerDamagePlus() {
        applyPowerDamage(GameSession.powerDamage + 1);
        if (gameCanvas != null) {
            gameCanvas.requestFocus();
        }
    }

    private void updateSpeedLabel() {
        if (speedLabel == null) return;
        speedLabel.setText(String.format("x%.1f", GameSession.speedMultiplier));
    }

    private void applySpeedMultiplier(double newMultiplier) {
        double clamped = Math.max(SPEED_MIN, Math.min(SPEED_MAX, newMultiplier));
        double old = GameSession.speedMultiplier;
        if (old <= 0) old = 1.0;

        // Rescaler la vitesse instantanée si la balle est en mouvement
        if (ballVX != 0 || ballVY != 0) {
            double factor = clamped / old;
            ballVX *= factor;
            ballVY *= factor;
        }

        GameSession.speedMultiplier = clamped;
        updateSpeedLabel();
    }

    @FXML
    private void onSpeedMinus() {
        applySpeedMultiplier(GameSession.speedMultiplier - SPEED_STEP);
    }

    @FXML
    private void onSpeedPlus() {
        applySpeedMultiplier(GameSession.speedMultiplier + SPEED_STEP);
    }

    private void tryStartServe() {
        if (gamePhase != GamePhase.WAITING_SERVE) {
            return;
        }
        double speed = BALL_SPEED * GameSession.speedMultiplier;
        ballVX = Math.cos(aimAngle) * speed;
        ballVY = Math.sin(aimAngle) * speed;
        gamePhase = GamePhase.PLAYING;
    }

    private void attachBallToPaddle(String player) {
        if ("BLACK".equalsIgnoreCase(player)) {
            ballX = paddleBlueX + PADDLE_WIDTH / 2.0;
            ballY = paddleBlueY;
            ballColor = Color.BLUE;
        } else {
            ballX = paddleRedX + PADDLE_WIDTH / 2.0;
            ballY = paddleRedY + PADDLE_HEIGHT;
            ballColor = Color.RED;
        }

        ballVX = 0;
        ballVY = 0;
    }

    private void resetBallAfterPoint(String nextServer) {
        currentServer = ("BLACK".equalsIgnoreCase(nextServer)) ? "BLACK" : "WHITE";
        gamePhase = GamePhase.WAITING_SERVE;
        attachBallToPaddle(currentServer);
    }

    private void endGame(String winnerColor) {
        gamePhase = GamePhase.GAME_OVER;
        ballVX = 0;
        ballVY = 0;
        statusLabel.setText("Partie terminée - Vainqueur: " + winnerColor);
    }

    private void loadImages() {
        System.out.println("[GameView] Chargement des images de pièces...");
        // Pièces
        loadPieceImage("KING_WHITE", "/images/King_white.png");
        loadPieceImage("KING_BLACK", "/images/King_dark.png");

        loadPieceImage("QUEEN_WHITE", "/images/Queen_white.png");
        loadPieceImage("QUEEN_BLACK", "/images/Queen_dark.png");

        loadPieceImage("ROOK_WHITE", "/images/Rook_white.png");
        loadPieceImage("ROOK_BLACK", "/images/Rook_dark.png");

        loadPieceImage("BISHOP_WHITE", "/images/Bishop_white.png");
        loadPieceImage("BISHOP_BLACK", "/images/Bishop_dark.png");

        loadPieceImage("KNIGHT_WHITE", "/images/Knight_white.png");
        loadPieceImage("KNIGHT_BLACK", "/images/Knight_dark.png");

        loadPieceImage("PAWN_WHITE", "/images/Pawn_white.png");
        loadPieceImage("PAWN_BLACK", "/images/Pawn_dark.png");

        System.out.println("[GameView] Images chargées (" + pieceImages.size() + "): " + pieceImages.keySet());
    }

    private void loadPieceImage(String key, String path) {
        try {
            System.out.println("[GameView] Tentative chargement image key=" + key + " path=" + path);
            var url = getClass().getResource(path);
            if (url == null) {
                System.out.println("[GameView] Image introuvable pour key=" + key + " path=" + path);
                return;
            }
            Image img = new Image(url.toExternalForm());
            if (img.isError()) {
                System.out.println("[GameView] Erreur de chargement image pour key=" + key + " path=" + path);
            } else {
                pieceImages.put(key, img);
                System.out.println("[GameView] Image chargée OK pour key=" + key + ", largeur=" + img.getWidth() + ", hauteur=" + img.getHeight());
            }
        } catch (Exception e) {
            System.out.println("[GameView] Exception lors du chargement de " + path + " : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadPiecesFromApi() {
        if (GameSession.gameId == null) {
            System.out.println("[GameView] loadPiecesFromApi: GameSession.gameId est null, aucune pièce à charger.");
            return;
        }
        try {
            List<GamePieceStateDTO> states = gameStateService.getByGame(GameSession.gameId);
            System.out.println("[GameView] Nombre de pièces reçues pour gameId=" + GameSession.gameId + " : " + states.size());
            pieces.clear();
            for (GamePieceStateDTO state : states) {
                if (Boolean.TRUE.equals(state.getCaptured())) {
                    continue;
                }
                GamePiece gp = new GamePiece();
                String typeName = state.getPieceType().getName(); // ex: "KING"
                String color = state.getColor(); // ex: "WHITE" ou "BLACK"
                String key = typeName.toUpperCase() + "_" + color.toUpperCase();
                String pos = state.getPosition(); // ex: "07" (col=0, row=7)
                System.out.println("[GameView] Pièce REST: id=" + state.getId() + " type=" + typeName + " color=" + color + " keyImage=" + key + " position=" + pos + " currentHealth=" + state.getCurrentHealth());

                gp.image = pieceImages.get(key);
                if (gp.image == null) {
                    System.out.println("[GameView] Aucune image trouvée pour key=" + key + ", la pièce ne sera pas affichée.");
                }

                if (pos == null || pos.length() < 2) {
                    System.out.println("[GameView] Position invalide pour la pièce id=" + state.getId() + " : " + pos);
                    continue;
                }

                int col;
                int row;

                // Support des coordonnées type échecs "A1".."H8" (utilisées par l'EJB)
                char fileChar = Character.toUpperCase(pos.charAt(0));
                if (fileChar >= 'A' && fileChar <= 'H') {
                    col = fileChar - 'A';
                    int rank = Character.getNumericValue(pos.charAt(1)); // 1..8
                    if (rank < 1 || rank > GameSession.BOARD_ROWS) {
                        System.out.println("[GameView] Rang invalide pour la pièce id=" + state.getId() + " : " + pos);
                        continue;
                    }
                    // rang 1 en bas de l'écran => row = BOARD_ROWS - rank
                    row = GameSession.BOARD_ROWS - rank;
                } else {
                    // Fallback: interpréter comme ancien format numérique "colrow" (ex: "07")
                    col = Character.getNumericValue(pos.charAt(0));
                    row = Character.getNumericValue(pos.charAt(1));
                }

                gp.x = col * CELL_SIZE;
                gp.y = row * CELL_SIZE;

                gp.currentHealth = state.getCurrentHealth();
                gp.maxHealth = state.getPieceType().getMaxHealth();
                gp.type = typeName;
                gp.color = color;
                gp.stateId = state.getId();

                pieces.add(gp);
            }
            System.out.println("[GameView] Nombre de pièces prêtes à l'affichage: " + pieces.size());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            statusLabel.setText("Erreur chargement pièces: " + e.getMessage());
        }
    }

    private List<String> buildBackRankLayout(int cols) {
        // Layout "type échecs" adapté au nombre de colonnes.
        // Remarque: ce layout exprime seulement les préférences de placement.
        // Si une pièce n'est pas disponible, on remplira avec une autre pièce disponible.
        if (cols <= 0) return java.util.Collections.emptyList();

        if (cols == 2) {
            return java.util.List.of("ROOK", "ROOK");
        }
        if (cols == 4) {
            // cas exceptionnel demandé
            return java.util.List.of("ROOK", "QUEEN", "KING", "ROOK");
        }
        if (cols == 6) {
            return java.util.List.of("ROOK", "KNIGHT", "QUEEN", "KING", "KNIGHT", "ROOK");
        }
        // 8 (standard)
        return java.util.List.of("ROOK", "KNIGHT", "BISHOP", "QUEEN", "KING", "BISHOP", "KNIGHT", "ROOK");
    }

    private java.util.Map<String, Integer> normalizedCounts(Map<String, Integer> pieceCounts) {
        java.util.Map<String, Integer> out = new java.util.HashMap<>();
        if (pieceCounts == null) return out;
        for (var e : pieceCounts.entrySet()) {
            if (e.getKey() == null) continue;
            String k = e.getKey().toUpperCase();
            int v = e.getValue() != null ? e.getValue() : 0;
            out.put(k, Math.max(0, v));
        }
        return out;
    }

    private String takeIfAvailable(java.util.Map<String, Integer> counts, String type) {
        if (counts == null || type == null) return null;
        String key = type.toUpperCase();
        int v = counts.getOrDefault(key, 0);
        if (v <= 0) return null;
        counts.put(key, v - 1);
        return key;
    }

    private String takeFirstAvailable(java.util.Map<String, Integer> counts, String[] priority) {
        if (counts == null) return null;
        for (String t : priority) {
            String got = takeIfAvailable(counts, t);
            if (got != null) return got;
        }
        return null;
    }

    private int getCountIgnoreCase(Map<String, Integer> counts, String key) {
        if (counts == null || key == null) return 0;
        Integer v = counts.get(key);
        if (v != null) return Math.max(0, v);
        v = counts.get(key.toLowerCase());
        if (v != null) return Math.max(0, v);
        v = counts.get(key.toUpperCase());
        if (v != null) return Math.max(0, v);
        return 0;
    }

    private int getHpForType(String type, String color) {
        if (type == null) return 1;
        Map<String, Integer> src = ("BLACK".equalsIgnoreCase(color))
                ? GameSession.blackCustomMaxHealth
                : GameSession.whiteCustomMaxHealth;
        int hp = getCountIgnoreCase(src, type);
        return hp > 0 ? hp : 1;
    }

    private void initPiecesLocally(int cols, int rows) {
        System.out.println("[GameView] initPiecesLocally cols=" + cols + " rows=" + rows);
        pieces.clear();

        // Blancs en haut (rangée 0 pour majeures, 1 pour pions)
        int whiteBackRow = 0;
        int whitePawnRow = 1;

        {
            java.util.Map<String, Integer> counts = normalizedCounts(GameSession.whitePieceCounts);

            // Rangée arrière: layout standard adapté, sans déborder
            List<String> layout = buildBackRankLayout(cols);
            String[] fallbackMajor = {"ROOK", "KNIGHT", "BISHOP", "QUEEN", "KING"};
            for (int c = 0; c < cols && c < layout.size(); c++) {
                String preferred = layout.get(c);
                String type = takeIfAvailable(counts, preferred);
                if (type == null) {
                    type = takeFirstAvailable(counts, fallbackMajor);
                }
                if (type == null) break;

                String imgKey = type + "_WHITE";
                Image img = pieceImages.get(imgKey);
                if (img == null) continue;

                int hp = getHpForType(type, "WHITE");

                GamePiece gp = new GamePiece();
                gp.image = img;
                gp.type = type;
                gp.color = "WHITE";
                gp.x = c * CELL_SIZE;
                gp.y = whiteBackRow * CELL_SIZE;
                gp.currentHealth = hp;
                gp.maxHealth = hp;
                gp.stateId = 0;
                pieces.add(gp);
            }

            // Rangée devant: d'abord les pions, puis remplir avec les autres pièces restantes (ex: cavaliers)
            int pawnCount = Math.min(cols, counts.getOrDefault("PAWN", 0));
            for (int c = 0; c < pawnCount; c++) {
                Image img = pieceImages.get("PAWN_WHITE");
                if (img == null) continue;

                int hp = getHpForType("PAWN", "WHITE");

                GamePiece gp = new GamePiece();
                gp.image = img;
                gp.type = "PAWN";
                gp.color = "WHITE";
                gp.x = c * CELL_SIZE;
                gp.y = whitePawnRow * CELL_SIZE;
                gp.currentHealth = hp;
                gp.maxHealth = hp;
                gp.stateId = 0;
                pieces.add(gp);
            }
            counts.put("PAWN", Math.max(0, counts.getOrDefault("PAWN", 0) - pawnCount));

            String[] fallbackFront = {"KNIGHT", "BISHOP", "ROOK", "QUEEN", "KING"};
            for (int c = pawnCount; c < cols; c++) {
                String type = takeFirstAvailable(counts, fallbackFront);
                if (type == null) break;

                String imgKey = type + "_WHITE";
                Image img = pieceImages.get(imgKey);
                if (img == null) continue;

                int hp = getHpForType(type, "WHITE");

                GamePiece gp = new GamePiece();
                gp.image = img;
                gp.type = type;
                gp.color = "WHITE";
                gp.x = c * CELL_SIZE;
                gp.y = whitePawnRow * CELL_SIZE;
                gp.currentHealth = hp;
                gp.maxHealth = hp;
                gp.stateId = 0;
                pieces.add(gp);
            }
        }

        // Noirs en bas (rangée rows-1 pour majeures, rows-2 pour pions)
        int blackBackRow = rows - 1;
        int blackPawnRow = rows - 2;

        {
            java.util.Map<String, Integer> counts = normalizedCounts(GameSession.blackPieceCounts);

            List<String> layout = buildBackRankLayout(cols);
            String[] fallbackMajor = {"ROOK", "KNIGHT", "BISHOP", "QUEEN", "KING"};
            for (int c = 0; c < cols && c < layout.size(); c++) {
                String preferred = layout.get(c);
                String type = takeIfAvailable(counts, preferred);
                if (type == null) {
                    type = takeFirstAvailable(counts, fallbackMajor);
                }
                if (type == null) break;

                String imgKey = type + "_BLACK";
                Image img = pieceImages.get(imgKey);
                if (img == null) continue;

                int hp = getHpForType(type, "BLACK");

                GamePiece gp = new GamePiece();
                gp.image = img;
                gp.type = type;
                gp.color = "BLACK";
                gp.x = c * CELL_SIZE;
                gp.y = blackBackRow * CELL_SIZE;
                gp.currentHealth = hp;
                gp.maxHealth = hp;
                gp.stateId = 0;
                pieces.add(gp);
            }

            int pawnCount = Math.min(cols, counts.getOrDefault("PAWN", 0));
            for (int c = 0; c < pawnCount; c++) {
                Image img = pieceImages.get("PAWN_BLACK");
                if (img == null) continue;

                int hp = getHpForType("PAWN", "BLACK");

                GamePiece gp = new GamePiece();
                gp.image = img;
                gp.type = "PAWN";
                gp.color = "BLACK";
                gp.x = c * CELL_SIZE;
                gp.y = blackPawnRow * CELL_SIZE;
                gp.currentHealth = hp;
                gp.maxHealth = hp;
                gp.stateId = 0;
                pieces.add(gp);
            }
            counts.put("PAWN", Math.max(0, counts.getOrDefault("PAWN", 0) - pawnCount));

            String[] fallbackFront = {"KNIGHT", "BISHOP", "ROOK", "QUEEN", "KING"};
            for (int c = pawnCount; c < cols; c++) {
                String type = takeFirstAvailable(counts, fallbackFront);
                if (type == null) break;

                String imgKey = type + "_BLACK";
                Image img = pieceImages.get(imgKey);
                if (img == null) continue;

                int hp = getHpForType(type, "BLACK");

                GamePiece gp = new GamePiece();
                gp.image = img;
                gp.type = type;
                gp.color = "BLACK";
                gp.x = c * CELL_SIZE;
                gp.y = blackPawnRow * CELL_SIZE;
                gp.currentHealth = hp;
                gp.maxHealth = hp;
                gp.stateId = 0;
                pieces.add(gp);
            }
        }

        System.out.println("[GameView] initPiecesLocally: " + pieces.size() + " pièces créées.");
    }

    private void initPaddlesAndBall(double canvasWidth, double canvasHeight) {
        // paddles centrés horizontalement
        paddleRedX = canvasWidth / 2.0 - PADDLE_WIDTH / 2.0;
        paddleBlueX = canvasWidth / 2.0 - PADDLE_WIDTH / 2.0;

        // Placement vertical à l'intérieur du plateau :
        // - pièces blanches : rangées 0 (majeures) et 1 (pions)
        //   -> paddle rouge dans la bande entre rangées 2 et 3 (légèrement plus vers le centre)
        double redBandTop = CELL_SIZE * 2; // début de la rangée 2
        paddleRedY = redBandTop + (CELL_SIZE - PADDLE_HEIGHT) / 2.0;

        // - pièces noires : rangées 6 (pions) et 7 (majeures)
        //   -> paddle bleu dans la bande entre rangées 5 et 6
        int rows = GameSession.BOARD_ROWS;
        double blueBandTop = CELL_SIZE * (rows - 3); // rangée 5 si rows=8
        paddleBlueY = blueBandTop + (CELL_SIZE - PADDLE_HEIGHT) / 2.0;

        resetBall(canvasWidth, canvasHeight, 1);

        ballPowerMode = false;
        powerDamageRemaining = 0;
        powerAvailable = false;
        activePowerPaddle = PaddleSide.NONE;
        powerMessage = null;
        powerMessageUntilNanos = 0L;
    }

    private void update(double deltaSeconds, double canvasWidth, double canvasHeight) {
        // Contraindre paddles à rester dans le canvas horizontalement (Y fixe)
        paddleRedX = clamp(paddleRedX, 0, canvasWidth - PADDLE_WIDTH);
        paddleBlueX = clamp(paddleBlueX, 0, canvasWidth - PADDLE_WIDTH);

        if (gamePhase == GamePhase.WAITING_SERVE) {
            attachBallToPaddle(currentServer);
            return;
        }

        if (gamePhase != GamePhase.PLAYING) {
            return;
        }

        // Déplacement de la balle
        ballX += ballVX * deltaSeconds;
        ballY += ballVY * deltaSeconds;

        // Collision avec les bordures du canvas (la balle ne doit jamais sortir du cadre)
        if (ballX - BALL_RADIUS < 0) {
            ballX = BALL_RADIUS;
            ballVX = Math.abs(ballVX);
        } else if (ballX + BALL_RADIUS > canvasWidth) {
            ballX = canvasWidth - BALL_RADIUS;
            ballVX = -Math.abs(ballVX);
        }

        if (ballY - BALL_RADIUS < 0) {
            ballY = BALL_RADIUS;
            ballVY = Math.abs(ballVY);
        } else if (ballY + BALL_RADIUS > canvasHeight) {
            ballY = canvasHeight - BALL_RADIUS;
            ballVY = -Math.abs(ballVY);
        }

        // Collision avec paddle rouge (dans l'espace entre rangées 1 et 2)
        {
            double closestX = clamp(ballX, paddleRedX, paddleRedX + PADDLE_WIDTH);
            double closestY = clamp(ballY, paddleRedY, paddleRedY + PADDLE_HEIGHT);
            double dx = ballX - closestX;
            double dy = ballY - closestY;
            if (dx * dx + dy * dy <= BALL_RADIUS * BALL_RADIUS && ballVY < 0) {
                // rebond vers le bas
                ballY = paddleRedY + PADDLE_HEIGHT + BALL_RADIUS;
                ballVY = Math.abs(ballVY);
                if (!ballPowerMode) {
                    ballColor = Color.RED;
                    normalBallColor = ballColor;
                }

                handlePaddleContact(PaddleSide.RED);
            }
        }

        // Collision avec paddle bleu (dans l'espace entre rangées 5 et 6)
        {
            double closestX = clamp(ballX, paddleBlueX, paddleBlueX + PADDLE_WIDTH);
            double closestY = clamp(ballY, paddleBlueY, paddleBlueY + PADDLE_HEIGHT);
            double dx = ballX - closestX;
            double dy = ballY - closestY;
            if (dx * dx + dy * dy <= BALL_RADIUS * BALL_RADIUS && ballVY > 0) {
                // rebond vers le haut
                ballY = paddleBlueY - BALL_RADIUS;
                ballVY = -Math.abs(ballVY);
                if (!ballPowerMode) {
                    ballColor = Color.BLUE;
                    normalBallColor = ballColor;
                }

                handlePaddleContact(PaddleSide.BLUE);
            }
        }

        // Collision balle / pièces (cercle-rectangle)
        List<GamePiece> toRemove = new ArrayList<>();
        for (GamePiece gp : pieces) {
            if (gamePhase == GamePhase.GAME_OVER) {
                break;
            }
            double px = gp.x;
            double py = gp.y;
            double pw = CELL_SIZE;
            double ph = CELL_SIZE;

            // Collision cercle-rectangle standard
            double closestX = clamp(ballX, px, px + pw);
            double closestY = clamp(ballY, py, py + ph);
            double dx = ballX - closestX;
            double dy = ballY - closestY;

            if (dx * dx + dy * dy <= BALL_RADIUS * BALL_RADIUS) {
                // Déterminer si l'attaque est latérale (droite/gauche du centre de la pièce)
                // ou frontale (haut/bas). On utilise le centre, pas la couleur.
                double centerX = px + pw / 2.0;
                double centerY = py + ph / 2.0;
                double relX = ballX - centerX;
                double relY = ballY - centerY;
                boolean sideHit = Math.abs(relX) > Math.abs(relY);

                if (ballPowerMode) {
                    String targetColor;
                    if (activePowerPaddle == PaddleSide.RED) {
                        targetColor = "BLACK";
                    } else if (activePowerPaddle == PaddleSide.BLUE) {
                        targetColor = "WHITE";
                    } else {
                        targetColor = null;
                    }

                    boolean isTarget = targetColor == null || (gp.color != null && targetColor.equalsIgnoreCase(gp.color));
                    if (!isTarget) {
                        // En mode pouvoir (option 2): rebondir sur ses propres pièces sans infliger de dégâts.
                        if (sideHit) {
                            if (dx > 0) {
                                ballX = closestX + BALL_RADIUS;
                            } else {
                                ballX = closestX - BALL_RADIUS;
                            }
                            ballVX = -ballVX;
                        } else {
                            if (dy > 0) {
                                ballY = closestY + BALL_RADIUS;
                            } else {
                                ballY = closestY - BALL_RADIUS;
                            }
                            ballVY = -ballVY;
                        }
                        break;
                    }
                }

                // 1 seul dégât par "attaque": cooldown par pièce
                int pieceKey = (gp.stateId > 0) ? gp.stateId : System.identityHashCode(gp);
                long now = System.nanoTime();
                long lastHit = lastHitByPiece.getOrDefault(pieceKey, 0L);
                boolean canDealDamage = ballPowerMode || (now - lastHit) >= PIECE_HIT_COOLDOWN_NANOS;

                if (canDealDamage) {
                    int damageToDeal = ballPowerMode ? Math.min(gp.currentHealth, powerDamageRemaining) : 1;
                    if (damageToDeal < 0) damageToDeal = 0;

                    int newHealth = gp.currentHealth - damageToDeal;
                    if (newHealth < 0) newHealth = 0;

                    boolean useBackend = GameSession.gameId != null && gp.stateId > 0;
                    System.out.println("[GameView] Collision balle/pièce id=" + gp.stateId + " type=" + gp.type + " color=" + gp.color + " nouvelle vie=" + newHealth + " backend=" + useBackend + " power=" + ballPowerMode + " dmg=" + damageToDeal + " rem=" + powerDamageRemaining);

                    if (!ballPowerMode && damageToDeal > 0) {
                        incrementPowerProgress(damageToDeal);
                    }

                    if (useBackend) {
                        try {
                            gameStateService.updatePieceHealth(gp.stateId, newHealth);
                            gp.currentHealth = newHealth;
                            if (gp.currentHealth <= 0) {
                                gameStateService.capturePiece(gp.stateId);
                                toRemove.add(gp);
                                if ("KING".equalsIgnoreCase(gp.type)) {
                                    statusLabel.setText("Roi " + gp.color + " capturé !");
                                    String winner = "WHITE".equalsIgnoreCase(gp.color) ? "BLACK" : "WHITE";
                                    endGame(winner);
                                }
                            }
                        } catch (IOException | InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        gp.currentHealth = newHealth;
                        if (gp.currentHealth <= 0) {
                            toRemove.add(gp);
                            if ("KING".equalsIgnoreCase(gp.type)) {
                                statusLabel.setText("Roi " + gp.color + " capturé !");
                                String winner = "WHITE".equalsIgnoreCase(gp.color) ? "BLACK" : "WHITE";
                                endGame(winner);
                            }
                        }
                    }

                    lastHitByPiece.put(pieceKey, now);

                    if (ballPowerMode) {
                        powerDamageRemaining -= damageToDeal;
                        if (powerDamageRemaining < 0) powerDamageRemaining = 0;

                        if (powerDamageRemaining > 0) {
                            // Pas de rebond: la balle traverse, mais on la ressort du rectangle pour éviter
                            // d'empiler plusieurs collisions sur la même frame.
                            if (sideHit) {
                                if (ballVX >= 0) {
                                    ballX = px + pw + BALL_RADIUS;
                                } else {
                                    ballX = px - BALL_RADIUS;
                                }
                            } else {
                                if (ballVY >= 0) {
                                    ballY = py + ph + BALL_RADIUS;
                                } else {
                                    ballY = py - BALL_RADIUS;
                                }
                            }

                            break;
                        }

                        // Fin du pouvoir: rebond + retour au mode normal
                        ballPowerMode = false;
                        ballColor = normalBallColor;
                    }

                    // Rebond (sans sortir du plateau). Sur impact latéral, inverser VX; sinon inverser VY.
                    if (sideHit) {
                        if (dx > 0) {
                            ballX = closestX + BALL_RADIUS;
                        } else {
                            ballX = closestX - BALL_RADIUS;
                        }
                        ballVX = -ballVX;
                    } else {
                        if (dy > 0) {
                            ballY = closestY + BALL_RADIUS;
                        } else {
                            ballY = closestY - BALL_RADIUS;
                        }
                        ballVY = -ballVY;
                    }

                    // Une seule collision gérée par frame
                    break;
                }
            }
        }
        pieces.removeAll(toRemove);
    }

    private void render(GraphicsContext gc, double width, double height) {
        gc.clearRect(0, 0, width, height);

        updatePowerUi();

        // Damier basé sur GameSession.BOARD_ROWS et boardCols
        int rows = GameSession.BOARD_ROWS;
        int cols = GameSession.boardCols > 0 ? GameSession.boardCols : BOARD_COLS;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                boolean light = (row + col) % 2 == 0;
                gc.setFill(light ? Color.web("#F0D9B5") : Color.web("#B58863"));
                gc.fillRect(col * CELL_SIZE, row * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            }
        }

        // Pièces
        for (GamePiece gp : pieces) {
            if (gp.image != null) {
                double size = CELL_SIZE; // occupe toute la case
                gc.drawImage(gp.image, gp.x, gp.y, size, size);
            }

            // Affichage du nombre de vies sur la pièce (au lieu d'une barre verte)
            if (gp.currentHealth > 0) {
                gc.setFont(Font.font(16));
                gc.setFill(Color.BLACK);
                gc.fillText(String.valueOf(gp.currentHealth), gp.x + 6, gp.y + 18);
            }
        }

        // paddles (rouge en haut intérieur, bleu en bas intérieur)
        gc.setFill(Color.RED);
        gc.fillRect(paddleRedX, paddleRedY, PADDLE_WIDTH, PADDLE_HEIGHT);
        gc.setFill(Color.BLUE);
        gc.fillRect(paddleBlueX, paddleBlueY, PADDLE_WIDTH, PADDLE_HEIGHT);

        // balle
        gc.setFill(ballColor);
        gc.fillOval(ballX - BALL_RADIUS, ballY - BALL_RADIUS,
                BALL_RADIUS * 2, BALL_RADIUS * 2);
        gc.setStroke(Color.BLACK);
        gc.strokeOval(ballX - BALL_RADIUS, ballY - BALL_RADIUS,
                BALL_RADIUS * 2, BALL_RADIUS * 2);

        if (gamePhase == GamePhase.WAITING_SERVE) {
            drawArrow(gc);
        }

        drawPowerMessage(gc, width);
    }

    private void incrementPowerProgress(int amount) {
        if (amount <= 0) return;
        GameSession.powerProgress += amount;
        if (!powerAvailable && GameSession.powerProgress >= GameSession.powerThreshold) {
            powerAvailable = true;
        }

        updatePowerUi();
    }

    private void handlePaddleContact(PaddleSide side) {
        if (side == null || side == PaddleSide.NONE) return;

        if (powerAvailable && !ballPowerMode) {
            // Pouvoir automatique: le premier paddle qui touche après le seuil l'active immédiatement.
            activePowerPaddle = side;
            powerAvailable = false;
            GameSession.powerProgress = 0;

            ballPowerMode = true;
            powerDamageRemaining = Math.max(1, GameSession.powerDamage);
            normalBallColor = ballColor;
            ballColor = Color.LIMEGREEN;

            showPowerMessage("Pouvoir activé: " + (side == PaddleSide.RED ? "Rouge" : "Bleu"));
            // L'aura reste visible tant que le paddle est propriétaire du pouvoir.
            updatePowerUi();
        }
    }

    private void showPowerMessage(String msg) {
        powerMessage = msg;
        powerMessageUntilNanos = System.nanoTime() + 1_800_000_000L;
    }

    private void drawPowerMessage(GraphicsContext gc, double width) {
        long now = System.nanoTime();
        if (powerMessage == null) return;
        if (now > powerMessageUntilNanos) {
            powerMessage = null;
            return;
        }

        gc.setFont(Font.font(18));
        gc.setFill(Color.BLACK);
        gc.fillText(powerMessage, 10, POWER_BAR_HEIGHT + 22);
    }

    private void drawArrow(GraphicsContext gc) {
        double startX = ballX;
        double startY = ballY;
        double endX = ballX + Math.cos(aimAngle) * 40;
        double endY = ballY + Math.sin(aimAngle) * 40;

        gc.setStroke(Color.BLACK);
        gc.strokeLine(startX, startY, endX, endY);

        double headSize = 8;
        double angle1 = aimAngle + Math.PI * 0.85;
        double angle2 = aimAngle - Math.PI * 0.85;
        double hx1 = endX + Math.cos(angle1) * headSize;
        double hy1 = endY + Math.sin(angle1) * headSize;
        double hx2 = endX + Math.cos(angle2) * headSize;
        double hy2 = endY + Math.sin(angle2) * headSize;

        gc.strokeLine(endX, endY, hx1, hy1);
        gc.strokeLine(endX, endY, hx2, hy2);
    }

    private void resetBall(double canvasWidth, double canvasHeight, int verticalDirection) {
        // replacer la balle au centre
        ballX = canvasWidth / 2.0;
        ballY = canvasHeight / 2.0;

        double baseSpeed = 200 * GameSession.speedMultiplier;
        // petite vitesse horizontale, direction aléatoire
        ballVX = (Math.random() < 0.5 ? -1 : 1) * baseSpeed * 0.5;
        // vitesse verticale principale, verticalDirection = 1 (vers le bas) ou -1 (vers le haut)
        ballVY = verticalDirection * baseSpeed;

        // couleur neutre au service
        ballColor = Color.WHITE;

        // pause de 2 secondes avant la reprise
        resumeTimeNanos = System.nanoTime() + 2_000_000_000L;
    }

    private void updateScoreLabel() {
        scoreLabel.setText("Score: " + scoreRed + " - " + scoreBlue);
    }

    @FXML
    private void onResetGame() {
        int cols = GameSession.boardCols > 0 ? GameSession.boardCols : BOARD_COLS;
        int rows = GameSession.BOARD_ROWS;

        double width = cols * CELL_SIZE;
        double height = rows * CELL_SIZE;

        scoreRed = 0;
        scoreBlue = 0;
        updateScoreLabel();
        statusLabel.setText("Jeu en cours");

        initPiecesLocally(cols, rows);
        initPaddlesAndBall(width, height);

        currentServer = ("BLACK".equalsIgnoreCase(GameSession.firstServer)) ? "BLACK" : "WHITE";
        gamePhase = GamePhase.WAITING_SERVE;
        attachBallToPaddle(currentServer);
    }

    @FXML
    private void onResetConfiguration() {
        if (timer != null) {
            timer.stop();
        }

        GameSession.reset();

        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/fxml/Configuration.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(MainApp.class.getResource("/css/styles.css").toExternalForm());

            Stage stage = (Stage) scoreLabel.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            statusLabel.setText("Erreur retour configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private double clamp(double v, double min, double max) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }
}
