package br.com.byop.aegis.audit.api;

import java.util.UUID;

/**
 * Porta que o modulo {@code audit} usa para resolver visibilidade/nome de
 * tenant sem depender diretamente de {@code tenant.api} — a implementacao
 * real vive no modulo {@code tenant} (que ja depende de {@code audit.api}
 * para gravar eventos); inverter a dependencia aqui evita o ciclo
 * {@code audit -> tenant -> audit} que o Spring Modulith rejeitaria.
 */
public interface TenantVisibilityPort {

    /**
     * Verifica se o subject possui membership ativa no tenant informado.
     *
     * @param tenantId tenant a verificar
     * @param userSubject subject do usuario autenticado
     * @return {@code true} se houver membership ativa
     */
    boolean hasActiveMembership(UUID tenantId, String userSubject);

    /**
     * Resolve o nome de exibicao do tenant.
     *
     * @param tenantId tenant a resolver
     * @return o nome do tenant, ou {@code null} se o tenant nao existir
     *         (ex.: tenant ja excluido — a trilha de auditoria deve
     *         continuar legivel mesmo assim)
     */
    String findTenantName(UUID tenantId);
}
