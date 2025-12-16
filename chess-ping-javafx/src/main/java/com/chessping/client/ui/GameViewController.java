package com.chessping.client.ui;

import com.chessping.client.MainApp;
import com.chessping.client.model.GamePieceStateDTO;
import com.chessping.client.service.GameStateService;
import com.chessping.client.session.GameSession;
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
import javafx.scene.paint.Color;
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

    // scoring et remise en jeu
    private int scoreRed = 0;
    private int scoreBlue = 0;
    private long resumeTimeNanos = 0L; // si now < resumeTimeNanos, la balle est en pause

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
                newScene.setOnKeyPressed(e -> {
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
            }
        });

        gameCanvas.setOnMouseMoved(e -> {
            if (gamePhase == GamePhase.WAITING_SERVE) {
                aimAngle = Math.atan2(e.getY() - ballY, e.getX() - ballX);
            }
        });

        gameCanvas.setOnMouseClicked(e -> tryStartServe());

        timer.start();
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

    private List<String> buildPieceOrder(Map<String, Integer> pieceCounts) {
        // Ordre strict (sans symétrie, sans trous):
        // KING -> QUEEN -> ROOK -> KNIGHT -> BISHOP
        // Chaque type est répété exactement selon sa quantité configurée.
        List<String> ordered = new ArrayList<>();

        int king = getCountIgnoreCase(pieceCounts, "KING");
        int queen = getCountIgnoreCase(pieceCounts, "QUEEN");
        int rook = getCountIgnoreCase(pieceCounts, "ROOK");
        int knight = getCountIgnoreCase(pieceCounts, "KNIGHT");
        int bishop = getCountIgnoreCase(pieceCounts, "BISHOP");

        for (int i = 0; i < king; i++) ordered.add("KING");
        for (int i = 0; i < queen; i++) ordered.add("QUEEN");
        for (int i = 0; i < rook; i++) ordered.add("ROOK");
        for (int i = 0; i < knight; i++) ordered.add("KNIGHT");
        for (int i = 0; i < bishop; i++) ordered.add("BISHOP");

        return ordered;
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

    private int getHpForType(String type) {
        if (type == null) return 1;
        int hp = getCountIgnoreCase(GameSession.customMaxHealth, type);
        if (hp <= 0) {
            hp = getCountIgnoreCase(GameSession.customMaxHealth, type.toLowerCase());
        }
        return hp > 0 ? hp : 1;
    }

    private void initPiecesLocally(int cols, int rows) {
        System.out.println("[GameView] initPiecesLocally cols=" + cols + " rows=" + rows);
        pieces.clear();

        // Blancs en haut (rangée 0 pour majeures, 1 pour pions)
        int whiteBackRow = 0;
        int whitePawnRow = 1;

        List<String> orderedWhitePieces = buildPieceOrder(GameSession.whitePieceCounts);

        int majorCol = 0;
        for (int i = 0; i < orderedWhitePieces.size() && majorCol < cols; i++) {
            String type = orderedWhitePieces.get(i);
            if (type == null) continue;

            String imgKey = type + "_WHITE";
            Image img = pieceImages.get(imgKey);
            if (img == null) continue;

            int hp = getHpForType(type);

            GamePiece gp = new GamePiece();
            gp.image = img;
            gp.type = type;
            gp.color = "WHITE";
            gp.x = majorCol * CELL_SIZE;
            gp.y = whiteBackRow * CELL_SIZE;
            gp.currentHealth = hp;
            gp.maxHealth = hp;
            gp.stateId = 0;
            pieces.add(gp);

            majorCol++;
        }

        {
            int pawnCount = getCountIgnoreCase(GameSession.whitePieceCounts, "PAWN");
            for (int c = 0; c < cols && c < pawnCount; c++) {
                Image img = pieceImages.get("PAWN_WHITE");
                if (img == null) continue;

                int hp = getHpForType("PAWN");

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
        }

        // Noirs en bas (rangée rows-1 pour majeures, rows-2 pour pions)
        int blackBackRow = rows - 1;
        int blackPawnRow = rows - 2;

        List<String> orderedBlackPieces = buildPieceOrder(GameSession.blackPieceCounts);

        majorCol = 0;
        for (int i = 0; i < orderedBlackPieces.size() && majorCol < cols; i++) {
            String type = orderedBlackPieces.get(i);
            if (type == null) continue;

            String imgKey = type + "_BLACK";
            Image img = pieceImages.get(imgKey);
            if (img == null) continue;

            int hp = getHpForType(type);

            GamePiece gp = new GamePiece();
            gp.image = img;
            gp.type = type;
            gp.color = "BLACK";
            gp.x = majorCol * CELL_SIZE;
            gp.y = blackBackRow * CELL_SIZE;
            gp.currentHealth = hp;
            gp.maxHealth = hp;
            gp.stateId = 0;
            pieces.add(gp);

            majorCol++;
        }

        {
            int pawnCount = getCountIgnoreCase(GameSession.blackPieceCounts, "PAWN");
            for (int c = 0; c < cols && c < pawnCount; c++) {
                Image img = pieceImages.get("PAWN_BLACK");
                if (img == null) continue;

                int hp = getHpForType("PAWN");

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
                ballColor = Color.RED;
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
                ballColor = Color.BLUE;
            }
        }

        // Collision balle / pièces (cercle-rectangle)
        List<GamePiece> toRemove = new ArrayList<>();
        for (GamePiece gp : pieces) {
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
                // collision avec une pièce
                int newHealth = gp.currentHealth - 1;
                if (newHealth < 0) newHealth = 0;

                boolean useBackend = GameSession.gameId != null && gp.stateId > 0;
                System.out.println("[GameView] Collision balle/pièce id=" + gp.stateId + " type=" + gp.type + " color=" + gp.color + " nouvelle vie=" + newHealth + " backend=" + useBackend);

                if (useBackend) {
                    try {
                        gameStateService.updatePieceHealth(gp.stateId, newHealth);
                        gp.currentHealth = newHealth;
                        if (gp.currentHealth <= 0) {
                            gameStateService.capturePiece(gp.stateId);
                            toRemove.add(gp);
                            if ("KING".equalsIgnoreCase(gp.type)) {
                                statusLabel.setText("Roi " + gp.color + " capturé !");
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
                        }
                    }
                }

                // inverser la direction verticale de la balle
                ballVY = -ballVY;
            }
        }
        pieces.removeAll(toRemove);
    }

    private void render(GraphicsContext gc, double width, double height) {
        gc.clearRect(0, 0, width, height);

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
