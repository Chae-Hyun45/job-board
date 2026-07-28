package com.jobboard.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AdminSeederTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void 애플리케이션_기동시_초기_관리자가_생성된다() {
        assertThat(userRepository.findByEmail("admin@jobboard.local"))
                .isPresent()
                .get()
                .satisfies(admin -> assertThat(admin.getRole()).isEqualTo(UserRole.ADMIN));
    }
}
