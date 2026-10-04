package com.county.coldchain.web;

import com.county.coldchain.domain.User;
import com.county.coldchain.security.AuthService;
import com.county.coldchain.web.dto.Dtos;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Dtos.LoginReq req) {
        if (req == null || req.username() == null || req.password() == null) {
            throw new ApiException("请输入用户名和密码");
        }
        AuthService.Session s;
        try {
            s = authService.login(req.username(), req.password());
        } catch (IllegalArgumentException e) {
            throw new ApiException(e.getMessage());
        }
        return Map.of(
                "token", s.token(),
                "user", Map.of(
                        "id", s.user().getId(),
                        "username", s.user().getUsername(),
                        "name", s.user().getName(),
                        "role", s.user().getRole()));
    }

    @GetMapping("/me")
    public Map<String, Object> me(HttpServletRequest request) {
        User u = (User) request.getAttribute(AuthInterceptor.ATTR_USER);
        return Map.of("id", u.getId(), "username", u.getUsername(), "name", u.getName(), "role", u.getRole());
    }
}
