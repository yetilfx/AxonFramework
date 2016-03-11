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

import java.lang.annotation.Annotation;

/**
 * Cluster Selector implementation that selects a cluster if an Annotation is present on the Event Listener class.
 * 注解式群组选择器
 * 
 * @author Allard Buijze
 * @since 2.0
 */
public class AnnotationClusterSelector extends AbstractClusterSelector {

    private final Class<? extends Annotation> annotationType;
    private final Cluster cluster;
    private final boolean inspectSuperClasses;

    /**
     * Initializes a ClusterSelector instance that selects the given <code>cluster</code> for Event Listeners that are
     * annotated with the given <code>annotationType</code>.
     * 初始化选择器实例，依据给定的注解类型为事件监听器选择群组。
     * <p/>
     * Note that this instance will <em>not</em> search superclasses for annotations by default. If annotation on
     * classes should also reflect on their subclasses, make sure to use the
     * {@link java.lang.annotation.Inherited @Inherited} Meta-Annotation, or use {@link
     * #AnnotationClusterSelector(Class, Cluster, boolean)}.
     * 注意，默认情况下该实例不检索超类中的注解。如果注解需要被子类继承，则需要使用@Inherited元注解（Meta-Annotation），
     * 或使用AnnotationClusterSelector(Class, Cluster, boolean)方法来创建。
     * @param annotationType The type of annotation to find on the Event Listeners 在事件监听器上需要查找的注解类型
     * @param cluster        The cluster to select if the annotation was found 注解所对应的群组
     */
    public AnnotationClusterSelector(Class<? extends Annotation> annotationType, Cluster cluster) {
        this(annotationType, cluster, false);
    }

    /**
     * Initializes a ClusterSelector instance that selects the given <code>cluster</code> for Event Listeners that are
     * annotated with the given <code>annotationType</code>. If <code>inspectSuperClasses</code> is <code>true</code>,
     * super classes will be searched for the annotation, regardless if the annotation is marked as {@link
     * java.lang.annotation.Inherited @Inherited}
     *
     * @param annotationType      The type of annotation to find on the Event Listeners
     * @param cluster             The cluster to select if the annotation was found
     * @param inspectSuperClasses Whether or not to inspect super classes as well
     */
    public AnnotationClusterSelector(Class<? extends Annotation> annotationType, Cluster cluster,
                                     boolean inspectSuperClasses) {
        this.annotationType = annotationType;
        this.cluster = cluster;
        this.inspectSuperClasses = inspectSuperClasses;
    }

    /**
     * 为给定的监听器和监听器类选择群组
     */
    @Override
    protected Cluster doSelectCluster(EventListener eventListener, Class<?> listenerType) {
        return annotationPresent(listenerType) ? cluster : null;
    }

    /**
     * 判断监听器类或其超类中是否包含指定类型的注解
     * @param listenerType 监听器类
     * @return
     */
    private boolean annotationPresent(Class<?> listenerType) {
        return listenerType.isAnnotationPresent(annotationType)
                || (inspectSuperClasses
                && listenerType.getSuperclass() != null
                && annotationPresent(listenerType.getSuperclass()));
    }
}
