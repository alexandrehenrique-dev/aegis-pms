package br.com.byop.aegis.content.service;

import br.com.byop.aegis.content.domain.ContentStatus;
import br.com.byop.aegis.content.exception.InsufficientContentRoleException;
import br.com.byop.aegis.content.exception.InvalidContentTransitionException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Maquina de estados do workflow editorial de {@link br.com.byop.aegis.content.domain.Content}
 * e regra de autorizacao de publicacao (Sprint 11).
 */
@Component
public class ContentWorkflowPolicy {

    private static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    private static final String ROLE_TENANT_ADMIN = "ROLE_TENANT_ADMIN";
    private static final String ROLE_PRODUCT_MANAGER = "ROLE_PRODUCT_MANAGER";

    private static final Map<ContentStatus, Set<ContentStatus>> ALLOWED_TRANSITIONS = Map.of(
            ContentStatus.DRAFT, Set.of(ContentStatus.IN_REVIEW),
            ContentStatus.IN_REVIEW, Set.of(ContentStatus.DRAFT, ContentStatus.PUBLISHED),
            ContentStatus.PUBLISHED, Set.of(ContentStatus.ARCHIVED),
            ContentStatus.ARCHIVED, Set.of(ContentStatus.DRAFT)
    );

    /**
     * Garante que a transicao de {@code from} para {@code to} e permitida pela
     * maquina de estados editorial e, quando o destino e {@code PUBLISHED}, que o
     * chamador tem um dos papeis autorizados a publicar.
     *
     * @param from status atual do conteudo
     * @param to status de destino solicitado
     * @param authorities authorities do chamador (formato {@code ROLE_<NOME>})
     * @throws InvalidContentTransitionException se a transicao nao for permitida pela maquina de estados
     * @throws InsufficientContentRoleException se o destino for {@code PUBLISHED} e o chamador nao tiver papel autorizado
     */
    public void assertAllowedTransition(ContentStatus from, ContentStatus to, Set<String> authorities) {
        if (!ALLOWED_TRANSITIONS.getOrDefault(from, Set.of()).contains(to)) {
            throw new InvalidContentTransitionException(from, to);
        }
        if (to == ContentStatus.PUBLISHED && !canPublish(authorities)) {
            throw new InsufficientContentRoleException();
        }
    }

    private boolean canPublish(Set<String> authorities) {
        return authorities.contains(ROLE_SUPER_ADMIN)
                || authorities.contains(ROLE_TENANT_ADMIN)
                || authorities.contains(ROLE_PRODUCT_MANAGER);
    }
}
