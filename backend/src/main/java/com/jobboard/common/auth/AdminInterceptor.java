package com.jobboard.common.auth;

import com.jobboard.common.SessionKeys;
import com.jobboard.user.UserRepository;
import com.jobboard.user.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

public class AdminInterceptor implements HandlerInterceptor {

    private final UserRepository userRepository;

    public AdminInterceptor(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        Object userId = session == null ? null : session.getAttribute(SessionKeys.USER_ID);
        boolean admin = userId instanceof Long id
                && userRepository.findById(id)
                        .map(user -> user.getRole() == UserRole.ADMIN)
                        .orElse(false);
        if (!admin) {
            ErrorResponseWriter.write(response, HttpServletResponse.SC_FORBIDDEN, "관리자만 접근할 수 있습니다.");
            return false;
        }
        return true;
    }
}
