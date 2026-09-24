package com.chetana.keystone.platform.admin.role;

import com.chetana.keystone.platform.admin.identity.Scope;

import java.util.List;

public record RoleRequest(String code, Scope scope, List<String> permissions) {
}
