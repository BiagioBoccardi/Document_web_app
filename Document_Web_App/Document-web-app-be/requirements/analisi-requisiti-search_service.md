# Analisi Requisiti - Search Service

## Scopo e perimetro

Questo documento dettaglia i task funzionali da implementare **solo per il microservizio 3 - Search Service (porta 83)**, sulla base di `README.md` (requisiti funzionali) e `Microservizi.md` (responsabilita, API, eventi e stack del servizio).

### Requisiti funzionali coperti (solo Search Service)

| ID | Requisito | Copertura in questo documento |
|---|---|---|
| FR3 | Ricerca semantica | Si (ricerca semantica, similarita, ranking risultati) |
| FR6 | API REST DB vettoriale | Si (API `search`, `embeddings`, `similar`) |
| FR7 | Integrazione message broker | Si (consumo eventi `document.*` per indicizzazione/rimozione) |
| FR8 | Autenticazione sicura | Si (protezione API search per utente autenticato) |
| FR5 | Notifiche asincrone | Parziale (supporto tramite eventuale evento `search.completed`) |
| FR4 | Gestione documenti | Parziale (solo consumo dati documento via eventi per indicizzazione) |



## API target del Search Service (da `Microservizi.md`)

- `POST /api/v1/search` - Ricerca semantica
- `POST /api/v1/embeddings` - Genera embedding per testo
- `GET /api/v1/search/similar/{documentId}` - Documenti simili

## Eventi consumati dal Search Service (da `Microservizi.md`)

- `document.uploaded` - Genera embedding per nuovo documento
- `document.updated` - Rigenera embedding
- `document.deleted` - Rimuovi vettore da Qdrant

## Task Front-end (Search Service)

| ID Task | Requisito (FR) | Funzionalita | Descrizione task | Output atteso | Priorita |
|---|---|---|---|---|---|
| SS-FE-01 | FR3, FR8 | Routing area ricerca | Creare pagina protetta `Ricerca Semantica` accessibile solo ad utenti autenticati | Accesso UI ricerca protetto | Alta |
| SS-FE-02 | FR3 | Form ricerca semantica | Implementare input query, submit, validazioni base (query non vuota, lunghezza minima) e opzioni base (`topK`) | Form ricerca pronto e usabile | Alta |
| SS-FE-03 | FR3, FR8 | Chiamata API ricerca | Collegare il form a `POST /api/v1/search` con JWT, stato loading, cancel richiesta precedente e gestione timeout lato UI | Ricerca semantica eseguibile da frontend | Alta |
| SS-FE-04 | FR3 | Visualizzazione risultati | Mostrare risultati ordinati per score con `filename`, snippet, similarita e azioni contestuali | Lista risultati ricerca chiara e leggibile | Alta |
| SS-FE-05 | FR3, FR4 (supporto) | Ricerca documenti simili | Aggiungere azione "trova simili" da documento e pagina/overlay che usa `GET /api/v1/search/similar/{documentId}` | Flusso documenti simili disponibile in UI | Alta |
| SS-FE-06 | FR6 | UI tecnica embedding (debug/admin) | Creare una vista opzionale (dev/admin) per testare `POST /api/v1/embeddings` con testo libero e visualizzare metadati embedding (dimensione, tempo) | Strumento UI di verifica API embeddings | Media |
| SS-FE-07 | FR3 | Filtri e parametri ricerca | Gestire parametri base (numero risultati, soglia score, eventuale filtro dataset utente) nel form | Ricerca piu controllabile lato utente | Media |
| SS-FE-08 | FR3, FR8 | Stati e gestione errori | Gestire `401/403/422/429/500/503`, empty state, retry, messaggi user-friendly e redirect login su token scaduto | UX robusta su errori e assenza risultati | Alta |
| SS-FE-09 | FR3 | Performance UX ricerca | Debounce opzionale, caching query recenti lato client e skeleton loading risultati | Esperienza ricerca piu fluida | Media |
| SS-FE-10 | FR3, FR6 | Test FE ricerca | Test componenti/integrazione per submit ricerca, risultati, errori e flusso similari | Copertura minima flussi search FE | Media |

## Task Back-end (Search Service)

| ID Task | Requisito (FR) | Area | Descrizione task | Output atteso | Priorita |
|---|---|---|---|---|---|
| SS-BE-01 | FR3, FR6, FR7 | Configurazione | Configurare `Search Service` (Javalin, porta 83, config env, client Qdrant, provider embedding, struttura package) | Servizio avviabile con dipendenze base | Alta |
| SS-BE-02 | FR6 | Collezione Qdrant | Definire/creare collection Qdrant (dimensione vettore, distanza coseno, payload schema) e inizializzazione startup | Storage vettoriale pronto e coerente | Alta |
| SS-BE-03 | FR8 | Middleware autenticazione | Proteggere endpoint `search`/`similar` (e opzionalmente `embeddings`) con validazione JWT e contesto utente | API search protette per utenti autenticati | Alta |
| SS-BE-04 | FR3, FR6 | Astrazione embedding provider | Implementare adapter per generazione embedding (`Sentence Transformers` o `OpenAI API`) con timeout, retry e fallback configurabile | Integrazione embedding isolata e sostituibile | Alta |
| SS-BE-05 | FR6 | Endpoint embeddings | Implementare `POST /api/v1/embeddings` con validazione input testo e risposta con vettore/metadati (o solo metadati se policy) | API embedding funzionante | Alta |
| SS-BE-06 | FR3 | Endpoint ricerca semantica | Implementare `POST /api/v1/search`: embedding query, ricerca Qdrant, ranking risultati e payload risposta con snippet/score | Ricerca semantica via API funzionante | Alta |
| SS-BE-07 | FR3 | Filtro per ownership utente | Applicare filtro per `userId` (payload Qdrant) nei risultati per garantire isolamento tra utenti | Risultati limitati ai documenti dell'utente | Alta |
| SS-BE-08 | FR3 | Endpoint documenti simili | Implementare `GET /api/v1/search/similar/{documentId}` recuperando vettore documento e ricercando similari in Qdrant | API documenti simili funzionante | Alta |
| SS-BE-09 | FR7, FR3 | Consumer `document.uploaded` | Consumare evento, generare embedding del contenuto/snippet e creare punto vettoriale in Qdrant con payload (`documentId`, `userId`, `filename`, `snippet`) | Indicizzazione nuovi documenti automatica | Alta |
| SS-BE-10 | FR7, FR3 | Consumer `document.updated` | Consumare evento e rigenerare embedding con upsert del punto esistente in Qdrant | Reindicizzazione documento aggiornata | Alta |
| SS-BE-11 | FR7, FR3 | Consumer `document.deleted` | Consumare evento e rimuovere vettore relativo dal database Qdrant | Pulizia indice vettoriale automatica | Alta |
| SS-BE-12 | FR7, FR5 (supporto) | Evento `search.completed` (opzionale) | Predisporre pubblicazione evento di ricerca completata con metadati minimi (userId, queryId, timestamp) per Notification Service | Hook eventi pronto per notifiche future | Bassa |
| SS-BE-13 | FR3, FR6 | Validazioni ed error handling | Standardizzare errori (`400`, `401`, `403`, `404`, `422`, `429`, `500`, `503`) e payload risposta API | API search coerenti per il frontend | Alta |
| SS-BE-14 | FR3, FR7 | Idempotenza e resilienza eventi | Gestire deduplica/retry eventi `document.*`, backoff su provider embedding e DLQ strategy (placeholder) | Consumo eventi piu robusto | Media |
| SS-BE-15 | FR3, NFR7 (supporto) | Performance e tuning | Configurare `topK` default/max, limiti lunghezza query, caching embedding query brevi e metriche tempi (embed/search) | API ricerca piu performanti e misurabili | Media |
| SS-BE-16 | FR3, FR6, FR7 | Test backend | Unit test su ranking/provider + integration test endpoint e consumer eventi con Qdrant/provider mock | Copertura minima flussi critici Search Service | Alta |
| SS-BE-17 | FR3, FR6 | Logging e osservabilita | Logging strutturato per query/eventi, metriche latenza `embedding` e `search`, error rate provider/Qdrant | Diagnostica minima del servizio | Media |

## Note implementative

1. Implementare prima pipeline minima `document.uploaded -> embedding -> Qdrant upsert -> POST /search`.
2. Definire subito il contratto del payload evento `document.*` (contenuto completo o snippet+fetch esterno) per evitare refactor tra servizi.
3. Applicare filtro `userId` a livello query Qdrant per evitare leakage di risultati tra utenti.
4. Se il provider embedding esterno non e disponibile, introdurre adapter mock/no-op per sviluppare API e consumer.
5. Rendere `POST /api/v1/embeddings` eventualmente limitato a uso tecnico/admin se non richiesto nella UI finale.
