# Document Service - Passaggi operativi fuori VS Code

Questa guida copre i passaggi **esterni al codice** per eseguire il backend `document_service` con MongoDB.

## 0) Dipendenze aggiuntive Maven
- Tesseract OCR (per elaborazione PDF/DOC/XLS)

- Apache POI (per leggere file Office):

Nel `pom.xml`:

```xml
<!-- Apache POI per DOC/DOCX/XLS/XLSX -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi</artifactId>
    <version>3.17</version>
</dependency>
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>3.17</version>
</dependency>

<!-- Tess4J per OCR -->
<dependency>
    <groupId>net.sourceforge.tess4j</groupId>
    <artifactId>tess4j</artifactId>
    <version>5.5.0</version>
</dependency>
```
## 1) Prerequisiti macchina

- Docker Desktop (consigliato), oppure MongoDB locale installato
- Java 21 runtime disponibile
- Porta libera per servizio backend (`8082`) e MongoDB (`27017`)

## 2) Configurare variabili ambiente

Il `DocumentServiceApplication` usa queste variabili:

- `APP_PORT` (default `8082`)
- `MONGO_URI` (default `mongodb://localhost:27017`)
- `MONGO_DB` (default `document_web_app`)
- `MONGO_COLLECTION` (default `documents`)
- `JWT_SECRET` (consigliato obbligatorio in ambienti reali)
- `JWT_ISSUER` (opzionale)

Esempio PowerShell:

```powershell
$env:APP_PORT="8082"
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

1. `POST /api/v1/documents` (multipart, campo `file` = `.txt, .pdf, .doc, .docx, .xls, .xlsx`)
2. `GET /api/v1/documents?page=0&size=20&sort=desc`
3. `GET /api/v1/documents/{id}`
4. `PUT /api/v1/documents/{id}`
5. `DELETE /api/v1/documents/{id}` (idempotente, `204`)

Header richiesto:

- `Authorization: Bearer <jwt-valido>`
  - oppure fallback gateway `X-User-Id` se configurato a monte

## 7) Note operative

- Il servizio supporta l’upload di file binari su MongoDB GridFS e gestisce estrazione testo tramite OCR per PDF/Office.

- I file .txt vengono processati direttamente senza OCR.

- I metadati dei documenti (dimensione, mime-type, checksum, tipo file) vengono calcolati automaticamente.