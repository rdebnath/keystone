package com.chetana.keystone.platform.admin.supabase;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * GoTrue's error body — e.g. {@code {"code":400,"error_code":"invalid_credentials","msg":"Invalid login
 * credentials"}} — as far as the adapter needs it: the machine-readable code, which is what tells an
 * operator a wrong password apart from an expired service key, a rate limit or a Supabase incident.
 *
 * <p>{@code msg} is parsed (so the record describes the whole shape) but is deliberately never logged:
 * it can echo the submitted identifier. Unknown fields are ignored
 * (docs/CODING_GUIDELINES_BACKEND.md §13).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record GoTrueError(Integer code, @JsonProperty("error_code") String errorCode, String msg) {

    /** The most specific code this body carries: {@code error_code}, else {@code code}, else {@code unknown}. */
    String describe() {
        if (errorCode != null && !errorCode.isBlank()) {
            return errorCode;
        }
        return code == null ? "unknown" : code.toString();
    }
}
