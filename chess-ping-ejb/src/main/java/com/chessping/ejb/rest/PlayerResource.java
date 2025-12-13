package com.chessping.ejb.rest;

import com.chessping.ejb.entity.Player;
import com.chessping.ejb.session.PlayerEJB;

import javax.ejb.EJB;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Path("/players")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PlayerResource {

    @EJB
    private PlayerEJB playerEJB;

    @GET
    public List<Player> findAll() {
        return playerEJB.findAll();
    }

    @POST
    public Player create(Player player) {
        if (player.getCreatedAt() == null) {
            player.setCreatedAt(Timestamp.from(Instant.now()));
        }
        playerEJB.create(player);
        return player;
    }
}
