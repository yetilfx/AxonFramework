/*
 * Copyright (c) 2010. Axon Framework
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

package org.axonframework.saga.annotation;

import org.axonframework.domain.Event;
import org.axonframework.saga.AssociationValue;
import org.axonframework.util.AbstractHandlerInspector;
import org.axonframework.util.AxonConfigurationException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author Allard Buijze
 */
class SagaAnnotationInspector extends AbstractHandlerInspector {

    public SagaAnnotationInspector() {
        super(SagaEventHandler.class);
    }

    public HandlerConfiguration findHandlerConfiguration(Class<?> sagaType, Event event) {
        Method handlerMethod = findHandlerMethod(sagaType, event.getClass());
        if (handlerMethod == null) {
            return HandlerConfiguration.noHandler();
        }
        SagaEventHandler handlerAnnotation = handlerMethod.getAnnotation(SagaEventHandler.class);
        StartSaga startAnnotation = handlerMethod.getAnnotation(StartSaga.class);
        EndSaga endAnnotation = handlerMethod.getAnnotation(EndSaga.class);
        String associationProperty = handlerAnnotation.associationProperty();
        String associationKey = handlerAnnotation.keyName().isEmpty() ? associationProperty : handlerAnnotation
                .keyName();
        Object associationValue = getPropertyValue(event, associationProperty);
        AssociationValue association = new AssociationValue(associationKey, associationValue);
        return new HandlerConfiguration(creationPolicy(startAnnotation),
                                        handlerMethod,
                                        endAnnotation != null,
                                        association);
    }

    private SagaCreationPolicy creationPolicy(StartSaga startSaga) {
        if (startSaga == null) {
            return SagaCreationPolicy.NONE;
        } else if (startSaga.forceNew()) {
            return SagaCreationPolicy.ALWAYS;
        } else {
            return SagaCreationPolicy.IF_NONE_FOUND;
        }
    }

    private Object getPropertyValue(Event event, String property) {
        try {
            Method m = event.getClass().getMethod("get" + capitalize(property));
            return m.invoke(event);
        } catch (NoSuchMethodException e) {
            throw new AxonConfigurationException("", e);
        } catch (InvocationTargetException e) {
            throw new AxonConfigurationException("", e);
        } catch (IllegalAccessException e) {
            throw new AxonConfigurationException("", e);
        }
    }

    private String capitalize(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

}