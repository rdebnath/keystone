package com.chetana.keystone.security;

import com.nimbusds.jose.jwk.JWKSet;

/**
 * OIDC resource-server settings: the token-signing keys (JWKS) and the expected
 * issuer/audience.
 */
public record SecurityConfig(JWKSet jwkSet, String issuer, String audience) {
}
