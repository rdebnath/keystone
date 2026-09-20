package com.chetana.keystone.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.chetana.keystone.common.error.AccessDeniedException;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

/**
 * Validates an OIDC access token (a signed JWT) against a JWKS, issuer, and audience, and
 * returns the authenticated {@link Principal}. Failure raises {@link AccessDeniedException}.
 */
public final class JwtAuthenticator {

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
        return new Principal(claims.getSubject(), claims.getClaims());
    }

    private static SignedJWT parse(String token) {
        try {
            return SignedJWT.parse(token);
        } catch (ParseException e) {
            throw new AccessDeniedException("Malformed access token");
        }
    }

    private void verifySignature(SignedJWT jwt) {
        try {
            for (JWK key : jwkSet.getKeys()) {
                if (key instanceof RSAKey rsa && jwt.verify(new RSASSAVerifier(rsa.toRSAPublicKey()))) {
                    return;
                }
            }
        } catch (JOSEException e) {
            throw new AccessDeniedException("Access token verification failed");
        }
        throw new AccessDeniedException("Invalid access token signature");
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
}
