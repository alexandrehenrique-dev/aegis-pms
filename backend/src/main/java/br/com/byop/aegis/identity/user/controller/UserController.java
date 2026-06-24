package br.com.byop.aegis.identity.user.controller;

import br.com.byop.aegis.identity.user.dto.UserResponse;
import br.com.byop.aegis.identity.user.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> findUsers() {
        return userService.findUsers();
    }

    @GetMapping("/{id}")
    public UserResponse findUserById(@PathVariable("id") String id) {
        return userService.findUserById(id);
    }
}
