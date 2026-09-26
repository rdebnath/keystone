package com.chetana.keystone.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.chetana.keystone.common.error.AccessDeniedException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtAuthenticatorTest {

    private static final String ISSUER = "https://issuer.example";
    private static final String AUDIENCE = "inventory";

    @Test
    void should_authenticate_valid_token() throws Exception {
        RSAKey rsaKey = new RSAKeyGenerator(2048).generate();
        JwtAuthenticator authenticator = new JwtAuthenticator(
                new JWKSet(rsaKey.toPublicJWK()), ISSUER, AUDIENCE);

        String token = sign(JWSAlgorithm.RS256, rsaKey, new RSASSASigner(rsaKey), "user-123", AUDIENCE);

        Principal principal = authenticator.authenticate(token);

        assertThat(principal.subject()).isEqualTo("user-123");
    }

    @Test
    void should_authenticate_es256_token_when_the_key_is_selected_by_kid() throws Exception {
        ECKey signingKey = new ECKeyGenerator(Curve.P_256).keyID("ec-key").generate();
        RSAKey otherKey = new RSAKeyGenerator(2048).keyID("rsa-key").generate();
        JwtAuthenticator authenticator = new JwtAuthenticator(
                new JWKSet(List.of(signingKey.toPublicJWK(), otherKey.toPublicJWK())), ISSUER, AUDIENCE);

        String token = sign(JWSAlgorithm.ES256, signingKey, new ECDSASigner(signingKey), "user-123", AUDIENCE);

        assertThat(authenticator.authenticate(token).subject()).isEqualTo("user-123");
    }

    @Test
    void should_reject_es256_token_when_the_signing_key_is_not_published() throws Exception {
        ECKey publishedKey = new ECKeyGenerator(Curve.P_256).keyID("ec-key").generate();
        ECKey unknownKey = new ECKeyGenerator(Curve.P_256).keyID("unknown-key").generate();
        JwtAuthenticator authenticator = new JwtAuthenticator(
                new JWKSet(publishedKey.toPublicJWK()), ISSUER, AUDIENCE);

        String token = sign(JWSAlgorithm.ES256, unknownKey, new ECDSASigner(unknownKey), "user-123", AUDIENCE);

        assertThatThrownBy(() -> authenticator.authenticate(token))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void should_reject_token_with_wrong_audience() throws Exception {
        RSAKey rsaKey = new RSAKeyGenerator(2048).generate();
        JwtAuthenticator authenticator = new JwtAuthenticator(
                new JWKSet(rsaKey.toPublicJWK()), ISSUER, AUDIENCE);

        String token = sign(JWSAlgorithm.RS256, rsaKey, new RSASSASigner(rsaKey), "user-123", "other-audience");

        assertThatThrownBy(() -> authenticator.authenticate(token))
                .isInstanceOf(AccessDeniedException.class);
    }

    /** Signs a token with {@code signer}, naming {@code key} in the {@code kid} header. */
    private static String sign(JWSAlgorithm algorithm, JWK key, JWSSigner signer, String subject,
                               String audience) throws Exception {
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(algorithm).keyID(key.getKeyID()).build(),
                new JWTClaimsSet.Builder()
                        .subject(subject)
                        .issuer(ISSUER)
                        .audience(audience)
                        .expirationTime(Date.from(Instant.now().plusSeconds(300)))
                        .build());
        jwt.sign(signer);
        return jwt.serialize();
    }
}
