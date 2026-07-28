package com.jobboard.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void 이메일로_회원을_조회한다() {
        User user = new User("test@jobboard.com", "encoded-pw", "홍길동", UserRole.USER);
        userRepository.save(user);

        assertThat(userRepository.findByEmail("test@jobboard.com")).isPresent();
        assertThat(userRepository.existsByEmail("test@jobboard.com")).isTrue();
        assertThat(userRepository.existsByEmail("none@jobboard.com")).isFalse();
    }
}
