package br.com.byop.aegis.identity.user.controller;

import br.com.byop.aegis.identity.user.dto.UserResponse;
import br.com.byop.aegis.identity.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@ImportAutoConfiguration(exclude = OAuth2ResourceServerWebSecurityAutoConfiguration.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void shouldFindUsers() throws Exception {
        when(userService.findUsers()).thenReturn(List.of(
                new UserResponse("user-id", "loki", "loki@teste.com", "loki", "de asgard", true)
        ));

        mockMvc.perform(get("/api/v1/users").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("user-id"));

        verify(userService).findUsers();
        verifyNoMoreInteractions(userService);
    }

    @Test
    void shouldFindUserById() throws Exception {
        when(userService.findUserById("user-id")).thenReturn(
                new UserResponse("user-id", "loki", "loki@teste.com", "loki", "de asgard", true)
        );

        mockMvc.perform(get("/api/v1/users/user-id").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user-id"));

        verify(userService).findUserById("user-id");
        verifyNoMoreInteractions(userService);
    }
}
