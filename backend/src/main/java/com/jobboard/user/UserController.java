package com.jobboard.user;

import com.jobboard.common.SessionKeys;
import com.jobboard.user.dto.RoleUpdateRequest;
import com.jobboard.user.dto.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> list() {
        return userService.findAll().stream().map(UserResponse::from).toList();
    }

    @PatchMapping("/{id}/role")
    public UserResponse updateRole(@PathVariable Long id, @Valid @RequestBody RoleUpdateRequest request,
                                  HttpServletRequest httpRequest) {
        Long requesterId = (Long) httpRequest.getSession().getAttribute(SessionKeys.USER_ID);
        User user = userService.updateRole(id, UserRole.valueOf(request.role()), requesterId);
        return UserResponse.from(user);
    }
}
