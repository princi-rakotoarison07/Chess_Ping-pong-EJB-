package com.chessping.client.model;

import com.chessping.client.chess.model.PieceTypeDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GamePieceStateDTO {
    private Integer id;
    private Integer gameId;
    private PieceTypeDTO pieceType;
    private String color; // "WHITE" ou "BLACK"
    private String position; // ex: "07"
    private Integer currentHealth;
    private Boolean captured;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getGameId() { return gameId; }
    public void setGameId(Integer gameId) { this.gameId = gameId; }

    public PieceTypeDTO getPieceType() { return pieceType; }
    public void setPieceType(PieceTypeDTO pieceType) { this.pieceType = pieceType; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public Integer getCurrentHealth() { return currentHealth; }
    public void setCurrentHealth(Integer currentHealth) { this.currentHealth = currentHealth; }

    public Boolean getCaptured() { return captured; }
    public void setCaptured(Boolean captured) { this.captured = captured; }
}
