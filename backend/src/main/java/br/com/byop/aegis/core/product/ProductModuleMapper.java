package br.com.byop.aegis.core.product;

import br.com.byop.aegis.core.product.dto.ProductModuleSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductModuleMapper {

    @Mapping(target = "productId", source = "product.id")
    ProductModuleSummary toSummary(ProductModule productModule);
}
