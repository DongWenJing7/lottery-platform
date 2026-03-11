package com.lottery.interceptor;

import com.lottery.common.exception.BusinessException;
import com.lottery.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<>();
    public static final ThreadLocal<String> CURRENT_USER_ROLE = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader("Authorization");
        if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (!StringUtils.hasText(token) || !jwtUtil.isValid(token)) {
            throw BusinessException.unauthorized();
        }

        CURRENT_USER_ID.set(jwtUtil.getUserId(token));
        CURRENT_USER_ROLE.set(jwtUtil.getRole(token));

        // 路径角色校验
        String path = request.getRequestURI();
        String role = CURRENT_USER_ROLE.get();
        if (path.startsWith("/api/admin") && !"admin".equals(role)) {
            throw BusinessException.forbidden();
        }
        if (path.startsWith("/api/agent") && !"agent".equals(role) && !"admin".equals(role)) {
            throw BusinessException.forbidden();
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CURRENT_USER_ID.remove();
        CURRENT_USER_ROLE.remove();
    }
}
