# Analisi Requisiti - User Service

## Scopo e perimetro

Questo documento dettaglia i task funzionali da implementare **solo per il microservizio 1 - User Service (porta 81)**, sulla base di `README.md` (requisiti funzionali) e `Microservizi.md` (API e responsabilita del servizio).

### Requisiti funzionali coperti (solo User Service)

| ID | Requisito | Copertura in questo documento |
|---|---|---|
| FR2 | Gestione utenti | Si (registrazione, login, profilo, aggiornamento profilo) |
| FR8 | Autenticazione sicura | Si (JWT, hashing password, protezione endpoint) |
| FR9 | Gestione ruoli | Si (Cliente/Amministratore, autorizzazione endpoint admin) |
| FR10 | Dashboard amministratore | Parziale (solo endpoint/lista utenti lato User Service a supporto) |



## API target del User Service (da `Microservizi.md`)

- `POST /api/v1/users/register`
- `POST /api/v1/users/login`
- `GET /api/v1/users/me`
- `PUT /api/v1/users/me`
- `GET /api/v1/users` (solo admin)

## Task Front-end (User Service)

| ID Task | Requisito (FR) | Funzionalita | Descrizione task | Output atteso | Priorita |
|---|---|---|---|---|---|
| US-FE-01 | FR2 | Routing autenticazione | Creare rotte/pagine `Login`, `Registrazione`, `Profilo` e redirect iniziale in base allo stato autenticato | Navigazione base auth funzionante | Alta |
| US-FE-02 | FR2 | Form registrazione | Implementare form registrazione con campi `username`, `email`, `password` + validazioni client (required, email valida, lunghezza password) | Form con validazione e submit verso `POST /users/register` | Alta |
| US-FE-03 | FR2, FR8 | Form login | Implementare form login con gestione submit, loading e messaggi errore | Login UI collegata a `POST /users/login` | Alta |
| US-FE-04 | FR8 | Gestione sessione JWT | Salvare token JWT (strategia definita dal team: storage/cookie), gestire logout e ripristino sessione all'avvio app | Sessione utente persistente e logout pulito | Alta |
| US-FE-05 | FR8, FR9 | Guard route e autorizzazione UI | Proteggere le pagine riservate e mostrare/nascondere sezioni admin in base al ruolo nel token/profilo | Accesso UI controllato per utente/admin | Alta |
| US-FE-06 | FR2 | Profilo utente corrente | Creare pagina profilo che legge i dati utente da `GET /users/me` | Pagina profilo con dati correnti | Media |
| US-FE-07 | FR2 | Aggiornamento profilo | Implementare form modifica profilo con submit a `PUT /users/me` e feedback esito | Aggiornamento profilo da UI | Media |
| US-FE-08 | FR9, FR10 (supporto) | Lista utenti admin | Creare vista tabellare utenti per ruolo admin con chiamata `GET /users` | Pagina admin lista utenti (read-only iniziale) | Media |
| US-FE-09 | FR2, FR8 | Gestione errori auth | Gestire errori `401/403/409/422`, token scaduto e redirect a login | UX coerente su errori e sessione scaduta | Alta |
| US-FE-10 | FR2, FR8, FR9 | Test FE base | Scrivere test componenti/integrazione per login, registrazione e route protette | Copertura minima flusso auth lato FE | Media |

## Task Back-end (User Service)

| ID Task | Requisito (FR) | Area | Descrizione task | Output atteso | Priorita |
|---|---|---|---|---|---|
| US-BE-01 | FR2, FR8, FR9 | Bootstrap servizio | Configurare microservizio `User Service` (Javalin, config porta 81, connessione PostgreSQL, struttura package) | Servizio avviabile con health/config base | Alta |
| US-BE-02 | FR2, FR9 | Database schema utenti | Creare schema tabella `utente` (id, username, email, password_hash, role, created_at, updated_at) con vincoli univoci e migration SQL | DB utenti pronto e versionato | Alta |
| US-BE-03 | FR2, FR8 | Modello dominio/DAO | Implementare model, repository/DAO e service layer per CRUD logico utente (create/find/update/list) | Accesso dati utenti incapsulato | Alta |
| US-BE-04 | FR8 | Sicurezza password | Integrare hashing password (`bcrypt`) e verifica password in login | Password mai salvate in chiaro | Alta |
| US-BE-05 | FR2, FR8 | Endpoint registrazione | Implementare `POST /api/v1/users/register` con validazione input, controllo duplicati username/email e risposta standard | Registrazione utente funzionante | Alta |
| US-BE-06 | FR2, FR8 | Endpoint login + JWT | Implementare `POST /api/v1/users/login` con verifica credenziali e generazione JWT contenente `sub`, `role`, expiry | Login con token JWT valido | Alta |
| US-BE-07 | FR8 | Middleware autenticazione | Implementare filtro/middleware JWT per estrarre utente corrente e proteggere endpoint autenticati | Endpoint `/me` protetti | Alta |
| US-BE-08 | FR9 | Middleware autorizzazione ruoli | Implementare controllo ruolo (`AMMINISTRATORE`) per endpoint admin | `GET /users` accessibile solo admin | Alta |
| US-BE-09 | FR2 | Endpoint profilo corrente | Implementare `GET /api/v1/users/me` usando il contesto autenticato | Recupero profilo utente corrente | Media |
| US-BE-10 | FR2 | Endpoint update profilo | Implementare `PUT /api/v1/users/me` con validazioni, update `updated_at` e regole su campi modificabili | Aggiornamento profilo sicuro | Media |
| US-BE-11 | FR9, FR10 (supporto) | Lista utenti admin | Implementare `GET /api/v1/users` (read-only) con paginazione base e filtro opzionale per ruolo/email | Lista utenti per amministratore | Media |
| US-BE-12 | FR2, FR8 | Validazione e error handling | Standardizzare errori (`400`, `401`, `403`, `409`, `422`, `500`) e payload risposta | API coerenti e gestibili dal frontend | Alta |
| US-BE-13 | FR2, FR8 | Audit campi timestamp | Gestire `created_at`/`updated_at` lato DB o service e serializzazione coerente | Tracciamento modifiche utenti | Media |
| US-BE-14 | FR2, FR8, FR9 | Test backend | Unit test service + integration test endpoint (`register`, `login`, `/me`, `/users`) | Copertura minima dei flussi critici auth/user | Alta |
| US-BE-15 | FR2 (event-driven, opzionale fase 1) | Eventi dominio | Predisporre pubblicazione eventi `user.registered` e `user.updated` (stub o interfaccia) per integrazione futura con RabbitMQ | Punto di estensione pronto per eventi | Bassa |

## Note implementative

1. Implementare prima il flusso minimo end-to-end: `register -> login -> GET /me`.
2. Lasciare `GET /users` admin come read-only iniziale per ridurre complessita.
3. Definire subito il contratto errori API per evitare refactor FE/BE successivi.
4. Se RabbitMQ non e ancora pronto, usare un adapter/event publisher astratto (no-op) nel User Service.
