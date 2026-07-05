package br.com.byop.aegis.product.service;

import br.com.byop.aegis.identity.api.IdentityUserInviteActivatedEvent;
import br.com.byop.aegis.product.api.AssetStorageStrategy;
import br.com.byop.aegis.product.domain.Product;
import br.com.byop.aegis.product.domain.ProductAssignment;
import br.com.byop.aegis.product.domain.ProductAssignmentRole;
import br.com.byop.aegis.product.domain.ProductAssignmentStatus;
import br.com.byop.aegis.product.domain.ProductTypeKey;
import br.com.byop.aegis.product.repository.ProductAssignmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductAssignmentActivationListenerTest {

    @Mock
    private ProductAssignmentRepository assignmentRepository;

    @InjectMocks
    private ProductAssignmentActivationListener listener;

    private static final String KEYCLOAK_ID = "user-keycloak-id";
    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void shouldDoNothingWhenNoInvitedAssignmentsExist() {
        when(assignmentRepository.findAllByUserSubjectAndStatus(KEYCLOAK_ID, ProductAssignmentStatus.INVITED))
                .thenReturn(List.of());

        listener.onUserInviteActivated(new IdentityUserInviteActivatedEvent(KEYCLOAK_ID));

        verify(assignmentRepository).findAllByUserSubjectAndStatus(KEYCLOAK_ID, ProductAssignmentStatus.INVITED);
        verifyNoMoreInteractions(assignmentRepository);
    }

    @Test
    void shouldTransitionSingleInvitedAssignmentToAssigned() {
        ProductAssignment assignment = invitedAssignment();
        when(assignmentRepository.findAllByUserSubjectAndStatus(KEYCLOAK_ID, ProductAssignmentStatus.INVITED))
                .thenReturn(List.of(assignment));

        listener.onUserInviteActivated(new IdentityUserInviteActivatedEvent(KEYCLOAK_ID));

        assertThat(assignment.getStatus()).isEqualTo(ProductAssignmentStatus.ASSIGNED);
        verify(assignmentRepository).save(assignment);
    }

    @Test
    void shouldTransitionAllInvitedAssignmentsWhenUserHasMultiple() {
        ProductAssignment first  = invitedAssignment();
        ProductAssignment second = invitedAssignment();
        when(assignmentRepository.findAllByUserSubjectAndStatus(KEYCLOAK_ID, ProductAssignmentStatus.INVITED))
                .thenReturn(List.of(first, second));

        listener.onUserInviteActivated(new IdentityUserInviteActivatedEvent(KEYCLOAK_ID));

        assertThat(first.getStatus()).isEqualTo(ProductAssignmentStatus.ASSIGNED);
        assertThat(second.getStatus()).isEqualTo(ProductAssignmentStatus.ASSIGNED);
        verify(assignmentRepository).save(first);
        verify(assignmentRepository).save(second);
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private ProductAssignment invitedAssignment() {
        Product product = new Product(
                TENANT_ID,
                "aegis-pms",
                "Aegis PMS",
                ProductTypeKey.SITE_INSTITUCIONAL,
                "pt-BR",
                AssetStorageStrategy.LOCAL
        );
        ProductAssignment assignment = new ProductAssignment(product, KEYCLOAK_ID, ProductAssignmentRole.EDITOR);
        // revoke() transiciona para INVITED (comportamento da entidade)
        assignment.revoke();
        assertThat(assignment.getStatus()).isEqualTo(ProductAssignmentStatus.INVITED);
        return assignment;
    }
}
