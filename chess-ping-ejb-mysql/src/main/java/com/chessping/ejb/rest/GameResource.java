package com.chessping.ejb.rest;

import com.chessping.ejb.entity.Game;
import com.chessping.ejb.session.GameEJB;

import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;

@Path("/games")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GameResource {

    @EJB
    private GameEJB gameEJB;

    @POST
    public Game createGame(Map<String, Integer> payload) {
        Integer playerWhiteId = payload.get("playerWhiteId");
        Integer playerBlackId = payload.get("playerBlackId");
        if (playerWhiteId == null || playerBlackId == null) {
            throw new BadRequestException("playerWhiteId and playerBlackId are required");
        }
        return gameEJB.createNewGame(playerWhiteId, playerBlackId);
    }

    @GET
    @Path("/{id}")
    public Game getGame(@PathParam("id") Integer id) {
        Game game = gameEJB.findById(id);
        if (game == null) {
            throw new NotFoundException("Game not found");
        }
        return game;
    }

    @PUT
    @Path("/{id}/turn")
    @Consumes(MediaType.TEXT_PLAIN)
    public void changeTurn(@PathParam("id") Integer id, String newTurn) {
        Game game = gameEJB.findById(id);
        if (game == null) {
            throw new NotFoundException("Game not found");
        }
        game.setCurrentTurn(newTurn);
        gameEJB.update(game);
    }
}
