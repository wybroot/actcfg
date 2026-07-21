package com.example.delivery.user;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    List<UserEntity> findAll();
    Optional<UserEntity> findById(Long id);
    Optional<UserEntity> findByUsername(String username);
    UserEntity save(UserEntity user);
    void deleteById(Long id);
    void updateRoles(Long userId, List<Long> roleIds);
    List<RoleEntity> findAllRoles();
}
