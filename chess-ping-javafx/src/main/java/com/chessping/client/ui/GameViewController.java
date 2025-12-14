package com.chessping.client.ui;

import com.chessping.client.model.GamePieceStateDTO;
import com.chessping.client.service.GameStateService;
import com.chessping.client.session.GameSession;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.control.Label;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameViewController {

    private static final int CELL_SIZE = 80;
    private static final int BOARD_COLS = 8;

    @FXML
    private Canvas gameCanvas;

    @FXML
    private Label scoreLabel;

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
        int stateId;
    }

    private final List<GamePiece> pieces = new ArrayList<>();

    // paddles
    private double leftPaddleY;
    private double rightPaddleY;
    private static final double PADDLE_WIDTH = 20;
    private static final double PADDLE_HEIGHT = 100;
    private static final double PADDLE_SPEED = 5;

    // ball
    private double ballX;
    private double ballY;
    private double ballVX;
    private double ballVY;
    private static final double BALL_RADIUS = 10;

    private AnimationTimer timer;

    @FXML
    private void initialize() {
        int rows = GameSession.boardRows > 0 ? GameSession.boardRows : 2;
        System.out.println("[GameView] initialize, GameSession.boardRows = " + GameSession.boardRows + ", rows utilisé = " + rows);

        double width = BOARD_COLS * CELL_SIZE;
        double height = rows * CELL_SIZE;
        gameCanvas.setWidth(width);
        gameCanvas.setHeight(height);

        scoreLabel.setText("Score: 0 - 0");
        statusLabel.setText("Jeu en cours");

        loadImages();
        loadPiecesFromApi();
        initPaddlesAndBall(height);

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

                update(deltaSeconds, height);
                render(gc, width, height);
            }
        };

        // gestion des touches pour les paddles
        gameCanvas.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(e -> {
                    if (e.getCode() == KeyCode.Z) {
                        leftPaddleY -= PADDLE_SPEED;
                    } else if (e.getCode() == KeyCode.S) {
                        leftPaddleY += PADDLE_SPEED;
                    } else if (e.getCode() == KeyCode.UP) {
                        rightPaddleY -= PADDLE_SPEED;
                    } else if (e.getCode() == KeyCode.DOWN) {
                        rightPaddleY += PADDLE_SPEED;
                    }
                });
            }
        });

        timer.start();
    }

    private void loadImages() {
        System.out.println("[GameView] Chargement des images de pièces...");
        // Pièces
        loadPieceImage("KING_WHITE", "/images/king_white.png");
        loadPieceImage("KING_BLACK", "/images/king_black.png");
        loadPieceImage("QUEEN_WHITE", "/images/queen_white.png");
        loadPieceImage("QUEEN_BLACK", "/images/queen_black.png");
        loadPieceImage("ROOK_WHITE", "/images/rook_white.png");
        loadPieceImage("ROOK_BLACK", "/images/rook_black.png");
        loadPieceImage("BISHOP_WHITE", "/images/bishop_white.png");
        loadPieceImage("BISHOP_BLACK", "/images/bishop_black.png");
        loadPieceImage("KNIGHT_WHITE", "/images/knight_white.png");
        loadPieceImage("KNIGHT_BLACK", "/images/knight_black.png");
        loadPieceImage("PAWN_WHITE", "/images/pawn_white.png");
        loadPieceImage("PAWN_BLACK", "/images/pawn_black.png");

        System.out.println("[GameView] Images chargées: " + pieceImages.keySet());
    }

    private void loadPieceImage(String key, String path) {
        try {
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
            }
        } catch (Exception e) {
            System.out.println("[GameView] Exception lors du chargement de " + path + " : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadPiecesFromApi() {
        if (GameSession.gameId == null) {
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
                System.out.println("[GameView] Pièce REST: type=" + typeName + " color=" + color + " keyImage=" + key + " position=" + state.getPosition());
                gp.image = pieceImages.get(key);

                String pos = state.getPosition(); // ex: "07"
                int col = Character.getNumericValue(pos.charAt(0));
                int row = Character.getNumericValue(pos.charAt(1));
                gp.x = col * CELL_SIZE + (CELL_SIZE - 60) / 2.0;
                gp.y = row * CELL_SIZE + (CELL_SIZE - 60) / 2.0;

                gp.currentHealth = state.getCurrentHealth();
                gp.maxHealth = state.getPieceType().getMaxHealth();
                gp.type = typeName;
                gp.color = color;
                gp.stateId = state.getId();

                pieces.add(gp);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            statusLabel.setText("Erreur chargement pièces: " + e.getMessage());
        }
    }

    private void initPaddlesAndBall(double canvasHeight) {
        leftPaddleY = canvasHeight / 2 - PADDLE_HEIGHT / 2;
        rightPaddleY = canvasHeight / 2 - PADDLE_HEIGHT / 2;

        ballX = (BOARD_COLS * CELL_SIZE) / 2.0;
        ballY = canvasHeight / 2.0;
        double baseSpeed = 200 * GameSession.speedMultiplier;
        ballVX = baseSpeed;
        ballVY = baseSpeed / 2;
    }

    private void update(double deltaSeconds, double canvasHeight) {
        // Contraindre paddles à rester dans le canvas
        leftPaddleY = clamp(leftPaddleY, 0, canvasHeight - PADDLE_HEIGHT);
        rightPaddleY = clamp(rightPaddleY, 0, canvasHeight - PADDLE_HEIGHT);

        // Déplacement de la balle
        ballX += ballVX * deltaSeconds;
        ballY += ballVY * deltaSeconds;

        // Rebond haut/bas
        if (ballY - BALL_RADIUS < 0 || ballY + BALL_RADIUS > canvasHeight) {
            ballVY = -ballVY;
        }

        // Rebond sur paddles (collision simple AABB)
        // Paddle gauche
        if (ballX - BALL_RADIUS <= PADDLE_WIDTH &&
                ballY >= leftPaddleY && ballY <= leftPaddleY + PADDLE_HEIGHT) {
            ballVX = Math.abs(ballVX);
        }
        // Paddle droit
        double rightPaddleX = BOARD_COLS * CELL_SIZE - PADDLE_WIDTH;
        if (ballX + BALL_RADIUS >= rightPaddleX &&
                ballY >= rightPaddleY && ballY <= rightPaddleY + PADDLE_HEIGHT) {
            ballVX = -Math.abs(ballVX);
        }

        // Collision balle / pièces (approximation rectangle de 60x60)
        List<GamePiece> toRemove = new ArrayList<>();
        for (GamePiece gp : pieces) {
            double px = gp.x;
            double py = gp.y;
            double pw = 60;
            double ph = 60;

            if (ballX >= px && ballX <= px + pw && ballY >= py && ballY <= py + ph) {
                // collision
                try {
                    int newHealth = gp.currentHealth - 1;
                    if (newHealth < 0) newHealth = 0;
                    gameStateService.updatePieceHealth(gp.stateId, newHealth);
                    gp.currentHealth = newHealth;
                    if (gp.currentHealth <= 0) {
                        gameStateService.capturePiece(gp.stateId);
                        toRemove.add(gp);
                        // Détection victoire simple si roi détruit
                        if ("KING".equalsIgnoreCase(gp.type)) {
                            statusLabel.setText("Roi " + gp.color + " capturé !");
                        }
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }

                // inverser la direction de la balle
                ballVX = -ballVX;
            }
        }
        pieces.removeAll(toRemove);
    }

    private void render(GraphicsContext gc, double width, double height) {
        gc.clearRect(0, 0, width, height);

        // Damier basé sur GameSession.boardRows
        int rows = GameSession.boardRows > 0 ? GameSession.boardRows : 2;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < BOARD_COLS; col++) {
                boolean light = (row + col) % 2 == 0;
                gc.setFill(light ? Color.web("#F0D9B5") : Color.web("#B58863"));
                gc.fillRect(col * CELL_SIZE, row * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            }
        }

        // Pièces
        for (GamePiece gp : pieces) {
            if (gp.image != null) {
                double size = CELL_SIZE - 20; // laisse un peu de marge dans la case
                gc.drawImage(gp.image, gp.x, gp.y, size, size);
            }

            // barre de vie au-dessus
            if (gp.maxHealth > 0 && gp.currentHealth > 0) {
                double ratio = (double) gp.currentHealth / gp.maxHealth;
                double barWidth = 60 * ratio;
                double barX = gp.x;
                double barY = gp.y - 10;
                gc.setFill(Color.RED);
                gc.fillRect(barX, barY, 60, 6);
                gc.setFill(Color.LIMEGREEN);
                gc.fillRect(barX, barY, barWidth, 6);
                gc.setStroke(Color.BLACK);
                gc.strokeRect(barX, barY, 60, 6);
            }
        }

        // paddles
        gc.setFill(Color.RED);
        gc.fillRect(0, leftPaddleY, PADDLE_WIDTH, PADDLE_HEIGHT);

        gc.setFill(Color.BLUE);
        double rightPaddleX = BOARD_COLS * CELL_SIZE - PADDLE_WIDTH;
        gc.fillRect(rightPaddleX, rightPaddleY, PADDLE_WIDTH, PADDLE_HEIGHT);

        // balle
        gc.setFill(Color.WHITE);
        gc.fillOval(ballX - BALL_RADIUS, ballY - BALL_RADIUS,
                BALL_RADIUS * 2, BALL_RADIUS * 2);
        gc.setStroke(Color.BLACK);
        gc.strokeOval(ballX - BALL_RADIUS, ballY - BALL_RADIUS,
                BALL_RADIUS * 2, BALL_RADIUS * 2);
    }

    private double clamp(double v, double min, double max) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }
}
