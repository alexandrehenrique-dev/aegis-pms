package br.com.byop.aegis.content.mapper;

import br.com.byop.aegis.content.domain.Content;
import br.com.byop.aegis.content.domain.ContentStatus;
import br.com.byop.aegis.content.domain.DifficultyLevel;
import br.com.byop.aegis.content.dto.ContentSummary;
import br.com.byop.aegis.content.dto.WorkflowItemSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Map;

@Mapper(componentModel = "spring")
public interface ContentMapper {

    String NO_PUBLICATION_PLACEHOLDER = "—";

    @Mapping(target = "author", source = "authorName")
    @Mapping(target = "status", expression = "java(content == null ? null : toContractStatus(content.getStatus()))")
    @Mapping(target = "difficultyLevel", expression = "java(content == null ? null : toContractDifficulty(content.getDifficultyLevel()))")
    @Mapping(target = "version", expression = "java(content == null ? null : \"v\" + content.getCurrentVersion())")
    @Mapping(target = "publication", expression = "java(content == null ? null : toContractPublication(content.getPublication()))")
    @Mapping(target = "body", source = "content.bodyMarkdown")
    @Mapping(target = "metadata", source = "metadata")
    ContentSummary toSummary(Content content, String authorName, Map<String, Object> metadata);

    @Mapping(target = "author", source = "authorName")
    @Mapping(target = "status", expression = "java(content == null ? null : toContractStatus(content.getStatus()))")
    @Mapping(target = "version", expression = "java(content == null ? null : \"v\" + content.getCurrentVersion())")
    WorkflowItemSummary toWorkflowItem(Content content, String authorName);

    @Named("toContractStatus")
    default String toContractStatus(ContentStatus status) {
        return status == null ? null : status.contractValue();
    }

    @Named("toContractDifficulty")
    default String toContractDifficulty(DifficultyLevel level) {
        return level == null ? null : level.contractValue();
    }

    /**
     * Reservado para o campo {@code publication} — anotado {@code @Named} para que o
     * MapStruct nunca o auto-selecione como conversor generico de qualquer outro
     * campo {@code String -> String} sem mapeamento explicito (ex.: {@code title},
     * {@code body}, {@code author}), o que substituiria {@code null} por "—" indevidamente.
     */
    @Named("toContractPublication")
    default String toContractPublication(String publication) {
        return publication == null ? NO_PUBLICATION_PLACEHOLDER : publication;
    }
}
