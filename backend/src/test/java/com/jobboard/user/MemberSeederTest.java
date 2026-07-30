package com.jobboard.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MemberSeederTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void 애플리케이션_기동시_초기_일반회원이_생성된다() {
        assertThat(userRepository.findByEmail("member@jobboard.local"))
                .isPresent()
                .get()
                .satisfies(member -> assertThat(member.getRole()).isEqualTo(UserRole.USER));
    }
}
