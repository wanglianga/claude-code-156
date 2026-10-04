package com.county.coldchain.security;

import com.county.coldchain.domain.User;
import com.county.coldchain.repo.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 极简令牌鉴权：登录签发随机 token，内存持有会话 */
@Service
public class AuthService {

    public record Session(String token, User user) {}

    private final UserRepo userRepo;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final Map<String, User> sessions = new ConcurrentHashMap<>();

    public AuthService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    public String hash(String raw) {
        return encoder.encode(raw);
    }

    public Session login(String username, String password) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));
        if (!encoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        sessions.put(token, user);
        return new Session(token, user);
    }

    public Optional<User> resolve(String token) {
        return token == null ? Optional.empty() : Optional.ofNullable(sessions.get(token));
    }
}
