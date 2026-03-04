package com.example.searchservice.embedding;

import com.example.searchservice.config.DotenvConfig;

public class EmbeddingProviderFactory {

    public static EmbeddingProvider createProvider() {
        String providerType = DotenvConfig.get("EMBEDDING_PROVIDER", "mock");

        switch (providerType.toLowerCase()) {
            // case "openai":
            //     return new OpenAIEmbeddingProvider();
            // case "sentence-transformer":
            //     return new SentenceTransformerProvider();
            default:
                System.out.println("--- Provider di embedding non specificato o non valido, uso 'mock'. ---");
                return new MockEmbeddingProvider();
        }
    }
}
