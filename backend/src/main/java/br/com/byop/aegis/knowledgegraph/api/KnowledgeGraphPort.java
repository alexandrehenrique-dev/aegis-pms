package br.com.byop.aegis.knowledgegraph.api;

import java.util.UUID;

/**
 * Porta publica do Knowledge Graph (Spring Modulith {@code NamedInterface}) — outros
 * modulos (ex.: {@code content}) integram com o grafo exclusivamente por esta
 * interface, nunca importando entidades/repositories/services internos do pacote
 * {@code knowledgegraph}.
 */
public interface KnowledgeGraphPort {

    /**
     * Verifica se um node existe dentro de um produto.
     *
     * @param productId identificador do produto proprietario
     * @param nodeId identificador do node
     * @return {@code true} se o node existir no produto informado
     */
    boolean nodeExists(UUID productId, UUID nodeId);

    /**
     * Garante (cria se ainda nao existir) um node de conteudo para a referencia
     * informada e devolve o identificador do node — operacao idempotente.
     *
     * @param productId identificador do produto proprietario
     * @param refId identificador da entidade de origem (ex.: id do {@code Content})
     * @param label rotulo legivel do node
     * @return identificador do node garantido
     */
    UUID ensureContentNode(UUID productId, String refId, String label);

    /**
     * Garante (idempotente) uma relacao {@code RELATED_TO} entre dois nodes do
     * mesmo produto — chamadas repetidas com o mesmo par de nodes nao duplicam a edge.
     *
     * @param productId identificador do produto proprietario
     * @param sourceNodeId node de origem da relacao
     * @param targetNodeId node de destino da relacao
     */
    void ensureRelatedToEdge(UUID productId, UUID sourceNodeId, UUID targetNodeId);
}
