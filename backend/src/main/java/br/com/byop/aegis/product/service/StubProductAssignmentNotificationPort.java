package br.com.byop.aegis.product.service;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Implementacao no-op de {@link ProductAssignmentNotificationPort}.
 *
 * <p>Retrofit pendente (Sprint 24): substituir por integracao oficial com o
 * NotificationService quando ele existir — ver {@code SPRINT-RESULTADO.md}.
 */
@Component
public class StubProductAssignmentNotificationPort implements ProductAssignmentNotificationPort {

    @Override
    public void notifyAssignment(UUID tenantId, UUID productId, String userSubject) {
        // implementacao intencionalmente vazia — ver Javadoc da classe
    }

    @Override
    public void notifyRevocation(UUID tenantId, UUID productId, String userSubject) {
        // implementacao intencionalmente vazia — ver Javadoc da classe
    }
}
