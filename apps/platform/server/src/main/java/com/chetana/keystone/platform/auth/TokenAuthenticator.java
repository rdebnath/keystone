package com.chetana.keystone.platform.auth;

import com.chetana.keystone.security.Principal;

/**
 * Port for validating a bearer access token and resolving the authenticated {@link Principal}.
 *
 * <p>The production implementation delegates to the platform's OIDC resource-server JWT
 * validator; tests override this port to avoid a live IdP.
 */
@FunctionalInterface
public interface TokenAuthenticator {

    Principal authenticate(String token);
}
