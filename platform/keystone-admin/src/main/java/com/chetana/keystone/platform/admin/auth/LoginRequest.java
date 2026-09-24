package com.chetana.keystone.platform.admin.auth;

/**
 * Backend-proxied login request. {@code identifier} is the full username in
 * {@code username@tenantid} form (e.g. {@code admin@keystone} or {@code alice@acme}).
 */
public record LoginRequest(String identifier, String password) {
}
