# Analisi Requisiti - Document Service

## Scopo e perimetro

Questo documento dettaglia i task funzionali da implementare **solo per il microservizio 2 - Document Service (porta 8082)**, sulla base di `README.md` (requisiti funzionali) e `Microservizi.md` (responsabilita, API, eventi e stack del servizio).




## API target del Document Service (da `Microservizi.md`)

- `POST /api/v1/documents` - Upload nuovo documento
- `GET /api/v1/documents` - Lista documenti utente
- `GET /api/v1/documents/{id}` - Recupera documento specifico
- `PUT /api/v1/documents/{id}` - Modifica documento
- `DELETE /api/v1/documents/{id}` - Elimina documento

## Task Front-end (Document Service)

| ID Task | Requisito (FR) | Funzionalita | Descrizione task | Output atteso | Priorita |
|---|---|---|---|---|---|
| DS-FE-01 | FR1, FR4, FR8 | Routing area documenti | Creare area protetta con pagine `Lista Documenti`, `Upload`, `Dettaglio/Modifica` accessibili solo ad utenti autenticati | Navigazione documenti protetta e coerente | Alta |
| DS-FE-02 | FR1 | Form upload TXT | Implementare form upload con selezione file `.txt`, validazioni client (estensione/mime, dimensione max, nome file) | Upload UI pronta con validazioni base | Alta |
| DS-FE-03 | FR1, FR8 | Chiamata upload API | Inviare `multipart/form-data` a `POST /api/v1/documents` con token JWT e gestione stato upload/loading | Upload documento funzionante da UI | Alta |
| DS-FE-04 | FR4 | Lista documenti utente | Implementare tabella/lista con dati da `GET /api/v1/documents` (nome, data upload, ultima modifica) | Lista documenti visualizzata correttamente | Alta |
| DS-FE-05 | FR4 | Dettaglio documento | Creare pagina dettaglio che usa `GET /api/v1/documents/{id}` e mostra contenuto + metadati | Visualizzazione documento singolo | Alta |
| DS-FE-06 | FR4 | Modifica documento | Implementare editor testuale e submit a `PUT /api/v1/documents/{id}` con feedback esito | Modifica contenuto documento da UI | Alta |
| DS-FE-07 | FR4 | Eliminazione documento | Aggiungere azione elimina con dialog di conferma e chiamata `DELETE /api/v1/documents/{id}` | Eliminazione documento con refresh lista | Alta |
| DS-FE-08 | FR1, FR4 | UX stati e feedback | Gestire loading, empty state, upload progress (se disponibile), success/error toast e retry | UX solida per flussi documenti | Media |
| DS-FE-09 | FR4, FR8 | Gestione errori API | Gestire errori `401/403/404/409/413/415/422` con messaggi chiari e redirect login su token scaduto | Error handling coerente lato FE | Alta |
| DS-FE-10 | FR1, FR4 | Test FE documenti | Scrivere test componenti/integrazione per upload, lista, modifica, delete e stati errore | Copertura minima flussi documenti FE | Media |

## Task Back-end (Document Service)

| ID Task | Requisito (FR) | Area | Descrizione task | Output atteso | Priorita |
|---|---|---|---|---|---|
| DS-BE-01 | FR1, FR4, FR7 | Bootstrap servizio | Configurare `Document Service` (Javalin, porta 82, config env, connessione MongoDB, bucket GridFS) | Servizio avviabile con configurazione base | Alta |
| DS-BE-02 | FR1, FR4 | Modello dati documenti | Definire modello metadata documento (userId, filename, content/stream ref, uploadDate, lastModified, metadata size/mime/checksum) e collection | Schema applicativo documenti pronto | Alta |
| DS-BE-03 | FR8 | Middleware autenticazione | Proteggere API documenti con validazione JWT e risoluzione `userId` dal token/headers gateway | Endpoint documenti accessibili solo ad autenticati | Alta |
| DS-BE-04 | FR4, FR8 | Autorizzazione owner | Implementare controlli ownership: un utente puo leggere/modificare/eliminare solo i propri documenti | Isolamento dati per utente garantito | Alta |
| DS-BE-05 | FR1 | Upload endpoint | Implementare `POST /api/v1/documents` con parsing multipart, validazione file TXT, size limit, checksum e salvataggio su Mongo/GridFS | Upload backend funzionante e sicuro | Alta |
| DS-BE-06 | FR4 | Lista documenti endpoint | Implementare `GET /api/v1/documents` con filtro per utente autenticato, paginazione e ordinamento per data | Lista documenti utente disponibile via API | Alta |
| DS-BE-07 | FR4 | Dettaglio documento endpoint | Implementare `GET /api/v1/documents/{id}` con verifica ownership e payload contenuto/metadati | Recupero documento specifico funzionante | Alta |
| DS-BE-08 | FR4 | Modifica documento endpoint | Implementare `PUT /api/v1/documents/{id}` per aggiornare contenuto testuale/nome file, ricalcolo checksum e `lastModified` | Modifica documento persistita correttamente | Alta |
| DS-BE-09 | FR4 | Eliminazione documento endpoint | Implementare `DELETE /api/v1/documents/{id}` con rimozione metadata + file GridFS e risposta idempotente | Eliminazione documento completa | Alta |
| DS-BE-10 | FR1, FR4 | Validazioni ed error handling | Standardizzare errori (`400`, `401`, `403`, `404`, `413`, `415`, `422`, `500`) e payload risposta | API documenti coerenti per il frontend | Alta |
| DS-BE-11 | FR7, FR5, FR3 (supporto) | Pubblicazione eventi | Pubblicare eventi `document.uploaded`, `document.updated`, `document.deleted` con payload minimo (`documentId`, `userId`, `filename`, timestamp`) | Integrazione asincrona pronta per Search/Notification | Alta |
| DS-BE-12 | FR7 | Consumo eventi `user.deleted` | Implementare consumer evento `user.deleted` per cancellazione massiva documenti utente (metadata + GridFS) | Cleanup automatico documenti utente eliminato | Media |
| DS-BE-13 | FR4, NFR7 (supporto) | Indici e performance | Creare indici Mongo (`userId`, `uploadDate`, `lastModified`) e definire limiti pagina default/max | Query lista/dettaglio piu efficienti | Media |
| DS-BE-14 | FR1, FR4, FR7 | Test backend | Unit test service/repository + integration test endpoint e persistenza Mongo (event publisher stub/mock) | Copertura minima flussi critici documenti | Alta |
| DS-BE-15 | FR1, FR4 | Logging e tracing base | Log strutturati per upload/update/delete con correlation/request id (senza dati sensibili) | Diagnostica minima del servizio | Media |

## Note implementative

1. Implementare prima il flusso minimo `POST /documents -> GET /documents -> GET /documents/{id}`.
2. Nella prima fase supportare solo file `TXT` come da requisito FR1.
3. Separare chiaramente metadata e contenuto (GridFS) per facilitare evoluzioni future su file piu grandi.
4. Definire subito il payload degli eventi `document.*` per evitare incompatibilita con Search/Notification Service.
5. Se RabbitMQ non e ancora disponibile, introdurre un publisher astratto (no-op/mock) mantenendo il contratto evento.
