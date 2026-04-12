package com.example.notification_service.config;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MetricsManager {
    private final InfluxDBClient client;
    private final String bucket;
    private final String influxOrg;

    public MetricsManager() {
        this.influxOrg = System.getenv().getOrDefault("INFLUXDB_ORG", "docs");
        this.bucket = System.getenv().getOrDefault("INFLUXDB_BUCKET", "home");
        String url = System.getenv().getOrDefault("INFLUXDB_URL", "http://influxdb2:8086");
        String tokenFilePath = System.getenv("INFLUXDB_TOKEN_FILE");

        String token = "";
        try {
            // Legge il token dal file montato da Docker Secrets
            token = new String(Files.readAllBytes(Paths.get(tokenFilePath))).trim();
        } catch (Exception e) {
            log.error("Errore critico: Impossibile leggere il token InfluxDB dal file secret!", e);
        }

        this.client = InfluxDBClientFactory.create(url, token.toCharArray(), influxOrg, bucket);
    }

    public void logEvent(String measurement, String tagKey, String tagValue, String field, Object value) {
        try {
            WriteApiBlocking writeApi = client.getWriteApiBlocking();
            Point point = Point.measurement(measurement)
                    .addTag(tagKey, tagValue)
                    .addField(field, (Number) value)
                    .time(Instant.now(), WritePrecision.NS);

            if (value instanceof Number) {
                point.addField(field, (Number) value);
            } else if (value instanceof String) {
                point.addField(field, (String) value);
            } else if (value instanceof Boolean) {
                point.addField(field, (Boolean) value);
            }

            writeApi.writePoint(bucket, influxOrg, point);
        } catch (Exception e) {
            log.warn("Invio metrica fallito: {}", e.getMessage());
        }
    }

    public void shutdown() {
        if (client != null) client.close();
    }
}