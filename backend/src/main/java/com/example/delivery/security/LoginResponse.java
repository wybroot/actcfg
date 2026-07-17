package com.example.delivery.security;

public record LoginResponse(String token, CurrentUser user) {
}
