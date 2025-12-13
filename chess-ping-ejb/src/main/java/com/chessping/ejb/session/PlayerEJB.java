package com.chessping.ejb.session;

import com.chessping.ejb.entity.Player;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Stateless
public class PlayerEJB {

    @PersistenceContext(unitName = "chessping-pu")
    private EntityManager em;

    public List<Player> findAll() {
        return em.createQuery("SELECT p FROM Player p", Player.class).getResultList();
    }

    public Player findById(Integer id) {
        return em.find(Player.class, id);
    }

    public void create(Player player) {
        em.persist(player);
    }

    public Player update(Player player) {
        return em.merge(player);
    }
}
