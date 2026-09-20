package com.chetana.keystone.platform.role;

import com.chetana.keystone.platform.identity.Scope;

import java.util.List;

public record RoleRequest(String code, Scope scope, List<String> permissions) {
}
