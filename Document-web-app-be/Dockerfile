# FASE 1: Build (utilizziamo Maven per compilare il progetto)
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build

# Definizione della cartella di lavoro
WORKDIR /app

# Copia solo il file delle dipendenze per sfruttare la cache di Docker
COPY pom.xml .

# Scarica le dipendenze (senza compilare il codice)
RUN mvn dependency:go-offline

# Copia il codice sorgente
COPY src ./src

# Compila e crea il pacchetto JAR (escludendo i test per velocizzare la build)
RUN mvn clean package -DskipTests

# FASE 2: Runtime (immagine finale leggera)
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copia il file JAR generato dalla fase di build
# Nota: assicurati che il nome del file JAR corrisponda a quello definito nel tuo pom.xml
COPY --from=build /Document-web-app-be/target/*.jar app.jar

# Espone la porta su cui gira la tua applicazione (es. 8080)
EXPOSE 8080

# Comando per avviare l'applicazione
ENTRYPOINT ["java", "-jar", "app.jar"]