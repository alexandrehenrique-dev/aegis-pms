package br.com.byop.aegis.core.product;

import br.com.byop.aegis.core.product.exception.ModuleDisabledException;
import br.com.byop.aegis.core.product.exception.ModuleProductIdMissingException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.UUID;

@Aspect
@Component
@EnableAspectJAutoProxy
public class ModuleAccessAspect {

    private final ProductModuleRepository moduleRepository;

    public ModuleAccessAspect(ProductModuleRepository moduleRepository) {
        this.moduleRepository = moduleRepository;
    }

    @Around("@annotation(requireModule)")
    public Object requireEnabledModule(ProceedingJoinPoint joinPoint, RequireModule requireModule) throws Throwable {
        UUID productId = extractProductId(joinPoint);
        ModuleKey moduleKey = requireModule.value();

        if (!moduleRepository.existsByProductIdAndModuleKeyAndEnabledTrue(productId, moduleKey)) {
            throw new ModuleDisabledException(moduleKey);
        }

        return joinPoint.proceed();
    }

    private UUID extractProductId(ProceedingJoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();

        for (int index = 0; index < parameters.length; index++) {
            PathVariable pathVariable = parameters[index].getAnnotation(PathVariable.class);
            if (pathVariable != null && "productId".equals(resolvePathVariableName(pathVariable))
                    && args[index] instanceof UUID productId) {
                return productId;
            }
        }

        throw new ModuleProductIdMissingException(method);
    }

    private String resolvePathVariableName(PathVariable pathVariable) {
        if (!pathVariable.value().isBlank()) {
            return pathVariable.value();
        }
        return pathVariable.name();
    }
}
