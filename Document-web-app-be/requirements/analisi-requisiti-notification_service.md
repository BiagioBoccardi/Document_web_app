# Analisi Requisiti - Notification Service

## Scopo e perimetro

Questo documento dettaglia i task funzionali da implementare **solo per il microservizio 4 - Notification Service (porta 84)**, sulla base di `README.md` (requisiti funzionali) e `Microservizi.md` (responsabilita, API, eventi e stack del servizio).

### Requisiti funzionali coperti (solo Notification Service)

| ID | Requisito | Copertura in questo documento |
|---|---|---|
| FR5 | Notifiche asincrone | Si (creazione, persistenza, lettura, marcatura come letto, eliminazione) |
| FR7 | Integrazione message broker | Si (consumo eventi da altri microservizi) |
| FR8 | Autenticazione sicura | Si (protezione API notifiche per utente autenticato) |
| FR1 | Upload documenti testuali | Parziale (notifica conseguente a `document.uploaded`) |
| FR2 | Gestione utenti | Parziale (notifica di benvenuto su `user.registered`) |
| FR3 | Ricerca semantica | Parziale (notifica su `search.completed`) |



## API target del Notification Service (da `Microservizi.md`)

- `GET /api/v1/notifications` - Lista notifiche utente
- `PUT /api/v1/notifications/{id}/read` - Marca notifica come letta
- `DELETE /api/v1/notifications/{id}` - Elimina notifica

## Eventi consumati dal Notification Service (da `Microservizi.md`)

- `document.uploaded` - Notifica "Documento caricato con successo"
- `search.completed` - Notifica "Ricerca completata"
- `user.registered` - Notifica di benvenuto

## Task Front-end (Notification Service)

| ID Task | Requisito (FR) | Funzionalita | Descrizione task | Output atteso | Priorita |
|---|---|---|---|---|---|
| NS-FE-01 | FR5, FR8 | UI entry notifiche | Integrare accesso notifiche (icona campanella/menu) in area autenticata con badge conteggio non lette | Punto di accesso notifiche visibile e protetto | Alta |
| NS-FE-02 | FR5 | Lista notifiche utente | Creare vista/pannello notifiche con dati da `GET /api/v1/notifications` (messaggio, tipo, stato, data, letta/non letta) | Lista notifiche consultabile da UI | Alta |
| NS-FE-03 | FR5, FR8 | Chiamata API lista | Implementare fetch notifiche con JWT, loading, paginazione/infinite scroll e refresh manuale | Recupero notifiche funzionante lato frontend | Alta |
| NS-FE-04 | FR5 | Marca come letta | Aggiungere azione per `PUT /api/v1/notifications/{id}/read` con update ottimistico del badge/lista | Marcatura notifica come letta da UI | Alta |
| NS-FE-05 | FR5 | Eliminazione notifica | Implementare azione elimina con conferma (se richiesta) e chiamata `DELETE /api/v1/notifications/{id}` | Eliminazione notifica con aggiornamento lista | Alta |
| NS-FE-06 | FR5 | Filtri e ordinamento UI | Aggiungere filtri `tutte/non lette/tipo` e ordinamento per data | Navigazione notifiche piu usabile | Media |
| NS-FE-07 | FR5, FR7 (supporto) | Aggiornamento automatico lista | Introdurre polling periodico (o hook predisposto a push/websocket futuro) per nuove notifiche | Lista notifiche aggiornata quasi real-time | Media |
| NS-FE-08 | FR5, FR8 | Error handling e stati UX | Gestire `401/403/404/500`, empty state, retry, skeleton/loading e redirect login per token scaduto | UX robusta in errori e assenza dati | Alta |
| NS-FE-09 | FR5 | Stato locale badge/unread | Mantenere sincronizzato conteggio non lette tra pannello notifiche e resto dell'app | Badge notifiche coerente | Media |
| NS-FE-10 | FR5 | Test FE notifiche | Test componenti/integrazione per lista, mark-as-read, delete, badge ed errori | Copertura minima flussi notifiche FE | Media |

## Task Back-end (Notification Service)

| ID Task | Requisito (FR) | Area | Descrizione task | Output atteso | Priorita |
|---|---|---|---|---|---|
| NS-BE-01 | FR5, FR7 | Bootstrap servizio (setup iniziale) | Impostare lo scheletro del `Notification Service`: entrypoint Javalin, porta 84 configurabile via env, config applicativa, connessione PostgreSQL (pool), struttura package, route `/health` e predisposizione config RabbitMQ (stub/base) | Servizio avviabile localmente con health-check e configurazioni base pronte | Alta |
| NS-BE-02 | FR5 | Schema database notifiche | Creare tabella `notifica` (id, user_id, message, type, status, sent_at, read_at, created_at) con migration SQL e indici | Persistenza notifiche pronta e versionata | Alta |
| NS-BE-03 | FR5 | Modello dominio/DAO | Implementare model, repository/DAO e service layer per creare/listare/aggiornare/eliminare notifiche | Accesso dati notifiche incapsulato | Alta |
| NS-BE-04 | FR8 | Middleware autenticazione | Proteggere API notifiche con validazione JWT e risoluzione `userId` dal token/headers gateway | Endpoint notifiche accessibili solo ad autenticati | Alta |
| NS-BE-05 | FR5, FR8 | Autorizzazione owner | Garantire che un utente possa leggere/modificare/eliminare solo le proprie notifiche | Isolamento notifiche per utente | Alta |
| NS-BE-06 | FR5 | Endpoint lista notifiche | Implementare `GET /api/v1/notifications` con filtro per utente, paginazione, filtro `read/unread` e ordinamento per data | API lista notifiche pronta per il frontend | Alta |
| NS-BE-07 | FR5 | Endpoint mark-as-read | Implementare `PUT /api/v1/notifications/{id}/read` con update `read_at`, stato e risposta idempotente | Marcatura come letta via API funzionante | Alta |
| NS-BE-08 | FR5 | Endpoint delete notifica | Implementare `DELETE /api/v1/notifications/{id}` con verifica ownership e risposta idempotente | Eliminazione notifica via API funzionante | Alta |
| NS-BE-09 | FR7, FR5 | Consumer `user.registered` | Consumare evento e generare notifica di benvenuto persistita per l'utente | Notifica onboarding automatica | Alta |
| NS-BE-10 | FR7, FR5 | Consumer `document.uploaded` | Consumare evento e creare notifica "documento caricato" con payload contestuale minimo | Notifica upload automatica | Alta |
| NS-BE-11 | FR7, FR5 | Consumer `search.completed` | Consumare evento e creare notifica "ricerca completata" con metadati minimi | Notifica ricerca automatica | Alta |
| NS-BE-12 | FR5 | Dispatcher/Delivery service | Implementare pipeline asincrona di invio (in-app e opzionale email via JavaMail) con stati `PENDING/SENT/FAILED` | Gestione delivery notifiche strutturata | Media |
| NS-BE-13 | FR5 | Template messaggi notifica | Centralizzare template e mapping evento->messaggio/tipo per coerenza e localizzazione futura | Messaggi notifiche standardizzati | Media |
| NS-BE-14 | FR7, FR5 | Idempotenza e retry eventi | Gestire deduplica eventi, retry su errori transitori e strategia DLQ/parking (placeholder) | Consumo eventi resiliente | Media |
| NS-BE-15 | FR5 | Validazioni ed error handling | Standardizzare errori (`400`, `401`, `403`, `404`, `422`, `500`) e payload risposta API | API notifiche coerenti e gestibili dal frontend | Alta |
| NS-BE-16 | FR5, NFR7 (supporto) | Indici e performance | Creare indici PostgreSQL su `user_id`, `created_at`, `read_at`, `status` e definire limiti pagina | Query lista notifiche piu efficienti | Media |
| NS-BE-17 | FR5, FR7 | Test backend | Unit test service/template + integration test endpoint e consumer eventi con DB/event bus mock | Copertura minima flussi critici Notification Service | Alta |
| NS-BE-18 | FR5 | Logging e osservabilita | Logging strutturato per eventi consumati/invio notifiche/API, metriche backlog e failure rate | Diagnostica minima del servizio | Media |

## Note implementative

1. Implementare prima il flusso minimo `evento consumato -> notifica persistita -> GET /notifications`.
2. Rendere `PUT /notifications/{id}/read` e `DELETE /notifications/{id}` idempotenti per semplificare il frontend.
3. In fase 1 e sufficiente supportare notifiche `IN_APP`; l'invio email puo restare opzionale dietro feature flag.
4. Definire un payload evento minimo condiviso (`userId`, tipo evento, timestamp, metadati contestuali) per evitare accoppiamento eccessivo.
5. Se RabbitMQ non e ancora disponibile, usare un consumer adapter/stub mantenendo invariato il contratto di dominio.

