package com.example.delivery.user;

import com.example.delivery.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('SUPER_ADMIN')")   // 用户管理仅超管可操作
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<List<UserVO>> listUsers() {
        return ApiResponse.ok(userService.listUsers().stream()
                .map(UserVO::from).toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<UserVO> getUser(@PathVariable Long id) {
        return ApiResponse.ok(UserVO.from(userService.getUser(id)));
    }

    @PostMapping
    public ApiResponse<UserVO> createUser(@Valid @RequestBody CreateUserRequest req) {
        UserEntity user = userService.createUser(req.username(), req.displayName(), req.password());
        return ApiResponse.ok(UserVO.from(user));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserVO> updateUser(@PathVariable Long id,
                                          @Valid @RequestBody UpdateUserRequest req) {
        UserEntity updated = userService.updateDisplayName(id, req.displayName());
        return ApiResponse.ok(UserVO.from(updated));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/roles")
    public ApiResponse<UserVO> assignRoles(@PathVariable Long id,
                                           @Valid @RequestBody AssignRolesRequest req) {
        UserEntity updated = userService.assignRoles(id, req.roleIds());
        return ApiResponse.ok(UserVO.from(updated));
    }

    @PutMapping("/{id}/password/reset")
    public ApiResponse<Void> resetPassword(@PathVariable Long id,
                                            @Valid @RequestBody ResetPasswordRequest req) {
        userService.resetPassword(id, req.newPassword());
        return ApiResponse.ok();
    }

    // ---- 角色列表（运维及以上可见）----

    @GetMapping("/roles")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OPS')")
    public ApiResponse<List<RoleEntity>> listRoles() {
        return ApiResponse.ok(userService.listRoles());
    }

    // ---- DTO ----

    public record CreateUserRequest(
            @NotBlank String username,
            @NotBlank String displayName,
            @NotBlank @Size(min = 6) String password) {}

    public record UpdateUserRequest(@NotBlank String displayName) {}

    public record AssignRolesRequest(@NotEmpty List<Long> roleIds) {}

    public record ResetPasswordRequest(@NotBlank @Size(min = 6) String newPassword) {}
}
