package com.chetana.keystone.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.chetana.keystone.common.error.AccessDeniedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Validates an OIDC access token (a signed JWT) against a JWKS, issuer, and audience, and
 * returns the authenticated {@link Principal}. Failure raises {@link AccessDeniedException}.
 *
 * <p>The signing key is the JWKS key whose {@code kid} matches the token's {@code kid}; a token
 * without a {@code kid} — or with one this key set does not know yet — is tried against every
 * key, so validation survives key rotation. Asymmetric keys are supported: RSA
 * ({@code RS256}/{@code RS384}/{@code RS512}) and elliptic curve
 * ({@code ES256}/{@code ES384}/{@code ES512}), which covers Supabase Auth's P-256 signing keys
 * and other OIDC providers (Auth0, Keycloak, Entra). The verifier is chosen from the key type
 * and the token's {@code alg} must match it, so no token can be verified by the wrong kind of
 * key. Symmetric (HMAC/shared-secret) tokens are rejected — the provider must sign with an
 * asymmetric key.
 */
public final class JwtAuthenticator {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticator.class);

    private static final Set<JWSAlgorithm> RSA_ALGORITHMS =
            Set.of(JWSAlgorithm.RS256, JWSAlgorithm.RS384, JWSAlgorithm.RS512);
    private static final Set<JWSAlgorithm> EC_ALGORITHMS =
            Set.of(JWSAlgorithm.ES256, JWSAlgorithm.ES384, JWSAlgorithm.ES512);

    private final JWKSet jwkSet;
    private final String issuer;
    private final String audience;

    public JwtAuthenticator(JWKSet jwkSet, String issuer, String audience) {
        this.jwkSet = jwkSet;
        this.issuer = issuer;
        this.audience = audience;
    }

    public Principal authenticate(String token) {
        SignedJWT jwt = parse(token);
        verifySignature(jwt);
        JWTClaimsSet claims = claims(jwt);
        validateClaims(claims);
        return new Principal(claims.getSubject(), toClaims(claims));
    }

    private static SignedJWT parse(String token) {
        try {
            return SignedJWT.parse(token);
        } catch (ParseException e) {
            throw new AccessDeniedException("Malformed access token");
        }
    }

    /**
     * Verifies the signature with the JWKS key selected by the token's {@code kid} — the token's
     * {@code alg} must be one the key type can verify.
     */
    private void verifySignature(SignedJWT jwt) {
        JWSHeader header = jwt.getHeader();
        boolean verified = candidateKeys(header.getKeyID()).stream()
                .map(key -> verifier(key, header.getAlgorithm()))
                .filter(Objects::nonNull)
                .anyMatch(verifier -> verifies(jwt, verifier));
        if (!verified) {
            log.warn("Rejected access token: no JWKS key verifies algorithm {}", header.getAlgorithm());
            throw new AccessDeniedException("Invalid access token signature");
        }
    }

    /** The keys to try: the one the token names, or every key when the token names none/unknown. */
    private List<JWK> candidateKeys(String keyId) {
        JWK named = keyId == null ? null : jwkSet.getKeyByKeyId(keyId);
        return named == null ? jwkSet.getKeys() : List.of(named);
    }

    /** A verifier built from {@code key} when it can verify {@code algorithm}, else {@code null}. */
    private static JWSVerifier verifier(JWK key, JWSAlgorithm algorithm) {
        if (algorithm == null) {
            return null;
        }
        try {
            return switch (key) {
                case RSAKey rsa when RSA_ALGORITHMS.contains(algorithm) ->
                        new RSASSAVerifier(rsa.toRSAPublicKey());
                case ECKey ec when EC_ALGORITHMS.contains(algorithm) -> new ECDSAVerifier(ec);
                default -> null;
            };
        } catch (JOSEException e) {
            throw new AccessDeniedException("Access token verification failed");
        }
    }

    private static boolean verifies(SignedJWT jwt, JWSVerifier verifier) {
        try {
            return jwt.verify(verifier);
        } catch (JOSEException e) {
            return false;
        }
    }

    private static JWTClaimsSet claims(SignedJWT jwt) {
        try {
            return jwt.getJWTClaimsSet();
        } catch (ParseException e) {
            throw new AccessDeniedException("Invalid access token claims");
        }
    }

    private void validateClaims(JWTClaimsSet claims) {
        if (issuer != null && !issuer.equals(claims.getIssuer())) {
            throw new AccessDeniedException("Invalid access token issuer");
        }
        if (audience != null) {
            List<String> audiences = claims.getAudience();
            if (audiences == null || !audiences.contains(audience)) {
                throw new AccessDeniedException("Invalid access token audience");
            }
        }
        Date expiry = claims.getExpirationTime();
        if (expiry != null && !expiry.after(new Date())) {
            throw new AccessDeniedException("Access token expired");
        }
    }

    /** Maps the validated claim set into the typed {@link Claims} Keystone models. */
    private static Claims toClaims(JWTClaimsSet claims) {
        return new Claims(
                stringClaim(claims, "email"),
                stringClaim(claims, "role"),
                toInstant(claims.getIssueTime()),
                toInstant(claims.getExpirationTime()));
    }

    /** An optional string claim, or {@code null} when the provider did not issue it. */
    private static String stringClaim(JWTClaimsSet claims, String name) {
        return claims.getClaim(name) instanceof String value ? value : null;
    }

    private static Instant toInstant(Date date) {
        return date == null ? null : date.toInstant();
    }
}
