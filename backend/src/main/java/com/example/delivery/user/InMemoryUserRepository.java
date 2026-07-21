package com.example.delivery.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 内存版 UserRepository，dev profile 下（无 DataSource）使用。
 * 预置四个测试账号，密码均为 Admin@123。
 */
@Component
public class InMemoryUserRepository implements UserRepository {

    private final Map<Long, UserEntity> users = new ConcurrentHashMap<>();
    private final Map<String, Long> usernameIndex = new ConcurrentHashMap<>();
    private final List<RoleEntity> roles;
    private final AtomicLong idSeq = new AtomicLong(10);

    public InMemoryUserRepository(PasswordEncoder passwordEncoder) {
        roles = List.of(
                new RoleEntity(1L, "SUPER_ADMIN",   "超级管理员", "ENABLED"),
                new RoleEntity(2L, "OPS",            "运维人员",   "ENABLED"),
                new RoleEntity(3L, "IMPL_ENGINEER",  "实施工程师", "ENABLED"),
                new RoleEntity(4L, "AUDITOR",        "审计人员",   "ENABLED")
        );

        String hash = passwordEncoder.encode("Admin@123");
        List<UserEntity> seed = List.of(
                new UserEntity(1L, "admin",   "超级管理员", hash, "ENABLED",
                        LocalDateTime.now(), null, List.of("SUPER_ADMIN")),
                new UserEntity(2L, "ops",     "运维人员",   hash, "ENABLED",
                        LocalDateTime.now(), null, List.of("OPS")),
                new UserEntity(3L, "impl",    "实施工程师", hash, "ENABLED",
                        LocalDateTime.now(), null, List.of("IMPL_ENGINEER")),
                new UserEntity(4L, "auditor", "审计人员",   hash, "ENABLED",
                        LocalDateTime.now(), null, List.of("AUDITOR"))
        );
        seed.forEach(u -> {
            users.put(u.id(), u);
            usernameIndex.put(u.username(), u.id());
        });
    }

    @Override
    public List<UserEntity> findAll() {
        return users.values().stream()
                .filter(u -> "ENABLED".equals(u.status()))
                .sorted(Comparator.comparing(UserEntity::id))
                .toList();
    }

    @Override
    public Optional<UserEntity> findById(Long id) {
        return Optional.ofNullable(users.get(id))
                .filter(u -> "ENABLED".equals(u.status()));
    }

    @Override
    public Optional<UserEntity> findByUsername(String username) {
        Long id = usernameIndex.get(username);
        if (id == null) return Optional.empty();
        return findById(id);
    }

    @Override
    public UserEntity save(UserEntity user) {
        if (user.id() == null) {
            long newId = idSeq.getAndIncrement();
            user = new UserEntity(newId, user.username(), user.displayName(),
                    user.passwordHash(), user.status(),
                    LocalDateTime.now(), null, user.roles());
        }
        users.put(user.id(), user);
        usernameIndex.put(user.username(), user.id());
        return user;
    }

    @Override
    public void deleteById(Long id) {
        UserEntity u = users.get(id);
        if (u != null) {
            // 软删除：标记为 DISABLED
            users.put(id, new UserEntity(u.id(), u.username(), u.displayName(),
                    u.passwordHash(), "DISABLED",
                    u.createdAt(), LocalDateTime.now(), u.roles()));
        }
    }

    @Override
    public void updateRoles(Long userId, List<Long> roleIds) {
        UserEntity u = users.get(userId);
        if (u == null) return;
        List<String> newRoles = roles.stream()
                .filter(r -> roleIds.contains(r.id()))
                .map(RoleEntity::roleCode)
                .toList();
        users.put(userId, new UserEntity(u.id(), u.username(), u.displayName(),
                u.passwordHash(), u.status(),
                u.createdAt(), LocalDateTime.now(), newRoles));
    }

    @Override
    public List<RoleEntity> findAllRoles() {
        return roles;
    }
}
