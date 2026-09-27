package com.chetana.keystone.realtime;

import java.util.Objects;

/**
 * The wire envelope of a Supabase Realtime broadcast: the channel to publish on, the event name,
 * and the event payload.
 *
 * <p>The payload is the caller's immutable event record — never a {@code Map}, a {@code JsonNode},
 * or a persistence row (see docs/CODING_GUIDELINES_BACKEND.md §13). Serializing this record
 * produces the broadcast body ({@code {"channel":…,"event":…,"payload":…}}).
 *
 * @param channel the namespaced, versioned channel (e.g. {@code orders.42})
 * @param event   the event name within the channel (e.g. {@code order.created})
 * @param payload the immutable event record being broadcast
 * @param <T>     the payload type
 */
public record RealtimeEnvelope<T>(String channel, String event, T payload) {

    public RealtimeEnvelope {
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(payload, "payload");
    }
}
