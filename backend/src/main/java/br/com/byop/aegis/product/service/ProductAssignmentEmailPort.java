package br.com.byop.aegis.product.service;

public interface ProductAssignmentEmailPort {

    void notifyAssignment(ProductAssignmentEmailCommand command);

    void notifyRevocation(ProductAssignmentEmailCommand command);
}
