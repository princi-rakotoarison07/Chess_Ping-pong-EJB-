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
}
