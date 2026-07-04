package br.com.byop.aegis.identity.user.service;

import br.com.byop.aegis.identity.auth.client.KeycloakAdminClient;
import br.com.byop.aegis.identity.user.dto.UserResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class UserService {

    private final KeycloakAdminClient keycloakAdminClient;

    public UserService(KeycloakAdminClient keycloakAdminClient) {
        this.keycloakAdminClient = keycloakAdminClient;
    }

    public List<UserResponse> findUsers() {
        log.debug("findUsers");
        return keycloakAdminClient.findUsers();
    }

    public UserResponse findUserById(String id) {
        log.debug("findUserById: id='{}'", id);
        return keycloakAdminClient.findUserById(id);
    }
}
