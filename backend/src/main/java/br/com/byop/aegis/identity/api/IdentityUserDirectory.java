package br.com.byop.aegis.identity.api;

import br.com.byop.aegis.identity.user.dto.UserResponse;
import br.com.byop.aegis.identity.user.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class IdentityUserDirectory {

    private final UserService userService;

    public IdentityUserDirectory(UserService userService) {
        this.userService = userService;
    }

    public IdentityUser getRequiredUser(String userId) {
        UserResponse user = userService.findUserById(userId);
        return new IdentityUser(
                user.id(),
                user.username(),
                user.email(),
                user.firstName(),
                user.lastName()
        );
    }
}
