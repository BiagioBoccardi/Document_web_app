package com.example.search_service.embedding;

/**
 * Implementazione SEGNAPOSTO per un provider di embedding basato su OpenAI.
 * Attualmente non implementata.
 */
public class OpenAiEmbeddingProvider implements EmbeddingProvider {

    public OpenAiEmbeddingProvider() {
        // Qui andrebbe il codice per inizializzare il client OpenAI,
        // leggendo la chiave API dall'ambiente.
        System.out.println("--- [INFO] Inizializzato OpenAiEmbeddingProvider (da implementare) ---");
    }

    @Override
    public float[] createEmbedding(String text) {
        // Logica per chiamare l'API di OpenAI e ottenere il vettore.
        // Fino ad allora, lanciamo un'eccezione per indicare che non è utilizzabile.
        throw new UnsupportedOperationException("L'integrazione con OpenAI non è ancora stata implementata.");
    }
}
