package org.axonframework.springboot.nativex;

import org.axonframework.common.ReflectionUtils;
import org.axonframework.common.annotation.AnnotationUtils;
import org.axonframework.messaging.annotation.MessageHandler;
import org.axonframework.spring.config.MessageHandlerConfigurer;
import org.axonframework.spring.config.MessageHandlerLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aot.context.bootstrap.generator.infrastructure.nativex.BeanFactoryNativeConfigurationProcessor;
import org.springframework.aot.context.bootstrap.generator.infrastructure.nativex.NativeConfigurationRegistry;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.nativex.hint.TypeAccess;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.List;

public class MessageHandlerNativeProcessor implements BeanFactoryNativeConfigurationProcessor {

    private static final Logger logger = LoggerFactory.getLogger(MessageHandlerNativeProcessor.class);

    @Override
    public void process(ConfigurableListableBeanFactory beanFactory, NativeConfigurationRegistry registry) {
        MessageHandlerConfigurer.Type[] types = MessageHandlerConfigurer.Type.values();
        for (MessageHandlerConfigurer.Type messageType : types) {
            List<String> beanNames = MessageHandlerLookup.messageHandlerBeans(messageType.getMessageType(), beanFactory, true);
            logger.info("Found {} beans for {} requiring reflective access", beanNames.size(), messageType);
            for (String beanName : beanNames) {
                Class<?> type = beanFactory.getType(beanName);
                if (type != null) {
                    logger.debug("Registering native access to {}", type.getSimpleName());
                    registry.reflection().forType(type).withAccess(TypeAccess.DECLARED_FIELDS, TypeAccess.DECLARED_CONSTRUCTORS,
                                                                   TypeAccess.DECLARED_METHODS);
                    for (Method method : ReflectionUtils.methodsOf(type)) {
                        registerParameterReflection(registry, method);
                    }
                    for (Constructor<?> constructor : type.getDeclaredConstructors()) {
                        registerParameterReflection(registry, constructor);
                    }
                } else {
                    logger.error("Unable to resolve type for bean named {}. Handler methods are probably not available at runtime.", beanName);
                }
            }

        }
    }

    private void registerParameterReflection(NativeConfigurationRegistry registry, Executable executable) {
        if (AnnotationUtils.isAnnotationPresent(executable, MessageHandler.class)) {
            registry.reflection().forType(executable.getDeclaringClass()).withExecutables(executable);
            for (Class<?> parameterType : executable.getParameterTypes()) {
                registry.reflection().forType(parameterType).withAccess(TypeAccess.PUBLIC_CLASSES,
                                                                        TypeAccess.DECLARED_CONSTRUCTORS,
                                                                        TypeAccess.DECLARED_METHODS,
                                                                        TypeAccess.DECLARED_FIELDS);
            }
            if (executable instanceof Method) {
                Class<?> returnType = ((Method) executable).getReturnType();
                if (!returnType.isPrimitive()) {
                    registry.reflection().forType(returnType).withAccess(TypeAccess.PUBLIC_CLASSES,
                                                                         TypeAccess.DECLARED_CONSTRUCTORS,
                                                                         TypeAccess.DECLARED_METHODS,
                                                                         TypeAccess.DECLARED_FIELDS);
                }
            }
        }
    }
}
