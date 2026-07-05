package br.com.byop.aegis.product.service;

import br.com.byop.aegis.identity.api.IdentityUserInviteActivatedEvent;
import br.com.byop.aegis.product.domain.ProductAssignmentStatus;
import br.com.byop.aegis.product.repository.ProductAssignmentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ouve {@link IdentityUserInviteActivatedEvent} e transiciona todos os
 * {@code ProductAssignment} do usuário de {@code INVITED} para {@code ASSIGNED}.
 * <p>
 * Separado de {@code ProductAssignmentService} para evitar dependência circular
 * com o módulo {@code identity} — o evento traversa a fronteira de módulo
 * via {@code ApplicationEventPublisher}, mantendo o fluxo de ativação no
 * módulo correto.
 */
@Slf4j
@Component
class ProductAssignmentActivationListener {

    private final ProductAssignmentRepository assignmentRepository;

    ProductAssignmentActivationListener(ProductAssignmentRepository assignmentRepository) {
        this.assignmentRepository = assignmentRepository;
    }

    @EventListener
    @Transactional
    public void onUserInviteActivated(IdentityUserInviteActivatedEvent event) {
        log.debug("onUserInviteActivated: ativando assignments para keycloakId='{}'", event.keycloakId());
        var pending = assignmentRepository.findAllByUserSubjectAndStatus(
                event.keycloakId(),
                ProductAssignmentStatus.INVITED
        );
        pending.forEach(assignment -> {
            assignment.assign();
            assignmentRepository.save(assignment);
            log.info("onUserInviteActivated: assignment id='{}' transitado para ASSIGNED", assignment.getId());
        });
    }
}
