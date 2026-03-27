-- Création de la base
CREATE DATABASE IF NOT EXISTS chess_ping_mysql CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE chess_ping_mysql;

-- 1) Types de pièces
CREATE TABLE piece_type (
    id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(20) NOT NULL UNIQUE,
    display_name VARCHAR(50) NOT NULL,
    max_health INT NOT NULL,
    attack INT NOT NULL,
    defense INT NOT NULL,
    PRIMARY KEY (id)
);

INSERT INTO piece_type (name, display_name, max_health, attack, defense) VALUES
('KING',   'Roi',      5,  1, 5),
('QUEEN',  'Reine',    4,  4, 3),
('ROOK',   'Tour',     3,  3, 3),
('BISHOP', 'Fou',      3,  3, 2),
('KNIGHT', 'Cavalier', 3,  3, 2),
('PAWN',   'Pion',     2,  1, 1);

-- 2) Joueurs
CREATE TABLE player (
    id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

INSERT INTO player (name) VALUES
('Alice_mysql'),
('Bob_mysql'),
('Charlie_mysql');

-- 3) Partie
CREATE TABLE game (
    id INT NOT NULL AUTO_INCREMENT,
    player_white INT NOT NULL,
    player_black INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    current_turn VARCHAR(5) NOT NULL DEFAULT 'WHITE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (player_white) REFERENCES player(id),
    FOREIGN KEY (player_black) REFERENCES player(id)
);

-- 4) État des pièces
CREATE TABLE game_piece_state (
    id INT NOT NULL AUTO_INCREMENT,
    game_id INT NOT NULL,
    piece_type_id INT NOT NULL,
    color VARCHAR(5) NOT NULL,
    position VARCHAR(5),
    current_health INT NOT NULL,
    is_captured BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    FOREIGN KEY (game_id) REFERENCES game(id),
    FOREIGN KEY (piece_type_id) REFERENCES piece_type(id)
);