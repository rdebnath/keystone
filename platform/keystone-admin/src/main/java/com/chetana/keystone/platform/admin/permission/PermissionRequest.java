package com.chetana.keystone.platform.admin.permission;

import com.chetana.keystone.platform.admin.identity.Scope;

public record PermissionRequest(String code, Scope scope) {
}
