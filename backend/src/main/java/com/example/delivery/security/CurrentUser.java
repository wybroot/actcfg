package com.example.delivery.security;

import java.util.List;

public record CurrentUser(Long id, String username, String displayName, List<String> roles) {
}
