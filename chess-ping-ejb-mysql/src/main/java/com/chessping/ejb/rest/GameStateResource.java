package com.chessping.ejb.rest;

import com.chessping.ejb.entity.GamePieceState;
import com.chessping.ejb.session.GameStateEJB;

import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

@Path("/game-states")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GameStateResource {

    @EJB
    private GameStateEJB gameStateEJB;

    @GET
    @Path("/game/{gameId}")
    public List<GamePieceState> getByGame(@PathParam("gameId") Integer gameId) {
        return gameStateEJB.findByGameId(gameId);
    }

    @PUT
    @Path("/{id}/health")
    public void updateHealth(@PathParam("id") Integer id, Map<String, Integer> payload) {
        Integer newHealth = payload.get("newHealth");
        if (newHealth == null) {
            throw new BadRequestException("newHealth is required");
        }
        gameStateEJB.updatePieceHealth(id, newHealth);
    }

    @PUT
    @Path("/{id}/move")
    public void move(@PathParam("id") Integer id, Map<String, String> payload) {
        String newPosition = payload.get("newPosition");
        if (newPosition == null) {
            throw new BadRequestException("newPosition is required");
        }
        gameStateEJB.movePiece(id, newPosition);
    }

    @PUT
    @Path("/{id}/capture")
    public void capture(@PathParam("id") Integer id) {
        gameStateEJB.capturePiece(id);
    }
}
