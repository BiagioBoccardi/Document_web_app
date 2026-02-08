# Document_Web_App
Il progetto consiste in una piattaforma web per l'upload di documenti testuali, ricerca semantica con embedding vettoriali, gestione utenti e documenti e notifiche di sistema asincrone. 
# Uno Spunto Alle Tecnolgie
Il tutto deve essere sviluppato in microservizi usando un linguaggio a piacere del gruppoPostgreSQLcome DBMS, MongoDB peri documenti, e un DB Vettoriale con API REST, RabbitMQ oppure Kafka per il message broking, usare nel possibile tecnologie simili a HTMX.
Tutto il progetto deve essere sviluppato secondo le regole della DevOps.
# Stack Tecnologico 
| Categoria         | Tecnologia            | Descrizione sintetica                          |
|-------------------|-----------------------|------------------------------------------------|
| Frontend          | JavaScript (React)    | Interfaccia utente web                         |
| Backend           | Java (Javalin)        | API e logica applicativa                       |
| Database Relazionale | PostgreSQL         | Gestione dati strutturati e transazionali     |
| Database Vettoriale  | Da Designare             | Ricerca semantica e gestione embedding  |
| Database NoSQL       | MongoDB            | Dati non strutturati / document-oriented      |
| Message Broker       | RabbitMQ           | Comunicazione asincrona tra servizi            |
# Requsiti Funzionali 
| ID   | Requisito | Descrizione |
|------|-----------|-------------|
| FR1  | Upload documenti testuali | L’utente deve poter caricare documenti testuali in formato TXT. |
| FR2  | Gestione utenti | Il sistema deve permettere la registrazione, autenticazione e gestione del profilo utente. |
| FR3  | Ricerca semantica | Il sistema deve permettere la ricerca semantica sui documenti caricati tramite embedding vettoriali. |
| FR4  | Gestione documenti | L’utente deve poter visualizzare, modificare ed eliminare i propri documenti. |
| FR5  | Notifiche asincrone | Il sistema deve inviare notifiche asincrone di sistema (es. nuovo documento caricato, ricerca completata). |
| FR6  | API REST DB vettoriale | Il sistema deve esporre API REST per la gestione degli embedding vettoriali. |
| FR7  | Integrazione message broker | Il sistema deve utilizzare un message broker (RabbitMQ/Kafka) per la comunicazione asincrona tra microservizi. |
# Requsiti Non Funzionali 
| ID    | Requisito | Descrizione |
|-------|-----------|-------------|
| NFR1  | Interfaccia reattiva | Il frontend deve essere reattivo e interattivo, preferibilmente realizzato con React. |
| NFR2  | Scalabilità | Il sistema deve supportare la crescita del numero di utenti e documenti. |
| NFR3  | Containerizzazione | Il sistema deve essere containerizzato tramite Docker. |
| NFR4  | Deployment automatico | Il sistema deve supportare il deployment automatico (es. Kubernetes). |
| NFR5  | Test automatici | Il sistema deve essere coperto da test automatici (unit, integration, end-to-end). |
| NFR6  | Affidabilità | Il sistema deve garantire l’integrità dei dati e la corretta gestione degli errori. |
# Rischi e Dipendenze
| ID  | Rischio | Descrizione | Impatto |
|-----|--------|-------------|---------|
| R1  | Integrazione tra database | La sincronizzazione tra PostgreSQL, MongoDB e Qdrant può introdurre complessità e incoerenze dei dati. | Alto |
| R2  | Gestione embedding vettoriali | La generazione, aggiornamento e indicizzazione degli embedding può causare problemi di performance. | Alto |
| R3  | Scalabilità della ricerca semantica | L’aumento dei documenti potrebbe degradare le prestazioni delle query vettoriali. | Medio |
| R4  | Comunicazione asincrona | Errori nella gestione dei messaggi del broker possono causare perdita o duplicazione di eventi. | Medio |
| R5  | Complessità architetturale | L’uso di più tecnologie e microservizi aumenta i costi di manutenzione e debugging. | Medio |
| R6  | Sicurezza dei dati | Una gestione non corretta dell’autenticazione può esporre dati sensibili degli utenti. | Alto |
# Architettura Del Sistema
Microservizi: Suddivisione logica dei servizi (es. servizio utenti, servizio documenti,
servizio ricerca, servizio notifiche).
# Develops
| Area | Tecnologia / Processo | Descrizione |
|-----|-----------------------|-------------|
| CI/CD | Pipeline CI/CD | Il progetto utilizza una pipeline CI/CD per automatizzare build, test e deployment del software. |
| Build | Maven / npm | Automazione della compilazione del backend Java e del frontend React. |
| Testing | Test JUnit | Esecuzione  di test Con JUnit, di integrazione ed end-to-end nella pipeline. |
| Containerizzazione | Docker | Ogni componente del sistema viene containerizzato per garantire portabilità e consistenza degli ambienti. |
| Logging | Slf4J | Raccolta e analisi centralizzata dei log per il debugging e il controllo del sistema. |


