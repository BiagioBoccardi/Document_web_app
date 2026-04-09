package com.example.search_service.dto;

/**
 * DTO che rappresenta il payload degli eventi documento
 * ricevuti tramite RabbitMQ.
 *
 * Usato da:
 * - document.uploaded
 * - document.updated
 * - document.deleted
 */
public class DocumentEvent {

    public String documentId;
    public String userId;
    public String filename;
    public String snippet;   // testo da indicizzare (usato per generare embedding)

    @Override
    public String toString() {
        return "DocumentEvent{" +
                "documentId='" + documentId + '\'' +
                ", userId='" + userId + '\'' +
                ", filename='" + filename + '\'' +
                '}';
    }
}
