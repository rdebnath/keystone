package com.chetana.keystone.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Publishes to Supabase Realtime over HTTPS using the JDK {@link HttpClient}.
 *
 * <p>NOTE: the exact broadcast endpoint/shape is still an open question (see
 * docs/ARCHITECTURE.md §12); this is the server-side transport skeleton using the service-role
 * key. The service-role key must never leave the backend.
 */
public final class SupabaseRealtimePublisher implements RealtimePublisher {

    private final URI endpoint;
    private final String serviceRoleKey;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public SupabaseRealtimePublisher(URI endpoint, String serviceRoleKey, ObjectMapper objectMapper) {
        this.endpoint = endpoint;
        this.serviceRoleKey = serviceRoleKey;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    @Override
    public void publish(RealtimeEnvelope<?> envelope) {
        try {
            String body = objectMapper.writeValueAsString(envelope);
            HttpRequest request = HttpRequest.newBuilder(endpoint)
                    .header("apikey", serviceRoleKey)
                    .header("Authorization", "Bearer " + serviceRoleKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new IllegalStateException("Realtime publish returned HTTP " + response.statusCode());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Realtime publish interrupted", e);
        } catch (Exception e) {
            throw new IllegalStateException("Realtime publish failed", e);
        }
    }
}
