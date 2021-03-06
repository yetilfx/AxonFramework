/*
 * Copyright (c) 2010-2015. Axon Framework
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

package org.axonframework.eventhandling.annotation;

import org.axonframework.common.annotation.ParameterResolver;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.GenericEventMessage;
import org.junit.Before;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.time.Instant;
import java.time.temporal.Temporal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Allard Buijze
 */
public class TimestampParameterResolverFactoryTest {

    private TimestampParameterResolverFactory testSubject;
    private Timestamp annotation;

    @Before
    public void setUp() throws Exception {
        testSubject = new TimestampParameterResolverFactory();
        annotation = mock(Timestamp.class);
        when (annotation.annotationType()).thenAnswer(invocationOnMock -> Timestamp.class);
    }

    @Test
    public void testResolvesToDateTimeWhenAnnotated() throws Exception {
        ParameterResolver resolver = testSubject.createInstance(new Annotation[0],
                                                                Instant.class,
                                                                new Annotation[]{annotation});
        final EventMessage<Object> message = GenericEventMessage.asEventMessage("test");
        assertTrue(resolver.matches(message));
        assertEquals(message.getTimestamp(), resolver.resolveParameterValue(message));
    }

    @Test
    public void testResolvesToReadableInstantWhenAnnotated() throws Exception {
        ParameterResolver resolver = testSubject.createInstance(new Annotation[0],
                                                                Temporal.class,
                                                                new Annotation[]{annotation});
        final EventMessage<Object> message = GenericEventMessage.asEventMessage("test");
        assertTrue(resolver.matches(message));
        assertEquals(message.getTimestamp(), resolver.resolveParameterValue(message));
    }

    @Test
    public void testIgnoredWhenNotAnnotated() throws Exception {
        ParameterResolver resolver = testSubject.createInstance(new Annotation[0],
                                                                Instant.class,
                                                                new Annotation[0]);
        assertNull(resolver);
    }

    @Test
    public void testIgnoredWhenWrongType() throws Exception {
        ParameterResolver resolver = testSubject.createInstance(new Annotation[0],
                                                                String.class,
                                                                new Annotation[]{annotation});
        assertNull(resolver);
    }
}
