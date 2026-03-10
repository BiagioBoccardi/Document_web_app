CREATE TABLE utente (
    id SERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    isAdmin BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE gruppo (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    owner_id INTEGER REFERENCES utente(id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE membri_gruppo (   --Tabella di Associazione Utenti-Gruppi
    group_id INTEGER REFERENCES gruppo(id) ON DELETE CASCADE,
    user_id INTEGER REFERENCES utente(id) ON DELETE CASCADE,
    PRIMARY KEY (group_id, user_id)
);

CREATE TABLE documento (
    id SERIAL PRIMARY KEY,
    titolo VARCHAR(255) NOT NULL,
    grandezza INT NOT NULL, -- Grandezza in KB
    data_creazione TIMESTAMP NOT NULL,
    data_modifica TIMESTAMP NOT NULL,
    tipologia VARCHAR(10) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,  -- Indica se il documento esiste o è stato eliminato
    utente_creatore INT NOT NULL,
    CONSTRAINT chk_tipologia CHECK (tipologia IN ('PDF', 'DOC', 'XLS', 'TXT')),
    CONSTRAINT fk_utente_creatore FOREIGN KEY (utente_creatore) REFERENCES utente(id) ON DELETE CASCADE
);

CREATE TABLE document_editors (  -- Per tenere traccia degli utenti che hanno accesso a modificare un documento
    id SERIAL PRIMARY KEY,
    documento_id INT NOT NULL,
    utente_id INT NOT NULL,
    data_aggiunta TIMESTAMP NOT NULL,
    aggiunto_da INT NOT NULL,
    CONSTRAINT fk_documento FOREIGN KEY (documento_id) REFERENCES documento(id) ON DELETE CASCADE,
    CONSTRAINT fk_utente FOREIGN KEY (utente_id) REFERENCES utente(id) ON DELETE CASCADE,
    CONSTRAINT fk_aggiunto_da FOREIGN KEY (aggiunto_da) REFERENCES utente(id) ON DELETE CASCADE
);

CREATE TABLE versione_documento (  -- Per tenere traccia delle versioni dei documenti
    id SERIAL PRIMARY KEY,
    documento_id INT NOT NULL,
    numero_versione INT NOT NULL,
    data_creazione TIMESTAMP NOT NULL,
    CONSTRAINT fk_documento FOREIGN KEY (documento_id) REFERENCES documento(id) ON DELETE CASCADE
);

CREATE TABLE documento_modifiche (  -- Per tenere traccia delle modifiche ai documenti
    id SERIAL PRIMARY KEY,
    documento_id INT NOT NULL,
    utente_id INT NOT NULL,
    data_modifica TIMESTAMP NOT NULL,
    tipo_modifica VARCHAR(10) NOT NULL,
    CONSTRAINT fk_documento FOREIGN KEY (documento_id) REFERENCES documento(id) ON DELETE CASCADE,
    CONSTRAINT fk_utente FOREIGN KEY (utente_id) REFERENCES utente(id) ON DELETE CASCADE,
    CONSTRAINT chk_tipo_modifica CHECK (tipo_modifica IN ('creazione', 'modifica', 'eliminazione'))
);

CREATE TABLE notifiche (
    uuid UUID PRIMARY KEY,
    messaggio TEXT NOT NULL,
    user_id INT NOT NULL,
    stato VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP
);
-- Indici per performance
CREATE INDEX idx_notifiche_user_id ON notifiche(user_id);
CREATE INDEX idx_notifiche_stato ON notifiche(stato);