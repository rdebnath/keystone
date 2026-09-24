package com.chetana.keystone.platform.admin.identity;

/**
 * Change-own-password request for the backend-proxied change-password flow.
 */
public record ChangePasswordRequest(String password) {
}
