package br.com.byop.aegis.core;

import br.com.byop.aegis.product.api.AssetStorageStrategy;
import br.com.byop.aegis.product.domain.Product;
import br.com.byop.aegis.product.domain.ProductTypeKey;
import br.com.byop.aegis.tenant.domain.Tenant;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class RepositoryTestSupport {

    protected Tenant tenant(String suffix) {
        return new Tenant("tenant-" + suffix, "Tenant " + suffix);
    }

    protected Product product(Tenant tenant, String suffix) {
        return new Product(
                tenant.getId(),
                "product-" + suffix,
                "Product " + suffix,
                ProductTypeKey.SITE_INSTITUCIONAL,
                "pt-BR",
                AssetStorageStrategy.LOCAL
        );
    }
}
