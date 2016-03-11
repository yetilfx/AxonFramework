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

/**
 * The ClusterSelector defines the mechanism that assigns each of the subscribed listeners to a Cluster instance. The
 * selector does *not* need to subscribe the listener to that cluster.
 * <p/>
 * <em>Thread safety note:</em> The implementation is expected to be thread safe.
 * 群组选择器接口：提供了分配订阅监听器到群组实例的机制。选择器不需要订阅该群组的监听器。
 *
 * @author Allard Buijze
 * @since 1.2
 */
public interface ClusterSelector {

    /**
     * Selects the cluster instance that the given {@code eventListener} should be member of. This may be an existing
     * (or pre-configured) cluster, as well as a newly created cluster.
     * 选择含有对应监听器成员的群组。可能是一个已经存在的集群，也可能是创建一个新的集群。
     * <p/>
     * When {@code null} is returned, this may cause the Event Listener not to be subscribed to any cluster at all.
     * 当返回Null时，这可能造成事件监听器无法订阅到全部任何集群
     *
     * @param eventListener the event listener to select a cluster for
     * @return The Cluster assigned to the listener
     */
    Cluster selectCluster(EventListener eventListener);
}
