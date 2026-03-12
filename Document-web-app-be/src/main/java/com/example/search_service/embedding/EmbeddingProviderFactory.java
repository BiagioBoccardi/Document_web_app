package com.example.search_service.embedding;

import com.example.search_service.config.DotenvConfig;

import lombok.extern.slf4j.Slf4j;

/**
 * Factory per la creazione del provider di embedding.
 * Il provider viene scelto tramite la variabile d'ambiente EMBEDDING_PROVIDER.
 * Valori supportati: "openai" | "mock" (default)
 */
@Slf4j
public class EmbeddingProviderFactory {

    public static EmbeddingProvider createProvider() {
        String providerType = DotenvConfig.get("EMBEDDING_PROVIDER", "mock");

        switch (providerType.toLowerCase()) {
            case "openai":
                log.info("EmbeddingProvider selezionato: OpenAI");
                return new OpenAiEmbeddingProvider();
            default:
                log.warn("EMBEDDING_PROVIDER='{}' non riconosciuto o non specificato, uso 'mock'", providerType);
                return new MockEmbeddingProvider();
        }
    }
}