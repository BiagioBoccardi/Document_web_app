package com.example.search_service.dto;

/**
 * DTO per la richiesta di ricerca semantica.
 * SS-BE-06: POST /api/v1/search
 */
public class SearchRequest {

    public String query;
    public int topK = 5; // default 5 risultati

    public SearchRequest() {}
}