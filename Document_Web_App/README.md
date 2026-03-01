# Document Web App - Piattaforma di Gestione Documenti con Ricerca Semantica


## Panoramica del Progetto

Il progetto consiste in una **piattaforma web per l'upload di documenti testuali**, con funzionalità avanzate di **ricerca semantica tramite embedding vettoriali**, **gestione utenti e documenti** e **notifiche di sistema asincrone**.

### Obiettivi Principali
- Permettere agli utenti di caricare e gestire documenti testuali
- Implementare ricerca semantica intelligente basata su AI
- Garantire notifiche real-time sugli eventi di sistema
- Sviluppare un'architettura scalabile e manutenibile secondo i principi DevOps

### Metodologia
Il progetto segue le best practices **DevOps** con:
- Architettura a microservizi
- Containerizzazione completa
- CI/CD pipeline automatizzata
- Infrastructure as Code

---

## Architettura del Sistema

### Diagramma Architetturale

```
┌──────────────────────────────────────────────────────────────┐
│                    FRONTEND (React)                          │
│                   http://localhost:7000                      │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
              ┌──────────────────────┐
              │    API Gateway       │ 
              │    (Nginx/Traefik)   │
              └──────────┬───────────┘
                         │
         ┌───────────────┼───────────────┬────────────────┐
         │               │               │                │
         ▼               ▼               ▼                ▼
  ┌────────────┐  ┌────────────┐  ┌────────────┐  ┌────────────┐
  │   USER     │  │  DOCUMENT  │  │   SEARCH   │  │NOTIFICATION│
  │  SERVICE   │  │  SERVICE   │  │  SERVICE   │  │  SERVICE   │
  │  8080:81   │  │  8080:82   │  │  8080:83   │  │  8080:84   │
  └──────┬─────┘  └──────┬─────┘  └──────┬─────┘  └──────┬─────┘
         │               │               │                │
         ▼               ▼               ▼                ▼
  ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
  │PostgreSQL│    │ MongoDB  │    │  Qdrant  │    │PostgreSQL│
  │ (users)  │    │  (docs)  │    │(vectors) │    │ (notif.) │
  └──────────┘    └──────────┘    └──────────┘    └──────────┘
         │               │               │                │
         └───────────────┴───────────────┴────────────────┘
                         │
                   ┌─────▼──────┐
                   │  RabbitMQ  │
                   │  (Events)  │
                   └────────────┘
```

### Principi Architetturali

1. **Separazione delle Responsabilità**: Ogni microservizio gestisce un dominio specifico
2. **Database per Microservizio**: Ogni servizio ha il proprio database indipendente
3. **Comunicazione Asincrona**: Eventi gestiti tramite RabbitMQ
4. **API REST**: Comunicazione sincrona tramite HTTP/JSON
5. **Stateless Services**: I servizi non mantengono stato tra le richieste

---

## Stack Tecnologico

| Categoria              | Tecnologia            | Versione | Descrizione                                    |
|------------------------|-----------------------|----------|------------------------------------------------|
| **Frontend**           | React                 | 19.x     | Interfaccia utente web reattiva                |
| **Backend**            | Java                  | 21       | Linguaggio principale                          |
|                        | Javalin               | 5.x      | Framework web leggero                          |
|                        | Maven                 | 3.9+     | Build automation                               |
| **Database Relazionale**| PostgreSQL           | 21       | Gestione dati strutturati (utenti, notifiche)  |
| **Database NoSQL**     | MongoDB               | 6.x      | Storage documenti testuali                     |
| **Database Vettoriale**| Qdrant                | 1.7+     | Ricerca semantica e gestione embedding         |
| **Message Broker**     | RabbitMQ              | 3.12+    | Comunicazione asincrona tra servizi            |
| **Containerizzazione** | Docker                | 24+      | Containerizzazione applicazioni                |
|                        | Docker Compose        | 2.x      | Orchestrazione multi-container                 |
| **CI/CD**              | GitHub Actions        | -        | Pipeline automatizzata                         |
| **Testing**            | JUnit                 | 4.11     | Unit testing                                   |
|                        | Testcontainers        | 1.19+    | Integration testing                            |
| **Logging**            | SLF4J + Logback       | 2.0.7    | Logging strutturato                            |
| **Monitoring**         | Prometheus            | 2.x      | Metriche                                       |
|                        | Grafana               | 10.x     | Visualizzazione metriche                       |

---

## Requisiti Funzionali

| ID   | Requisito                    | Descrizione                                                                                           | Priorità |
|------|------------------------------|-------------------------------------------------------------------------------------------------------|----------|
| FR1  | Upload documenti testuali    | L'utente deve poter caricare documenti testuali in formato TXT                                        | Alta     |
| FR2  | Gestione utenti              | Il sistema deve permettere registrazione, login, logout del profilo utente                 | Media     |
| FR3  | Ricerca semantica            | Il sistema deve permettere la ricerca semantica sui documenti caricati tramite embedding vettoriali   | Alta     |
| FR4  | Gestione documenti           | L'utente deve poter visualizzare, modificare ed eliminare i propri documenti                          | Alta     |
| FR5  | Notifiche asincrone          | Il sistema deve inviare notifiche asincrone (es. documento caricato, ricerca completata)              | Media    |
| FR6  | API REST DB vettoriale       | Il sistema deve esporre API REST per la gestione degli embedding vettoriali                           | Alta     |
| FR7  | Integrazione message broker  | Il sistema deve utilizzare RabbitMQ per la comunicazione asincrona tra microservizi                   | Alta     |
| FR8  | Autenticazione sicura        | Il sistema deve implementare autenticazione JWT per proteggere le API                                 | Alta     |
| FR9  | Gestione ruoli               | Il sistema deve supportare ruoli utente (Cliente, Amministratore) con permessi differenziati          | Media    |
| FR10 | Dashboard amministratore     | Gli amministratori devono avere accesso a statistiche e gestione globale del sistema                  | Bassa    |

---

## Requisiti Non Funzionali

| ID   | Requisito                | Descrizione                                                                                       | Metrica                     |
|------|--------------------------|---------------------------------------------------------------------------------------------------|-----------------------------|
| NFR1 | Interfaccia reattiva     | Il frontend deve essere reattivo e interattivo, realizzato con React                             | Time to Interactive < 3s    |
| NFR2 | Scalabilità              | Il sistema deve supportare la crescita del numero di utenti e documenti                          | 1000+ utenti concorrenti    |
| NFR3 | Containerizzazione       | Il sistema deve essere completamente containerizzato tramite Docker                              | 100% servizi containerizzati|
| NFR4 | Deployment automatico    | Il sistema deve supportare il deployment automatico con rollback                                 | Deploy time < 10 min        |
| NFR5 | Test automatici          | Il sistema deve avere copertura di test >65% (unit, integration, e2e)                            | Code coverage > 65%         |
| NFR6 | Affidabilità             | Il sistema deve garantire l'integrità dei dati e la corretta gestione degli errori               | Uptime > 99%                |
| NFR7 | Performance              | Le API devono rispondere in meno di 500ms per il 95° percentile                                  | p95 < 500ms                 |
| NFR8 | Sicurezza                | Tutte le comunicazioni devono essere cifrate, password hashate con bcrypt                         | OWASP Top 10 compliant      |
| NFR9 | Osservabilità            | Tutti i servizi devono avere logging strutturato e metriche esposte                              | 100% servizi monitorati     |

---

## Considerazioni sui Database

1. **PostgreSQL** per User e Notification:
   - Dati strutturati e transazionali
   - ACID compliance necessaria
   - Relazioni chiare tra entità

2. **MongoDB** per Documents:
   - Schema flessibile per documenti testuali
   - Ottimo per storage di grandi quantità di testo
   - GridFS per file di grandi dimensioni

3. **Qdrant** per Embeddings:
   - Ottimizzato per ricerca vettoriale
   - Similarità coseno ad alta performance
   - Scalabilità orizzontale

