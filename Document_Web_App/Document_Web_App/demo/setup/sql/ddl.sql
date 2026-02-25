CREATE TABLE utente (
    id SERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    isAdmin BOOLEAN NOT NULL DEFAULT FALSE
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