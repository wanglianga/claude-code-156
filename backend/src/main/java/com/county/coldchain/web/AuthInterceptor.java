package com.county.coldchain.web;

import com.county.coldchain.domain.User;
import com.county.coldchain.security.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** 除登录接口外，/api/** 均需 X-Auth-Token */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String HEADER = "X-Auth-Token";
    public static final String ATTR_USER = "currentUser";

    private final AuthService authService;

    public AuthInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        User user = authService.resolve(request.getHeader(HEADER)).orElse(null);
        if (user == null) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"未登录或会话已失效\"}");
            return false;
        }
        request.setAttribute(ATTR_USER, user);
        return true;
    }
}
