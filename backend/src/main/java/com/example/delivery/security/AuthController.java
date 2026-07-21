package com.example.delivery.security;

import com.example.delivery.common.api.ApiResponse;
import com.example.delivery.common.api.ErrorCode;
import com.example.delivery.common.exception.BusinessException;
import com.example.delivery.user.UserEntity;
import com.example.delivery.user.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        UserEntity user = userService.findByUsername(request.username())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误"));

        if (!"ENABLED".equals(user.status())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已被禁用");
        }
        if (!userService.verifyPassword(request.password(), user.passwordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }

        String token = jwtUtil.generate(user.id(), user.username(), user.displayName(), user.roles());
        CurrentUser currentUser = new CurrentUser(user.id(), user.username(), user.displayName(), user.roles());
        return ApiResponse.ok(new LoginResponse(token, currentUser));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        // JWT 无状态，客户端清除 token 即可
        return ApiResponse.ok();
    }

    @GetMapping("/profile")
    public ApiResponse<CurrentUser> profile(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.ok(currentUser);
    }

    @PutMapping("/profile")
    public ApiResponse<CurrentUser> updateProfile(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserEntity updated = userService.updateDisplayName(currentUser.id(), request.displayName());
        return ApiResponse.ok(new CurrentUser(updated.id(), updated.username(),
                updated.displayName(), updated.roles()));
    }

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(currentUser.id(), request.oldPassword(), request.newPassword());
        return ApiResponse.ok();
    }

    // ---- 内部 DTO ----

    public record UpdateProfileRequest(@NotBlank String displayName) {}

    public record ChangePasswordRequest(
            @NotBlank String oldPassword,
            @NotBlank @Size(min = 6) String newPassword) {}
}

