package br.com.byop.aegis.identity.user.service;

import br.com.byop.aegis.identity.auth.client.KeycloakAdminClient;
import br.com.byop.aegis.identity.user.dto.UserResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private final KeycloakAdminClient keycloakAdminClient = mock(KeycloakAdminClient.class);
    private final UserService service = new UserService(keycloakAdminClient);

    @Test
    void shouldFindUsers() {
        List<UserResponse> users = List.of(new UserResponse("user-id", "loki", "loki@teste.com", "loki", "de asgard", true));
        when(keycloakAdminClient.findUsers()).thenReturn(users);

        assertThat(service.findUsers()).isEqualTo(users);

        verify(keycloakAdminClient).findUsers();
        verifyNoMoreInteractions(keycloakAdminClient);
    }

    @Test
    void shouldFindUserById() {
        UserResponse user = new UserResponse("user-id", "loki", "loki@teste.com", "loki", "de asgard", true);
        when(keycloakAdminClient.findUserById("user-id")).thenReturn(user);

        assertThat(service.findUserById("user-id")).isEqualTo(user);

        verify(keycloakAdminClient).findUserById("user-id");
        verifyNoMoreInteractions(keycloakAdminClient);
    }
}
