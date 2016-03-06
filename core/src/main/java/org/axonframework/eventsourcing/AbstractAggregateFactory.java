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

package org.axonframework.eventsourcing;

import org.axonframework.common.annotation.ParameterResolverFactory;
import org.axonframework.domain.DomainEventMessage;
import org.axonframework.eventsourcing.annotation.AbstractAnnotatedAggregateRoot;

/**
 * Abstract AggregateFactory implementation that is aware of snapshot events. If an incoming event is not a snapshot
 * event, creation is delegated to the subclass.
 * 聚合工厂识别快照事件的抽象实现。如果传入事件不是快照事件，则将被委派给子类。
 *
 * @param <T> The type of Aggregate created by this factory（工厂创建的聚合类型）
 * @author Allard Buijze
 * @since 2.0
 */
public abstract class AbstractAggregateFactory<T extends EventSourcedAggregateRoot> implements AggregateFactory<T> {

	//参数分析工厂类
    private final ParameterResolverFactory parameterResolverFactory;

    /*
     * 默认构造器，参数分析工厂类为null
     */
    protected AbstractAggregateFactory() {
        this(null);
    }

    /**
     * 构造器
     * @param parameterResolverFactory
     */
    protected AbstractAggregateFactory(ParameterResolverFactory parameterResolverFactory) {
        this.parameterResolverFactory = parameterResolverFactory;
    }

    @SuppressWarnings("unchecked")
    @Override
    public final T createAggregate(Object aggregateIdentifier, DomainEventMessage<?> firstEvent) {
        T aggregate;
        if (EventSourcedAggregateRoot.class.isAssignableFrom(firstEvent.getPayloadType())) {
            aggregate = (T) firstEvent.getPayload();
        } else {
            aggregate = doCreateAggregate(aggregateIdentifier, firstEvent);
        }
        if (parameterResolverFactory != null && aggregate instanceof AbstractAnnotatedAggregateRoot) {
            ((AbstractAnnotatedAggregateRoot) aggregate).registerParameterResolverFactory(parameterResolverFactory);
        }
        return postProcessInstance(aggregate);
    }

    /**
     * Perform any processing that must be done on an aggregate instance that was reconstructed from a Snapshot
     * Event. Implementations may choose to modify the existing instance, or return a new instance.
     * <p/>
     * This method can be safely overridden. This implementation does nothing.
     *
     * @param aggregate The aggregate to post-process.
     * @return The aggregate to initialize with the Event Stream
     */
    protected T postProcessInstance(T aggregate) {
        return aggregate;
    }

    /**
     * Create an uninitialized Aggregate instance with the given <code>aggregateIdentifier</code>. The given
     * <code>firstEvent</code> can be used to define the requirements of the aggregate to create.
     * <p/>
     * The given <code>firstEvent</code> is never a snapshot event.
     *
     * @param aggregateIdentifier The identifier of the aggregate to create
     * @param firstEvent          The first event in the Event Stream of the Aggregate
     * @return The aggregate instance to initialize with the Event Stream
     */
    protected abstract T doCreateAggregate(Object aggregateIdentifier, DomainEventMessage firstEvent);
}
