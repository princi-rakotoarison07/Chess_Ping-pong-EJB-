package com.chessping.ejb.session;

import com.chessping.ejb.entity.Game;
import com.chessping.ejb.entity.GamePieceState;
import com.chessping.ejb.entity.PieceType;
import com.chessping.ejb.entity.Player;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Stateless
public class GameEJB {

    @PersistenceContext(unitName = "chessping-pu")
    private EntityManager em;

    @Inject
    private PlayerEJB playerEJB;

    @Inject
    private PieceTypeEJB pieceTypeEJB;

    public List<Game> findAll() {
        return em.createQuery("SELECT g FROM Game g", Game.class).getResultList();
    }

    public Game findById(Integer id) {
        return em.find(Game.class, id);
    }

    public void create(Game game) {
        em.persist(game);
    }

    public Game update(Game game) {
        return em.merge(game);
    }

    public void delete(Integer id) {
        Game g = findById(id);
        if (g != null) {
            em.remove(g);
        }
    }

    public Game createNewGame(Integer playerWhiteId, Integer playerBlackId) {
        Player white = playerEJB.findById(playerWhiteId);
        Player black = playerEJB.findById(playerBlackId);
        if (white == null || black == null) {
            throw new IllegalArgumentException("Players must exist");
        }

        Game game = new Game();
        game.setPlayerWhite(white);
        game.setPlayerBlack(black);
        game.setStatus("IN_PROGRESS");
        game.setCurrentTurn("WHITE");
        Timestamp now = Timestamp.from(Instant.now());
        game.setCreatedAt(now);
        game.setUpdatedAt(now);
        em.persist(game);

        initGame(game.getId());
        return game;
    }

    public void initGame(Integer gameId) {
        Game game = findById(gameId);
        if (game == null) {
            throw new IllegalArgumentException("Game not found");
        }

        List<PieceType> pieceTypes = pieceTypeEJB.findAll();
        Map<String, PieceType> typeByName = new HashMap<>();
        for (PieceType pt : pieceTypes) {
            if (pt.getName() != null) {
                typeByName.put(pt.getName().toUpperCase(), pt);
            }
        }

        String[] backRank = {"ROOK", "KNIGHT", "BISHOP", "QUEEN", "KING", "BISHOP", "KNIGHT", "ROOK"};

        // White pieces
        for (int i = 0; i < 8; i++) {
            createPieceState(game, typeByName.get("PAWN"), "WHITE", "" + (char) ('A' + i) + "2");
        }
        for (int i = 0; i < 8; i++) {
            PieceType type = typeByName.get(backRank[i]);
            createPieceState(game, type, "WHITE", "" + (char) ('A' + i) + "1");
        }

        // Black pieces
        for (int i = 0; i < 8; i++) {
            createPieceState(game, typeByName.get("PAWN"), "BLACK", "" + (char) ('A' + i) + "7");
        }
        for (int i = 0; i < 8; i++) {
            PieceType type = typeByName.get(backRank[i]);
            createPieceState(game, type, "BLACK", "" + (char) ('A' + i) + "8");
        }
    }

    private void createPieceState(Game game, PieceType type, String color, String position) {
        if (type == null) {
            return; // piece type not defined in DB
        }
        GamePieceState state = new GamePieceState();
        state.setGame(game);
        state.setPieceType(type);
        state.setColor(color);
        state.setPosition(position);
        state.setCurrentHealth(type.getMaxHealth());
        state.setIsCaptured(false);
        em.persist(state);
    }
}
