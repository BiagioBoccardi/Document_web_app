package com.example.search_service.embedding;

import com.example.search_service.config.DotenvConfig;

public class EmbeddingProviderFactory {

    public static EmbeddingProvider createProvider() {
        String providerType = DotenvConfig.get("EMBEDDING_PROVIDER", "mock");

        switch (providerType.toLowerCase()) {
            default -> {
                System.out.println("--- Provider di embedding non specificato o non valido, uso 'mock'. ---");
                return new MockEmbeddingProvider();
            }
        }
        // case "openai":
        //     return new OpenAIEmbeddingProvider();
        // case "sentence-transformer":
        //     return new SentenceTransformerProvider();
            }
}
