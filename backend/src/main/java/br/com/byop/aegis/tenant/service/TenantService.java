package br.com.byop.aegis.tenant.service;

import br.com.byop.aegis.tenant.command.CreateTenantCommand;
import br.com.byop.aegis.tenant.contract.DeleteTenantRequest;
import br.com.byop.aegis.tenant.contract.UpdateTenantRequest;
import br.com.byop.aegis.tenant.domain.Tenant;
import br.com.byop.aegis.tenant.domain.TenantMembership;
import br.com.byop.aegis.tenant.domain.TenantMembershipStatus;
import br.com.byop.aegis.tenant.domain.TenantStatus;
import br.com.byop.aegis.tenant.dto.TenantSummary;
import br.com.byop.aegis.tenant.exception.TenantAlreadyExistsException;
import br.com.byop.aegis.tenant.exception.InvalidTenantConfirmationException;
import br.com.byop.aegis.tenant.exception.InvalidTenantStatusException;
import br.com.byop.aegis.tenant.exception.TenantNotFoundException;
import br.com.byop.aegis.tenant.mapper.TenantMapper;
import br.com.byop.aegis.tenant.repository.TenantMembershipRepository;
import br.com.byop.aegis.tenant.repository.TenantRepository;
import br.com.byop.aegis.security.AuthenticatedUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class TenantService {

    private static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    private static final String TENANT_ADMIN = "TENANT_ADMIN";

    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository membershipRepository;
    private final TenantMapper tenantMapper;
    private final JdbcTemplate jdbcTemplate;

    public TenantService(TenantRepository tenantRepository, TenantMembershipRepository membershipRepository,
                         TenantMapper tenantMapper, JdbcTemplate jdbcTemplate) {
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
        this.tenantMapper = tenantMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public TenantSummary createTenant(AuthenticatedUser caller, CreateTenantCommand command) {
        if (tenantRepository.existsByKey(command.key())) {
            throw new TenantAlreadyExistsException(command.key());
        }

        Tenant tenant = new Tenant(command.key(), command.name());
        if (command.plan() != null && !command.plan().isBlank()) {
            tenant.changePlan(command.plan());
        }
        Tenant savedTenant = tenantRepository.save(tenant);
        membershipRepository.save(new TenantMembership(savedTenant, caller.subject(), TENANT_ADMIN));

        return tenantMapper.toSummary(savedTenant);
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

    @Transactional
    public TenantSummary updateTenant(UUID tenantId, UpdateTenantRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));

        tenant.rename(request.name());
        tenant.changePlan(request.plan());
        applyStatus(tenant, parseStatus(request.status()));

        return tenantMapper.toSummary(tenantRepository.save(tenant));
    }

    @Transactional
    public void deleteTenant(AuthenticatedUser caller, UUID tenantId, DeleteTenantRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));

        if (!tenant.getName().equals(request.confirmationText())) {
            throw new InvalidTenantConfirmationException();
        }

        recordTenantDeletionAudit(caller, tenant);
        tenantRepository.delete(tenant);
    }

    @Transactional(readOnly = true)
    public Tenant getTenantOrThrow(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
    }

    private TenantStatus parseStatus(String status) {
        String normalized = String.valueOf(status).trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "ACTIVE", "ATIVO" -> TenantStatus.ACTIVE;
            case "SUSPENDED", "SUSPENSO" -> TenantStatus.SUSPENDED;
            case "ARCHIVED", "ARQUIVADO" -> TenantStatus.ARCHIVED;
            default -> throw new InvalidTenantStatusException(status);
        };
    }

    private void applyStatus(Tenant tenant, TenantStatus status) {
        Runnable statusChange = switch (status) {
            case ACTIVE -> tenant::activate;
            case SUSPENDED -> tenant::suspend;
            case ARCHIVED -> tenant::archive;
        };
        statusChange.run();
    }

    private void recordTenantDeletionAudit(AuthenticatedUser caller, Tenant tenant) {
        jdbcTemplate.update("""
                        INSERT INTO audit_events (
                            id, tenant_id, product_id, actor_user_id, event_type, entity_type, entity_id, payload, occurred_at
                        )
                        VALUES (?, NULL, NULL, NULL, ?, ?, ?, ?::jsonb, CURRENT_TIMESTAMP)
                        """,
                UUID.randomUUID(),
                "TENANT_DELETED",
                "Tenant",
                tenant.getId(),
                "{\"tenantKey\":\"" + tenant.getKey() + "\",\"tenantName\":\"" + tenant.getName()
                        + "\",\"actorSubject\":\"" + caller.subject() + "\"}"
        );
    }
}
