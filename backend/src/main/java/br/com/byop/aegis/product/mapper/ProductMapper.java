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

    /**
     * Método escrito à mão (não gerado pelo MapStruct): um mapeamento auto-gerado
     * com único parâmetro nullable produz um segundo "if (product != null)"
     * defensivo logicamente inatingível após o guard inicial — branch morto que
     * o JaCoCo sempre reporta como não coberto. Evitar a geração automática aqui
     * elimina esse branch em vez de mascará-lo no gate de cobertura.
     */
    default ProductSummary toSummary(Product product, int enabledModuleCount, List<String> enabledModules, String callerAssignedRole) {
        if (product == null) {
            return null;
        }
        return new ProductSummary(
                product.getId(),
                product.getTenantId(),
                product.getKey(),
                product.getName(),
                product.getType(),
                product.getStatus(),
                product.getDefaultLocale(),
                product.getAssetStorageStrategy(),
                product.getCreatedAt(),
                product.getUpdatedAt(),
                enabledModuleCount,
                enabledModules,
                callerAssignedRole
        );
    }

    @Mapping(target = "modules", source = "modules")
    ProductDetail toDetail(Product product, List<ProductModuleSummary> modules);
}
