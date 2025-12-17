package com.chessping.ejb.rest;

import com.chessping.ejb.entity.PieceType;
import com.chessping.ejb.session.PieceTypeEJB;

import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;
import java.util.List;

@Path("/piece-types")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PieceTypeResource {

    @EJB
    private PieceTypeEJB pieceTypeEJB;

    @GET
    public List<PieceType> findAll() {
        return pieceTypeEJB.findAll();
    }

    @GET
    @Path("/{id}")
    public PieceType findById(@PathParam("id") Integer id) {
        return pieceTypeEJB.findById(id);
    }

    @PUT
    @Path("/{id}/max-health")
    public PieceType updateMaxHealth(@PathParam("id") Integer id, Map<String, Integer> payload) {
        Integer newMaxHealth = payload.get("newMaxHealth");
        if (newMaxHealth == null) {
            throw new BadRequestException("newMaxHealth is required");
        }
        PieceType updated = pieceTypeEJB.updateMaxHealth(id, newMaxHealth);
        if (updated == null) {
            throw new NotFoundException("PieceType not found");
        }
        return updated;
    }
}
