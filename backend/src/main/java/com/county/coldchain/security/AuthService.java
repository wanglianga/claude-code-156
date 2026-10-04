package com.county.coldchain.security;

import com.county.coldchain.domain.entity.SysUser;
import com.county.coldchain.repo.SysUserRepository;
import com.county.coldchain.support.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 演示用轻量鉴权：登录后颁发随机 token，内存保存会话。
 * 密码以 SHA-256 摘要存储（种子数据同口径），重启后所有会话失效需重新登录。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserRepository userRepository;

    @Value("${app.auth.ttl-hours:12}")
    private long ttlHours;

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public record Session(String token, SysUser user, LocalDateTime expireAt) {}

    public Session login(String username, String password) {
        SysUser u = userRepository.findByUsername(username)
                .orElseThrow(() -> new BizException("用户名或密码错误"));
        if (!Boolean.TRUE.equals(u.getEnabled()) || !sha256(password).equals(u.getPassword())) {
            throw new BizException("用户名或密码错误");
        }
        String token = sha256(username + System.nanoTime() + Math.random()).substring(0, 32);
        Session s = new Session(token, u, LocalDateTime.now().plusHours(ttlHours));
        sessions.put(token, s);
        return s;
    }

    public void logout(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }

    public Optional<Session> resolve(String token) {
        if (token == null) {
            return Optional.empty();
        }
        Session s = sessions.get(token);
        if (s == null) {
            return Optional.empty();
        }
        if (s.expireAt().isBefore(LocalDateTime.now())) {
            sessions.remove(token);
            return Optional.empty();
        }
        return Optional.of(s);
    }

    public static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
