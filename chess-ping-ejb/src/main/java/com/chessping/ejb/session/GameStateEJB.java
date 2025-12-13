package com.chessping.ejb.session;

import com.chessping.ejb.entity.GamePieceState;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

@Stateless
public class GameStateEJB {

    @PersistenceContext(unitName = "chessping-pu")
    private EntityManager em;

    public List<GamePieceState> findByGameId(Integer gameId) {
        return em.createQuery("SELECT s FROM GamePieceState s WHERE s.game.id = :gameId", GamePieceState.class)
                .setParameter("gameId", gameId)
                .getResultList();
    }

    public GamePieceState findById(Integer id) {
        return em.find(GamePieceState.class, id);
    }

    public void updatePieceHealth(Integer pieceStateId, Integer newHealth) {
        GamePieceState state = findById(pieceStateId);
        if (state != null) {
            state.setCurrentHealth(newHealth);
        }
    }

    public void capturePiece(Integer pieceStateId) {
        GamePieceState state = findById(pieceStateId);
        if (state != null) {
            state.setIsCaptured(true);
            state.setPosition(null);
        }
    }

    public void movePiece(Integer pieceStateId, String newPosition) {
        GamePieceState state = findById(pieceStateId);
        if (state != null) {
            state.setPosition(newPosition);
        }
    }
}
