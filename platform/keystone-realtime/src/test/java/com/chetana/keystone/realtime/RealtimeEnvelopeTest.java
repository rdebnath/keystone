package com.chetana.keystone.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The broadcast body is the serialized envelope record. Asserted here so the wire shape clients
 * subscribe to ({@code {"channel":…,"event":…,"payload":…}}) stays stable now that the envelope is a
 * record rather than an ad-hoc map (docs/CODING_GUIDELINES_BACKEND.md §13).
 */
class RealtimeEnvelopeTest {

    /** An example typed event payload — payloads are immutable records, never maps. */
    private record OrderCreated(String orderId, int quantity) {
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void should_serialize_the_broadcast_body_with_channel_event_and_payload() throws Exception {
        RealtimeEnvelope<OrderCreated> envelope =
                new RealtimeEnvelope<>("orders.42", "order.created", new OrderCreated("o-1", 3));

        String body = objectMapper.writeValueAsString(envelope);

        assertThat(body).isEqualTo("{\"channel\":\"orders.42\",\"event\":\"order.created\","
                + "\"payload\":{\"orderId\":\"o-1\",\"quantity\":3}}");
    }
}
