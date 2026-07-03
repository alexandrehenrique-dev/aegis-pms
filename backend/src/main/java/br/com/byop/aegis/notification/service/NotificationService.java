package br.com.byop.aegis.notification.service;

import br.com.byop.aegis.identity.api.IdentityUserDirectory;
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
import br.com.byop.aegis.tenant.api.TenantLifecycleTransition;
import br.com.byop.aegis.tenant.api.TenantStatusChangedEvent;
import br.com.byop.aegis.tenant.api.TenantUserAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class NotificationService {

    private static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    private static final String TARGET_ALL = "ALL";
    private static final String TARGET_TENANT = "TENANT";
    private static final String TARGET_USERS = "USERS";
    private static final String TENANT_SUSPENDED_TITLE = "Tenant suspenso";
    private static final String TENANT_SUSPENDED_BODY =
            "Este tenant foi suspenso pelo administrador da plataforma. Contate o suporte para mais informações.";
    private static final String TENANT_REACTIVATED_TITLE = "Tenant reativado";
    private static final String TENANT_REACTIVATED_BODY = "Este tenant foi reativado.";

    private final NotificationRepository notificationRepository;
    private final UserNotificationStatusRepository statusRepository;
    private final TenantUserAccessService tenantUserAccessService;
    private final IdentityUserDirectory identityUserDirectory;
    private final SharedMarkdownSanitizer markdownSanitizer;
    private final NotificationMapper notificationMapper;

    public NotificationService(NotificationRepository notificationRepository,
                               UserNotificationStatusRepository statusRepository,
                               TenantUserAccessService tenantUserAccessService,
                               IdentityUserDirectory identityUserDirectory,
                               SharedMarkdownSanitizer markdownSanitizer,
                               NotificationMapper notificationMapper) {
        this.notificationRepository = notificationRepository;
        this.statusRepository = statusRepository;
        this.tenantUserAccessService = tenantUserAccessService;
        this.identityUserDirectory = identityUserDirectory;
        this.markdownSanitizer = markdownSanitizer;
        this.notificationMapper = notificationMapper;
    }

    @Transactional(readOnly = true)
    public List<NotificationWithStatus> listMine(AuthenticatedUser caller) {
        return statusRepository.findAllByUserSubjectOrderByNotificationCreatedAtDesc(caller.subject())
                .stream()
                .map(notificationMapper::toWithStatus)
                .toList();
    }

    @Transactional(readOnly = true)
    public NotificationWithStatus getPendingModal(AuthenticatedUser caller) {
        return statusRepository
                .findFirstByUserSubjectAndNotificationPresentationModeAndAutoShownFalseOrderByNotificationCreatedAtAsc(
                        caller.subject(),
                        NotificationPresentationMode.MODAL_ONCE
                )
                .map(notificationMapper::toWithStatus)
                .orElse(null);
    }

    @Transactional
    public void markShown(AuthenticatedUser caller, UUID notificationId) {
        UserNotificationStatus status = getStatusForCaller(caller, notificationId);
        status.markShown(now());
    }

    @Transactional
    public void markRead(AuthenticatedUser caller, UUID notificationId) {
        UserNotificationStatus status = getStatusForCaller(caller, notificationId);
        status.markRead(now());
    }

    @Transactional
    public NotificationResponse create(AuthenticatedUser caller, CreateNotificationRequest request) {
        assertSuperAdmin(caller);
        List<String> recipients = resolveRecipients(request.target());
        String sanitizedBody = markdownSanitizer.sanitize(request.bodyMarkdown());
        Notification notification = new Notification(
                parseType(request.type()),
                request.title(),
                sanitizedBody,
                parsePresentationMode(request.presentationMode()),
                caller.subject()
        );
        Notification saved = notificationRepository.save(notification);
        saveStatuses(saved, recipients);
        return notificationMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> listAll(AuthenticatedUser caller) {
        assertSuperAdmin(caller);
        return notificationRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(notificationMapper::toResponse)
                .toList();
    }

    /**
     * Cria a notificacao interna {@code BELL_ONLY} de suspensao/reativacao de
     * tenant (Etapa 26, retrofit da etapa 09), com fan-out para os usuarios
     * ativos do tenant. Reaproveita a mesma persistencia/resolucao de
     * destinatarios de {@link #create}, sem o gate de {@code SUPER_ADMIN}
     * daquele metodo — o chamador ({@code TenantService}, via {@link
     * TenantStatusChangedEvent}) ja validou a propria operacao de atualizar o
     * tenant antes de chegar aqui.
     *
     * @param tenantId tenant cujo status mudou
     * @param transition direcao da mudanca (suspensao ou reativacao)
     * @param actorSubject subject de quem alterou o status do tenant
     */
    @Transactional
    public void notifyTenantStatusChange(UUID tenantId, TenantLifecycleTransition transition, String actorSubject) {
        doNotifyTenantStatusChange(tenantId, transition, actorSubject);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onTenantStatusChanged(TenantStatusChangedEvent event) {
        doNotifyTenantStatusChange(event.tenantId(), event.transition(), event.actorSubject());
    }

    /**
     * Logica real de {@link #notifyTenantStatusChange}, extraida para um metodo
     * privado nao-transacional para evitar self-invocation entre o metodo
     * publico e {@link #onTenantStatusChanged} (java:S6809) — mesmo padrao ja
     * usado em {@code ContentService.doTransition} (Sprint 11).
     */
    private void doNotifyTenantStatusChange(UUID tenantId, TenantLifecycleTransition transition, String actorSubject) {
        boolean suspended = transition == TenantLifecycleTransition.SUSPENDED;
        NotificationType type = suspended ? NotificationType.WARNING : NotificationType.GENERAL;
        String title = suspended ? TENANT_SUSPENDED_TITLE : TENANT_REACTIVATED_TITLE;
        String body = suspended ? TENANT_SUSPENDED_BODY : TENANT_REACTIVATED_BODY;

        List<String> recipients = distinct(tenantUserAccessService.listActiveUserSubjects(tenantId));
        Notification notification = new Notification(type, title, markdownSanitizer.sanitize(body),
                NotificationPresentationMode.BELL_ONLY, actorSubject);
        Notification saved = notificationRepository.save(notification);
        saveStatuses(saved, recipients);
    }

    @Transactional
    public void assignOnboarding(String userSubject) {
        Notification onboarding = notificationRepository.findFirstByType(NotificationType.ONBOARDING)
                .orElseThrow(OnboardingNotificationNotFoundException::new);
        if (statusRepository.existsByNotificationIdAndUserSubject(onboarding.getId(), userSubject)) {
            return;
        }
        statusRepository.save(new UserNotificationStatus(onboarding, userSubject));
    }

    private UserNotificationStatus getStatusForCaller(AuthenticatedUser caller, UUID notificationId) {
        return statusRepository.findByNotificationIdAndUserSubject(notificationId, caller.subject())
                .orElseThrow(NotificationNotFoundException::new);
    }

    private List<String> resolveRecipients(CreateNotificationRequest.Target target) {
        String targetType = normalizeTargetType(target.type());
        return switch (targetType) {
            case TARGET_ALL -> distinct(tenantUserAccessService.listActiveUserSubjects());
            case TARGET_TENANT -> resolveTenantRecipients(target);
            case TARGET_USERS -> resolveUserRecipients(target);
            default -> throw new InvalidNotificationTargetException();
        };
    }

    private List<String> resolveTenantRecipients(CreateNotificationRequest.Target target) {
        if (target.tenantId() == null) {
            throw new InvalidNotificationTargetException();
        }
        return distinct(tenantUserAccessService.listActiveUserSubjects(target.tenantId()));
    }

    private List<String> resolveUserRecipients(CreateNotificationRequest.Target target) {
        if (target.userIds() == null || target.userIds().isEmpty()) {
            throw new InvalidNotificationTargetException();
        }
        List<String> recipients = distinct(target.userIds());
        validateUsersExist(recipients);
        return recipients;
    }

    private void validateUsersExist(List<String> recipients) {
        for (String userSubject : recipients) {
            try {
                identityUserDirectory.getRequiredUser(userSubject);
            } catch (RuntimeException _) {
                throw new NotificationTargetUserNotFoundException();
            }
        }
    }

    private void saveStatuses(Notification notification, List<String> recipients) {
        Set<String> existing = Set.copyOf(statusRepository.findUserSubjectsByNotificationId(notification.getId()));
        recipients.stream()
                .filter(recipient -> !existing.contains(recipient))
                .map(recipient -> new UserNotificationStatus(notification, recipient))
                .forEach(statusRepository::save);
    }

    private void assertSuperAdmin(AuthenticatedUser caller) {
        if (!caller.authorities().contains(ROLE_SUPER_ADMIN)) {
            throw new InsufficientNotificationRoleException();
        }
    }

    private NotificationType parseType(String type) {
        return NotificationType.valueOf(String.valueOf(type).trim().toUpperCase(Locale.ROOT));
    }

    private NotificationPresentationMode parsePresentationMode(String presentationMode) {
        return NotificationPresentationMode.valueOf(String.valueOf(presentationMode).trim().toUpperCase(Locale.ROOT));
    }

    private String normalizeTargetType(String type) {
        return String.valueOf(type).trim().toUpperCase(Locale.ROOT);
    }

    private List<String> distinct(List<String> subjects) {
        return new LinkedHashSet<>(subjects.stream().filter(Objects::nonNull).toList()).stream().toList();
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}
