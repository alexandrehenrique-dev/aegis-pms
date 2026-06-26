package br.com.byop.aegis.product.service;

public class StubProductAssignmentEmailPort implements ProductAssignmentEmailPort {

    @Override
    public void notifyAssignment(ProductAssignmentEmailCommand command) {
        // TODO Sprint futura: usar apenas em testes/ambientes sem JavaMailSender oficial configurado.
    }

    @Override
    public void notifyRevocation(ProductAssignmentEmailCommand command) {
        // TODO Sprint futura: usar apenas em testes/ambientes sem JavaMailSender oficial configurado.
    }
}
