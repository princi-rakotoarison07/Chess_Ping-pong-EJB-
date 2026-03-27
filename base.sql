-- Création de la base (adapter le nom si besoin)
CREATE DATABASE chess_ping;
-- Connectez-vous ensuite à la base chess_ping avant d'exécuter le reste du script.

-- 1) Types de pièces (configuration : points de vie, etc.)
CREATE TABLE piece_type (
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(20) NOT NULL UNIQUE,  -- 'KING', 'QUEEN', ...
    display_name VARCHAR(50) NOT NULL,        -- 'Roi', 'Reine', ...
    max_health  INT NOT NULL,                 -- vie maximale du type de pièce
    attack      INT NOT NULL,                 -- optionnel : force
    defense     INT NOT NULL                  -- optionnel : défense
);

-- Quelques valeurs par défaut (à adapter)
INSERT INTO piece_type (name, display_name, max_health, attack, defense) VALUES
('KING',   'Roi',      5,  1, 5),
('QUEEN',  'Reine',    4,  4, 3),
('ROOK',   'Tour',     3,  3, 3),
('BISHOP', 'Fou',      3,  3, 2),
('KNIGHT', 'Cavalier', 3,  3, 2),
('PAWN',   'Pion',     2,  1, 1);

-- 2) Joueurs
CREATE TABLE player (
    id        SERIAL PRIMARY KEY,
    name      VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Données de test pour les joueurs
INSERT INTO player (name) VALUES
('Alice'),
('Bob'),
('Charlie');

-- 3) Partie (sauvegarde globale)
CREATE TABLE game (
    id            SERIAL PRIMARY KEY,
    player_white  INT NOT NULL,
    player_black  INT NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS', -- IN_PROGRESS, FINISHED, etc.
    current_turn  VARCHAR(5) NOT NULL DEFAULT 'WHITE',        -- à qui le tour
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (player_white) REFERENCES player(id),
    FOREIGN KEY (player_black) REFERENCES player(id)
);

-- 4) État des pièces dans une sauvegarde
CREATE TABLE game_piece_state (
    id           SERIAL PRIMARY KEY,
    game_id      INT NOT NULL,
    piece_type_id INT NOT NULL,
    color        VARCHAR(5) NOT NULL,  -- 'WHITE' ou 'BLACK'
    position     VARCHAR(5),           -- ex: 'e4', NULL si la pièce est capturée
    current_health INT NOT NULL,       -- vie actuelle de la pièce
    is_captured  BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (game_id) REFERENCES game(id),
    FOREIGN KEY (piece_type_id) REFERENCES piece_type(id)
);