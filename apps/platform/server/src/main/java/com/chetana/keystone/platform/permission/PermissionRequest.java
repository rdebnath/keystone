package com.chetana.keystone.platform.permission;

import com.chetana.keystone.platform.identity.Scope;

public record PermissionRequest(String code, Scope scope) {
}
