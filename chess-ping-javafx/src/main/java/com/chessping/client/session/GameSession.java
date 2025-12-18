package com.chessping.client.session;

import com.chessping.client.model.Player;

import java.util.HashMap;
import java.util.Map;

public class GameSession {

    public static Player playerWhite;
    public static Player playerBlack;

    public static final int BOARD_ROWS = 8; // hauteur fixe
    public static int boardCols = 8; // 2,4,6,8 colonnes
    public static int boardRows = 2; // laissé pour compat, mais non utilisé pour le rendu
    public static String firstServer = "WHITE"; // "WHITE" ou "BLACK"
    public static double speedMultiplier = 1.0;

    public static Map<String, Integer> whitePieceCounts = new HashMap<>();
    public static Map<String, Integer> blackPieceCounts = new HashMap<>();
    public static Map<String, Integer> whiteCustomMaxHealth = new HashMap<>();
    public static Map<String, Integer> blackCustomMaxHealth = new HashMap<>();

    public static int powerThreshold = 10;
    public static int powerProgress = 0;
    public static int powerDamage = 3;

    public static Long gameId;

    public static void reset() {
        playerWhite = null;
        playerBlack = null;
        boardCols = 8;
        boardRows = 2;
        firstServer = "WHITE";
        speedMultiplier = 1.0;
        whitePieceCounts.clear();
        blackPieceCounts.clear();
        whiteCustomMaxHealth.clear();
        blackCustomMaxHealth.clear();
        powerThreshold = 10;
        powerProgress = 0;
        powerDamage = 3;
        gameId = null;
    }

    public static void setBoardCols(int cols) {
        if (cols <= 0) {
            boardCols = 2;
        } else if (cols > 8) {
            boardCols = 8;
        } else {
            boardCols = cols;
        }
    }
}
