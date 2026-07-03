package br.com.byop.aegis.feedback.service;

import br.com.byop.aegis.asset.api.AssetReference;
import br.com.byop.aegis.asset.api.AssetReferenceService;
import br.com.byop.aegis.feedback.contract.CreateFeedbackRequest;
import br.com.byop.aegis.feedback.domain.Feedback;
import br.com.byop.aegis.feedback.domain.FeedbackCategory;
import br.com.byop.aegis.feedback.domain.FeedbackPriority;
import br.com.byop.aegis.feedback.domain.FeedbackStatus;
import br.com.byop.aegis.feedback.dto.FeedbackSummary;
import br.com.byop.aegis.feedback.exception.FeedbackAttachmentNotFoundException;
import br.com.byop.aegis.feedback.exception.FeedbackContextRequiredException;
import br.com.byop.aegis.feedback.exception.FeedbackForbiddenException;
import br.com.byop.aegis.feedback.exception.FeedbackNotFoundException;
import br.com.byop.aegis.feedback.mapper.FeedbackMapper;
import br.com.byop.aegis.feedback.repository.FeedbackRepository;
import br.com.byop.aegis.notification.api.CriticalFeedbackNotificationRequest;
import br.com.byop.aegis.notification.api.FeedbackNotificationService;
import br.com.byop.aegis.product.api.ProductAccessPort;
import br.com.byop.aegis.product.api.ProductReference;
import br.com.byop.aegis.product.api.ProductReferenceService;
import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.tenant.api.TenantMembershipReference;
import br.com.byop.aegis.tenant.api.TenantUserAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class FeedbackService {

    private static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    private static final String ROLE_TENANT_ADMIN = "ROLE_TENANT_ADMIN";
    private static final String TENANT_ADMIN = "TENANT_ADMIN";
    private static final String ACTIVE_STATUS = "ativo";
    private static final String PUBLIC_ID_PREFIX = "AGS-";

    private final FeedbackRepository feedbackRepository;
    private final FeedbackMapper feedbackMapper;
    private final ProductReferenceService productReferenceService;
    private final ProductAccessPort productAccessPort;
    private final AssetReferenceService assetReferenceService;
    private final TenantUserAccessService tenantUserAccessService;
    private final FeedbackNotificationService feedbackNotificationService;

    public FeedbackService(FeedbackRepository feedbackRepository,
                           FeedbackMapper feedbackMapper,
                           ProductReferenceService productReferenceService,
                           ProductAccessPort productAccessPort,
                           AssetReferenceService assetReferenceService,
                           TenantUserAccessService tenantUserAccessService,
                           FeedbackNotificationService feedbackNotificationService) {
        this.feedbackRepository = feedbackRepository;
        this.feedbackMapper = feedbackMapper;
        this.productReferenceService = productReferenceService;
        this.productAccessPort = productAccessPort;
        this.assetReferenceService = assetReferenceService;
        this.tenantUserAccessService = tenantUserAccessService;
        this.feedbackNotificationService = feedbackNotificationService;
    }

    @Transactional
    public FeedbackSummary create(AuthenticatedUser caller, CreateFeedbackRequest request) {
        ProductContext context = resolveProductContext(caller, request.productId());
        validateAttachment(context.tenantId(), request.attachmentAssetId());
        FeedbackPriority priority = FeedbackPriority.fromContractValue(request.priority());
        Feedback feedback = new Feedback(new Feedback.Creation(
                nextPublicId(),
                context.tenantId(),
                context.productId(),
                caller.subject(),
                FeedbackCategory.fromContractValue(request.category()),
                priority,
                request.description(),
                request.screenName(),
                request.attachmentAssetId()
        ));
        Feedback saved = feedbackRepository.save(feedback);
        notifyCriticalFeedback(caller, saved);
        return feedbackMapper.toSummary(saved);
    }

    @Transactional(readOnly = true)
    public List<FeedbackSummary> listAll(AuthenticatedUser caller) {
        assertSuperAdmin(caller);
        return feedbackRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(feedbackMapper::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FeedbackSummary> listByTenant(AuthenticatedUser caller, UUID tenantId) {
        assertTenantFeedbackAccess(caller, tenantId);
        return feedbackRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                .map(feedbackMapper::toSummary)
                .toList();
    }

    @Transactional
    public FeedbackSummary updateStatus(AuthenticatedUser caller, String feedbackId, String status) {
        assertSuperAdmin(caller);
        Feedback feedback = feedbackRepository.findByPublicId(feedbackId)
                .orElseThrow(() -> new FeedbackNotFoundException(feedbackId));
        feedback.changeStatus(FeedbackStatus.fromContractValue(status));
        return feedbackMapper.toSummary(feedback);
    }

    private ProductContext resolveProductContext(AuthenticatedUser caller, UUID productId) {
        if (productId != null) {
            productAccessPort.assertAccessible(productId, caller);
            ProductReference reference = productReferenceService.getRequiredReference(productId);
            return new ProductContext(reference.tenantId(), reference.productId());
        }
        List<TenantMembershipReference> memberships = tenantUserAccessService.listActiveMemberships(caller.subject());
        if (memberships.size() != 1) {
            throw new FeedbackContextRequiredException();
        }
        return new ProductContext(memberships.getFirst().tenantId(), null);
    }

    private void validateAttachment(UUID tenantId, UUID attachmentAssetId) {
        if (attachmentAssetId == null) {
            return;
        }
        AssetReference asset = assetReferenceService.getRequiredReference(attachmentAssetId);
        ProductReference assetProduct = productReferenceService.getRequiredReference(asset.productId());
        if (!assetProduct.tenantId().equals(tenantId)) {
            throw new FeedbackAttachmentNotFoundException(attachmentAssetId);
        }
    }

    private void assertTenantFeedbackAccess(AuthenticatedUser caller, UUID tenantId) {
        if (isSuperAdmin(caller)) {
            tenantUserAccessService.getRequiredTenant(tenantId);
            return;
        }
        TenantMembershipReference membership = tenantUserAccessService.findMembership(tenantId, caller.subject())
                .orElseThrow(() -> new FeedbackNotFoundException(tenantId.toString()));
        if (!isActiveTenantAdmin(membership) || !caller.authorities().contains(ROLE_TENANT_ADMIN)) {
            throw new FeedbackForbiddenException();
        }
    }

    private void assertSuperAdmin(AuthenticatedUser caller) {
        if (!isSuperAdmin(caller)) {
            throw new FeedbackForbiddenException();
        }
    }

    private boolean isSuperAdmin(AuthenticatedUser caller) {
        return caller.authorities().contains(ROLE_SUPER_ADMIN);
    }

    private boolean isActiveTenantAdmin(TenantMembershipReference membership) {
        return TENANT_ADMIN.equals(membership.role()) && ACTIVE_STATUS.equals(membership.status());
    }

    private String nextPublicId() {
        return PUBLIC_ID_PREFIX + "%04d".formatted(feedbackRepository.nextPublicIdSequence());
    }

    private void notifyCriticalFeedback(AuthenticatedUser caller, Feedback feedback) {
        if (feedback.getPriority() != FeedbackPriority.CRITICAL) {
            return;
        }
        feedbackNotificationService.notifyCriticalFeedback(new CriticalFeedbackNotificationRequest(
                feedback.getPublicId(),
                feedback.getCategory().contractValue(),
                feedback.getPriority().contractValue(),
                feedback.getDescription(),
                feedback.getTenantId(),
                feedback.getProductId(),
                caller.subject(),
                feedback.getScreenName(),
                feedback.getAttachmentAssetId()
        ));
    }

    private record ProductContext(UUID tenantId, UUID productId) {
    }
}
