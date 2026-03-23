package com.example.search_service.dto;

import java.util.List;

/**
 * DTO per la risposta dell'endpoint POST /api/v1/search.
 */
public class SearchResponse {

    public String query;
    public int total;
    public List<SearchResult> results;

    public SearchResponse(String query, List<SearchResult> results) {
        this.query = query;
        this.results = results;
        this.total = results.size();
    }
}