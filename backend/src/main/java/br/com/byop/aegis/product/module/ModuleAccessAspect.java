package br.com.byop.aegis.product.module;

import br.com.byop.aegis.product.api.ModuleKey;
import br.com.byop.aegis.product.api.RequireModule;
import br.com.byop.aegis.product.exception.ModuleDisabledException;
import br.com.byop.aegis.product.exception.ModuleProductIdMissingException;
import br.com.byop.aegis.product.repository.ProductModuleRepository;
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
    public Object requireEnabledModuleOnMethod(ProceedingJoinPoint joinPoint, RequireModule requireModule) throws Throwable {
        return requireEnabledModule(joinPoint, requireModule);
    }

    @Around("@within(requireModule) && !@annotation(br.com.byop.aegis.product.api.RequireModule)")
    public Object requireEnabledModuleOnClass(ProceedingJoinPoint joinPoint, RequireModule requireModule) throws Throwable {
        return requireEnabledModule(joinPoint, requireModule);
    }

    private Object requireEnabledModule(ProceedingJoinPoint joinPoint, RequireModule requireModule) throws Throwable {
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
