package br.com.byop.aegis.core.tenant;

import br.com.byop.aegis.core.tenant.command.CreateTenantCommand;
import br.com.byop.aegis.core.tenant.dto.TenantSummary;
import br.com.byop.aegis.core.tenant.exception.TenantAlreadyExistsException;
import br.com.byop.aegis.core.tenant.exception.TenantNotFoundException;
import br.com.byop.aegis.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TenantService {

    private static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    private static final String TENANT_ADMIN = "TENANT_ADMIN";

    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository membershipRepository;
    private final TenantMapper tenantMapper;

    public TenantService(TenantRepository tenantRepository, TenantMembershipRepository membershipRepository,
                         TenantMapper tenantMapper) {
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
        this.tenantMapper = tenantMapper;
    }

    @Transactional
    public TenantSummary createTenant(AuthenticatedUser caller, CreateTenantCommand command) {
        if (tenantRepository.existsByKey(command.key())) {
            throw new TenantAlreadyExistsException(command.key());
        }

        Tenant tenant = tenantRepository.save(new Tenant(command.key(), command.name()));
        membershipRepository.save(new TenantMembership(tenant, caller.subject(), TENANT_ADMIN));

        return tenantMapper.toSummary(tenant);
    }

    @Transactional(readOnly = true)
    public List<TenantSummary> listTenants(AuthenticatedUser caller) {
        if (caller.authorities().contains(ROLE_SUPER_ADMIN)) {
            return tenantRepository.findAll()
                    .stream()
                    .map(tenantMapper::toSummary)
                    .toList();
        }

        return membershipRepository.findAllByUserSubject(caller.subject())
                .stream()
                .filter(membership -> membership.getStatus() == TenantMembershipStatus.ACTIVE)
                .map(TenantMembership::getTenant)
                .distinct()
                .map(tenantMapper::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public Tenant getTenantOrThrow(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
    }
}
