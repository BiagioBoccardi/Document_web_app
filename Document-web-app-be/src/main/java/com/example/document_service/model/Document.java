package com.example.document_service.model;
import java.time.LocalDate;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Document {
    private int id;
    private String titolo;
    private LocalDate dataModifica;
    private String tipologia;
    private boolean stato; // true se il documento esiste, false se è stato eliminato
}
