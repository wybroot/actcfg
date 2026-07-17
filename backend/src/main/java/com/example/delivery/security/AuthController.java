package com.example.delivery.security;

import com.example.delivery.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        CurrentUser user = new CurrentUser(1L, request.username(), "系统管理员", List.of("SUPER_ADMIN"));
        return ApiResponse.ok(new LoginResponse("dev-token", user));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.ok();
    }

    @GetMapping("/profile")
    public ApiResponse<CurrentUser> profile() {
        return ApiResponse.ok(new CurrentUser(1L, "admin", "系统管理员", List.of("SUPER_ADMIN")));
    }
}
