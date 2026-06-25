package br.com.byop.aegis.product.api;

import br.com.byop.aegis.security.AuthenticatedUser;

import java.util.UUID;

public interface ProductAccessPort {

    void assertAccessible(UUID productId, AuthenticatedUser user);
}
