# Document Service - Passaggi operativi fuori VS Code

Questa guida copre i passaggi **esterni al codice** per eseguire il backend `document_service` con MongoDB.

## 1) Prerequisiti macchina

- Docker Desktop (consigliato), oppure MongoDB locale installato
- Java 21 runtime disponibile
- Porta libera per servizio backend (`82`) e MongoDB (`27017`)

## 2) Configurare variabili ambiente

Il `DocumentServiceApplication` usa queste variabili:

- `APP_PORT` (default `82`)
- `MONGO_URI` (default `mongodb://localhost:27017`)
- `MONGO_DB` (default `document_web_app`)
- `MONGO_COLLECTION` (default `documents`)
- `JWT_SECRET` (consigliato obbligatorio in ambienti reali)
- `JWT_ISSUER` (opzionale)

Esempio PowerShell:

```powershell
$env:APP_PORT="82"
$env:MONGO_URI="mongodb://localhost:27017"
$env:MONGO_DB="document_web_app"
$env:MONGO_COLLECTION="documents"
$env:JWT_SECRET="super-secret-change-me"
$env:JWT_ISSUER="document-web-app"
```

## 3) Avviare MongoDB (opzione Docker)

```powershell
docker run -d --name mongo-document-service -p 27017:27017 mongo:6
```

Verifica container:

```powershell
docker ps
```

## 4) Verifica indici Mongo (DS-BE-13)

Il repository crea automaticamente gli indici all'avvio del servizio:

- `userId`
- `uploadDate`
- `lastModified`
- composto (`userId`, `lastModified`)

Verifica da shell Mongo:

```javascript
use document_web_app
db.documents.getIndexes()
```

## 5) Avvio backend

Eseguire il backend con le env impostate (da root `Document-web-app-be`).

```powershell
cd ".\Document-web-app-be\"
mvn clean compile
mvn exec:java
```

## 6) Smoke test API minimi

1. `POST /api/v1/documents` (multipart, campo `file` = `.txt`)
2. `GET /api/v1/documents?page=0&size=20&sort=desc`
3. `GET /api/v1/documents/{id}`
4. `PUT /api/v1/documents/{id}`
5. `DELETE /api/v1/documents/{id}` (idempotente, `204`)

Header richiesto:

- `Authorization: Bearer <jwt-valido>`
  - oppure fallback gateway `X-User-Id` se configurato a monte
