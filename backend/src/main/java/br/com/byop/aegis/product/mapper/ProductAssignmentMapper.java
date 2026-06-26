package br.com.byop.aegis.product.mapper;

import br.com.byop.aegis.product.domain.ProductAssignment;
import br.com.byop.aegis.product.domain.ProductAssignmentRole;
import br.com.byop.aegis.product.domain.ProductAssignmentStatus;
import br.com.byop.aegis.product.dto.ProductAssignmentSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductAssignmentMapper {

    @Mapping(target = "productName", source = "assignment.product.name")
    @Mapping(target = "userName", source = "userName")
    @Mapping(target = "userEmail", source = "userEmail")
    @Mapping(target = "role", expression = "java(assignment == null ? null : toContractRole(assignment.getRole()))")
    @Mapping(target = "status", expression = "java(assignment == null ? null : toContractStatus(assignment.getStatus()))")
    ProductAssignmentSummary toSummary(ProductAssignment assignment, String userName, String userEmail);

    default String toContractStatus(ProductAssignmentStatus status) {
        if (status == null) {
            return null;
        }
        return status.contractValue();
    }

    default String toContractRole(ProductAssignmentRole role) {
        if (role == null) {
            return null;
        }
        return role.name();
    }
}
