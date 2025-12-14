package com.chessping.client.chess.model;

public class PieceTypeDTO {
    private Integer id;
    private String name;
    private String displayName;
    private Integer maxHealth;
    private Integer attack;
    private Integer defense;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Integer getMaxHealth() {
        return maxHealth;
    }

    public void setMaxHealth(Integer maxHealth) {
        this.maxHealth = maxHealth;
    }

    public Integer getAttack() {
        return attack;
    }

    public void setAttack(Integer attack) {
        this.attack = attack;
    }

    public Integer getDefense() {
        return defense;
    }

    public void setDefense(Integer defense) {
        this.defense = defense;
    }
}
