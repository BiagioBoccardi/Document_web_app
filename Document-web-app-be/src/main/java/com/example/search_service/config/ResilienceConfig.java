package com.example.search_service.config;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;

import java.time.Duration;

public class ResilienceConfig {

    // Inizializziamo il registry direttamente sulla stessa riga per massima sicurezza
    private static final RetryRegistry registry = RetryRegistry.ofDefaults();

    static {
        // Configurazione per Qdrant: 3 tentativi, attesa esponenziale (2s, 4s, 8s...)
        RetryConfig qdrantConfig = RetryConfig.custom()
                .maxAttempts(3)
                .intervalFunction(IntervalFunction.ofExponentialBackoff(Duration.ofSeconds(2), 2))
                .retryExceptions(Exception.class) 
                .build();

        // Configurazione per l'Embedding Provider: 3 tentativi, attesa esponenziale (1s, 2s, 4s...)
        RetryConfig embeddingConfig = RetryConfig.custom()
                .maxAttempts(3)
                .intervalFunction(IntervalFunction.ofExponentialBackoff(Duration.ofSeconds(1), 2))
                .retryExceptions(Exception.class)
                .build();

        registry.addConfiguration("qdrant", qdrantConfig);
        registry.addConfiguration("embedding", embeddingConfig);
    }

    public static Retry getQdrantRetry() {
        return registry.retry("qdrantRetry", "qdrant");
    }

    public static Retry getEmbeddingRetry() {
        return registry.retry("embeddingRetry", "embedding");
    }
}