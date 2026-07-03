package br.com.byop.aegis.product.service;

import br.com.byop.aegis.audit.api.AuditRecordCommand;
import br.com.byop.aegis.audit.api.AuditService;
import br.com.byop.aegis.identity.api.IdentityUser;
import br.com.byop.aegis.identity.api.IdentityUserDirectory;
import br.com.byop.aegis.product.contract.AssignProductUserRequest;
import br.com.byop.aegis.product.domain.Product;
import br.com.byop.aegis.product.domain.ProductAssignment;
import br.com.byop.aegis.product.domain.ProductAssignmentRole;
import br.com.byop.aegis.product.dto.ProductAssignmentSummary;
import br.com.byop.aegis.product.exception.InvalidProductAssignmentException;
import br.com.byop.aegis.product.exception.ProductAssignmentNotFoundException;
import br.com.byop.aegis.product.exception.ProductNotFoundException;
import br.com.byop.aegis.product.mapper.ProductAssignmentMapper;
import br.com.byop.aegis.product.repository.ProductAssignmentRepository;
import br.com.byop.aegis.product.repository.ProductRepository;
import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.tenant.api.TenantAccessService;
import br.com.byop.aegis.tenant.api.TenantReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ProductAssignmentService {

    private static final String TARGET_TYPE_PRODUCT_ASSIGNMENT = "ProductAssignment";

    private final ProductRepository productRepository;
    private final ProductAssignmentRepository assignmentRepository;
    private final TenantAccessService tenantAccessService;
    private final IdentityUserDirectory userDirectory;
    private final ProductAssignmentInvitePort invitePort;
    private final ProductAssignmentEmailPort emailPort;
    private final ProductAssignmentNotificationPort notificationPort;
    private final ProductAssignmentMapper assignmentMapper;
    private final AuditService auditService;

    public ProductAssignmentService(ProductRepository productRepository,
                                    ProductAssignmentRepository assignmentRepository,
                                    TenantAccessService tenantAccessService,
                                    IdentityUserDirectory userDirectory,
                                    ProductAssignmentInvitePort invitePort,
                                    ProductAssignmentEmailPort emailPort,
                                    ProductAssignmentNotificationPort notificationPort,
                                    ProductAssignmentMapper assignmentMapper,
                                    AuditService auditService) {
        this.productRepository = productRepository;
        this.assignmentRepository = assignmentRepository;
        this.tenantAccessService = tenantAccessService;
        this.userDirectory = userDirectory;
        this.invitePort = invitePort;
        this.emailPort = emailPort;
        this.notificationPort = notificationPort;
        this.assignmentMapper = assignmentMapper;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ProductAssignmentSummary> listAssignments(UUID productId) {
        Product product = getRequiredProduct(productId);
        return assignmentRepository.findAllByProductId(product.getId())
                .stream()
                .map(assignment -> assignmentMapper.toSummary(assignment, null, null))
                .toList();
    }

    @Transactional
    public ProductAssignmentSummary assignUser(AuthenticatedUser caller, UUID pathProductId,
                                               AssignProductUserRequest request) {
        validateProductPath(pathProductId, request.productId());
        validateUserXor(request.userId(), request.inviteEmail());

        Product product = getRequiredProduct(pathProductId);
        if (!product.getTenantId().equals(request.tenantId())) {
            throw new InvalidProductAssignmentException("Request tenantId does not match product tenantId");
        }

        ProductAssignmentRole role = parseRole(request.role());
        if (hasText(request.userId())) {
            return assignExistingUser(caller, product, request.userId(), role);
        }
        return inviteUser(caller, product, request.inviteEmail(), role);
    }

    @Transactional
    public void removeAssignment(AuthenticatedUser caller, UUID productId, String userSubject) {
        Product product = getRequiredProduct(productId);
        ProductAssignment assignment = assignmentRepository.findByProductIdAndUserSubject(product.getId(), userSubject)
                .orElseThrow(() -> new ProductAssignmentNotFoundException(productId, userSubject));

        IdentityUser user = userDirectory.getRequiredUser(userSubject);
        emailPort.notifyRevocation(emailCommand(product, user));
        notificationPort.notifyRevocation(product.getTenantId(), product.getId(), user.id());
        assignmentRepository.delete(assignment);
        recordAssignmentAudit(caller, product, user, "PRODUCT_ASSIGNMENT_REMOVED",
                assignment.getRole().name(), null);
    }

    private ProductAssignmentSummary assignExistingUser(AuthenticatedUser caller, Product product, String userId,
                                                         ProductAssignmentRole role) {
        if (!tenantAccessService.hasActiveMembership(product.getTenantId(), userId)) {
            throw new InvalidProductAssignmentException("User does not have active membership in product tenant");
        }

        IdentityUser user = userDirectory.getRequiredUser(userId);
        ProductAssignment assignment = assignmentRepository.save(new ProductAssignment(product, user.id(), role));
        emailPort.notifyAssignment(emailCommand(product, user));
        notificationPort.notifyAssignment(product.getTenantId(), product.getId(), user.id());
        recordAssignmentAudit(caller, product, user, "PRODUCT_ASSIGNMENT_CREATED", null, role.name());

        return assignmentMapper.toSummary(assignment, user.displayName(), user.email());
    }

    private ProductAssignmentSummary inviteUser(AuthenticatedUser caller, Product product, String inviteEmail,
                                                ProductAssignmentRole role) {
        IdentityUser invitedUser = inviteUserThroughPort(caller, product, inviteEmail, role);
        ProductAssignment assignment = new ProductAssignment(product, invitedUser.id(), role);
        assignment.revoke();
        ProductAssignment saved = assignmentRepository.save(assignment);
        notificationPort.notifyAssignment(product.getTenantId(), product.getId(), invitedUser.id());
        recordAssignmentAudit(caller, product, invitedUser, "PRODUCT_ASSIGNMENT_CREATED", null, role.name());

        return assignmentMapper.toSummary(saved, invitedUser.displayName(), invitedUser.email());
    }

    private IdentityUser inviteUserThroughPort(AuthenticatedUser caller, Product product, String inviteEmail,
                                               ProductAssignmentRole role) {
        return invitePort.invite(
                product.getTenantId(),
                product.getId(),
                product.getName(),
                inviteEmail,
                role.name(),
                caller.name()
        );
    }

    private void recordAssignmentAudit(AuthenticatedUser caller, Product product, IdentityUser user, String action,
                                       String beforeRole, String afterRole) {
        auditService.recordEvent(new AuditRecordCommand(
                product.getTenantId(), product.getId(), caller.subject(), action, TARGET_TYPE_PRODUCT_ASSIGNMENT,
                user.id(), user.displayName(), null,
                beforeRole == null ? null : Map.of("role", beforeRole),
                afterRole == null ? null : Map.of("role", afterRole)
        ));
    }

    private ProductAssignmentEmailCommand emailCommand(Product product, IdentityUser user) {
        TenantReference tenant = tenantAccessService.getRequiredReference(product.getTenantId());
        return new ProductAssignmentEmailCommand(
                tenant.tenantId(),
                tenant.name(),
                product.getId(),
                product.getName(),
                user.email(),
                user.displayName()
        );
    }

    private Product getRequiredProduct(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    private void validateProductPath(UUID pathProductId, UUID bodyProductId) {
        if (!pathProductId.equals(bodyProductId)) {
            throw new InvalidProductAssignmentException("Path productId does not match request productId");
        }
    }

    private void validateUserXor(String userId, String inviteEmail) {
        boolean hasUserId = hasText(userId);
        boolean hasInviteEmail = hasText(inviteEmail);
        if (hasUserId == hasInviteEmail) {
            throw new InvalidProductAssignmentException("Exactly one of userId or inviteEmail is required");
        }
    }

    private ProductAssignmentRole parseRole(String role) {
        try {
            return ProductAssignmentRole.valueOf(role.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException _) {
            throw new InvalidProductAssignmentException("Invalid product assignment role: " + role);
        }
    }

    private boolean hasText(String value) {
        return StringUtils.hasText(value);
    }
}
