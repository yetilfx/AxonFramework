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

package org.axonframework.common;

import java.lang.annotation.Annotation;
import java.util.LinkedList;
import java.util.List;

/**
 * Utility methods for operations on collections.
 *
 * @author Allard Buijze
 * @since 0.7
 */
public abstract class CollectionUtils {

    private CollectionUtils() {
        // prevent instantiation
    }

    /**
     * Returns a list of objects of <code>expectedType</code> contained in the given <code>collection</code>. Any
     * objects in the collection that are not assignable to the given <code>expectedType</code> are filtered out.
     * <p/>
     * The order of the items in the list is the same as the order they were provided by the collection. The given
     * <code>collection</code> remains unchanged by this method.
     * <p/>
     * If the given collection is null, en empty list is returned.
     *
     * @param collection   An iterable (e.g. Collection) containing the unfiltered items.
     * @param expectedType The type items in the returned List must be assignable to.
     * @param <T>          The type items in the returned List must be assignable to.
     * @return a list of objects of <code>expectedType</code>. May be empty, but never <code>null</code>.
     */

    public static <T> List<T> filterByType(Iterable<?> collection, Class<T> expectedType) {
        List<T> filtered = new LinkedList<T>();
        if (collection != null) {
            for (Object item : collection) {
                if (item != null && expectedType.isInstance(item)) {
                    filtered.add(expectedType.cast(item));
                }
            }
        }
        return filtered;
    }

    /**
     * Finds an annotation of given <code>annotationType</code> from the given <code>annotations</code>. If
     * <code>annotations</code> contains multiple annotations of the given type, the first one is returned. If none
     * is found, this method returns <code>null</code>.
     * 从给定的一组注解中查找指定注解类型的注解。如果该组注解中包含多个匹配类型的注解时，则返回第一个。如果没有，则返回null.
     *
     * @param annotations    The annotations to search in 待查找的一组注解
     * @param annotationType The type of annotation to search for 待查找的注解类型
     * @param <T>            The type of annotation to search for 待查找的注解类型
     * @return the first annotation found, or <code>null</code> if no such annotation is present 找到的第一个匹配的注解或者null
     */
    @SuppressWarnings({"unchecked"})
    public static <T> T getAnnotation(Annotation[] annotations, Class<T> annotationType) {
        for (Annotation annotation : annotations) {
            if (annotation.annotationType().equals(annotationType)) {
                return (T) annotation;
            }
        }
        return null;
    }
}
