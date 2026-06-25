package br.com.byop.aegis.product.exception;

import br.com.byop.aegis.product.api.ModuleKey;

public class ModuleDisabledException extends RuntimeException {

    private final ModuleKey moduleKey;

    public ModuleDisabledException(ModuleKey moduleKey) {
        super("Module disabled: " + moduleKey);
        this.moduleKey = moduleKey;
    }

    public ModuleKey getModuleKey() {
        return moduleKey;
    }
}
