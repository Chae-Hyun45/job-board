package com.jobboard.user;

import com.jobboard.common.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    void 중복되지_않은_이메일로_회원가입에_성공한다() {
        when(userRepository.existsByEmail("new@jobboard.com")).thenReturn(false);
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User user = userService.register("new@jobboard.com", "password123", "홍길동");

        assertThat(user.getEmail()).isEqualTo("new@jobboard.com");
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        assertThat(passwordEncoder.matches("password123", user.getPassword())).isTrue();
    }

    @Test
    void 이미_가입된_이메일이면_예외를_던진다() {
        when(userRepository.existsByEmail("dup@jobboard.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register("dup@jobboard.com", "password123", "홍길동"))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void 올바른_비밀번호로_인증에_성공한다() {
        String encoded = passwordEncoder.encode("password123");
        User existing = new User("login@jobboard.com", encoded, "홍길동", UserRole.USER);
        when(userRepository.findByEmail("login@jobboard.com")).thenReturn(Optional.of(existing));

        User authenticated = userService.authenticate("login@jobboard.com", "password123");

        assertThat(authenticated.getEmail()).isEqualTo("login@jobboard.com");
    }

    @Test
    void 비밀번호가_틀리면_예외를_던진다() {
        String encoded = passwordEncoder.encode("password123");
        User existing = new User("login@jobboard.com", encoded, "홍길동", UserRole.USER);
        when(userRepository.findByEmail("login@jobboard.com")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> userService.authenticate("login@jobboard.com", "wrong-password"))
                .isInstanceOf(ApiException.class);
    }
}
