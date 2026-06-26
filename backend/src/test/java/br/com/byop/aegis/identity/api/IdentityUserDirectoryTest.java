package br.com.byop.aegis.identity.api;

import br.com.byop.aegis.identity.user.dto.UserResponse;
import br.com.byop.aegis.identity.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentityUserDirectoryTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private IdentityUserDirectory directory;

    @Test
    void shouldMapUserServiceResponseToIdentityUser() {
        when(userService.findUserById("user-1"))
                .thenReturn(new UserResponse("user-1", "editor", "editor@byop.dev", "Editor", "User", true));

        IdentityUser user = directory.getRequiredUser("user-1");

        assertThat(user.id()).isEqualTo("user-1");
        assertThat(user.username()).isEqualTo("editor");
        assertThat(user.email()).isEqualTo("editor@byop.dev");
        assertThat(user.firstName()).isEqualTo("Editor");
        assertThat(user.lastName()).isEqualTo("User");
        assertThat(user.displayName()).isEqualTo("Editor User");
    }

    @Test
    void shouldUseUsernameWhenDisplayNameIsBlank() {
        IdentityUser user = new IdentityUser("user-1", "editor", "editor@byop.dev", null, " ");

        assertThat(user.displayName()).isEqualTo("editor");
    }
}
