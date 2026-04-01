# 🤖 Istruzioni Agente: Search Service (Microservizio 3)

**Ultimo aggiornamento:** 23 marzo 2026

## 🎯 Obiettivo e Perimetro

Il focus esclusivo di questo progetto è lo sviluppo del **Microservizio 3 - Search Service**, esposto sulla **porta 83**.

- **Regola ferrea:** Non implementare logiche appartenenti al Document Service o al Notification Service. Limitarsi al consumo dei dati via eventi per l'indicizzazione e alla fornitura delle API di ricerca.

---

## 🛠️ Stack Tecnologico

- **Backend Framework:** Javalin (Java/Kotlin) - Porta 83
- **Vector Database:** Qdrant (Distanza Coseno)
- **Embedding Provider:** Sentence Transformers o OpenAI API (implementati tramite Adapter)
- **Autenticazione:** JWT

---

## 📜 Regole d'Oro e Note Implementative

1. **Priorità di Sviluppo:** Implementare prima la pipeline minima: `document.uploaded` -> `embedding` -> `Qdrant upsert` -> `POST /search`.
2. **Sicurezza e Isolamento (Critico):** Applicare _sempre_ il filtro `userId` (dal payload Qdrant) a livello di query per evitare leakage di documenti tra utenti diversi.
3. **Astrazione Embeddings:** Utilizzare il pattern Adapter per il provider di embedding. Introdurre un adapter mock/no-op se il provider esterno non è temporaneamente disponibile.
4. **Resilienza Eventi:** Gestire la deduplica e il retry per gli eventi `document.*`.
5. **Gestione Errori:** Standardizzare le risposte di errore API (400, 401, 403, 404, 422, 429, 500, 503).

---

## 🔌 Contratti (API ed Eventi)

### API REST Fornite

- `POST /api/v1/search`: Ricerca semantica (Richiede JWT).
- `POST /api/v1/embeddings`: Genera embedding per testo (Richiede JWT, possibilmente limitata ad admin/dev).
- `GET /api/v1/search/similar/{documentId}`: Ricerca documenti simili (Richiede JWT).

### Eventi Message Broker Consumati

- `document.uploaded`: Genera embedding (contenuto/snippet) e crea punto vettoriale in Qdrant. Payload minimo: `documentId`, `userId`, `filename`, `snippet`.
- `document.updated`: Rigenera embedding e fa upsert in Qdrant.
- `document.deleted`: Rimuove il vettore da Qdrant.

---

## 📍 Roadmap e Stato Avanzamento (Task)

### Backend (SS-BE)

- [x] **SS-BE-01:** Configurazione base (Javalin port 83, env, client Qdrant, provider embedding). _(Alta)_
- [x] **SS-BE-02:** Setup Collezione Qdrant (startup init, schema, cosine distance). _(Alta)_
- [x] **SS-BE-03:** Middleware JWT per protezione endpoint `search`/`similar`. _(Alta)_
- [X] **SS-BE-04:** Astrazione provider embedding (Adapter pattern con fallback). _(Alta)_
- [x ] **SS-BE-05:** Endpoint `POST /api/v1/embeddings`. _(Alta)_
- [ x ] **SS-BE-09:** Consumer `document.uploaded` (indicizzazione automatica). _(Alta)_
- [x] **SS-BE-06:** Endpoint `POST /api/v1/search` (embedding query, ricerca, ranking). _(Alta)_
- [x] **SS-BE-07:** Implementazione filtro `userId` nei risultati Qdrant. _(Alta)_
- [x] **SS-BE-08:** Endpoint `GET /api/v1/search/similar/{documentId}`. _(Alta)_
- [x] **SS-BE-10:** Consumer `document.updated` (reindicizzazione). _(Alta)_
- [x] **SS-BE-11:** Consumer `document.deleted` (pulizia vettore). _(Alta)_
- [x] **SS-BE-13:** Standardizzazione error handling API. _(Alta)_
- [x] **SS-BE-16:** Unit/Integration Test (Qdrant mock, endpoint, consumer). _(Alta)_
- [x] **SS-BE-14:** Resilienza eventi (deduplica, retry, backoff). _(Media)_
- [ ] **SS-BE-15:** Performance tuning (topK, caching query brevi). _(Media)_
- [ ] **SS-BE-17:** Logging e osservabilità. _(Media)_
- [ ] **SS-BE-12:** Hook evento opzionale `search.completed`. _(Bassa)_

### Frontend (SS-FE)

- [x] **SS-FE-01:** Routing pagina protetta `Ricerca Semantica` (verifica JWT). _(Alta)_
- [x] **SS-FE-02:** Form ricerca semantica (validazione, opzioni `topK`). _(Alta)_
- [x] **SS-FE-03:** Integrazione `POST /api/v1/search` (loading state, abort controller). _(Alta)_
- [x] **SS-FE-04:** UI Risultati ricerca (score, snippet, filename). _(Alta)_
- [ ] **SS-FE-05:** UI Documenti simili via `GET /api/v1/search/similar/{documentId}`. _(Alta)_
- [ ] **SS-FE-08:** Error handling UI (4xx/5xx, token scaduto, empty state). _(Alta)_
- [ ] **SS-FE-06:** UI Dev/Admin per test `POST /api/v1/embeddings`. _(Media)_
- [ ] **SS-FE-07:** Filtri avanzati nel form di ricerca. _(Media)_
- [ ] **SS-FE-09:** Performance UX (debounce input, skeleton loading). _(Media)_
- [ ] **SS-FE-10:** Component & E2E Testing UI. _(Media)_
