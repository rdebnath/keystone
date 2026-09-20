package com.chetana.keystone.platform.user;

import java.util.List;

public record UserRequest(String email, String temporaryPassword, List<String> roles) {
}
