package br.com.byop.aegis.audit.repository;

import br.com.byop.aegis.audit.domain.AuditEvent;
import br.com.byop.aegis.audit.domain.AuditRisk;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de {@link AuditEvent}, sempre escopado por tenant. A trilha de
 * auditoria nao possui chave estrangeira para {@code tenants}/{@code products}
 * (ver migration {@code V8__audit_trail_normalization.sql}) — precisa
 * sobreviver a exclusao do tenant ou produto que ela audita.
 */
@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

    /**
     * Lista os eventos de um tenant com filtros opcionais combinaveis,
     * ordenados do mais recente para o mais antigo. Um filtro {@code null}
     * nao restringe o resultado naquela dimensao.
     *
     * @param tenantId tenant ao qual os eventos devem pertencer (escopo obrigatorio)
     * @param actorSubject subject do ator que originou o evento, ou {@code null} para nao filtrar
     * @param productId produto relacionado ao evento, ou {@code null} para nao filtrar
     * @param module modulo relacionado ao evento, ou {@code null} para nao filtrar
     * @param risk nivel de risco do evento, ou {@code null} para nao filtrar
     * @return eventos do tenant que casam com todos os filtros informados
     */
    @Query("""
            SELECT e FROM AuditEvent e
            WHERE e.tenantId = :tenantId
              AND (:actorSubject IS NULL OR e.actorSubject = :actorSubject)
              AND (:productId IS NULL OR e.productId = :productId)
              AND (:module IS NULL OR e.module = :module)
              AND (:risk IS NULL OR e.risk = :risk)
            ORDER BY e.createdAt DESC
            """)
    List<AuditEvent> findAllByFilters(@Param("tenantId") UUID tenantId,
                                       @Param("actorSubject") String actorSubject,
                                       @Param("productId") UUID productId,
                                       @Param("module") String module,
                                       @Param("risk") AuditRisk risk);

    @Query("""
            SELECT e FROM AuditEvent e
            WHERE e.tenantId = :tenantId
              AND (:actorSubject IS NULL OR e.actorSubject = :actorSubject)
              AND (:productId IS NULL OR e.productId = :productId)
              AND (:module IS NULL OR e.module = :module)
              AND (:risk IS NULL OR e.risk = :risk)
            ORDER BY e.createdAt DESC
            """)
    Page<AuditEvent> findPageByFilters(@Param("tenantId") UUID tenantId,
                                        @Param("actorSubject") String actorSubject,
                                        @Param("productId") UUID productId,
                                        @Param("module") String module,
                                        @Param("risk") AuditRisk risk,
                                        Pageable pageable);

    @Query("""
            SELECT e FROM AuditEvent e
            WHERE e.tenantId = :tenantId
              AND (:actorSubject IS NULL OR e.actorSubject = :actorSubject)
              AND (:productId IS NULL OR e.productId = :productId)
              AND (:module IS NULL OR e.module = :module)
              AND (:risk IS NULL OR e.risk = :risk)
              AND (LOWER(e.actorSubject) LIKE :queryPattern OR
                   LOWER(e.action) LIKE :queryPattern OR
                   LOWER(COALESCE(e.targetLabel, '')) LIKE :queryPattern OR
                   LOWER(COALESCE(e.targetType, '')) LIKE :queryPattern OR
                   LOWER(COALESCE(e.module, '')) LIKE :queryPattern)
            ORDER BY e.createdAt DESC
            """)
    Page<AuditEvent> findPageByFiltersAndQuery(@Param("tenantId") UUID tenantId,
                                        @Param("actorSubject") String actorSubject,
                                        @Param("productId") UUID productId,
                                        @Param("module") String module,
                                        @Param("risk") AuditRisk risk,
                                        @Param("queryPattern") String queryPattern,
                                        Pageable pageable);

    /**
     * Busca um evento pelo identificador, restrito ao tenant informado —
     * nunca permite acessar um evento de outro tenant a partir do id isolado.
     *
     * @param id identificador do evento
     * @param tenantId tenant ao qual o evento deve pertencer
     * @return o evento encontrado, ou {@link Optional#empty()} se nao existir
     *         ou pertencer a outro tenant
     */
    Optional<AuditEvent> findByIdAndTenantId(UUID id, UUID tenantId);
}
