package com.jobboard.user;

import com.jobboard.common.SessionKeys;
import com.jobboard.user.dto.LoginRequest;
import com.jobboard.user.dto.RegisterRequest;
import com.jobboard.user.dto.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request.email(), request.password(), request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        User user = userService.authenticate(request.email(), request.password());
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(SessionKeys.USER_ID, user.getId());
        session.setAttribute(SessionKeys.USER_ROLE, user.getRole().name());
        session.setAttribute(SessionKeys.USER_NAME, user.getName());
        session.setAttribute(SessionKeys.USER_EMAIL, user.getEmail());
        return UserResponse.from(user);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UserResponse me(HttpServletRequest request) {
        HttpSession session = request.getSession();
        return new UserResponse(
                (Long) session.getAttribute(SessionKeys.USER_ID),
                (String) session.getAttribute(SessionKeys.USER_EMAIL),
                (String) session.getAttribute(SessionKeys.USER_NAME),
                (String) session.getAttribute(SessionKeys.USER_ROLE)
        );
    }
}
