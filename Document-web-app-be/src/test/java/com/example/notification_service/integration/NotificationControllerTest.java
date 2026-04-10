package com.example.notification_service.integration;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.notification_service.NotificationServiceApplication;

import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;

public class NotificationControllerTest {
    // Utilizziamo JavalinTest per avviare un server temporaneo durante il test
    @Test
    @DisplayName("Test: Accesso negato senza Header di autenticazione")
    void testListNotifications_Unauthorized() {
        Javalin app = new NotificationServiceApplication().getJavalinInstance(); // Metodo per ottenere l'istanza app

        JavalinTest.test(app, (server, client) -> {
            // Eseguiamo una GET senza l'header X-User-ID
            var response = client.get("/api/v1/notifications");
            
            // Verifichiamo che risponda 401 Unauthorized
            assertThat(response.code()).isEqualTo(401);
        });
    }

    @Test
    @DisplayName("Test: Recupero lista notifiche con successo")
    void testListNotifications_Success() {

        com.example.notification_service.config.HibernateUtil.getSessionFactory();

        Javalin app = new NotificationServiceApplication().getJavalinInstance();

        JavalinTest.test(app, (server, client) -> {
            // Eseguiamo la chiamata con l'header corretto
            var response = client.get("/api/v1/notifications", q -> q.header("X-User-ID", "1"));
            
            assertThat(response.code()).isEqualTo(200);
            assertThat(response.body().string()).contains("["); 
        });
    }

    @Test
    @DisplayName("Test: Segna come letta una notifica inesistente")
    void testMarkAsRead_NotFound() {
        Javalin app = new NotificationServiceApplication().getJavalinInstance();

        JavalinTest.test(app, (server, client) -> {
            // RestAssured ci permette di configurare la richiesta in modo fluido
            io.restassured.RestAssured.given()
                .port(server.port()) 
                .header("X-User-ID", "1")
            .when()
                .put("/api/v1/notifications/" + java.util.UUID.randomUUID() + "/read")
            .then()
                .statusCode(org.hamcrest.Matchers.not(200)); 
        });
    }
}
