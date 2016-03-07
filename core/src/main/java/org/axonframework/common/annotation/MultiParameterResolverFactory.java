/*
 * Copyright (c) 2010-2014. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.common.annotation;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * ParameterResolverFactory instance that delegates to multiple other instances, in the order provided.
 * 该实例被用于代理多个其它实例，并按顺序提供
 *
 * @author Allard Buijze
 * @since 2.1
 */
public class MultiParameterResolverFactory implements ParameterResolverFactory {

    private final ParameterResolverFactory[] factories;

    /**
     * Creates a MultiParameterResolverFactory instance with the given <code>delegates</code>, which are automatically
     * ordered based on the {@link org.axonframework.common.Priority @Priority} annotation on their respective classes.
     * Classes with the same Priority are kept in the order as provided in the <code>delegates</code>.
     * 创建一个代理多个给定参数解析器工厂类的，MultiParameterResolverFactory实例。其将自动排序按照被委托的类上标注的“@Priority”注解，进行排序。
     * 相同优先级的则保持原有的加载顺序。
     * <p/>
     * If one of the delegates is a MultiParameterResolverFactory itself, that factory's delegates are 'mixed' with
     * the given <code>delegates</code>, based on their respective order.
     * 如果代理中也包含与自己相同的实例，则按照他们各自的顺序混排在一起。
     *
     * @param delegates The delegates to include in the factory
     * @return an instance delegating to the given <code>delegates</code>
     */
    public static MultiParameterResolverFactory ordered(ParameterResolverFactory... delegates) {
        return ordered(Arrays.asList(delegates));
    }

    /**
     * Creates a MultiParameterResolverFactory instance with the given <code>delegates</code>, which are automatically
     * ordered based on the {@link org.axonframework.common.Priority @Priority} annotation on their respective classes.
     * Classes with the same Priority are kept in the order as provided in the <code>delegates</code>.
     * <p/>
     * If one of the delegates is a MultiParameterResolverFactory itself, that factory's delegates are 'mixed' with
     * the given <code>delegates</code>, based on their respective order.
     *
     * @param delegates The delegates to include in the factory
     * @return an instance delegating to the given <code>delegates</code>
     */
    public static MultiParameterResolverFactory ordered(List<ParameterResolverFactory> delegates) {
        return new MultiParameterResolverFactory(flatten(delegates));
    }

    /**
     * Initializes an instance that delegates to the given <code>delegates</code>, in the order provided. Changes in
     * the given array are not reflected in the created instance.
     *
     * @param delegates The factories providing the parameter values to use
     */
    public MultiParameterResolverFactory(ParameterResolverFactory... delegates) {
        this.factories = Arrays.copyOf(delegates, delegates.length);
    }

    /**
     * Initializes an instance that delegates to the given <code>delegates</code>, in the order provided. Changes in
     * the given List are not reflected in the created instance.
     *
     * @param delegates The list of factories providing the parameter values to use
     */
    public MultiParameterResolverFactory(List<ParameterResolverFactory> delegates) {
        this.factories = delegates.toArray(new ParameterResolverFactory[delegates.size()]);
    }

    /**
     * 用于解决被代理的参数解析工厂中还有同样的MultiParameterResolverFactory实例则混排在一起
     * @param factories
     * @return
     */
    private static ParameterResolverFactory[] flatten(List<ParameterResolverFactory> factories) {
        List<ParameterResolverFactory> flattened = new ArrayList<ParameterResolverFactory>(factories.size());
        for (ParameterResolverFactory parameterResolverFactory : factories) {
            if (parameterResolverFactory instanceof MultiParameterResolverFactory) {
                flattened.addAll(((MultiParameterResolverFactory) parameterResolverFactory).getDelegates());
            } else {
                flattened.add(parameterResolverFactory);
            }
        }
        Collections.sort(flattened, PriorityAnnotationComparator.getInstance());
        return flattened.toArray(new ParameterResolverFactory[flattened.size()]);
    }

    /**
     * Returns the delegates of this instance, in the order they are evaluated to resolve parameters.
     * 排序后返回第一个可用的参数解析器
     * @return the delegates of this instance, in the order they are evaluated to resolve parameters
     */
    public List<ParameterResolverFactory> getDelegates() {
        return Arrays.asList(factories);
    }

    @Override
    public ParameterResolver createInstance(Annotation[] memberAnnotations, Class<?> parameterType,
                                            Annotation[] parameterAnnotations) {
        for (ParameterResolverFactory factory : factories) {
            ParameterResolver resolver = factory.createInstance(memberAnnotations, parameterType, parameterAnnotations);
            if (resolver != null) {
                return resolver;
            }
        }
        return null;
    }
}
