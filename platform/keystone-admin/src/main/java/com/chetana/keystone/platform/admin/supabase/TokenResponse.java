package com.chetana.keystone.platform.admin.supabase;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * The GoTrue password-grant token response, mapped into the {@link Session} handed back to the
 * client. The wire fields are snake_case, so the naming strategy maps them once here instead of
 * repeating string keys at the call site (see docs/CODING_GUIDELINES_BACKEND.md §13).
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {

    TokenResponse {
        refreshToken = refreshToken == null ? "" : refreshToken;
        tokenType = tokenType == null ? "" : tokenType;
    }
}
