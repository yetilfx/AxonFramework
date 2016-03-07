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

import org.axonframework.common.CollectionUtils;
import org.axonframework.common.Priority;
import org.axonframework.domain.Message;

import java.lang.annotation.Annotation;

import static org.axonframework.common.CollectionUtils.getAnnotation;

/**
 * Factory for the default parameter resolvers. This factory is capable for providing parameter resolvers for Message,
 * MetaData and @MetaData annotated parameters.
 * 默认的参数解析器工厂。工厂为消息提供，按照元数据和元数据注解的参数来解析的参数解析器。
 * 解析器加载顺序，先消息解析、其次使用注解元数据参数解析、最后使用元数据参数解析
 *
 * @author Allard Buijze
 * @since 2.0
 */
@Priority(Priority.FIRST)
public class DefaultParameterResolverFactory implements ParameterResolverFactory {

	/**
	 * 1.优先依据期望参数类型，返回该参数类型对应的消息参数解析器（MessageParameterResolver），最终返回参数为消息本身；
	 * 2.其次依据一组参数的注解中为MetaData类型的注解，创建元数据注解参数解析器（AnnotatedMetaDataParameterResolver），
	 * 最终返回消息中元数据，于参数的元数据注解值匹配的Key的，消息元数据值；
	 * 3.最后,当上述均不成立时，按照期望参数类型，返回对应的元数据参数解析器(MetaDataParameterResolver),最终返回消息的元数据
	 * 
	 */
    @Override
    public ParameterResolver createInstance(Annotation[] methodAnnotations, Class<?> parameterType,
                                            Annotation[] parameterAnnotations) {
        if (Message.class.isAssignableFrom(parameterType)) {
            return new MessageParameterResolver(parameterType);
        }
        if (getAnnotation(parameterAnnotations, MetaData.class) != null) {
            return new AnnotatedMetaDataParameterResolver(CollectionUtils.getAnnotation(parameterAnnotations,
                                                                                        MetaData.class), parameterType);
        }
        if (org.axonframework.domain.MetaData.class.isAssignableFrom(parameterType)) {
            return MetaDataParameterResolver.INSTANCE;
        }
        return null;
    }

    private static class AnnotatedMetaDataParameterResolver implements ParameterResolver {

        private final MetaData metaData;
        private final Class parameterType;

        public AnnotatedMetaDataParameterResolver(MetaData metaData, Class parameterType) {
            this.metaData = metaData;
            this.parameterType = parameterType;
        }

        @Override
        public Object resolveParameterValue(Message message) {
            return message.getMetaData().get(metaData.value());
        }

        @Override
        public boolean matches(Message message) {
            return !(parameterType.isPrimitive() || metaData.required())
                    || (
                    message.getMetaData().containsKey(metaData.value())
                            && parameterType.isInstance(message.getMetaData().get(metaData.value()))
            );
        }
    }

    private static final class MetaDataParameterResolver implements ParameterResolver {

        private static final MetaDataParameterResolver INSTANCE = new MetaDataParameterResolver();

        private MetaDataParameterResolver() {
        }

        @Override
        public Object resolveParameterValue(Message message) {
            return message.getMetaData();
        }

        @Override
        public boolean matches(Message message) {
            return true;
        }
    }

    private static class MessageParameterResolver implements ParameterResolver {

        private final Class<?> parameterType;

        public MessageParameterResolver(Class<?> parameterType) {
            this.parameterType = parameterType;
        }

        @Override
        public Object resolveParameterValue(Message message) {
            return message;
        }

        @Override
        public boolean matches(Message message) {
            return parameterType.isInstance(message);
        }
    }
}
