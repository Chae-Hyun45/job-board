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
    void 본인의_권한을_변경하려_하면_예외를_던진다() {
        assertThatThrownBy(() -> userService.updateRole(1L, UserRole.USER, 1L))
                .isInstanceOf(ApiException.class)
                .hasMessage("본인의 권한은 변경할 수 없습니다.");
    }

    @Test
    void 마지막_관리자의_권한을_회수하려_하면_예외를_던진다() {
        User lastAdmin = new User("admin@jobboard.com", "pw", "관리자", UserRole.ADMIN);
        when(userRepository.findById(2L)).thenReturn(Optional.of(lastAdmin));
        when(userRepository.countByRole(UserRole.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> userService.updateRole(2L, UserRole.USER, 1L))
                .isInstanceOf(ApiException.class)
                .hasMessage("마지막 관리자의 권한은 회수할 수 없습니다.");
    }

    @Test
    void 관리자가_둘_이상이면_권한을_회수할_수_있다() {
        User admin = new User("admin2@jobboard.com", "pw", "관리자2", UserRole.ADMIN);
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(userRepository.countByRole(UserRole.ADMIN)).thenReturn(2L);
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User updated = userService.updateRole(2L, UserRole.USER, 1L);

        assertThat(updated.getRole()).isEqualTo(UserRole.USER);
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
