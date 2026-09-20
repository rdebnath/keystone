package com.chetana.keystone.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.chetana.keystone.common.error.AccessDeniedException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtAuthenticatorTest {

    @Test
    void should_authenticate_valid_token() throws Exception {
        RSAKey rsaKey = new RSAKeyGenerator(2048).generate();
        JwtAuthenticator authenticator = new JwtAuthenticator(
                new JWKSet(rsaKey.toPublicJWK()), "https://issuer.example", "inventory");

        String token = sign(rsaKey, "user-123", "https://issuer.example", "inventory",
                Date.from(Instant.now().plusSeconds(300)));

        Principal principal = authenticator.authenticate(token);

        assertThat(principal.subject()).isEqualTo("user-123");
    }

    @Test
    void should_reject_token_with_wrong_audience() throws Exception {
        RSAKey rsaKey = new RSAKeyGenerator(2048).generate();
        JwtAuthenticator authenticator = new JwtAuthenticator(
                new JWKSet(rsaKey.toPublicJWK()), "https://issuer.example", "inventory");

        String token = sign(rsaKey, "user-123", "https://issuer.example", "other-audience",
                Date.from(Instant.now().plusSeconds(300)));

        assertThatThrownBy(() -> authenticator.authenticate(token))
                .isInstanceOf(AccessDeniedException.class);
    }

    private static String sign(RSAKey key, String subject, String issuer, String audience, Date expiry) throws Exception {
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(),
                new JWTClaimsSet.Builder()
                        .subject(subject)
                        .issuer(issuer)
                        .audience(audience)
                        .expirationTime(expiry)
                        .build());
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }
}
