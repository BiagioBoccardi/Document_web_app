package com.example.user_service.config;

import java.time.Instant;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;

public class MetricsManager {
    private final InfluxDBClient client;
    private final String bucket;
    private final String org;

    public MetricsManager() {
        // Leggi le config dalle variabili d'ambiente del docker-compose
        String url = System.getenv().getOrDefault("INFLUXDB_URL", "http://influxdb2:8086");
        String token = System.getenv("INFLUXDB_TOKEN"); // Il token generato al setup
        this.org = "docs";
        this.bucket = "home";

        this.client = InfluxDBClientFactory.create(url, token.toCharArray(), org, bucket);
    }

    public void logEvent(String measurement, String tagKey, String tagValue, String field, Object value) {
        WriteApiBlocking writeApi = client.getWriteApiBlocking();

        Point point = Point.measurement(measurement)
                .addTag(tagKey, tagValue)
                .addField(field, (Number) value)
                .time(Instant.now(), WritePrecision.NS);

        writeApi.writePoint(bucket, org, point);
    }

    public void shutdown() {
        client.close();
    }
}
