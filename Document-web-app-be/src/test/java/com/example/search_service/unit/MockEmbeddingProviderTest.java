package com.example.search_service.unit;

import com.example.search_service.embedding.MockEmbeddingProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
@DisplayName("MockEmbeddingProvider - Test unitari")
class MockEmbeddingProviderTest {

    private MockEmbeddingProvider provider;

    @BeforeEach
    void setUp() {
        provider = new MockEmbeddingProvider();
    }

    // ═════════════════════════════════════════════
    // createEmbedding()
    // ═════════════════════════════════════════════
    @Nested
    @DisplayName("createEmbedding()")
    class CreateEmbedding {

        @Test
        @DisplayName("OK: restituisce un array non nullo")
        void shouldReturnNonNullArray() {
            float[] result = provider.createEmbedding("testo di prova");
            assertNotNull(result);
        }

        @Test
        @DisplayName("OK: il vettore ha esattamente 384 dimensioni")
        void shouldReturn384Dimensions() {
            float[] result = provider.createEmbedding("qualsiasi testo");
            assertEquals(384, result.length);
        }

        @Test
        @DisplayName("OK: i valori del vettore sono compresi tra -1.0 e 1.0")
        void shouldReturnValuesInRange() {
            float[] result = provider.createEmbedding("testo valido");
            for (float v : result) {
                assertTrue(v >= -1.0f && v <= 1.0f,
                        "Valore fuori range: " + v);
            }
        }

        @Test
        @DisplayName("OK: due chiamate con testi diversi producono vettori diversi")
        void shouldProduceDifferentVectorsForDifferentInputs() {
            float[] v1 = provider.createEmbedding("primo testo");
            float[] v2 = provider.createEmbedding("secondo testo completamente diverso");
            // Con vettori casuali, la probabilità che siano identici è astronomicamente bassa
            assertFalse(java.util.Arrays.equals(v1, v2),
                    "Due vettori casuali non dovrebbero essere identici");
        }

        @Test
        @DisplayName("KO: testo nullo lancia IllegalArgumentException")
        void shouldThrowOnNullText() {
            assertThrows(IllegalArgumentException.class,
                    () -> provider.createEmbedding(null));
        }

        @Test
        @DisplayName("KO: testo vuoto lancia IllegalArgumentException")
        void shouldThrowOnBlankText() {
            assertThrows(IllegalArgumentException.class,
                    () -> provider.createEmbedding("   "));
        }
    }
}
