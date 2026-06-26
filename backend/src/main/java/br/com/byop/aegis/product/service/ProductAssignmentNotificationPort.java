package br.com.byop.aegis.product.service;

import java.util.UUID;

public interface ProductAssignmentNotificationPort {

    void notifyAssignment(UUID tenantId, UUID productId, String userSubject);

    void notifyRevocation(UUID tenantId, UUID productId, String userSubject);
}
