package com.chetana.keystone.platform.admin.tenant;

/**
 * Tenant create/update request. {@code country} is optional — an ISO 3166-1 alpha-2 code
 * ({@code IN}, {@code DE}), case-insensitive on the wire; blank or absent clears it, because the
 * update takes the same full body as create (as it does for {@code name} and {@code slug}).
 */
public record TenantRequest(String name, String slug, String country) {
}
