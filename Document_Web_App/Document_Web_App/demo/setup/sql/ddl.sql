CREATE TABLE documento (
    id SERIAL PRIMARY KEY,
    titolo VARCHAR(255) NOT NULL,
    data_modifica DATE NOT NULL,
    tipologia VARCHAR(10) NOT NULL,
    stato BOOLEAN NOT NULL DEFAULT TRUE  -- Indica se il documento esiste o è stato eliminato
);

CREATE TABLE utente (
    id SERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    isAdmin BOOLEAN NOT NULL DEFAULT FALSE
)