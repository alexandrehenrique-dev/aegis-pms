package br.com.byop.aegis.product.exception;

import java.lang.reflect.Method;

public class ModuleProductIdMissingException extends RuntimeException {

    public ModuleProductIdMissingException(Method method) {
        super("@RequireModule method must declare @PathVariable(\"productId\") UUID productId: "
                + method.getDeclaringClass().getName() + "#" + method.getName());
    }
}
