package com.chetana.keystone.realtime;

/**
 * Port for publishing messages to Supabase Realtime (see docs/ARCHITECTURE.md §5.2).
 */
public interface RealtimePublisher {

    void publish(String channel, String event, Object payload);
}
