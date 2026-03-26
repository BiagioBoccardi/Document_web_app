# 📚 Documentazione Tecnica - Notification Service

Il microservizio **Notification Service** (Porta 8084) è responsabile della gestione, persistenza e distribuzione delle notifiche agli utenti [cite: Analisi Requisiti - Notification Service].

---

## 📂 Struttura Modello e Persistenza

### `com.example.notification_service.model.Notification`
**Scopo:** Entità JPA che rappresenta una singola notifica nel database [cite: NS-BE-02, NS-BE-03].
* **Attributi Chiave:**
    * `uuid`: Identificativo univoco (UUID) [cite: NS-BE-02].
    * `userId`: ID intero dell'utente destinatario [cite: NS-BE-05].
    * `messaggio`: Contenuto testuale della notifica [cite: NS-BE-02].
    * `stato`: Stato della delivery (`PENDING`, `SENT`, `READ`, `FAILED`) [cite: NS-BE-12].
    * `createdAt`: Timestamp di creazione [cite: NS-BE-16].

### `com.example.notification_service.config.HibernateUtil`
**Scopo:** Gestisce il ciclo di vita della `SessionFactory` e la connessione a PostgreSQL [cite: NS-BE-01, NS-BE-02].
* **Caratteristiche:**
    * Caricamento dinamico delle credenziali da variabili d'ambiente (`POSTGRES_USER`, `POSTGRES_PASSWORD`) [cite: NS-BE-01].
    * Inizializzazione del file di configurazione specifico `notification.hibernate.cfg.xml` [cite: NS-BE-02].

---

## ⚙️ Logica di Business e Accesso Dati

### `com.example.notification_service.repository.NotificationRepository`
**Scopo:** Incapsula le query al database tramite Hibernate [cite: NS-BE-03].
* **Metodi Principali:**
    * `findByUserId`: Recupera notifiche filtrate per utente con supporto a paginazione e stato [cite: NS-BE-06].
    * `findByIdAndUser`: Garantisce l'autorizzazione verificando che la notifica appartenga all'utente richiedente [cite: NS-BE-05].

### `com.example.notification_service.service.NotificationService`
**Scopo:** Coordina la logica di business tra repository e controller [cite: NS-BE-03].
* **Metodi Principali:**
    * `markAsRead`: Aggiorna lo stato della notifica e registra il timestamp di lettura [cite: NS-BE-07].
    * `createNotification`: Metodo centrale per la generazione di nuove notifiche [cite: NS-BE-03].

---

## 📡 Comunicazione e API

### `com.example.notification_service.controller.NotificationController`
**Scopo:** Gestisce gli endpoint REST e la comunicazione con il frontend [cite: Analisi Requisiti - Notification Service].
* **Endpoint gestiti:**
    * `GET /api/v1/notifications`: Lista notifiche utente [cite: NS-BE-06].
    * `PUT /api/v1/notifications/{id}/read`: Marcatura come letto [cite: NS-BE-07].
    * `DELETE /api/v1/notifications/{id}`: Eliminazione notifica [cite: NS-BE-08].

### `com.example.notification_service.messaging.EventConsumer`
**Scopo:** Ascolta i messaggi da RabbitMQ per reagire ad eventi esterni [cite: FR7, NS-BE-09].
* **Eventi gestiti:**
    * `user.registered`: Genera notifica di benvenuto [cite: NS-BE-09].
    * `document.uploaded`: Notifica caricamento file [cite: NS-BE-10].
    * `search.completed`: Notifica completamento ricerca [cite: NS-BE-11].

### `com.example.notification_service.service.NotificationTemplateService`
**Scopo:** Centralizza la generazione dei testi delle notifiche in base ai metadati dell'evento [cite: NS-BE-13].
* **Metodo:** `generateMessage` mappa il tipo di evento a un template testuale standardizzato [cite: NS-BE-13].

---

## 🛠️ Bootstrap e Configurazione

### `com.example.notification_service.NotificationApplication`
**Scopo:** Classe di bootstrap che inizializza Javalin, i middleware di sicurezza e avvia i consumer [cite: NS-BE-01].
* **Funzioni:**
    * Middleware `before()`: Valida la presenza dell'header `X-User-ID` per la sicurezza [cite: NS-BE-04, NS-BE-15].
    * Health Check: Fornisce l'endpoint `/health` per il monitoraggio Docker [cite: NS-BE-01].

### `notification.hibernate.cfg.xml`
**Scopo:** File XML di configurazione per il dialetto SQL, il pool di connessioni (C3P0) e il mapping delle entity [cite: NS-BE-01, NS-BE-02].

---

## 📋 Riepilogo API Core

| Metodo | Path | Descrizione | Requisito |
| :--- | :--- | :--- | :--- |
| **GET** | `/api/v1/notifications` | Lista notifiche (paginata) | FR5, FR8 |
| **PUT** | `/api/v1/notifications/{id}/read` | Marca notifica come letta | FR5 |
| **DELETE** | `/api/v1/notifications/{id}` | Elimina notifica | FR5 |
| **GET** | `/health` | Stato del servizio | NFR7 |
