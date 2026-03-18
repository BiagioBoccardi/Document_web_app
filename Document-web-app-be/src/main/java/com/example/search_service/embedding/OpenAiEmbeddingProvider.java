package com.example.search_service.embedding;

// import com.example.search_service.config.DotenvConfig;
// import com.openai.client.OpenAIClient;
// import com.openai.client.okhttp.OpenAIOkHttpClient;
// import com.openai.models.embeddings.CreateEmbeddingResponse;
// import com.openai.models.embeddings.EmbeddingCreateParams;

// import lombok.extern.slf4j.Slf4j;

/**
 * Implementazione del provider di embedding basato su OpenAI.
 * Utilizza il modello text-embedding-3-small per generare vettori a 384 dimensioni.
 */
// @Slf4j
// public class OpenAiEmbeddingProvider implements EmbeddingProvider {

//     private static final String MODEL = "text-embedding-3-small";
//     private static final int VECTOR_SIZE = 384;

    // private final OpenAIClient client;

    // public OpenAiEmbeddingProvider() {
    //     String apiKey = DotenvConfig.get("OPENAI_API_KEY", "");
    //     if (apiKey.isBlank()) {
    //         throw new IllegalStateException("OPENAI_API_KEY non configurata nel file .env");
    //     }
    //     this.client = OpenAIOkHttpClient.builder()
    //             .apiKey(apiKey)
    //             .build();
    //     log.info("OpenAiEmbeddingProvider inizializzato con modello '{}'", MODEL);
    // }

    // @Override
    // public float[] createEmbedding(String text) {
    //     if (text == null || text.isBlank()) {
    //         throw new IllegalArgumentException("Il testo per l'embedding non può essere nullo o vuoto");
    //     }

    //     log.debug("Generazione embedding per testo di {} caratteri", text.length());

    //     EmbeddingCreateParams params = EmbeddingCreateParams.builder()
    //             .input(text)
    //             .model(MODEL)
    //             .dimensions(VECTOR_SIZE)
    //             .build();

    //     CreateEmbeddingResponse response = client.embeddings().create(params);

    //     // Estrae il vettore dalla risposta
    //     java.util.List<Double> values = response.data().get(0).embedding();
    //     float[] embedding = new float[values.size()];
    //     for (int i = 0; i < values.size(); i++) {
    //         embedding[i] = values.get(i).floatValue();
    //     }

    //     log.debug("Embedding generato con {} dimensioni", embedding.length);
    //     return embedding;
    // }
// }