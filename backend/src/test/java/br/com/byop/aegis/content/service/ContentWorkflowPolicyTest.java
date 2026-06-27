package br.com.byop.aegis.content.service;

import br.com.byop.aegis.content.domain.ContentStatus;
import br.com.byop.aegis.content.exception.InsufficientContentRoleException;
import br.com.byop.aegis.content.exception.InvalidContentTransitionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContentWorkflowPolicyTest {

    private final ContentWorkflowPolicy policy = new ContentWorkflowPolicy();

    @ParameterizedTest
    @CsvSource({
            "DRAFT, IN_REVIEW",
            "IN_REVIEW, DRAFT",
            "PUBLISHED, ARCHIVED",
            "ARCHIVED, DRAFT"
    })
    void shouldAllowDocumentedTransitionsForNonPublishRoles(ContentStatus from, ContentStatus to) {
        Set<String> editorAuthorities = Set.of("ROLE_EDITOR");

        assertThatCode(() -> policy.assertAllowedTransition(from, to, editorAuthorities))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({
            "ROLE_SUPER_ADMIN",
            "ROLE_TENANT_ADMIN",
            "ROLE_PRODUCT_MANAGER"
    })
    void shouldAllowPublishingForAuthorizedRoles(String role) {
        Set<String> authorities = Set.of(role);

        assertThatCode(() -> policy.assertAllowedTransition(ContentStatus.IN_REVIEW, ContentStatus.PUBLISHED, authorities))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({
            "ROLE_EDITOR",
            "ROLE_VIEWER"
    })
    void shouldRejectPublishingForUnauthorizedRoles(String role) {
        Set<String> authorities = Set.of(role);

        assertThatThrownBy(() -> policy.assertAllowedTransition(ContentStatus.IN_REVIEW, ContentStatus.PUBLISHED, authorities))
                .isInstanceOf(InsufficientContentRoleException.class);
    }

    @ParameterizedTest
    @CsvSource({
            "DRAFT, PUBLISHED",
            "DRAFT, ARCHIVED",
            "PUBLISHED, DRAFT",
            "PUBLISHED, IN_REVIEW",
            "ARCHIVED, IN_REVIEW",
            "ARCHIVED, PUBLISHED",
            "IN_REVIEW, ARCHIVED"
    })
    void shouldRejectUndocumentedTransitions(ContentStatus from, ContentStatus to) {
        Set<String> superAdminAuthorities = Set.of("ROLE_SUPER_ADMIN");

        assertThatThrownBy(() -> policy.assertAllowedTransition(from, to, superAdminAuthorities))
                .isInstanceOf(InvalidContentTransitionException.class);
    }

    @Test
    void shouldRejectSameStatusTransition() {
        Set<String> superAdminAuthorities = Set.of("ROLE_SUPER_ADMIN");

        assertThatThrownBy(() -> policy.assertAllowedTransition(ContentStatus.DRAFT, ContentStatus.DRAFT, superAdminAuthorities))
                .isInstanceOf(InvalidContentTransitionException.class);
    }
}
