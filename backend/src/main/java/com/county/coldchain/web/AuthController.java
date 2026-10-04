package com.county.coldchain.web;

import com.county.coldchain.domain.entity.SysUser;
import com.county.coldchain.security.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "登录鉴权")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Data
    public static class LoginRequest {
        @NotBlank
        private String username;
        @NotBlank
        private String password;
    }

    @Operation(summary = "登录，返回 token")
    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody LoginRequest req) {
        AuthService.Session s = authService.login(req.getUsername(), req.getPassword());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("token", s.token());
        m.put("user", userView(s.user()));
        return m;
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Map<String, Object> logout(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            authService.logout(token.substring(7));
        }
        return Map.of("success", true);
    }

    @Operation(summary = "当前登录用户")
    @GetMapping("/me")
    public Map<String, Object> me(HttpServletRequest request) {
        return userView((SysUser) request.getAttribute("currentUser"));
    }

    private Map<String, Object> userView(SysUser u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("username", u.getUsername());
        m.put("displayName", u.getDisplayName());
        m.put("role", u.getRole().name());
        m.put("roleDesc", u.getRole().desc);
        return m;
    }
}
