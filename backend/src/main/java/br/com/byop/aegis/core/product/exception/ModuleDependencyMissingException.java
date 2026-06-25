package br.com.byop.aegis.core.product.exception;

import br.com.byop.aegis.core.product.ModuleKey;

public class ModuleDependencyMissingException extends RuntimeException {

    private final ModuleKey moduleKey;
    private final ModuleKey requires;

    public ModuleDependencyMissingException(ModuleKey moduleKey, ModuleKey requires) {
        super("Module " + moduleKey + " requires module " + requires);
        this.moduleKey = moduleKey;
        this.requires = requires;
    }

    public ModuleKey getModuleKey() {
        return moduleKey;
    }

    public ModuleKey getRequires() {
        return requires;
    }
}
