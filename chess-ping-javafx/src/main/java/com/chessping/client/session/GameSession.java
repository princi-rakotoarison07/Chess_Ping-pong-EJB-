package com.chessping.client.session;

import com.chessping.client.model.Player;

import java.util.HashMap;
import java.util.Map;

public class GameSession {

    public static Player playerWhite;
    public static Player playerBlack;

    public static int boardRows = 2; // 2,4,6,8
    public static String firstServer = "WHITE"; // "WHITE" ou "BLACK"
    public static double speedMultiplier = 1.0;

    public static Map<String, Integer> whitePieceCounts = new HashMap<>();
    public static Map<String, Integer> blackPieceCounts = new HashMap<>();
    public static Map<String, Integer> customMaxHealth = new HashMap<>();

    public static Long gameId;

    public static void reset() {
        playerWhite = null;
        playerBlack = null;
        boardRows = 2;
        firstServer = "WHITE";
        speedMultiplier = 1.0;
        whitePieceCounts.clear();
        blackPieceCounts.clear();
        customMaxHealth.clear();
        gameId = null;
    }
}
