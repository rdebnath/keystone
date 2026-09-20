package com.chetana.keystone.security;

import java.util.Map;

/**
 * An authenticated caller (the JWT {@code sub} plus the full claim set).
 */
public record Principal(String subject, Map<String, Object> claims) {
}
