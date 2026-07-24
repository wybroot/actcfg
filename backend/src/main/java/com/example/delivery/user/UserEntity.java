package com.example.delivery.user;

import java.time.LocalDateTime;
import java.util.List;

public record UserEntity(
        Long id,
        String username,
        String displayName,
        String passwordHash,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<String> roles
) {
    /** 构造新用户时使用（无 id、时间由数据库生成） */
    public static UserEntity ofNew(String username, String displayName, String passwordHash) {
        return new UserEntity(null, username, displayName, passwordHash,
                "ENABLED", null, null, List.of());
    }
}
