package br.com.byop.aegis.core.tenant;

import br.com.byop.aegis.core.tenant.dto.TenantSummary;
import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.security.AuthenticatedUserProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TenantController {

    private final TenantService tenantService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public TenantController(TenantService tenantService, AuthenticatedUserProvider authenticatedUserProvider) {
        this.tenantService = tenantService;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @GetMapping("/api/v1/tenants")
    public List<TenantSummary> listTenants(Authentication authentication) {
        AuthenticatedUser caller = authenticatedUserProvider.from(authentication);
        return tenantService.listTenants(caller);
    }

    @PostMapping("/api/v1/tenants")
    @ResponseStatus(HttpStatus.CREATED)
    public TenantSummary createTenant(@Valid @RequestBody CreateTenantRequest request, Authentication authentication) {
        AuthenticatedUser caller = authenticatedUserProvider.from(authentication);
        return tenantService.createTenant(caller, request.toCommand());
    }
}
