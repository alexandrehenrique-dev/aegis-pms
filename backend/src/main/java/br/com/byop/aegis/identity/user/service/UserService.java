package br.com.byop.aegis.identity.user.service;

import br.com.byop.aegis.identity.auth.client.KeycloakAdminClient;
import br.com.byop.aegis.identity.user.dto.UserResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final KeycloakAdminClient keycloakAdminClient;

    public UserService(KeycloakAdminClient keycloakAdminClient) {
        this.keycloakAdminClient = keycloakAdminClient;
    }

    public List<UserResponse> findUsers() {
        return keycloakAdminClient.findUsers();
    }

    public UserResponse findUserById(String id) {
        return keycloakAdminClient.findUserById(id);
    }
}
