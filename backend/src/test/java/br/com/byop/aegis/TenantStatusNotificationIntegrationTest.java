package br.com.byop.aegis;

import br.com.byop.aegis.notification.domain.Notification;
import br.com.byop.aegis.notification.domain.NotificationPresentationMode;
import br.com.byop.aegis.notification.domain.NotificationType;
import br.com.byop.aegis.notification.domain.UserNotificationStatus;
import br.com.byop.aegis.notification.repository.NotificationRepository;
import br.com.byop.aegis.notification.repository.UserNotificationStatusRepository;
import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.tenant.command.CreateTenantCommand;
import br.com.byop.aegis.tenant.contract.UpdateTenantRequest;
import br.com.byop.aegis.tenant.dto.TenantSummary;
import br.com.byop.aegis.tenant.service.TenantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ponta a ponta (Spring context real + Postgres real, Etapa 26): {@code
 * PUT /tenants/{tenantId}} (via {@code TenantService.updateTenant}) cria uma
 * notificacao interna {@code BELL_ONLY} para os usuarios do tenant quando o
 * status transiciona entre ativo e suspenso, via {@code TenantStatusChangedEvent}
 * + {@code NotificationService}, ambos reais — dentro da mesma transacao.
 */
@SpringBootTest
class TenantStatusNotificationIntegrationTest {

    @Autowired
    private TenantService tenantService;

    @Autowired
    private UserNotificationStatusRepository statusRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void shouldNotifyTenantMembersOnSuspensionAndReactivation() {
        String suffix = UUID.randomUUID().toString();
        String adminSubject = "admin-" + suffix;
        AuthenticatedUser caller = caller(adminSubject);
        TenantSummary tenant = tenantService.createTenant(caller,
                new CreateTenantCommand("tenant-" + suffix, "Tenant " + suffix, "FREE", null));

        tenantService.updateTenant(caller, tenant.id(), new UpdateTenantRequest("Tenant " + suffix, "FREE", "suspenso"));

        List<UserNotificationStatus> afterSuspension =
                statusRepository.findAllByUserSubjectOrderByNotificationCreatedAtDesc(adminSubject);
        assertThat(afterSuspension).hasSize(1);
        Notification suspensionNotification = notificationRepository.findById(afterSuspension.get(0).getNotification().getId())
                .orElseThrow();
        assertThat(suspensionNotification.getType()).isEqualTo(NotificationType.WARNING);
        assertThat(suspensionNotification.getPresentationMode()).isEqualTo(NotificationPresentationMode.BELL_ONLY);
        assertThat(suspensionNotification.getCreatedBySubject()).isEqualTo(adminSubject);

        tenantService.updateTenant(caller, tenant.id(), new UpdateTenantRequest("Tenant " + suffix, "FREE", "ativo"));

        List<UserNotificationStatus> afterReactivation =
                statusRepository.findAllByUserSubjectOrderByNotificationCreatedAtDesc(adminSubject);
        assertThat(afterReactivation).hasSize(2);
        Notification reactivationNotification = notificationRepository.findById(afterReactivation.get(0).getNotification().getId())
                .orElseThrow();
        assertThat(reactivationNotification.getType()).isEqualTo(NotificationType.GENERAL);
        assertThat(reactivationNotification.getPresentationMode()).isEqualTo(NotificationPresentationMode.BELL_ONLY);
    }

    @Test
    void shouldNotNotifyWhenStatusDoesNotChange() {
        String suffix = UUID.randomUUID().toString();
        String adminSubject = "admin-" + suffix;
        AuthenticatedUser caller = caller(adminSubject);
        TenantSummary tenant = tenantService.createTenant(caller,
                new CreateTenantCommand("tenant-" + suffix, "Tenant " + suffix, "FREE", null));

        tenantService.updateTenant(caller, tenant.id(), new UpdateTenantRequest("Tenant " + suffix, "PRO", "ativo"));

        assertThat(statusRepository.findAllByUserSubjectOrderByNotificationCreatedAtDesc(adminSubject)).isEmpty();
    }

    private AuthenticatedUser caller(String subject) {
        return new AuthenticatedUser(subject, subject + "@byop.dev", subject, subject, Set.of("ROLE_TENANT_ADMIN"));
    }
}
