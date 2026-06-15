# docs/operations — Operação, Observabilidade e Continuidade

## Propósito

Documentação **operacional** do Aegis: observabilidade, jobs assíncronos, runbooks, disaster recovery, backup e comunicação operacional.

## Conteúdo esperado

- **Observabilidade**: logs, métricas, traces, alertas, dashboards, retenção e cardinalidade.
- **Jobs e processamento assíncrono**: filas, idempotência, jobs tenant-aware.
- **Notificações operacionais**: Telegram como canal (não domínio — ver `adr/`), e-mail, in-app.
- **Backup e retenção**: políticas, RPO/RTO, restore testado.
- **Disaster Recovery**: continuidade operacional e sobrevivência da plataforma.
- **Runbooks**: procedimentos para incidentes recorrentes.

## Regras

- Todo job que processa dados tenant-owned deve receber `tenantId` explicitamente ou iterar por tenants com isolamento por lote.
- Logs devem incluir tenant quando houver contexto e nunca vazar dados sensíveis.
- Observabilidade deve ter owner e padrões.
- Incidente crítico pode gerar ADR e revisão arquitetural (post mortem).
- Telegram é provider operacional substituível (Slack, Discord, e-mail) sem quebrar domínio.
