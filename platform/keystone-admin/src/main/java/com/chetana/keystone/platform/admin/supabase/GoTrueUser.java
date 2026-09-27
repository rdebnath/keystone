package com.chetana.keystone.platform.admin.supabase;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A GoTrue user as returned by the Auth admin API — {@code id} is the Auth {@code sub} Keystone
 * provisions the identity with. Unknown fields are ignored so a field the provider adds later does
 * not break provisioning (see docs/CODING_GUIDELINES_BACKEND.md §13).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record GoTrueUser(String id, String email) {
}
