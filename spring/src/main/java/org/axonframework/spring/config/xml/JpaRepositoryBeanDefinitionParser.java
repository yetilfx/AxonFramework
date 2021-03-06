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

package org.axonframework.spring.config.xml;

import org.axonframework.common.jpa.ContainerManagedEntityManagerProvider;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.commandhandling.model.GenericJpaRepository;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.w3c.dom.Element;

import static org.axonframework.spring.config.AutowiredBean.createAutowiredBean;

/**
 * BeanDefinitionParser for the jpa-repository elements in a Spring context.
 *
 * @author Allard Buijze
 * @since 2.0
 */
public class JpaRepositoryBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    private static final String ENTITY_MANAGER_PROVIDER = "entity-manager-provider";
    private static final String EVENT_BUS = "event-bus";
    private static final String LOCK_FACTORY = "lock-factory";
    private static final String LOCKING_STRATEGY = "locking-strategy";
    private static final String AGGREGATE_TYPE = "aggregate-type";

    @Override
    protected Class<?> getBeanClass(Element element) {
        return GenericJpaRepository.class;
    }

    @Override
    protected void doParse(Element element, BeanDefinitionBuilder builder) {
        if (element.hasAttribute(ENTITY_MANAGER_PROVIDER)) {
            builder.addConstructorArgReference(element.getAttribute(ENTITY_MANAGER_PROVIDER));
        } else {
            builder.addConstructorArgValue(
                    BeanDefinitionBuilder.genericBeanDefinition(ContainerManagedEntityManagerProvider.class)
                                         .getBeanDefinition()
            );
        }
        builder.addConstructorArgValue(element.getAttribute(AGGREGATE_TYPE));
        parseLockFactory(element, builder);
        parseEventBus(element, builder);
    }

    private void parseLockFactory(Element element, BeanDefinitionBuilder builder) {
        if (element.hasAttribute(LOCK_FACTORY)) {
            builder.addConstructorArgReference(element.getAttribute(LOCK_FACTORY));
        } else if (element.hasAttribute(LOCKING_STRATEGY)) {
            LockingStrategy strategy = LockingStrategy.valueOf(element.getAttribute(LOCKING_STRATEGY));
            GenericBeanDefinition LockFactory = new GenericBeanDefinition();
            LockFactory.setBeanClass(strategy.getLockFactoryType());
            builder.addConstructorArgValue(LockFactory);
        }
    }

    private void parseEventBus(Element element, BeanDefinitionBuilder builder) {
        if (element.hasAttribute(EVENT_BUS)) {
            builder.addPropertyReference("eventBus", element.getAttribute(EVENT_BUS));
        } else {
            builder.addPropertyValue("eventBus", createAutowiredBean(EventBus.class));
        }
    }

    @Override
    protected boolean shouldGenerateIdAsFallback() {
        return true;
    }
}
