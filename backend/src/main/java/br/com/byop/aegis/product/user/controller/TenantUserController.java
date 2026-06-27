package br.com.byop.aegis.product.user.controller;

import br.com.byop.aegis.product.user.contract.InviteTenantUserRequest;
import br.com.byop.aegis.product.user.contract.UpdateTenantUserRequest;
import br.com.byop.aegis.product.user.dto.TenantUserSummary;
import br.com.byop.aegis.product.user.service.TenantUserService;
import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.security.AuthenticatedUserProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class TenantUserController {

    private final TenantUserService userService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public TenantUserController(TenantUserService userService, AuthenticatedUserProvider authenticatedUserProvider) {
        this.userService = userService;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @GetMapping("/api/v1/tenants/{tenantId}/users")
    public List<TenantUserSummary> listTenantUsers(@PathVariable("tenantId") UUID tenantId,
                                                   Authentication authentication) {
        return userService.listUsers(caller(authentication), tenantId);
    }

    @PostMapping("/api/v1/tenants/{tenantId}/users/invite")
    @ResponseStatus(HttpStatus.CREATED)
    public TenantUserSummary inviteTenantUser(@PathVariable("tenantId") UUID tenantId,
                                              @Valid @RequestBody InviteTenantUserRequest request,
                                              Authentication authentication) {
        return userService.inviteUser(caller(authentication), tenantId, request);
    }

    @GetMapping("/api/v1/tenants/{tenantId}/users/{userId}")
    public TenantUserSummary getTenantUser(@PathVariable("tenantId") UUID tenantId,
                                           @PathVariable("userId") String userId,
                                           Authentication authentication) {
        return userService.getUser(caller(authentication), tenantId, userId);
    }

    @PutMapping("/api/v1/tenants/{tenantId}/users/{userId}")
    public TenantUserSummary updateTenantUser(@PathVariable("tenantId") UUID tenantId,
                                              @PathVariable("userId") String userId,
                                              @Valid @RequestBody UpdateTenantUserRequest request,
                                              Authentication authentication) {
        return userService.updateUser(caller(authentication), tenantId, userId, request);
    }

    @PostMapping("/api/v1/tenants/{tenantId}/users/{userId}/resend-invite")
    public TenantUserSummary resendInvite(@PathVariable("tenantId") UUID tenantId,
                                          @PathVariable("userId") String userId,
                                          Authentication authentication) {
        return userService.resendInvite(caller(authentication), tenantId, userId);
    }

    @PostMapping("/api/v1/tenants/{tenantId}/users/{userId}/block")
    public TenantUserSummary blockTenantUser(@PathVariable("tenantId") UUID tenantId,
                                             @PathVariable("userId") String userId,
                                             Authentication authentication) {
        return userService.blockUser(caller(authentication), tenantId, userId);
    }

    @DeleteMapping("/api/v1/tenants/{tenantId}/users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeTenantUser(@PathVariable("tenantId") UUID tenantId,
                                 @PathVariable("userId") String userId,
                                 Authentication authentication) {
        userService.removeUser(caller(authentication), tenantId, userId);
    }

    @PostMapping("/api/v1/tenants/{tenantId}/users/{userId}/restore")
    public TenantUserSummary restoreTenantUser(@PathVariable("tenantId") UUID tenantId,
                                               @PathVariable("userId") String userId,
                                               Authentication authentication) {
        return userService.restoreUser(caller(authentication), tenantId, userId);
    }

    private AuthenticatedUser caller(Authentication authentication) {
        return authenticatedUserProvider.from(authentication);
    }
}
