package br.com.byop.aegis.content.exception;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ContentExceptionHandlerTest {

    private final ContentExceptionHandler handler = new ContentExceptionHandler();

    @Test
    void shouldReturnContentErrorCodes() {
        assertThat(handler.handleContentNotFound().error()).isEqualTo("CONTENT_NOT_FOUND");
        assertThat(handler.handleInvalidContentTransition().error()).isEqualTo("INVALID_CONTENT_TRANSITION");
        assertThat(handler.handleInsufficientContentRole().error()).isEqualTo("CONTENT_PUBLISH_FORBIDDEN");
        assertThat(handler.handleInvalidContentStatus().error()).isEqualTo("INVALID_CONTENT_STATUS");
        assertThat(handler.handleInvalidDifficultyLevel().error()).isEqualTo("INVALID_DIFFICULTY_LEVEL");
        assertThat(handler.handleInvalidContentReference().error()).isEqualTo("INVALID_KG_REFERENCE");
        assertThat(handler.handleDuplicateContentTitle().error()).isEqualTo("CONTENT_TITLE_ALREADY_EXISTS");
    }

    @Test
    void shouldExposeExceptionMessages() {
        UUID contentId = UUID.randomUUID();
        assertThat(new ContentNotFoundException(contentId)).hasMessage("Content not found: " + contentId);
        assertThat(new InvalidContentStatusException("unknown")).hasMessage("Invalid content status: unknown");
        assertThat(new InvalidDifficultyLevelException("unknown")).hasMessage("Invalid difficulty level: unknown");
        assertThat(new InvalidContentReferenceException("node-1"))
                .hasMessage("Knowledge graph node referenced by kg-ref does not exist: node-1");
        assertThat(new InsufficientContentRoleException())
                .hasMessage("Only SUPER_ADMIN, TENANT_ADMIN or PRODUCT_MANAGER can publish content");
        assertThat(new DuplicateContentTitleException("Artigo novo"))
                .hasMessage("Content title already exists in this product: Artigo novo");
    }
}
