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

package org.axonframework.eventhandling;

import org.axonframework.common.Assert;
import org.axonframework.domain.EventMessage;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Abstract {@code Cluster} implementation that keeps track of Cluster members ({@link EventListener EventListeners}).
 * This implementation is thread-safe. The {@link #getMembers()} method returns a read-only runtime view of the members
 * in the cluster.
 * Cluster抽象实现，用于保持对Cluster中事件监听器的跟踪。这个实现是线程安全的。
 * getMembers方法返回一个群组中只读的运行时视图。
 * @author Allard Buijze
 * @since 1.2
 */
public abstract class AbstractCluster implements Cluster {

    private final String name;//群组名称
    private final Set<EventListener> eventListeners;//该群的事件监听器集合
    private final Set<EventListener> immutableEventListeners;//不可变的事件监听器集合
    private final ClusterMetaData clusterMetaData = new DefaultClusterMetaData();//集合的原数据，默认值为默认的集合原数据
    private final EventProcessingMonitorCollection subscribedMonitors = new EventProcessingMonitorCollection();
    private final MultiplexingEventProcessingMonitor eventProcessingMonitor = new MultiplexingEventProcessingMonitor(subscribedMonitors);

    /**
     * Initializes the cluster with given <code>name</code>. The order in which listeners are organized in the cluster
     * is undefined.
     *
     * @param name The name of this cluster
     */
    protected AbstractCluster(String name) {
        Assert.notNull(name, "name may not be null");
        this.name = name;
        eventListeners = new CopyOnWriteArraySet<EventListener>();
        immutableEventListeners = Collections.unmodifiableSet(eventListeners);
    }

    /**
     * Initializes the cluster with given <code>name</code>, using given <code>comparator</code> to order the listeners
     * in the cluster. The order of invocation of the members in this cluster is according the order provided by the
     * comparator.
     *
     * @param name       The name of this cluster
     * @param comparator The comparator providing the ordering of the Event Listeners
     */
    protected AbstractCluster(String name, Comparator<EventListener> comparator) {
        Assert.notNull(name, "name may not be null");
        this.name = name;
        eventListeners = new ConcurrentSkipListSet<EventListener>(comparator);
        immutableEventListeners = Collections.unmodifiableSet(eventListeners);
    }

    @Override
    public void publish(EventMessage... events) {
        doPublish(Arrays.asList(events), eventListeners, eventProcessingMonitor);
    }

    /**
     * Publish the given list of <code>events</code> to the given set of <code>eventListeners</code>, and notify the
     * given <code>eventProcessingMonitor</code> after completion. The given set of <code>eventListeners</code> is a
     * live view on the memberships of the cluster. Any subscription changes are immediately visible in this set.
     * Iterators created on the set iterate over an immutable view reflecting the state at the moment the iterator was
     * created.
     * <p/>
     * When this method is invoked as part of a Unit of Work (see
     * {@link org.axonframework.unitofwork.CurrentUnitOfWork#isStarted()}), the monitor invocation should be postponed
     * until the Unit of Work is committed or rolled back, to ensure any transactions are properly propagated when the
     * monitor is invoked.
     * <p/>
     * It is the implementation's responsibility to ensure that &ndash;eventually&ndash; the each of the given
     * <code>events</code> is provided to the <code>eventProcessingMonitor</code>, either to the {@link
     * org.axonframework.eventhandling.EventProcessingMonitor#onEventProcessingCompleted(java.util.List)} or the {@link
     * org.axonframework.eventhandling.EventProcessingMonitor#onEventProcessingFailed(java.util.List, Throwable)}
     * method.
     *
     * @param events                 The events to publish
     * @param eventListeners         The event listeners subscribed at the moment the event arrived
     * @param eventProcessingMonitor The monitor to notify after completion.
     */
    protected abstract void doPublish(List<EventMessage> events, Set<EventListener> eventListeners,
                                      MultiplexingEventProcessingMonitor eventProcessingMonitor);

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void subscribe(EventListener eventListener) {
        eventListeners.add(eventListener);
        if (eventListener instanceof EventProcessingMonitorSupport) {
            ((EventProcessingMonitorSupport) eventListener).subscribeEventProcessingMonitor(eventProcessingMonitor);
        }
    }

    @Override
    public void unsubscribe(EventListener eventListener) {
        eventListeners.remove(eventListener);
    }

    @Override
    public ClusterMetaData getMetaData() {
        return clusterMetaData;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This implementation returns a real-time view on the actual members, which changes when members join or leave the
     * cluster. Iterators created from the returned set are thread-safe and iterate over the members available at the
     * time the iterator was created. The iterator does not allow the {@link java.util.Iterator#remove()} method to be
     * invoked.
     */
    @Override
    public Set<EventListener> getMembers() {
        return immutableEventListeners;
    }

    @Override
    public void subscribeEventProcessingMonitor(EventProcessingMonitor monitor) {
        subscribedMonitors.subscribeEventProcessingMonitor(monitor);
    }

    @Override
    public void unsubscribeEventProcessingMonitor(EventProcessingMonitor monitor) {
        subscribedMonitors.unsubscribeEventProcessingMonitor(monitor);
    }
}
