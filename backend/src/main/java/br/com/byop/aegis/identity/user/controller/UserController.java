package br.com.byop.aegis.identity.user.controller;

import br.com.byop.aegis.identity.user.dto.UserResponse;
import br.com.byop.aegis.identity.user.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/api/v1/users")
    public List<UserResponse> findUsers() {
        return userService.findUsers();
    }

    @GetMapping("/api/v1/users/{id}")
    public UserResponse findUserById(@PathVariable("id") String id) {
        return userService.findUserById(id);
    }
}
