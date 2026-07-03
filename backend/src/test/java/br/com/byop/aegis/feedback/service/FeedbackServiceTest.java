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
import br.com.byop.aegis.tenant.api.TenantReference;
import br.com.byop.aegis.tenant.api.TenantUserAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FeedbackServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_TENANT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PRODUCT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID ASSET_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID ASSET_PRODUCT_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    private FeedbackRepository feedbackRepository;
    private ProductReferenceService productReferenceService;
    private ProductAccessPort productAccessPort;
    private AssetReferenceService assetReferenceService;
    private TenantUserAccessService tenantUserAccessService;
    private FeedbackNotificationService feedbackNotificationService;
    private TelegramFeedbackNotifier telegramFeedbackNotifier;
    private FeedbackService service;

    @BeforeEach
    void setUp() {
        feedbackRepository = mock(FeedbackRepository.class);
        productReferenceService = mock(ProductReferenceService.class);
        productAccessPort = mock(ProductAccessPort.class);
        assetReferenceService = mock(AssetReferenceService.class);
        tenantUserAccessService = mock(TenantUserAccessService.class);
        feedbackNotificationService = mock(FeedbackNotificationService.class);
        telegramFeedbackNotifier = mock(TelegramFeedbackNotifier.class);
        FeedbackMapper mapper = Mappers.getMapper(FeedbackMapper.class);
        service = new FeedbackService(feedbackRepository, mapper, productReferenceService, productAccessPort,
                assetReferenceService, tenantUserAccessService, feedbackNotificationService, telegramFeedbackNotifier);
    }

    @Test
    void shouldCreateFeedbackWithAnyAuthenticatedRoleAndReadablePublicId() {
        when(productReferenceService.getRequiredReference(PRODUCT_ID)).thenReturn(new ProductReference(PRODUCT_ID, TENANT_ID));
        when(feedbackRepository.nextPublicIdSequence()).thenReturn(42L);
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(invocation -> withTimestamps(invocation.getArgument(0)));
        CreateFeedbackRequest request = request(PRODUCT_ID, null);

        FeedbackSummary summary = service.create(editor(), request);

        assertThat(summary.id()).isEqualTo("AGS-0042");
        assertThat(summary.id()).doesNotContain(PRODUCT_ID.toString());
        assertThat(summary.tenantId()).isEqualTo(TENANT_ID);
        assertThat(summary.productId()).isEqualTo(PRODUCT_ID);
        assertThat(summary.status()).isEqualTo("aberto");
        ArgumentCaptor<Feedback> feedbackCaptor = ArgumentCaptor.forClass(Feedback.class);
        verify(feedbackRepository).save(feedbackCaptor.capture());
        assertThat(feedbackCaptor.getValue().getCreatedBySubject()).isEqualTo("editor-subject");
        verify(productAccessPort).assertAccessible(PRODUCT_ID, editor());
        verify(telegramFeedbackNotifier).notify(feedbackCaptor.getValue());
        verify(feedbackNotificationService, never()).notifyCriticalFeedback(any());
    }

    @Test
    void shouldNotifySuperAdminsWhenFeedbackIsCritical() {
        when(productReferenceService.getRequiredReference(PRODUCT_ID)).thenReturn(new ProductReference(PRODUCT_ID, TENANT_ID));
        when(feedbackRepository.nextPublicIdSequence()).thenReturn(43L);
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(invocation -> withTimestamps(invocation.getArgument(0)));
        CreateFeedbackRequest request = request(PRODUCT_ID, null, "crítica");

        FeedbackSummary summary = service.create(editor(), request);

        assertThat(summary.priority()).isEqualTo("crítica");
        ArgumentCaptor<CriticalFeedbackNotificationRequest> notificationCaptor =
                ArgumentCaptor.forClass(CriticalFeedbackNotificationRequest.class);
        verify(feedbackNotificationService).notifyCriticalFeedback(notificationCaptor.capture());
        CriticalFeedbackNotificationRequest notification = notificationCaptor.getValue();
        assertThat(notification.publicId()).isEqualTo("AGS-0043");
        assertThat(notification.category()).isEqualTo("Bug");
        assertThat(notification.priority()).isEqualTo("crítica");
        assertThat(notification.tenantId()).isEqualTo(TENANT_ID);
        assertThat(notification.productId()).isEqualTo(PRODUCT_ID);
        assertThat(notification.createdBySubject()).isEqualTo("editor-subject");
    }

    @Test
    void shouldCreateFeedbackWithoutProductWhenCallerHasSingleActiveTenant() {
        when(tenantUserAccessService.listActiveMemberships("editor-subject"))
                .thenReturn(List.of(membership(TENANT_ID, "EDITOR", "ativo")));
        when(feedbackRepository.nextPublicIdSequence()).thenReturn(7L);
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(invocation -> withTimestamps(invocation.getArgument(0)));
        CreateFeedbackRequest request = request(null, null);

        FeedbackSummary summary = service.create(editor(), request);

        assertThat(summary.id()).isEqualTo("AGS-0007");
        assertThat(summary.tenantId()).isEqualTo(TENANT_ID);
        assertThat(summary.productId()).isNull();
        verify(productAccessPort, never()).assertAccessible(any(), any());
    }

    @Test
    void shouldRejectFeedbackWithoutProductWhenTenantContextIsAmbiguous() {
        when(tenantUserAccessService.listActiveMemberships("editor-subject"))
                .thenReturn(List.of(membership(TENANT_ID, "EDITOR", "ativo"), membership(OTHER_TENANT_ID, "VIEWER", "ativo")));
        CreateFeedbackRequest request = request(null, null);
        AuthenticatedUser caller = editor();

        assertThatThrownBy(() -> service.create(caller, request))
                .isInstanceOf(FeedbackContextRequiredException.class);
        verify(feedbackRepository, never()).save(any());
    }

    @Test
    void shouldValidateAttachmentFromSameTenant() {
        when(productReferenceService.getRequiredReference(PRODUCT_ID)).thenReturn(new ProductReference(PRODUCT_ID, TENANT_ID));
        when(assetReferenceService.getRequiredReference(ASSET_ID))
                .thenReturn(new AssetReference(ASSET_ID, ASSET_PRODUCT_ID, "image/png", "image"));
        when(productReferenceService.getRequiredReference(ASSET_PRODUCT_ID))
                .thenReturn(new ProductReference(ASSET_PRODUCT_ID, TENANT_ID));
        when(feedbackRepository.nextPublicIdSequence()).thenReturn(8L);
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(invocation -> withTimestamps(invocation.getArgument(0)));
        CreateFeedbackRequest request = request(PRODUCT_ID, ASSET_ID);

        FeedbackSummary summary = service.create(editor(), request);

        assertThat(summary.attachmentAssetId()).isEqualTo(ASSET_ID);
    }

    @Test
    void shouldRejectAttachmentFromAnotherTenant() {
        when(productReferenceService.getRequiredReference(PRODUCT_ID)).thenReturn(new ProductReference(PRODUCT_ID, TENANT_ID));
        when(assetReferenceService.getRequiredReference(ASSET_ID))
                .thenReturn(new AssetReference(ASSET_ID, ASSET_PRODUCT_ID, "image/png", "image"));
        when(productReferenceService.getRequiredReference(ASSET_PRODUCT_ID))
                .thenReturn(new ProductReference(ASSET_PRODUCT_ID, OTHER_TENANT_ID));
        CreateFeedbackRequest request = request(PRODUCT_ID, ASSET_ID);
        AuthenticatedUser caller = editor();

        assertThatThrownBy(() -> service.create(caller, request))
                .isInstanceOf(FeedbackAttachmentNotFoundException.class);
        verify(feedbackRepository, never()).save(any());
    }

    @Test
    void shouldListGlobalFeedbackOnlyForSuperAdmin() {
        Feedback feedback = feedback("AGS-0001", TENANT_ID, PRODUCT_ID, FeedbackStatus.OPEN);
        when(feedbackRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(feedback));
        AuthenticatedUser superAdmin = superAdmin();
        AuthenticatedUser editor = editor();

        List<FeedbackSummary> summaries = service.listAll(superAdmin);

        assertThat(summaries).singleElement().satisfies(summary -> assertThat(summary.id()).isEqualTo("AGS-0001"));
        assertThatThrownBy(() -> service.listAll(editor)).isInstanceOf(FeedbackForbiddenException.class);
    }

    @Test
    void shouldListTenantFeedbackForSuperAdminAndTenantAdminOfSameTenant() {
        Feedback feedback = feedback("AGS-0002", TENANT_ID, null, FeedbackStatus.OPEN);
        when(tenantUserAccessService.getRequiredTenant(TENANT_ID)).thenReturn(new TenantReference(TENANT_ID, "BYOP"));
        when(tenantUserAccessService.findMembership(TENANT_ID, "tenant-admin-subject"))
                .thenReturn(Optional.of(membership(TENANT_ID, "TENANT_ADMIN", "ativo")));
        when(feedbackRepository.findAllByTenantIdOrderByCreatedAtDesc(TENANT_ID)).thenReturn(List.of(feedback));

        assertThat(service.listByTenant(superAdmin(), TENANT_ID)).hasSize(1);
        assertThat(service.listByTenant(tenantAdmin(), TENANT_ID)).hasSize(1);
    }

    @Test
    void shouldRejectTenantListForWrongTenantOrNonAdminRole() {
        when(tenantUserAccessService.findMembership(TENANT_ID, "tenant-admin-subject")).thenReturn(Optional.empty());
        AuthenticatedUser tenantAdmin = tenantAdmin();
        UUID tenantId = TENANT_ID;

        assertThatThrownBy(() -> service.listByTenant(tenantAdmin, tenantId))
                .isInstanceOf(FeedbackNotFoundException.class);

        when(tenantUserAccessService.findMembership(TENANT_ID, "editor-subject"))
                .thenReturn(Optional.of(membership(TENANT_ID, "EDITOR", "ativo")));
        AuthenticatedUser editor = editor();

        assertThatThrownBy(() -> service.listByTenant(editor, tenantId))
                .isInstanceOf(FeedbackForbiddenException.class);
    }

    @Test
    void shouldRejectTenantListWhenMembershipOrAuthorityIsNotActiveTenantAdmin() {
        when(tenantUserAccessService.findMembership(TENANT_ID, "tenant-admin-subject"))
                .thenReturn(Optional.of(membership(TENANT_ID, "TENANT_ADMIN", "bloqueado")));
        AuthenticatedUser tenantAdmin = tenantAdmin();
        UUID tenantId = TENANT_ID;

        assertThatThrownBy(() -> service.listByTenant(tenantAdmin, tenantId))
                .isInstanceOf(FeedbackForbiddenException.class);

        when(tenantUserAccessService.findMembership(TENANT_ID, "editor-subject"))
                .thenReturn(Optional.of(membership(TENANT_ID, "TENANT_ADMIN", "ativo")));
        AuthenticatedUser editor = editor();

        assertThatThrownBy(() -> service.listByTenant(editor, tenantId))
                .isInstanceOf(FeedbackForbiddenException.class);
    }

    @Test
    void shouldUpdateStatusOnlyForSuperAdmin() {
        Feedback feedback = feedback("AGS-0003", TENANT_ID, PRODUCT_ID, FeedbackStatus.OPEN);
        when(feedbackRepository.findByPublicId("AGS-0003")).thenReturn(Optional.of(feedback));
        AuthenticatedUser superAdmin = superAdmin();
        AuthenticatedUser editor = editor();

        FeedbackSummary summary = service.updateStatus(superAdmin, "AGS-0003", "em_analise");

        assertThat(summary.status()).isEqualTo("em_analise");
        assertThat(feedback.getStatus()).isEqualTo(FeedbackStatus.IN_REVIEW);
        assertThatThrownBy(() -> service.updateStatus(editor, "AGS-0003", "resolvido"))
                .isInstanceOf(FeedbackForbiddenException.class);
    }

    @Test
    void shouldRejectMissingFeedbackAndInvalidStatusOnUpdate() {
        when(feedbackRepository.findByPublicId("AGS-9999")).thenReturn(Optional.empty());
        AuthenticatedUser caller = superAdmin();

        assertThatThrownBy(() -> service.updateStatus(caller, "AGS-9999", "resolvido"))
                .isInstanceOf(FeedbackNotFoundException.class);

        Feedback feedback = feedback("AGS-0004", TENANT_ID, PRODUCT_ID, FeedbackStatus.OPEN);
        when(feedbackRepository.findByPublicId("AGS-0004")).thenReturn(Optional.of(feedback));

        assertThatThrownBy(() -> service.updateStatus(caller, "AGS-0004", "invalid"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private CreateFeedbackRequest request(UUID productId, UUID attachmentAssetId) {
        return request(productId, attachmentAssetId, "alta");
    }

    private CreateFeedbackRequest request(UUID productId, UUID attachmentAssetId, String priority) {
        return new CreateFeedbackRequest(
                productId,
                "Bug",
                priority,
                "Botao X nao responde ao clicar.",
                "/content/list",
                attachmentAssetId
        );
    }

    private Feedback feedback(String publicId, UUID tenantId, UUID productId, FeedbackStatus status) {
        Feedback feedback = new Feedback(new Feedback.Creation(
                publicId,
                tenantId,
                productId,
                "user-subject",
                FeedbackCategory.BUG,
                FeedbackPriority.HIGH,
                "Descricao",
                "/settings",
                null
        ));
        feedback.changeStatus(status);
        return withTimestamps(feedback);
    }

    private Feedback withTimestamps(Feedback feedback) {
        ReflectionTestUtils.setField(feedback, "createdAt", OffsetDateTime.parse("2026-07-03T10:00:00Z"));
        ReflectionTestUtils.setField(feedback, "updatedAt", OffsetDateTime.parse("2026-07-03T10:00:00Z"));
        return feedback;
    }

    private TenantMembershipReference membership(UUID tenantId, String role, String status) {
        return new TenantMembershipReference(UUID.randomUUID(), tenantId, "Tenant", "user-subject", role, status,
                OffsetDateTime.parse("2026-07-03T10:00:00Z"), OffsetDateTime.parse("2026-07-03T10:00:00Z"));
    }

    private AuthenticatedUser superAdmin() {
        return user("super-admin-subject", "ROLE_SUPER_ADMIN");
    }

    private AuthenticatedUser tenantAdmin() {
        return user("tenant-admin-subject", "ROLE_TENANT_ADMIN");
    }

    private AuthenticatedUser editor() {
        return user("editor-subject", "ROLE_EDITOR");
    }

    private AuthenticatedUser user(String subject, String role) {
        return new AuthenticatedUser(subject, subject + "@example.com", subject, subject, Set.of(role));
    }
}
