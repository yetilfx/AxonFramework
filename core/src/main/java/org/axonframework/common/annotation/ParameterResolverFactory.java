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

/**
 * Interface for objects capable of creating Parameter Resolver instances for annotated handler methods. These
 * resolvers provide the parameter values to use, given an incoming {@link org.axonframework.domain.Message}.
 * 该接口针对注解形式的（命令或事件）处理器方法，创建参数解析器实例的能力。这些解析器依据输入的消息，提供参数值
 * <p/>
 * One of the implementations is the {@link ClasspathParameterResolverFactory}, which allows application developers to
 * provide custom ParameterResolverFactory implementations using the ServiceLoader mechanism. To do so, place a file
 * called <code>org.axonframework.common.annotation.ParameterResolverFactory</code> in the
 * <code>META-INF/services</code> folder. In this file, place the fully qualified class names of all available
 * implementations.
 * ClasspathParameterResolverFactory是实现之一，开发者可以通过ServiceLoader机制来注入自己的参数分析器工厂。
 * 创建一个“org.axonframework.common.annotation.ParameterResolverFactory”文件在“META-INF/services”文件夹中。
 * 在这个文件中写入完整的实现类名称。
 * <p/>
 * The factory implementations must be public, non-abstract, have a default public constructor and implement the
 * ParameterResolverFactory interface.
 * 工厂实现必须是public、非抽象的、有默认构造函数和ParameterResolverFactory接口
 *
 * @author Allard Buijze
 * @see ClasspathParameterResolverFactory
 * @since 2.1
 */
public interface ParameterResolverFactory {

    /**
     * If available, creates a ParameterResolver instance that can provide a parameter of type
     * <code>parameterType</code> for a given message.
     * 如果可用，创建一个参数解析器实例，依据给定的消息，按参数类型提供参数
     * <p/>
     * If the ParameterResolverFactory cannot provide a suitable ParameterResolver, returns <code>null</code>.
     * 如果参数解析器工厂不能提供适合的参数解析器，则返回Null
     *
     * @param memberAnnotations    annotations placed on the member (e.g. method) 成员的注解（例如：方法）
     * @param parameterType        the parameter type to find a resolver for 期望解析的参数类型
     * @param parameterAnnotations annotations placed on the parameter 参数注解
     * @return a suitable ParameterResolver, or <code>null</code> if none is found
     */
    ParameterResolver createInstance(Annotation[] memberAnnotations, Class<?> parameterType,
                                     Annotation[] parameterAnnotations);
}
