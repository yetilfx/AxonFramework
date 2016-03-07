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

import org.axonframework.domain.Message;

/**
 * ParameterResolver implementation that injects a fixed value. Useful for injecting parameter values that do not rely
 * on information contained in the incoming message itself.
 * 返回固定注入值得参数解析器实现。用于注入固定的参数值，而不依赖于消息本身。
 * SimpleResourceParameterResolverFactory中使用，将资源对象直接返回作为参数。
 * 
 * @param <T> The type of value resolved by this parameter
 * @author Allard Buijze
 * @since 2.0
 */
public class FixedValueParameterResolver<T> implements ParameterResolver<T> {

    private final T value;

    /**
     * Initialize the ParameterResolver to inject the given <code>value</code> for each incoming message.
     *
     * @param value The value to inject as parameter
     */
    public FixedValueParameterResolver(T value) {
        this.value = value;
    }

    @Override
    public T resolveParameterValue(Message message) {
        return value;
    }

    @Override
    public boolean matches(Message message) {
        return true;
    }
}
