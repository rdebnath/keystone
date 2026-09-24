package com.chetana.keystone.platform.admin.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.chetana.keystone.security.SecurityConfig;

import java.io.InputStream;
import java.net.URL;

/**
 * Loads the OIDC token-signing keys (JWKS) and builds the admin {@link SecurityConfig} for
 * {@link com.chetana.keystone.security.SecurityModule}.
 */
public final class SecurityConfigFactory {

    private SecurityConfigFactory() {
    }

    public static SecurityConfig load(AdminConfig.Security security) {
        try (InputStream in = new URL(security.jwksUrl()).openStream()) {
            JWKSet jwkSet = JWKSet.load(in);
            return new SecurityConfig(jwkSet, security.issuer(), security.audience());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load OIDC JWKS from " + security.jwksUrl(), e);
        }
    }
}
