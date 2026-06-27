package br.com.byop.aegis.product.service;

/**
 * Implementacao no-op de {@link ProductAssignmentEmailPort}, usada apenas em
 * testes/ambientes sem {@code JavaMailSender} oficial configurado — nunca bean
 * principal em producao (ver {@link FreemarkerProductAssignmentEmailPort}).
 */
public class StubProductAssignmentEmailPort implements ProductAssignmentEmailPort {

    @Override
    public void notifyAssignment(ProductAssignmentEmailCommand command) {
        // implementacao intencionalmente vazia — ver Javadoc da classe
    }

    @Override
    public void notifyRevocation(ProductAssignmentEmailCommand command) {
        // implementacao intencionalmente vazia — ver Javadoc da classe
    }
}
