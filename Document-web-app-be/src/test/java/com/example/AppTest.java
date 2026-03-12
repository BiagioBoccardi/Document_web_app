package com.example;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatCode;

class AppTest {

    @Test
    void appMainClassExists() {
        // Verifica che la classe App sia istanziabile senza errori di caricamento
        assertThatCode(() -> Class.forName("com.example.App"))
                .doesNotThrowAnyException();
    }
}