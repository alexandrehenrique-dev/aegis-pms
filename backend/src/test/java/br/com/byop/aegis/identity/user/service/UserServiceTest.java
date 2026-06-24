package br.com.byop.aegis.identity.user.service;

import br.com.byop.aegis.identity.auth.client.KeycloakAdminClient;
import br.com.byop.aegis.identity.user.dto.UserResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private final KeycloakAdminClient keycloakAdminClient = mock(KeycloakAdminClient.class);
    private final UserService service = new UserService(keycloakAdminClient);

    @Test
    void shouldFindUsers() {
        List<UserResponse> users = List.of(
                new UserResponse("user-id", "loki", "loki@teste.com", "loki", "de asgard", true)
        );

        when(keycloakAdminClient.findUsers()).thenReturn(users);

        List<UserResponse> response = service.findUsers();

        assertEquals(users, response);
        verify(keycloakAdminClient).findUsers();
        verifyNoMoreInteractions(keycloakAdminClient);
    }

    @Test
    void shouldFindUserById() {
        UserResponse user = new UserResponse(
                "user-id",
                "loki",
                "loki@teste.com",
                "loki",
                "de asgard",
                true
        );

        when(keycloakAdminClient.findUserById("user-id")).thenReturn(user);

        UserResponse response = service.findUserById("user-id");

        assertEquals(user, response);
        verify(keycloakAdminClient).findUserById("user-id");
        verifyNoMoreInteractions(keycloakAdminClient);
    }
}