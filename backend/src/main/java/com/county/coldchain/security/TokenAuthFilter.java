package com.county.coldchain.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Token 鉴权过滤器：放行登录、Swagger 与静态资源路径。 */
@Component
@RequiredArgsConstructor
public class TokenAuthFilter extends OncePerRequestFilter {

    private final AuthService authService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Set<String> PREFIX_WHITELIST = Set.of(
            "/api/auth/login",
            "/swagger-ui", "/v3/api-docs", "/swagger-resources",
            "/webjars", "/favicon.ico", "/"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || isWhitelisted(path)) {
            chain.doFilter(request, response);
            return;
        }
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        var session = authService.resolve(token);
        if (session.isEmpty()) {
            writeUnauthorized(response, request.getRequestURI());
            return;
        }
        request.setAttribute("currentUser", session.get().user());
        chain.doFilter(request, response);
    }

    private boolean isWhitelisted(String path) {
        for (String p : PREFIX_WHITELIST) {
            if (path.equals(p) || path.startsWith(p + "/") || (p.equals("/") && path.equals("/index.html"))) {
                return true;
            }
        }
        return false;
    }

    private void writeUnauthorized(HttpServletResponse response, String path) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", 401);
        body.put("error", "Unauthorized");
        body.put("message", "未登录或登录已过期");
        body.put("path", path);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
