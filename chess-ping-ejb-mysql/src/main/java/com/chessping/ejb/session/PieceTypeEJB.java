package com.chessping.ejb.session;

import com.chessping.ejb.entity.PieceType;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

@Stateless
public class PieceTypeEJB {

    @PersistenceContext(unitName = "chessping-pu")
    private EntityManager em;

    public List<PieceType> findAll() {
        return em.createQuery("SELECT p FROM PieceType p", PieceType.class).getResultList();
    }

    public PieceType findById(Integer id) {
        return em.find(PieceType.class, id);
    }

    public void create(PieceType pt) {
        em.persist(pt);
    }

    public PieceType update(PieceType pt) {
        return em.merge(pt);
    }

    public PieceType updateMaxHealth(Integer id, Integer maxHealth) {
        PieceType pt = em.find(PieceType.class, id);
        if (pt == null) {
            return null;
        }
        if (maxHealth == null || maxHealth <= 0) {
            throw new IllegalArgumentException("maxHealth must be > 0");
        }
        pt.setMaxHealth(maxHealth);
        return em.merge(pt);
    }
}
