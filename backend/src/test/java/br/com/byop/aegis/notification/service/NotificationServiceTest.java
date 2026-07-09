package br.com.byop.aegis.notification.service;

import br.com.byop.aegis.identity.api.IdentityUser;
import br.com.byop.aegis.identity.api.IdentityUserDirectory;
import br.com.byop.aegis.notification.api.CriticalFeedbackNotificationRequest;
import br.com.byop.aegis.notification.contract.CreateNotificationRequest;
import br.com.byop.aegis.notification.domain.Notification;
import br.com.byop.aegis.notification.domain.NotificationPresentationMode;
import br.com.byop.aegis.notification.domain.NotificationType;
import br.com.byop.aegis.notification.domain.UserNotificationStatus;
import br.com.byop.aegis.notification.dto.NotificationResponse;
import br.com.byop.aegis.notification.dto.NotificationWithStatus;
import br.com.byop.aegis.notification.exception.InsufficientNotificationRoleException;
import br.com.byop.aegis.notification.exception.InvalidNotificationTargetException;
import br.com.byop.aegis.notification.exception.NotificationNotFoundException;
import br.com.byop.aegis.notification.exception.NotificationTargetUserNotFoundException;
import br.com.byop.aegis.notification.exception.OnboardingNotificationNotFoundException;
import br.com.byop.aegis.notification.mapper.NotificationMapper;
import br.com.byop.aegis.notification.repository.NotificationRepository;
import br.com.byop.aegis.notification.repository.UserNotificationStatusRepository;
import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.shared.markdown.SharedMarkdownSanitizer;
import br.com.byop.aegis.tenant.api.TenantUserAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTest {

    private static final UUID NOTIFICATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TENANT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-07-02T10:00:00Z");

    private NotificationRepository notificationRepository;
    private UserNotificationStatusRepository statusRepository;
    private TenantUserAccessService tenantUserAccessService;
    private IdentityUserDirectory identityUserDirectory;
    private NotificationService service;

    @BeforeEach
    void setUp() {
        notificationRepository = mock(NotificationRepository.class);
        statusRepository = mock(UserNotificationStatusRepository.class);
        tenantUserAccessService = mock(TenantUserAccessService.class);
        identityUserDirectory = mock(IdentityUserDirectory.class);
        NotificationMapper mapper = Mappers.getMapper(NotificationMapper.class);
        service = new NotificationService(notificationRepository, statusRepository, tenantUserAccessService,
                identityUserDirectory, new SharedMarkdownSanitizer(), mapper);
    }

    @Test
    void shouldCreateNotificationForAllActiveUsersWithoutDuplicatingRecipients() {
        when(tenantUserAccessService.listActiveUserSubjects()).thenReturn(List.of("user-1", "user-2", "user-1"));
        when(notificationRepository.save(any())).thenAnswer(invocation -> withId(invocation.getArgument(0)));
        when(statusRepository.findUserSubjectsByNotificationId(NOTIFICATION_ID)).thenReturn(List.of());
        CreateNotificationRequest request = request(target("ALL", null, null));

        NotificationResponse response = service.create(superAdmin(), request);

        assertThat(response.id()).isEqualTo(NOTIFICATION_ID);
        ArgumentCaptor<UserNotificationStatus> statusCaptor = ArgumentCaptor.forClass(UserNotificationStatus.class);
        verify(statusRepository, times(2)).save(statusCaptor.capture());
        assertThat(statusCaptor.getAllValues()).extracting(UserNotificationStatus::getUserSubject)
                .containsExactly("user-1", "user-2");
    }

    @Test
    void shouldCreateNotificationForTenantActiveUsers() {
        when(tenantUserAccessService.listActiveUserSubjects(TENANT_ID)).thenReturn(List.of("tenant-user"));
        when(notificationRepository.save(any())).thenAnswer(invocation -> withId(invocation.getArgument(0)));
        when(statusRepository.findUserSubjectsByNotificationId(NOTIFICATION_ID)).thenReturn(List.of());
        CreateNotificationRequest request = request(target("TENANT", TENANT_ID, null));

        service.create(superAdmin(), request);

        ArgumentCaptor<UserNotificationStatus> statusCaptor = ArgumentCaptor.forClass(UserNotificationStatus.class);
        verify(statusRepository).save(statusCaptor.capture());
        assertThat(statusCaptor.getValue().getUserSubject()).isEqualTo("tenant-user");
    }

    @Test
    void shouldCreateNotificationForSpecificUsersAfterValidatingAllUsers() {
        when(identityUserDirectory.getRequiredUser("user-1")).thenReturn(user("user-1"));
        when(identityUserDirectory.getRequiredUser("user-2")).thenReturn(user("user-2"));
        when(notificationRepository.save(any())).thenAnswer(invocation -> withId(invocation.getArgument(0)));
        when(statusRepository.findUserSubjectsByNotificationId(NOTIFICATION_ID)).thenReturn(List.of());
        CreateNotificationRequest request = request(target("USERS", null, List.of("user-1", "user-2", "user-1")));

        service.create(superAdmin(), request);

        ArgumentCaptor<UserNotificationStatus> statusCaptor = ArgumentCaptor.forClass(UserNotificationStatus.class);
        verify(statusRepository, times(2)).save(statusCaptor.capture());
        assertThat(statusCaptor.getAllValues()).extracting(UserNotificationStatus::getUserSubject)
                .containsExactly("user-1", "user-2");
    }

    @Test
    void shouldNotCreateStatusesForExistingRecipients() {
        when(tenantUserAccessService.listActiveUserSubjects()).thenReturn(List.of("user-1"));
        when(notificationRepository.save(any())).thenAnswer(invocation -> withId(invocation.getArgument(0)));
        when(statusRepository.findUserSubjectsByNotificationId(NOTIFICATION_ID)).thenReturn(List.of("user-1"));
        CreateNotificationRequest request = request(target("ALL", null, null));

        service.create(superAdmin(), request);

        verify(statusRepository, never()).save(any(UserNotificationStatus.class));
    }

    @Test
    void shouldRejectUnknownUserWithoutPartialFanOut() {
        when(identityUserDirectory.getRequiredUser("missing")).thenThrow(new IllegalArgumentException("missing"));
        CreateNotificationRequest.Target target = target("USERS", null, List.of("missing"));
        CreateNotificationRequest request = request(target);
        AuthenticatedUser caller = superAdmin();

        assertThatThrownBy(() -> service.create(caller, request))
                .isInstanceOf(NotificationTargetUserNotFoundException.class);
        verify(notificationRepository, never()).save(any());
        verify(statusRepository, never()).save(any());
    }

    @Test
    void shouldSanitizeBodyBeforePersisting() {
        when(tenantUserAccessService.listActiveUserSubjects()).thenReturn(List.of());
        when(notificationRepository.save(any())).thenAnswer(invocation -> withId(invocation.getArgument(0)));
        CreateNotificationRequest request = new CreateNotificationRequest(
                "FEATURE",
                "Titulo",
                "<p style=\"color:red\">ok</p><script>alert(1)</script>",
                "BELL_ONLY",
                target("ALL", null, null)
        );

        service.create(superAdmin(), request);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().getBodyMarkdown())
                .isEqualTo("<p>ok</p>");
    }

    @Test
    void shouldRejectAdminEndpointsForNonSuperAdmin() {
        CreateNotificationRequest request = request(target("ALL", null, null));
        AuthenticatedUser caller = editor();

        assertThatThrownBy(() -> service.create(caller, request))
                .isInstanceOf(InsufficientNotificationRoleException.class);
        assertThatThrownBy(() -> service.listAll(caller))
                .isInstanceOf(InsufficientNotificationRoleException.class);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidTargets")
    void shouldRejectInvalidTargets(String scenario, CreateNotificationRequest.Target target) {
        CreateNotificationRequest request = request(target);
        AuthenticatedUser caller = superAdmin();

        assertThatThrownBy(() -> service.create(caller, request))
                .isInstanceOf(InvalidNotificationTargetException.class);
    }

    @Test
    void shouldListAllNotificationsForSuperAdmin() {
        Notification notification = notification();
        when(notificationRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(notification));

        List<NotificationResponse> notifications = service.listAll(superAdmin());

        assertThat(notifications)
                .singleElement()
                .satisfies(response -> assertThat(response.id()).isEqualTo(NOTIFICATION_ID));
    }

    @Test
    void shouldReturnPendingModalAndListMine() {
        UserNotificationStatus status = new UserNotificationStatus(notification(), "user-1");
        when(statusRepository.findAllByUserSubjectOrderByNotificationCreatedAtDesc("user-1")).thenReturn(List.of(status));
        when(statusRepository
                .findFirstByUserSubjectAndNotificationPresentationModeAndAutoShownFalseOrderByNotificationCreatedAtAsc(
                        "user-1",
                        NotificationPresentationMode.MODAL_ONCE
                ))
                .thenReturn(Optional.of(status));

        List<NotificationWithStatus> mine = service.listMine(user("user-1", "ROLE_EDITOR"));
        NotificationWithStatus pending = service.getPendingModal(user("user-1", "ROLE_EDITOR"));

        assertThat(mine).hasSize(1);
        assertThat(pending.title()).isEqualTo("Titulo");
    }

    @Test
    void shouldReturnNullWhenNoPendingModalExists() {
        when(statusRepository
                .findFirstByUserSubjectAndNotificationPresentationModeAndAutoShownFalseOrderByNotificationCreatedAtAsc(
                        "user-1",
                        NotificationPresentationMode.MODAL_ONCE
                ))
                .thenReturn(Optional.empty());

        assertThat(service.getPendingModal(user("user-1", "ROLE_EDITOR"))).isNull();
    }

    @Test
    void shouldMarkShownAndReadIdempotently() {
        UserNotificationStatus status = new UserNotificationStatus(notification(), "user-1");
        status.markShown(CREATED_AT);
        when(statusRepository.findByNotificationIdAndUserSubject(NOTIFICATION_ID, "user-1"))
                .thenReturn(Optional.of(status));

        service.markShown(user("user-1", "ROLE_EDITOR"), NOTIFICATION_ID);
        service.markRead(user("user-1", "ROLE_EDITOR"), NOTIFICATION_ID);
        service.markRead(user("user-1", "ROLE_EDITOR"), NOTIFICATION_ID);

        assertThat(status.isAutoShown()).isTrue();
        assertThat(status.isRead()).isTrue();
        assertThat(status.getReadAt()).isNotNull();
    }

    @Test
    void shouldReturnNotFoundWhenStatusDoesNotBelongToCaller() {
        when(statusRepository.findByNotificationIdAndUserSubject(NOTIFICATION_ID, "user-1"))
                .thenReturn(Optional.empty());
        AuthenticatedUser caller = user("user-1", "ROLE_EDITOR");

        assertThatThrownBy(() -> service.markRead(caller, NOTIFICATION_ID))
                .isInstanceOf(NotificationNotFoundException.class);
    }

    @Test
    void shouldAssignOnboardingIdempotently() {
        Notification onboarding = notification(NotificationType.ONBOARDING);
        when(notificationRepository.findFirstByType(NotificationType.ONBOARDING)).thenReturn(Optional.of(onboarding));
        when(statusRepository.existsByNotificationIdAndUserSubject(NOTIFICATION_ID, "user-1")).thenReturn(false, true);

        service.assignOnboarding("user-1");
        service.assignOnboarding("user-1");

        verify(statusRepository).save(any(UserNotificationStatus.class));
    }

    @Test
    void shouldRejectOnboardingAssignmentWhenSeedNotificationDoesNotExist() {
        when(notificationRepository.findFirstByType(NotificationType.ONBOARDING)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignOnboarding("user-1"))
                .isInstanceOf(OnboardingNotificationNotFoundException.class);
    }

    @Test
    void shouldCreateWarningBellOnlyNotificationOnTenantSuspended() {
        when(tenantUserAccessService.listActiveUserSubjects(TENANT_ID)).thenReturn(List.of("tenant-user"));
        when(notificationRepository.save(any())).thenAnswer(invocation -> withId(invocation.getArgument(0)));

        service.notifyTenantStatusChange(TENANT_ID, br.com.byop.aegis.tenant.api.TenantLifecycleTransition.SUSPENDED,
                "super-admin");

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification saved = notificationCaptor.getValue();
        assertThat(saved.getType()).isEqualTo(NotificationType.WARNING);
        assertThat(saved.getPresentationMode()).isEqualTo(NotificationPresentationMode.BELL_ONLY);
        assertThat(saved.getCreatedBySubject()).isEqualTo("super-admin");
        ArgumentCaptor<UserNotificationStatus> statusCaptor = ArgumentCaptor.forClass(UserNotificationStatus.class);
        verify(statusRepository).save(statusCaptor.capture());
        assertThat(statusCaptor.getValue().getUserSubject()).isEqualTo("tenant-user");
    }

    @Test
    void shouldCreateWarningBellOnlyNotificationForCriticalFeedback() {
        when(tenantUserAccessService.listActiveSuperAdminSubjects())
                .thenReturn(List.of("super-admin", "super-admin-2", "super-admin"));
        when(notificationRepository.save(any())).thenAnswer(invocation -> withId(invocation.getArgument(0)));
        when(statusRepository.findUserSubjectsByNotificationId(NOTIFICATION_ID)).thenReturn(List.of());
        CriticalFeedbackNotificationRequest request = criticalFeedbackRequest();

        service.notifyCriticalFeedback(request);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification notification = notificationCaptor.getValue();
        assertThat(notification.getType()).isEqualTo(NotificationType.WARNING);
        assertThat(notification.getPresentationMode()).isEqualTo(NotificationPresentationMode.BELL_ONLY);
        assertThat(notification.getCreatedBySubject()).isEqualTo("editor-subject");
        assertThat(notification.getTitle()).isEqualTo("Feedback critico recebido");
        assertThat(notification.getBodyMarkdown()).contains("AGS-0043", "Bug", "crítica", "editor-subject");
        ArgumentCaptor<UserNotificationStatus> statusCaptor = ArgumentCaptor.forClass(UserNotificationStatus.class);
        verify(statusRepository, times(2)).save(statusCaptor.capture());
        assertThat(statusCaptor.getAllValues()).extracting(UserNotificationStatus::getUserSubject)
                .containsExactly("super-admin", "super-admin-2");
    }

    @Test
    void shouldSkipCriticalFeedbackNotificationWhenNoSuperAdminIsActive() {
        when(tenantUserAccessService.listActiveSuperAdminSubjects()).thenReturn(List.of());
        CriticalFeedbackNotificationRequest request = criticalFeedbackRequest();

        service.notifyCriticalFeedback(request);

        verify(notificationRepository, never()).save(any());
        verify(statusRepository, never()).save(any());
    }

    @Test
    void shouldSummarizeCriticalFeedbackNotificationAndRenderMissingOptionalContext() {
        when(tenantUserAccessService.listActiveSuperAdminSubjects()).thenReturn(List.of("super-admin"));
        when(notificationRepository.save(any())).thenAnswer(invocation -> withId(invocation.getArgument(0)));
        when(statusRepository.findUserSubjectsByNotificationId(NOTIFICATION_ID)).thenReturn(List.of());
        String longDescription = "A".repeat(220);
        CriticalFeedbackNotificationRequest request = new CriticalFeedbackNotificationRequest(
                "AGS-0044",
                "UX confusa",
                "crítica",
                longDescription,
                TENANT_ID,
                null,
                "editor-subject",
                null,
                null
        );

        service.notifyCriticalFeedback(request);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        String bodyMarkdown = notificationCaptor.getValue().getBodyMarkdown();
        assertThat(bodyMarkdown)
                .contains("A".repeat(200), "Produto: `n/a`", "Tela: `n/a`", "Anexo: `n/a`")
                .doesNotContain("A".repeat(201));
    }

    @Test
    void shouldCreateGeneralBellOnlyNotificationOnTenantReactivated() {
        when(tenantUserAccessService.listActiveUserSubjects(TENANT_ID)).thenReturn(List.of("tenant-user"));
        when(notificationRepository.save(any())).thenAnswer(invocation -> withId(invocation.getArgument(0)));

        service.notifyTenantStatusChange(TENANT_ID, br.com.byop.aegis.tenant.api.TenantLifecycleTransition.REACTIVATED,
                "super-admin");

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().getType()).isEqualTo(NotificationType.GENERAL);
        assertThat(notificationCaptor.getValue().getPresentationMode()).isEqualTo(NotificationPresentationMode.BELL_ONLY);
    }

    @Test
    void shouldNotifyOnTenantStatusChangedEvent() {
        when(tenantUserAccessService.listActiveUserSubjects(TENANT_ID)).thenReturn(List.of("tenant-user"));
        when(notificationRepository.save(any())).thenAnswer(invocation -> withId(invocation.getArgument(0)));

        service.onTenantStatusChanged(new br.com.byop.aegis.tenant.api.TenantStatusChangedEvent(TENANT_ID,
                br.com.byop.aegis.tenant.api.TenantLifecycleTransition.SUSPENDED, "super-admin"));

        verify(notificationRepository).save(any(Notification.class));
    }

    private static Stream<Arguments> invalidTargets() {
        CreateNotificationRequest.Target tenantWithoutId = new CreateNotificationRequest.Target("TENANT", null, null);
        CreateNotificationRequest.Target unknownTarget = new CreateNotificationRequest.Target("UNKNOWN", null, null);
        CreateNotificationRequest.Target usersWithoutIds = new CreateNotificationRequest.Target("USERS", null, List.of());
        CreateNotificationRequest.Target usersWithNullIds = new CreateNotificationRequest.Target("USERS", null, null);
        return Stream.of(
                Arguments.of("tenant sem tenantId", tenantWithoutId),
                Arguments.of("target desconhecido", unknownTarget),
                Arguments.of("users sem lista", usersWithoutIds),
                Arguments.of("users com lista nula", usersWithNullIds)
        );
    }

    private CreateNotificationRequest request(CreateNotificationRequest.Target target) {
        return new CreateNotificationRequest("FEATURE", "Titulo", "<p>body</p>", "MODAL_ONCE", target);
    }

    private CreateNotificationRequest.Target target(String type, UUID tenantId, List<String> userIds) {
        return new CreateNotificationRequest.Target(type, tenantId, userIds);
    }

    private CriticalFeedbackNotificationRequest criticalFeedbackRequest() {
        return new CriticalFeedbackNotificationRequest(
                "AGS-0043",
                "Bug",
                "crítica",
                "Botao X nao responde ao clicar.",
                TENANT_ID,
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                "editor-subject",
                "/content/list",
                UUID.fromString("44444444-4444-4444-4444-444444444444")
        );
    }

    private Notification notification() {
        return notification(NotificationType.FEATURE);
    }

    private Notification notification(NotificationType type) {
        Notification notification = new Notification(type, "Titulo", "<p>body</p>",
                NotificationPresentationMode.MODAL_ONCE, "admin");
        return withId(notification);
    }

    private Notification withId(Notification notification) {
        ReflectionTestUtils.setField(notification, "id", NOTIFICATION_ID);
        ReflectionTestUtils.setField(notification, "createdAt", CREATED_AT);
        ReflectionTestUtils.setField(notification, "updatedAt", CREATED_AT);
        return notification;
    }

    private AuthenticatedUser superAdmin() {
        return user("admin", "ROLE_SUPER_ADMIN");
    }

    private AuthenticatedUser editor() {
        return user("editor", "ROLE_EDITOR");
    }

    private AuthenticatedUser user(String subject, String role) {
        return new AuthenticatedUser(subject, subject + "@byop.dev", subject, subject, Set.of(role));
    }

    private IdentityUser user(String userSubject) {
        return new IdentityUser(userSubject, userSubject, userSubject + "@byop.dev", userSubject, null);
    }
}
