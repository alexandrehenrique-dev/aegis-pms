package br.com.byop.aegis.product.mapper;

import br.com.byop.aegis.product.domain.ProductModule;
import br.com.byop.aegis.product.dto.ProductModuleSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductModuleMapper {

    @Mapping(target = "productId", source = "product.id")
    ProductModuleSummary toSummary(ProductModule productModule);
}
