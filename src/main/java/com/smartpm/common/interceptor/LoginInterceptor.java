package com.smartpm.common.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartpm.common.result.R;
import com.smartpm.common.utils.JWTUtil;
import com.smartpm.common.utils.UserHolder;
import com.smartpm.entity.User;
import com.smartpm.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class LoginInterceptor implements HandlerInterceptor {

    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(401);
            response.getWriter().write(objectMapper.writeValueAsString(R.error(401, "未登录")));
            return false;
        }

        String token = authHeader.substring(7);
        if (!JWTUtil.validate(token)) {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(401);
            response.getWriter().write(objectMapper.writeValueAsString(R.error(401, "token无效或已过期")));
            return false;
        }

        Long userId = JWTUtil.getUserId(token);
        User user = userMapper.selectById(userId);
        if (user == null) {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(401);
            response.getWriter().write(objectMapper.writeValueAsString(R.error(401, "用户不存在")));
            return false;
        }

        UserHolder.set(user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserHolder.remove();
    }
}
