package br.com.byop.aegis.core.product;

import br.com.byop.aegis.core.product.dto.ProductDetail;
import br.com.byop.aegis.core.product.dto.ProductModuleSummary;
import br.com.byop.aegis.core.product.dto.ProductSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "tenantId", source = "tenant.id")
    ProductSummary toSummary(Product product);

    @Mapping(target = "tenantId", source = "product.tenant.id")
    @Mapping(target = "modules", source = "modules")
    ProductDetail toDetail(Product product, List<ProductModuleSummary> modules);
}
