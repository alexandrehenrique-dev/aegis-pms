package br.com.byop.aegis.product.mapper;

import br.com.byop.aegis.product.domain.Product;
import br.com.byop.aegis.product.dto.ProductDetail;
import br.com.byop.aegis.product.dto.ProductModuleSummary;
import br.com.byop.aegis.product.dto.ProductSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    ProductSummary toSummary(Product product);

    @Mapping(target = "modules", source = "modules")
    ProductDetail toDetail(Product product, List<ProductModuleSummary> modules);
}
