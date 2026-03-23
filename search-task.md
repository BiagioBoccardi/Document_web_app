# Task SS-BE: Search Service Backend

**Ultimo aggiornamento:** 23 marzo 2026
**Servizio:** Search Service — porta 83
**Stack:** Javalin · Qdrant · RabbitMQ · JWT (auth0)

---

## SS-BE-01 — Configurazione base

| Campo        | Valore                                      |
|--------------|---------------------------------------------|
| **Stato**    | ✅ Completato                               |
| **Priorità** | Alta                                        |
| **File**     | `SearchService.java`, `DotenvConfig.java`, `QdrantClientFactory.java`, `EmbeddingProviderFactory.java` |

### Descrizione
Configurazione iniziale del microservizio: avvio di Javalin sulla porta definita da `SEARCH_SERVICE_PORT` (default 83), caricamento delle variabili d'ambiente tramite `DotenvConfig` (file `.env` nella root), inizializzazione del client Qdrant e selezione del provider di embedding tramite la variabile `EMBEDDING_PROVIDER`.

### Come è implementato
- `DotenvConfig` carica il file `.env` con fallback sui valori di default
- `QdrantClientFactory.createClient()` apre un canale gRPC verso Qdrant (`QDRANT_HOST:QDRANT_PORT`)
- `EmbeddingProviderFactory.createProvider()` restituisce `OpenAiEmbeddingProvider` o `MockEmbeddingProvider` in base all'env
- `SearchService.main()` orchestra tutto e avvia Javalin

---

## SS-BE-02 — Setup Collezione Qdrant

| Campo        | Valore                          |
|--------------|---------------------------------|
| **Stato**    | ✅ Completato                   |
| **Priorità** | Alta                            |
| **File**     | `SearchService.java`            |

### Descrizione
All'avvio del servizio viene verificata l'esistenza della collezione `documents` in Qdrant. Se non esiste, viene creata con la configurazione corretta.

### Come è implementato
```
setupQdrantCollection(qdrantClient)
  ├── listCollectionsAsync()     → controlla se "documents" esiste
  └── createCollectionAsync()   → crea con VectorParams(size=384, distance=Cosine)
```
- Dimensione vettore: **384** (compatibile con Sentence Transformers / Mock)
- Distanza: **Coseno**
- Se la creazione fallisce → `System.exit(1)` (il servizio non può funzionare senza collezione)

---

## SS-BE-03 — Middleware JWT

| Campo        | Valore                              |
|--------------|-------------------------------------|
| **Stato**    | ✅ Completato                       |
| **Priorità** | Alta                                |
| **File**     | `auth/JwtAuthMiddleware.java`, `SearchService.java` |

### Descrizione
Protezione di tutti gli endpoint `/api/v1/*` tramite JWT. Il middleware viene eseguito prima di ogni handler protetto e verifica la firma del token, estraendo lo `userId` (claim `sub`) per renderlo disponibile ai controller.

### Come è implementato
```
app.before("/api/v1/*", JwtAuthMiddleware::handle)
  ├── Legge header "Authorization: Bearer <token>"
  ├── Verifica firma con Algorithm.HMAC256(JWT_SECRET)
  ├── Estrae subject → ctx.attribute("userId", userId)
  └── Se non valido → 401 + ctx.skipRemainingHandlers()
```

| Scenario                        | Risposta |
|---------------------------------|----------|
| Header mancante o malformato    | `401`    |
| Token con firma non valida      | `401`    |
| Token scaduto                   | `401`    |
| Token valido                    | Prosegue verso il controller |

---

## SS-BE-04 — Astrazione Provider Embedding

| Campo        | Valore                                                                 |
|--------------|------------------------------------------------------------------------|
| **Stato**    | ✅ Completato                                                          |
| **Priorità** | Alta                                                                   |
| **File**     | `embedding/EmbeddingProvider.java`, `embedding/OpenAiEmbeddingProvider.java`, `embedding/MockEmbeddingProvider.java`, `embedding/EmbeddingProviderFactory.java` |

### Descrizione
Pattern Adapter per isolare il resto del codice dal provider concreto di embedding. Permette di sostituire il provider senza modificare i consumer (controller, consumer RabbitMQ).

### Come è implementato

```
EmbeddingProvider (interface)
  └── createEmbedding(String text) → float[]

        ├── OpenAiEmbeddingProvider   → chiama OpenAI API
        └── MockEmbeddingProvider     → restituisce vettore casuale (dev/test)

EmbeddingProviderFactory
  └── EMBEDDING_PROVIDER=openai → OpenAiEmbeddingProvider
  └── altro / non impostato     → MockEmbeddingProvider (fallback)
```

| Variabile env          | Provider selezionato       |
|------------------------|----------------------------|
| `openai`               | `OpenAiEmbeddingProvider`  |
| qualsiasi altro valore | `MockEmbeddingProvider`    |

---

## SS-BE-05 — Endpoint POST /api/v1/embeddings

| Campo        | Valore                          |
|--------------|---------------------------------|
| **Stato**    | ✅ Completato                   |
| **Priorità** | Alta                            |
| **File**     | `controller/SearchController.java`, `dto/EmbeddingRequest.java`, `dto/EmbeddingResponse.java` |

### Descrizione
Endpoint per generare l'embedding di un testo arbitrario. Protetto da JWT. Utile per test e per ambienti di sviluppo/admin.

### Request / Response

**Request** `POST /api/v1/embeddings`
```json
{ "text": "testo da vettorializzare" }
```

**Response** `200 OK`
```json
{ "embedding": [0.12, -0.34, ...], "model": "MockEmbeddingProvider" }
```

### Gestione errori

| Caso                          | Stato HTTP |
|-------------------------------|------------|
| `text` nullo o vuoto          | `400`      |
| Provider non disponibile      | `503`      |
| Errore interno                | `500`      |

---

## SS-BE-06 — Endpoint POST /api/v1/search

| Campo        | Valore                          |
|--------------|---------------------------------|
| **Stato**    | ✅ Completato                   |
| **Priorità** | Alta                            |
| **File**     | `controller/SearchController.java`, `dto/SearchRequest.java`, `dto/SearchResponse.java`, `dto/SearchResult.java` |

### Descrizione
Endpoint principale di ricerca semantica. Converte la query testuale in un vettore tramite il provider di embedding, esegue la ricerca in Qdrant e restituisce i documenti più rilevanti dell'utente autenticato.

### Request / Response

**Request** `POST /api/v1/search`
```json
{ "query": "contratto di lavoro 2024", "topK": 5 }
```

**Response** `200 OK`
```json
{
  "query": "contratto di lavoro 2024",
  "total": 3,
  "results": [
    { "documentId": "uuid", "filename": "contratto.pdf", "snippet": "...", "score": 0.92 },
    ...
  ]
}
```

### Pipeline interna
```
1. Estrai userId dal JWT (ctx.attribute)
2. Valida body (query non vuota, 1 ≤ topK ≤ 50)
3. embeddingProvider.createEmbedding(query) → float[]
4. Costruisci filtro userId su Qdrant (→ SS-BE-07)
5. qdrantClient.searchAsync(SearchPoints) → List<ScoredPoint>
6. Mappa ScoredPoint → SearchResult
7. Restituisce SearchResponse
```

### Gestione errori

| Caso                          | Stato HTTP |
|-------------------------------|------------|
| `query` vuota                 | `400`      |
| `topK` fuori range (1–50)     | `422`      |
| Provider embedding non disp.  | `503`      |
| Errore interno                | `500`      |

---

## SS-BE-07 — Filtro userId in Qdrant

| Campo        | Valore                          |
|--------------|---------------------------------|
| **Stato**    | ✅ Completato                   |
| **Priorità** | Alta                            |
| **File**     | `controller/SearchController.java` |

### Descrizione
Ogni query verso Qdrant include un filtro obbligatorio sul campo `userId` del payload. Questo garantisce che un utente non possa mai vedere documenti indicizzati da un altro utente (isolamento dei dati).

### Come è implementato
```java
Filter.newBuilder()
    .addMust(matchKeyword("userId", userId))
    .build();
```
Il filtro viene applicato sia in `POST /search` che in `GET /search/similar/{documentId}`. Usa `ConditionFactory.matchKeyword()` dell'SDK Qdrant 1.9.0.

### Sicurezza

| Scenario                                    | Risultato                  |
|---------------------------------------------|----------------------------|
| Utente A cerca documenti di Utente B        | 0 risultati (filtro blocca)|
| Utente A cerca i propri documenti           | Risultati corretti         |
| Token mancante (nessun userId)              | `401` dal middleware JWT   |

---

## SS-BE-08 — Endpoint GET /api/v1/search/similar/{documentId}

| Campo        | Valore                          |
|--------------|---------------------------------|
| **Stato**    | ✅ Completato                   |
| **Priorità** | Alta                            |
| **File**     | `controller/SearchController.java` |

### Descrizione
Dato l'ID di un documento già indicizzato, restituisce i documenti più simili vettorialmente. A differenza di `POST /search`, **non genera un nuovo embedding da testo**: recupera direttamente il vettore già salvato in Qdrant e lo usa come query.

### Request / Response

**Request** `GET /api/v1/search/similar/{documentId}?topK=5`

**Response** `200 OK`
```json
{
  "query": "uuid-del-documento-originale",
  "total": 4,
  "results": [
    { "documentId": "uuid2", "filename": "simile.pdf", "snippet": "...", "score": 0.88 },
    ...
  ]
}
```

### Pipeline interna
```
1. Estrai userId dal JWT
2. Leggi documentId dal path, topK dal query param (default 5)
3. qdrantClient.retrieveAsync(documentId) → recupera vettore esistente
4. Se non trovato → 404
5. Costruisci filtro userId
6. searchAsync con vettore recuperato, limit = topK+1
7. Escludi il documento originale dai risultati
8. Restituisce i primi topK risultati
```

### Gestione errori

| Caso                          | Stato HTTP |
|-------------------------------|------------|
| `topK` non numerico           | `400`      |
| `topK` fuori range (1–50)     | `422`      |
| Documento non trovato         | `404`      |
| Errore interno                | `500`      |

---

## SS-BE-09 — Consumer document.uploaded

| Campo        | Valore                                      |
|--------------|---------------------------------------------|
| **Stato**    | ✅ Completato                               |
| **Priorità** | Alta                                        |
| **File**     | `messaging/DocumentEventConsumer.java`, `dto/DocumentEvent.java` |

### Descrizione
Ascolta la coda RabbitMQ `document.uploaded`. Quando arriva un evento, genera l'embedding dallo snippet del documento e lo indicizza in Qdrant tramite upsert. È il punto di ingresso della pipeline di indicizzazione automatica.

### Payload evento atteso

```json
{
  "documentId": "uuid",
  "userId":     "uuid",
  "filename":   "contratto.pdf",
  "snippet":    "testo estratto dal documento..."
}
```

### Pipeline interna
```
1. Deserializza JSON → DocumentEvent
2. Valida snippet (non vuoto, altrimenti skip con warning)
3. embeddingProvider.createEmbedding(snippet) → float[]
4. Costruisce PointStruct con payload: documentId, userId, filename, snippet
5. qdrantClient.upsertAsync(COLLECTION_NAME, point)
```

### Note
- Se `snippet` è vuoto l'indicizzazione viene saltata con un log di warning
- L'upsert è idempotente: se il documento era già indicizzato viene sovrascritto
- `documentId` deve essere un UUID valido (usato come ID del punto in Qdrant)

---

## SS-BE-10 — Consumer document.updated

| Campo        | Valore                                      |
|--------------|---------------------------------------------|
| **Stato**    | ✅ Completato                               |
| **Priorità** | Alta                                        |
| **File**     | `messaging/DocumentEventConsumer.java`      |

### Descrizione
Ascolta la coda RabbitMQ `document.updated`. Quando un documento viene modificato, rigenera l'embedding dal nuovo snippet e aggiorna il vettore in Qdrant.

### Come è implementato
```java
private void handleDocumentUpdated(DocumentEvent event) {
    // L'upsert sovrascrive il vettore esistente con lo stesso ID
    handleDocumentUploaded(event);
}
```
Riutilizza integralmente la logica di `handleDocumentUploaded`: l'upsert di Qdrant sovrascrive automaticamente il punto esistente con lo stesso `documentId`, aggiornando vettore e payload in un'unica operazione.

### Payload evento atteso
Identico a `document.uploaded` — deve includere il nuovo `snippet` aggiornato.

---

## SS-BE-11 — Consumer document.deleted

| Campo        | Valore                                      |
|--------------|---------------------------------------------|
| **Stato**    | ✅ Completato                               |
| **Priorità** | Alta                                        |
| **File**     | `messaging/DocumentEventConsumer.java`      |

### Descrizione
Ascolta la coda RabbitMQ `document.deleted`. Quando un documento viene eliminato, rimuove il corrispondente vettore da Qdrant per mantenere l'indice allineato con i documenti effettivamente esistenti.

### Pipeline interna
```
1. Deserializza JSON → DocumentEvent
2. qdrantClient.deleteAsync(COLLECTION_NAME, List.of(id(UUID.fromString(documentId))))
3. Log di conferma
```

### Payload evento atteso

```json
{
  "documentId": "uuid"
}
```

### Note
- Solo `documentId` è necessario per la cancellazione
- Se il documento non esiste in Qdrant, Qdrant ignora silenziosamente l'operazione (no errore)
- In caso di eccezione viene loggato l'errore senza propagarlo (il consumer rimane attivo)

---

## SS-BE-13 — Standardizzazione Error Handling API

| Campo        | Valore                                                                 |
|--------------|------------------------------------------------------------------------|
| **Stato**    | ✅ Completato                                                          |
| **Priorità** | Alta                                                                   |
| **File**     | `dto/ErrorResponse.java`, `controller/SearchController.java`, `SearchService.java` |

### Descrizione
Standardizzazione del formato di risposta per tutti gli errori API. Prima di questo task ogni errore veniva restituito come stringa JSON raw (`"{\"error\": \"...\"}"`). Ora tutte le risposte di errore usano un DTO uniforme e un gestore globale cattura le eccezioni non gestite.

### Struttura ErrorResponse

```json
{
  "status": 404,
  "error": "Documento con id 'uuid' non trovato",
  "timestamp": "2026-03-23T10:30:00.000Z"
}
```

### Modifiche apportate

| Componente | Modifica |
|------------|----------|
| `dto/ErrorResponse.java` | Nuovo DTO con `status`, `error`, `timestamp` |
| `SearchController.java` | Tutte le risposte di errore usano `new ErrorResponse(...)` invece di stringhe raw |
| `SearchController.java` | Aggiunto catch `IllegalArgumentException` in `findSimilar` per UUID non valido → `400` |
| `SearchService.java` | Aggiunto `app.exception(Exception.class, ...)` come fallback globale → `500` |

### Codici HTTP gestiti

| Codice | Quando viene restituito |
|--------|------------------------|
| `400` | Campo obbligatorio mancante, `topK` non numerico, `documentId` non UUID valido |
| `401` | Token JWT mancante, non valido o scaduto |
| `404` | Documento non trovato in Qdrant |
| `422` | `topK` fuori dal range consentito (1–50) |
| `500` | Errore interno generico (catch globale Javalin) |
| `503` | Provider di embedding non disponibile |
