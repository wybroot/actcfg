package com.example.delivery.user;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户信息对外视图对象，不含密码哈希。
 */
public record UserVO(
        Long id,
        String username,
        String displayName,
        String status,
        List<String> roles,
        LocalDateTime createdAt
) {
    public static UserVO from(UserEntity u) {
        return new UserVO(u.id(), u.username(), u.displayName(),
                u.status(), u.roles(), u.createdAt());
    }
}
