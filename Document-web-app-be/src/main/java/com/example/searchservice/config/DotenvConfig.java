package com.example.searchservice.config;

import io.github.cdimascio.dotenv.Dotenv;

public class DotenvConfig {

    private static final Dotenv dotenv = Dotenv.configure()
            .directory("../") // Carica dalla root del progetto
            .ignoreIfMissing()
            .load();

    public static String get(String key, String defaultValue) {
        String value = dotenv.get(key);
        return value != null ? value : defaultValue;
    }

    public static int getAsInt(String key, int defaultValue) {
        try {
            String value = dotenv.get(key);
            if (value != null) {
                return Integer.parseInt(value);
            }
        } catch (NumberFormatException e) {
            System.err.printf("Variabile d'ambiente '%s' non valida. Uso il valore di default: %d%n", key, defaultValue);
        }
        return defaultValue;
    }
}
