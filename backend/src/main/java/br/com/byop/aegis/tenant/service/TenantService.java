package br.com.byop.aegis.tenant.service;

import br.com.byop.aegis.audit.api.AuditRecordCommand;
import br.com.byop.aegis.audit.api.AuditService;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class TenantService {

    private static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    private static final String TENANT_ADMIN = "TENANT_ADMIN";
    private static final String TARGET_TYPE_TENANT = "Tenant";

    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository membershipRepository;
    private final TenantMapper tenantMapper;
    private final AuditService auditService;

    public TenantService(TenantRepository tenantRepository, TenantMembershipRepository membershipRepository,
                         TenantMapper tenantMapper, AuditService auditService) {
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
        this.tenantMapper = tenantMapper;
        this.auditService = auditService;
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
        recordTenantCreationAudit(caller, savedTenant);

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
    public TenantSummary updateTenant(AuthenticatedUser caller, UUID tenantId, UpdateTenantRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
        Map<String, Object> before = tenantSnapshot(tenant);

        tenant.rename(request.name());
        tenant.changePlan(request.plan());
        applyStatus(tenant, parseStatus(request.status()));
        Tenant savedTenant = tenantRepository.save(tenant);
        recordTenantUpdateAudit(caller, savedTenant, before);

        return tenantMapper.toSummary(savedTenant);
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

    private void recordTenantCreationAudit(AuthenticatedUser caller, Tenant tenant) {
        auditService.recordEvent(new AuditRecordCommand(
                tenant.getId(), null, caller.subject(), "TENANT_CREATED", TARGET_TYPE_TENANT,
                tenant.getId().toString(), tenant.getName(), null, null, tenantSnapshot(tenant)
        ));
    }

    private void recordTenantUpdateAudit(AuthenticatedUser caller, Tenant tenant, Map<String, Object> before) {
        auditService.recordEvent(new AuditRecordCommand(
                tenant.getId(), null, caller.subject(), "TENANT_UPDATED", TARGET_TYPE_TENANT,
                tenant.getId().toString(), tenant.getName(), null, before, tenantSnapshot(tenant)
        ));
    }

    private Map<String, Object> tenantSnapshot(Tenant tenant) {
        return Map.of("name", tenant.getName(), "plan", tenant.getPlan(), "status", tenant.getStatus().name());
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
        auditService.recordEvent(new AuditRecordCommand(
                tenant.getId(), null, caller.subject(), "TENANT_DELETED", TARGET_TYPE_TENANT,
                tenant.getId().toString(), tenant.getName(), null, tenantSnapshot(tenant), null
        ));
    }
}
