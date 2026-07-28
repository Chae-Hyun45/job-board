package com.jobboard.common.auth;

import com.jobboard.common.SessionKeys;
import com.jobboard.user.User;
import com.jobboard.user.UserRepository;
import com.jobboard.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminInterceptorTest {

    private UserRepository userRepository;
    private AdminInterceptor interceptor;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        interceptor = new AdminInterceptor(userRepository);
    }

    private MockHttpServletRequest requestWithSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKeys.USER_ID, 1L);
        session.setAttribute(SessionKeys.USER_ROLE, "ADMIN");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        return request;
    }

    @Test
    void DB의_role이_ADMIN이면_통과한다() throws Exception {
        User user = new User("admin@jobboard.com", "pw", "관리자", UserRole.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(requestWithSession(), response, new Object())).isTrue();
    }

    @Test
    void 세션에_ADMIN이_남아있어도_DB의_role이_USER면_403이다() throws Exception {
        User user = new User("member@jobboard.com", "pw", "회원", UserRole.USER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(requestWithSession(), response, new Object())).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void 사용자를_찾을_수_없으면_403이다() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(requestWithSession(), response, new Object())).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
    }
}
