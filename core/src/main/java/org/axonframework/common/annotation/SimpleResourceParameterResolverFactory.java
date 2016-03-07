package org.axonframework.common.annotation;

import org.axonframework.common.Priority;

import java.lang.annotation.Annotation;

/**
 * ParameterResolverFactory implementation that resolves parameters for a specific given Resource.
 * 按照指定的资源解析参数的参数解析器工厂类实现（依赖于FixedValueParameterResolver-固定值参数解析器），
 * 返回的参数值为给定的资源对象
 *
 * @author Allard Buijze
 * @since 2.4.2
 */
@Priority(Priority.LOW)
public class SimpleResourceParameterResolverFactory implements ParameterResolverFactory {

    private final Object resource;

    /**
     * Initialize the ParameterResolverFactory to inject the given <code>resource</code> in applicable parameters.
     *
     * @param resource The resource to inject
     */
    public SimpleResourceParameterResolverFactory(Object resource) {
        this.resource = resource;
    }

    @Override
    public ParameterResolver createInstance(Annotation[] memberAnnotations, Class<?> parameterType,
                                            Annotation[] parameterAnnotations) {
        if (parameterType.isInstance(resource)) {
            return new FixedValueParameterResolver<Object>(resource);
        }
        return null;
    }
}
