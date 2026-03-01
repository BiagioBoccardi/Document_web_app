# Microservizi

## 1. User Service (Port :81)

**Responsabilità**: Gestione utenti, autenticazione, autorizzazione

**Tecnologie**:
- Java + Javalin
- PostgreSQL
- JWT per autenticazione

**API Principali**:
- `POST /api/v1/users/register` - Registrazione nuovo utente
- `POST /api/v1/users/login` - Login e generazione JWT
- `GET /api/v1/users/me` - Profilo utente corrente
- `PUT /api/v1/users/me` - Aggiornamento profilo
- `GET /api/v1/users` - Lista utenti (solo admin)

**Esempio Database Schema**:
```sql
CREATE TABLE utente (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL, -- 'CLIENTE' o 'AMMINISTRATORE'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**Eventi Pubblicati**:
- `user.registered` - Nuovo utente registrato
- `user.updated` - Profilo utente aggiornato
- `user.deleted` - Utente eliminato

---

## 2. Document Service (Port :82)

**Responsabilità**: Upload, storage e gestione documenti testuali

**Tecnologie**:
- Java + Javalin
- MongoDB
- GridFS per file storage

**API Principali**:
- `POST /api/v1/documents` - Upload nuovo documento
- `GET /api/v1/documents` - Lista documenti utente
- `GET /api/v1/documents/{id}` - Recupera documento specifico
- `PUT /api/v1/documents/{id}` - Modifica documento
- `DELETE /api/v1/documents/{id}` - Elimina documento

**Esempio MongoDB Schema**:
```javascript
{
  "_id": ObjectId("..."),
  "userId": NumberLong(123),
  "filename": "contratto.txt",
  "content": "Contenuto del documento...",
  "uploadDate": ISODate("2026-01-29T10:30:00Z"),
  "lastModified": ISODate("2026-01-29T10:30:00Z"),
  "metadata": {
    "size": 15420,
    "mimeType": "text/plain",
    "checksum": "abc123..."
  }
}
```

**Eventi Pubblicati**:
- `document.uploaded` - Nuovo documento caricato
- `document.updated` - Documento modificato
- `document.deleted` - Documento eliminato

**Eventi Consumati**:
- `user.deleted` - Elimina tutti i documenti dell'utente

---

## 3. Search Service (Port :83)

**Responsabilità**: Generazione embedding, indicizzazione vettoriale, ricerca semantica

**Tecnologie**:
- Java + Javalin
- Qdrant (Vector Database)
- Sentence Transformers / OpenAI API per embedding

**API Principali**:
- `POST /api/v1/search` - Ricerca semantica
- `POST /api/v1/embeddings` - Genera embedding per testo
- `GET /api/v1/search/similar/{documentId}` - Documenti simili

**Esempio Qdrant Schema**:
```json
{
  "id": "doc_123",
  "vector": [0.123, -0.456, 0.789, ...],
  "payload": {
    "documentId": "507f1f77bcf86cd799439011",
    "userId": 123,
    "filename": "contratto.txt",
    "snippet": "Prime 200 caratteri del documento..."
  }
}
```

**Eventi Consumati**:
- `document.uploaded` - Genera embedding per nuovo documento
- `document.updated` - Rigenera embedding
- `document.deleted` - Rimuovi vettore da Qdrant

---

## 4. Notification Service (Port :84)

**Responsabilità**: Gestione e invio notifiche asincrone

**Tecnologie**:
- Java + Javalin
- PostgreSQL
- Email service (JavaMail)

**API Principali**:
- `GET /api/v1/notifications` - Lista notifiche utente
- `PUT /api/v1/notifications/{id}/read` - Marca notifica come letta
- `DELETE /api/v1/notifications/{id}` - Elimina notifica

**Esempio Database Schema**:
```sql
CREATE TABLE notifica (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(20) NOT NULL, -- 'IN_APP'
    status VARCHAR(20) NOT NULL, -- 'PENDING', 'SENT', 'FAILED'
    sent_at TIMESTAMP,
    read_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**Eventi Consumati**:
- `document.uploaded` - Notifica "Documento caricato con successo"
- `search.completed` - Notifica "Ricerca completata"
- `user.registered` - Notifica di benvenuto

---