package br.com.byop.aegis.product.service;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class StubProductAssignmentNotificationPort implements ProductAssignmentNotificationPort {

    @Override
    public void notifyAssignment(UUID tenantId, UUID productId, String userSubject) {
        // TODO Sprint 24: substituir stub pela integração oficial com NotificationService.
    }

    @Override
    public void notifyRevocation(UUID tenantId, UUID productId, String userSubject) {
        // TODO Sprint 24: substituir stub pela integração oficial com NotificationService.
    }
}
