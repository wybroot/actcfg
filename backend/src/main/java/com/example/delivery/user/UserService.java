package com.example.delivery.user;

import com.example.delivery.common.api.ErrorCode;
import com.example.delivery.common.exception.BusinessException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(ObjectProvider<JdbcUserRepository> jdbcProvider,
                       InMemoryUserRepository inMemoryRepo,
                       PasswordEncoder passwordEncoder) {
        UserRepository jdbc = jdbcProvider.getIfAvailable();
        this.userRepository = (jdbc != null) ? jdbc : inMemoryRepo;
        this.passwordEncoder = passwordEncoder;
    }

    // ---- 查询 ----

    public List<UserEntity> listUsers() {
        return userRepository.findAll();
    }

    public UserEntity getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
    }

    public Optional<UserEntity> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public List<RoleEntity> listRoles() {
        return userRepository.findAllRoles();
    }

    // ---- 新增 ----

    public UserEntity createUser(String username, String displayName, String rawPassword) {
        userRepository.findByUsername(username).ifPresent(u -> {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "用户名已存在");
        });
        String hash = passwordEncoder.encode(rawPassword);
        UserEntity user = UserEntity.ofNew(username, displayName, hash);
        return userRepository.save(user);
    }

    // ---- 修改昵称 ----

    public UserEntity updateDisplayName(Long id, String displayName) {
        UserEntity user = getUser(id);
        UserEntity updated = new UserEntity(user.id(), user.username(), displayName,
                user.passwordHash(), user.status(),
                user.createdAt(), user.updatedAt(), user.roles());
        return userRepository.save(updated);
    }

    // ---- 修改密码 ----

    public void changePassword(Long id, String oldPassword, String newPassword) {
        UserEntity user = getUser(id);
        if (!passwordEncoder.matches(oldPassword, user.passwordHash())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "原密码错误");
        }
        String newHash = passwordEncoder.encode(newPassword);
        UserEntity updated = new UserEntity(user.id(), user.username(), user.displayName(),
                newHash, user.status(),
                user.createdAt(), user.updatedAt(), user.roles());
        userRepository.save(updated);
    }

    /** 管理员重置密码（不需要旧密码） */
    public void resetPassword(Long id, String newPassword) {
        UserEntity user = getUser(id);
        String newHash = passwordEncoder.encode(newPassword);
        UserEntity updated = new UserEntity(user.id(), user.username(), user.displayName(),
                newHash, user.status(),
                user.createdAt(), user.updatedAt(), user.roles());
        userRepository.save(updated);
    }

    // ---- 删除 ----

    public void deleteUser(Long id) {
        getUser(id); // 不存在则抛 NOT_FOUND
        userRepository.deleteById(id);
    }

    // ---- 角色分配 ----

    public UserEntity assignRoles(Long userId, List<Long> roleIds) {
        getUser(userId);
        userRepository.updateRoles(userId, roleIds);
        return getUser(userId);
    }

    // ---- 供 AuthController / UserDetailsService 使用 ----

    public boolean verifyPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
