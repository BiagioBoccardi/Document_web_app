package com.example.search_service.dto;

/**
 * DTO per un singolo risultato di ricerca semantica.
 */
public class SearchResult {

    public String documentId;
    public String filename;
    public String snippet;
    public double score;

    public SearchResult(String documentId, String filename, String snippet, double score) {
        this.documentId = documentId;
        this.filename = filename;
        this.snippet = snippet;
        this.score = score;
    }
}