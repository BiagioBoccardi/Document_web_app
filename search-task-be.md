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

---

## SS-BE-16 — Unit/Integration Test

| Campo        | Valore                                                                 |
|--------------|------------------------------------------------------------------------|
| **Stato**    | ✅ Completato                                                          |
| **Priorità** | Alta                                                                   |
| **File**     | `test/.../unit/MockEmbeddingProviderTest.java`, `test/.../unit/JwtAuthMiddlewareTest.java`, `test/.../unit/DocumentEventConsumerTest.java`, `test/.../integration/SearchControllerTest.java` |

### Descrizione
Copertura di test per tutti i componenti principali del Search Service. I test unitari usano Mockito per isolare le dipendenze; il test di integrazione avvia un'istanza Javalin reale su porta casuale con dipendenze mockate.

### Struttura test

| File | Tipo | Cosa testa |
|------|------|------------|
| `MockEmbeddingProviderTest` | Unit | Dimensione vettore (384), range valori [-1,1], eccezione su testo nullo/vuoto |
| `JwtAuthMiddlewareTest` | Unit | Token valido → userId impostato; header mancante / firma errata / scaduto / sub assente → 401 |
| `DocumentEventConsumerTest` | Unit | `uploaded` snippet valido → upsert; snippet vuoto/null → skip; `updated` → upsert; `deleted` → delete |
| `SearchControllerTest` | Integration | Health 200; embeddings 401/200/400; search 401/200/400/422; similar 401/400/404/200 |

### Tecnologie

| Componente | Libreria |
|------------|---------|
| Test runner | JUnit Jupiter 5.10.0 |
| Mocking | Mockito 5.5.0 |
| HTTP client (integration) | `java.net.http.HttpClient` (Java 11+) |
| JWT generazione token di test | auth0 java-jwt 4.4.0 |
| Server di test (integration) | Javalin su porta 0 (OS assegna porta libera) |

### Come eseguire i test

```bash
cd Document-web-app-be

# Tutti i test
mvn test

# Solo unit test
mvn test -Dgroups=unit

# Solo integration test
mvn test -Dgroups=integration

```
## SS-BE-14 — Resilienza Eventi

| Campo        | Valore                                                                                                    |
|--------------|-----------------------------------------------------------------------------------------------------------|
| **Stato** | ✅ Completato                                                                                             |
| **Priorità** | Media                                                                                                     |
| **File** | `config/ResilienceConfig.java`, `messaging/DocumentEventConsumer.java`, `pom.xml`                         |

### Descrizione
Implementazione di meccanismi di affidabilità avanzati per il consumo dei messaggi RabbitMQ. Il sistema è ora in grado di gestire fallimenti temporanei (es. disservizi di rete verso Qdrant o rate-limiting del provider di Embedding) tramite pattern di Retry con Exponential Backoff, garantendo al contempo che messaggi perennemente errati non blocchino le code (Poison Message Handling).

### Come è implementato
Sono state introdotte tre difese principali utilizzando la libreria **Resilience4j**:
- **Ack Manuale:** Disabilitato l'auto-acknowledgement di RabbitMQ (`autoAck = false`). Il messaggio viene rimosso dalla coda solo dopo il successo esplicito (`channel.basicAck()`).
- **Retry con Exponential Backoff:** Le chiamate critiche sono avvolte in `Retry.decorateSupplier` o `Retry.decorateRunnable`.
- **Gestione Scarti (DLQ Routing):** Se tutti i tentativi falliscono, il messaggio viene scartato definitivamente tramite `channel.basicReject(..., false)`, permettendo al broker di instradarlo verso una Dead Letter Queue (se configurata).

### Configurazione Resilience4j (`ResilienceConfig.java`)

### Tecnologie

| Componente  | Libreria / Pattern | 
|--------------|---------------|
|Libreria Retry| io.github.resilience4j:resilience4j-retry:2.2.0|
|RabbitMQ Acks| Manual Acknowledgement (basicAck, basicReject)|
|Idempotenza| Nativa tramite upsert su Qdrant usando il documentId come chiave|

### Pipeline interna (Esempio su `document.uploaded`)
```text
1. Ricezione messaggio da RabbitMQ
2. Avvio Supplier decorato per Embedding (max 3 tentativi)
3. Costruzione PointStruct
4. Avvio Runnable decorato per Qdrant Upsert (max 3 tentativi)
5. Se successo: channel.basicAck() → Messaggio processato
6. Se eccezione dopo tutti i retry: channel.basicReject() → Messaggio scartato

### SS-FE-01 — Routing pagina protetta `Ricerca Semantica`

| Campo        | Valore                                                                                                    |
|--------------|-------------------------------------------------sers\biagi\Desktop\progetti Infobasic\Develops\Document_web_app\search-task-be.md
perfetto dimmi cosa hai fatto nell'ultimo task cosi so cosa testare
Ecco cosa testare per SS-FE-04:

Cosa è cambiato visivamente
Card risultati — ogni risultato ora mostra:

Badge numerico con il rank (1°, 2°, 3°...)
Filename con icona a sinistra
Score badge colorato in alto a destra:
Verde se ≥ 80%
Giallo se ≥ 60%
Rosso se < 60%
Barra di progresso (visibile da sm in su) che riflette lo score
Snippet in stile citazione corsivo con bordo a sinistra
Bottone "Mostra tutto / Mostra meno" se il testo supera 280 caratteri
ID documento in piccolo sotto il filename (se presente nel payload)
Casi da testare
Ricerca con risultati — verifica rank, score color, snippet
Snippet lungo — controlla che il testo venga troncato e che il bottone espandi/collassi funzioni
Loading state — mentre la risposta arriva devono comparire le skeleton card animate (non lo spinner)
Empty state — ricerca senza risultati deve mostrare l'icona SearchX con la query usata nel messaggio
Header risultati — deve mostrare "(N) risultati per: 'tua query'" dopo la risposta
File coinvolti
SearchResultCard.tsx
SearchResultSkeleton.tsx
SearchPage.tsx----------------------------------------------------------|
| **Stato** | ✅ Completato                                                                                             |
| **Priorità** | Alta                                                                                                      |
| **File** | `App.tsx`, `components/protected-route.tsx`, `pages/SearchPage.tsx`, `components/navbar.tsx`              |

### Descrizione
Creazione dell'impalcatura per la pagina di Ricerca Semantica. L'accesso è limitato esclusivamente agli utenti autenticati tramite un componente "guardiano". È stato inoltre integrato il link di accesso rapido nella barra di navigazione principale, sia per la visualizzazione desktop che mobile.

### Come è implementato
Sono stati configurati tre elementi chiave nel frontend:
- **Guardiano (ProtectedRoute):** Un componente che intercetta la navigazione. Verifica la presenza del `token` nel `localStorage`; se assente, esegue un redirect forzato (`<Navigate replace />`) alla root del sito.
- **Routing centralizzato (`App.tsx`):** Le rotte sono state raggruppate. Il componente `<Layout />` funge da wrapper globale (fornendo la Navbar tramite `<Outlet />`), mentre `<ProtectedRoute />` avvolge specificamente le rotte `/group` e `/ricerca`.
- **Integrazione Navbar:** Aggiunto il link "Ricerca" con l'icona della lente di ingrandimento (`Search` da Lucide-React). Il link applica dinamicamente stili visivi differenti per indicare la pagina attiva (`isActive`).

### Tecnologie

| Componente           | Libreria / Funzionalità                                  |
|----------------------|----------------------------------------------------------|
| Gestione Rotte       | `react-router-dom` (`Routes`, `Route`, `Outlet`, `Maps`) |
| Iconografia          | `lucide-react`                                           |
| UI Framework         | Tailwind CSS (classi utility per styling e hover states) |

## SS-FE-02 — Form Ricerca Semantica

| Campo        | Valore                                                                                                    |
|--------------|-----------------------------------------------------------------------------------------------------------|
| **Stato** | ✅ Completato                                                                                             |
| **Priorità** | Alta                                                                                                      |
| **File** | `src/pages/SearchPage.tsx`                                                                                |

### Descrizione
Creazione dell'interfaccia utente per l'inserimento dei parametri di ricerca semantica. È stato implementato un form reattivo con validazione lato client sia per la query di testo (obbligatoria) che per il parametro `topK` (limitato a un range tra 1 e 50).

### Come è implementato
- **UI Design:** Utilizzo dei componenti della libreria **Shadcn UI** (`Card`, `Input`, `Button`, `Label`) per mantenere un design pulito, accessibile e coerente con il resto dell'applicazione.
- **Gestione Stato:** Utilizzo dell'hook `useState` di React per controllare i valori degli input (`query`, `topK`) in tempo reale.
- **Validazione:** Funzione `handleSearch` che intercetta il submit del form (`e.preventDefault()`), verifica la validità dei dati e gestisce la visualizzazione di eventuali messaggi di errore (`error` state) prima di procedere alla chiamata API.
- **Responsività:** Layout flessibile gestito tramite classi Tailwind (`flex-col sm:flex-row`) per adattare i campi di input e il bottone agli schermi dei dispositivi mobili.

### Tecnologie

| Componente       | Libreria / Pattern                                      |
|------------------|---------------------------------------------------------|
| UI Components    | Shadcn UI, Tailwind CSS, Lucide-React (Icone)           |
| State Management | React Hooks (`useState`)                                |
| Event Handling   | React Form Events (`onSubmit`, `onChange`)              |

## SS-FE-03 — Integrazione API Ricerca Semantica

| Campo        | Valore                                                                                                    |
|--------------|-----------------------------------------------------------------------------------------------------------|
| **Stato** | ✅ Completato                                                                                             |
| **Priorità** | Alta                                                                                                      |
| **File** | `src/pages/SearchPage.tsx`                                                                                |

### Descrizione
Integrazione del form di ricerca con l'endpoint backend `POST /api/v1/search`. Il sistema ora gestisce l'intero ciclo di vita della richiesta asincrona, prevenendo problemi di performance e sovrapposizione di dati (race conditions) durante le ricerche multiple o i cambi di pagina veloci.

### Come è implementato
- **Chiamata API Autenticata:** Utilizzo della `Fetch API` nativa, iniettando il token JWT recuperato dal `localStorage` nell'header `Authorization`.
- **Prevenzione Race Conditions:** Implementazione di un `AbortController` persistito tramite l'hook `useRef`. Se viene avviata una nuova ricerca mentre la precedente è in corso, la vecchia chiamata di rete viene interrotta istantaneamente. Il controller pulisce anche le chiamate pendenti se l'utente cambia pagina (gestito tramite la cleanup function di `useEffect`).
- **UX e Loading State:** Introduzione di uno stato `isLoading` che disabilita interattivamente gli input e i bottoni (per evitare invii multipli accidentali) e mostra uno spinner animato (`Loader2` di Lucide-React) durante l'attesa.
- **Rendering Dinamico Risultati:** I dati restituiti dal backend vengono mappati su una lista di componenti `Card` (Shadcn UI), mostrando in modo pulito il contenuto del documento trovato e la percentuale di pertinenza (Score).

### Tecnologie

| Componente           | Libreria / API Nativa                                    |
|----------------------|----------------------------------------------------------|
| Network Request      | Fetch API                                                |
| Gestione Concorrenza | `AbortController`, `AbortSignal`                         |
| Reactivity           | React Hooks (`useEffect`, `useRef`, `useState`)          |

## SS-FE-04 — UI Risultati Ricerca

| Campo        | Valore                                                                                                                        |
|--------------|-------------------------------------------------------------------------------------------------------------------------------|
| **Stato**    | ✅ Completato                                                                                                                 |
| **Priorità** | Alta                                                                                                                          |
| **File**     | `src/search_service/SearchResultCard.tsx`, `src/search_service/SearchResultSkeleton.tsx`, `src/pages/SearchPage.tsx`          |

### Descrizione
Implementazione dell'interfaccia di visualizzazione dei risultati della ricerca semantica. I risultati vengono ora presentati tramite componenti dedicati e riutilizzabili, con indicazione visiva della pertinenza (score), del nome del documento, e del relativo snippet testuale. È stato migliorato anche il feedback visivo durante il caricamento tramite card skeleton animate.

### Come è implementato
- **Componente `SearchResultCard`:** Componente riutilizzabile (già predisposto per SS-FE-05) che riceve un singolo risultato e ne mostra il rank (badge numerico), il filename con icona, lo score come badge colorato (verde ≥80%, giallo ≥60%, rosso <60%) con barra di progresso visiva, e lo snippet con possibilità di espansione tramite bottone "Mostra tutto" / "Mostra meno" per testi lunghi oltre 280 caratteri.
- **Componente `SearchResultSkeleton`:** Sostituisce il singolo spinner durante il caricamento con un numero configurabile di card skeleton animate, costruite con il componente `Skeleton` di Shadcn UI, rispecchiando fedelmente la struttura di `SearchResultCard`.
- **Aggiornamento `SearchPage`:** La logica di rendering inline è stata estratta nei componenti dedicati. È stato aggiunto un header descrittivo sopra la lista che mostra il conteggio dei risultati e la query effettuata (es. *"5 risultati per: 'procedure ferie'"*). L'empty state è stato migliorato con l'icona `SearchX` di Lucide-React e un messaggio contestuale che riporta la query cercata.
- **Normalizzazione Score:** La logica di normalizzazione dello score (gestione `score > 1` vs `score * 100`) è stata centralizzata in `SearchResultCard` tramite la funzione `normalizeScore`, eliminando la logica duplicata presente in precedenza in `SearchPage`.

### Tecnologie

| Componente          | Libreria / Pattern                                        |
|---------------------|-----------------------------------------------------------|
| UI Components       | Shadcn UI (`Card`, `Skeleton`), Tailwind CSS              |
| Iconografia         | `lucide-react` (`FileText`, `ChevronDown`, `ChevronUp`, `SearchX`) |
| State Management    | React Hooks (`useState`)                                  |
| Pattern             | Composizione componenti, separazione responsabilità       |
