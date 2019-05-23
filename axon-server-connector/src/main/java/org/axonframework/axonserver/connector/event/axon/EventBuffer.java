/*
 * Copyright (c) 2010-2018. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.axonserver.connector.event.axon;

import io.axoniq.axonserver.grpc.event.EventWithToken;
import org.axonframework.eventhandling.EventUtils;
import org.axonframework.eventhandling.GlobalSequenceTrackingToken;
import org.axonframework.eventhandling.TrackedDomainEventData;
import org.axonframework.eventhandling.TrackedEventData;
import org.axonframework.eventhandling.TrackedEventMessage;
import org.axonframework.eventhandling.TrackingEventStream;
import org.axonframework.eventhandling.TrackingToken;
import org.axonframework.serialization.SerializedType;
import org.axonframework.serialization.Serializer;
import org.axonframework.serialization.UnknownSerializedType;
import org.axonframework.serialization.upcasting.event.EventUpcaster;
import org.axonframework.serialization.upcasting.event.NoOpEventUpcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Optional;
import java.util.Spliterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

import static org.axonframework.common.ObjectUtils.getOrDefault;

/**
 * Client-side buffer of messages received from the server. Once consumed from this buffer, the client is notified
 * of a permit being consumed, potentially triggering a permit refresh, if flow control is enabled.
 * <p>
 * This class is intended for internal use. Be cautious.
 *
 * @author Marc Gathier
 * @author Allard Buijze
 */
public class EventBuffer implements TrackingEventStream {
    final Logger logger = LoggerFactory.getLogger(EventBuffer.class);

    private final BlockingQueue<TrackedEventData<byte[]>> events;

    private final Iterator<TrackedEventMessage<?>> eventStream;

    private TrackedEventData<byte[]> peekData;
    private TrackedEventMessage<?> peekEvent;
    private Consumer<EventBuffer> closeCallback;
    private volatile RuntimeException exception;
    private volatile boolean closed;
    private Consumer<Integer> consumeListener = i -> {
    };
    private Consumer<SerializedType> blacklistListener = type -> {};

    /**
     * Initializes an Event Buffer, passing messages through given {@code upcasterChain} and deserializing events using
     * given {@code serializer}.
     *
     * @param upcasterChain The upcasterChain to translate serialized representations before deserializing
     * @param serializer    The serializer capable of deserializing incoming messages
     */
    public EventBuffer(EventUpcaster upcasterChain, Serializer serializer) {
        this.events = new LinkedBlockingQueue<>();
        eventStream = EventUtils.upcastAndDeserializeTrackedEvents(StreamSupport.stream(new SimpleSpliterator<>(this::poll), false),
                                                                   new GrpcMetaDataAwareSerializer(serializer),
                                                                   getOrDefault(upcasterChain, NoOpEventUpcaster.INSTANCE)
        )
                                .iterator();
    }

    private TrackedEventData<byte[]> poll() {
        TrackedEventData<byte[]> nextItem;
        if (peekData != null) {
            nextItem = peekData;
            peekData = null;
            return nextItem;
        }
        nextItem = events.poll();
        if (nextItem != null) {
            consumeListener.accept(1);
        }
        return nextItem;
    }

    private void waitForData(long deadline) throws InterruptedException {
        long now = System.currentTimeMillis();
        if (peekData == null && now < deadline) {
            peekData = events.poll(deadline - now, TimeUnit.MILLISECONDS);
            if (peekData != null) {
                consumeListener.accept(1);
            }
        }
    }

    /**
     * Calls the blacklist listener if the message contains an UnknownSerializedType as payload. The listener reports the
     * type and revision to AxonServer, so it will not send events with this payload type and revision anymore.
     * @param trackedEventMessage
     */
    @Override
    public void reportIgnored(TrackedEventMessage<?> trackedEventMessage) {
        if (blacklistListener == null) {
            return;
        }
        if (UnknownSerializedType.class.equals(trackedEventMessage.getPayloadType())) {
            UnknownSerializedType unknownSerializedType = (UnknownSerializedType) trackedEventMessage.getPayload();
            blacklistListener.accept(unknownSerializedType.serializedType());
        }
    }

    /**
     * Registers the callback to invoke when the reader wishes to close the stream.
     *
     * @param closeCallback The callback to invoke when the reader wishes to close the stream
     */
    public void registerCloseListener(Consumer<EventBuffer> closeCallback) {
        this.closeCallback = closeCallback;
    }

    /**
     * Registers the callback to invoke when a raw input message was consumed from the buffer. Note that there can only
     * be one listener registered.
     *
     * @param consumeListener the callback to invoke when a raw input message was consumed from the buffer
     */
    public void registerConsumeListener(Consumer<Integer> consumeListener) {
        this.consumeListener = consumeListener;
    }

    /**
     * Registers the callback to invoke when a the event processor determines that a type should be blacklisted.
     * Note that there can only be one listener registered.
     *
     * @param blacklistListener the callback to invoke when a payload type is to be blacklisted.
     */
    public void registerBlacklistListener(Consumer<SerializedType> blacklistListener) {
        this.blacklistListener = blacklistListener;
    }

    @Override
    public Optional<TrackedEventMessage<?>> peek() {
        if (peekEvent == null && eventStream.hasNext()) {
            peekEvent = eventStream.next();
        }
        return Optional.ofNullable(peekEvent);
    }

    @Override
    public boolean hasNextAvailable(int timeout, TimeUnit timeUnit) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeUnit.toMillis(timeout);
        try {
            while (peekEvent == null && !eventStream.hasNext() && System.currentTimeMillis() < deadline) {
                if (exception != null) {
                    RuntimeException runtimeException = exception;
                    this.exception = null;
                    throw runtimeException;
                }
                waitForData(deadline);
            }
            return peekEvent != null || eventStream.hasNext();
        } catch (InterruptedException e) {
            logger.warn("Consumer thread was interrupted. Returning thread to event processor.", e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public TrackedEventMessage<?> nextAvailable() {
        try {
            hasNextAvailable(Integer.MAX_VALUE, TimeUnit.MILLISECONDS);
            return peekEvent == null ? eventStream.next() : peekEvent;
        } catch (InterruptedException e) {
            logger.warn("Consumer thread was interrupted. Returning thread to event processor.", e);
            Thread.currentThread().interrupt();
            return null;
        } finally {
            peekEvent = null;
        }
    }

    @Override
    public void close() {
        closed = true;
        if (closeCallback != null) closeCallback.accept(this);
        events.clear();
    }

    public boolean push(EventWithToken event) {
        if( closed) {
            logger.debug("Received event while closed: {}", event.getToken());
            return false;
        }
        try {
            TrackingToken trackingToken = new GlobalSequenceTrackingToken(event.getToken());
            events.put(new TrackedDomainEventData<>(trackingToken, new GrpcBackedDomainEventData(event.getEvent())));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            closeCallback.accept(this);
            return false;
        }
        return true;
    }

    public void fail(RuntimeException e) {
        this.exception = e;
    }

    private static class SimpleSpliterator<T> implements Spliterator<T> {

        private final Supplier<T> supplier;

        protected SimpleSpliterator(Supplier<T> supplier) {
            this.supplier = supplier;
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            T nextValue = supplier.get();
            if (nextValue != null) {
                action.accept(nextValue);
            }
            return nextValue != null;
        }

        @Override
        public Spliterator<T> trySplit() {
            return null;
        }

        @Override
        public long estimateSize() {
            return Long.MAX_VALUE;
        }

        @Override
        public int characteristics() {
            return ORDERED | NONNULL | IMMUTABLE;
        }
    }
}
