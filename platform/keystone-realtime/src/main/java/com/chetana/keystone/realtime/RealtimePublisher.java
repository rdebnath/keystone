package com.chetana.keystone.realtime;

/**
 * Port for publishing messages to Supabase Realtime (see docs/ARCHITECTURE.md §5.2).
 */
public interface RealtimePublisher {

    /**
     * Publishes {@code envelope} to its channel. The payload is an immutable event record — never a
     * {@code Map}/{@code JsonNode} or a persistence row (docs/CODING_GUIDELINES_BACKEND.md §13).
     */
    void publish(RealtimeEnvelope<?> envelope);
}
