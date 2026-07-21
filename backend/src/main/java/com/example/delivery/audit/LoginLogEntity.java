package com.example.delivery.audit;

import java.time.LocalDateTime;

public record LoginLogEntity(
        Long id,
        String username,
        String loginResult,
        String ipAddress,
        LocalDateTime createdAt
) {}
