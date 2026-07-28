package com.jobboard.user;

import com.jobboard.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(String email, String rawPassword, String name) {
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 가입된 이메일입니다.");
        }
        User user = new User(email, passwordEncoder.encode(rawPassword), name, UserRole.USER);
        return userRepository.save(user);
    }

    public User authenticate(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."));
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        return user;
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User updateRole(Long id, UserRole role, Long requesterId) {
        if (id.equals(requesterId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "본인의 권한은 변경할 수 없습니다.");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."));
        if (user.getRole() == UserRole.ADMIN && role == UserRole.USER
                && userRepository.countByRole(UserRole.ADMIN) <= 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "마지막 관리자의 권한은 회수할 수 없습니다.");
        }
        user.setRole(role);
        return userRepository.save(user);
    }
}
