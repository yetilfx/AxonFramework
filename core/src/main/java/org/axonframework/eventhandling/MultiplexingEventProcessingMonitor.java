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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of the EventProcessingMonitor that receives multiple invocations of downstream monitors and
 * translates that to a single invocation to a target monitor.
 * 事件处理监视器（EventProcessingMonitor）实现，接收下游监视器的多重调用并转换为目标监视器的单一调用。
 * <p/>
 * This allows for multiple asynchronous executions to be reported in a single invocation to a monitor. Only when all
 * downstream monitors report successful event handling, it will be reported as successful to the target monitor. When
 * a failure is reported, it is reported to the target monitor as a failure as soon as all expected reports have been
 * received.
 * 这样可以允许多个异步执行，仅单独调用一个监视器来报告。仅当所有的下游监视器报告事件处理成功，才会向目标监视器报告成功。
 * 当某个失败被报告，目标监视器被立即报告失败。
 *
 * @author Allard Buijze
 * @since 2.1
 */
public class MultiplexingEventProcessingMonitor implements EventProcessingMonitor {

    private final ConcurrentMap<String, Counter> eventCounters = new ConcurrentHashMap<String, Counter>();//key：事件id

    private final EventProcessingMonitor targetMonitor;

    /**
     * Creates an instance with the given <code>targetMonitor</code> as the monitor to eventually forward calls to.
     * 用给定的目标监视器创建一个实例，作为最终调用的监视器。
     *
     * @param targetMonitor The monitor to eventually forward calls to
     */
    public MultiplexingEventProcessingMonitor(EventProcessingMonitor targetMonitor) {
        this.targetMonitor = targetMonitor;
    }

    /**
     * Prepare the monitor for processing the given <code>message</code>. This means a tracker will be created to this
     * event, which keeps track of the required and actual invocations.
     * 准备监视器处理给定的消息。这就意味着将创建一个跟踪器跟踪这个事件，跟踪要求和实际的调用。
     * <p/>
     * When an unprepared message is acknowledged or reported as failed, that notification is immediately forwarded to
     * the target monitor.
     * 当一个为准备的消息被确认接收或者报告失败，目标监视器将会立即收到通知
     * 
     * @param eventMessage The event message to prepare for.
     */
    public void prepare(EventMessage eventMessage) {
        eventCounters.put(eventMessage.getIdentifier(), new Counter());
    }

    /**
     * Prepare the monitor for the invocation of the given <code>message</code> to the given <code>member</code>. This
     * means that, if the given member implement {@link org.axonframework.eventhandling.EventProcessingMonitorSupport},
     * the monitor will wait for an invocation by that member's monitor.
     * 准备给定事件与给定调用成员间的监视器。这意味着，如果给定成员实现EventProcessingMonitorSupport，监视器将等待成员监视器的调用。
     * <p/>
     * It is important that this invocation happens <em>before</em> the actual invocation of the member.
     * 注意这个调用发生在成员实际调用前。
     * @param eventMessage The message about to be sent
     * @param member       The member that will be invoked
     * @throws java.lang.IllegalArgumentException if the given <code>eventMessage</code> has not been {@link
     * #prepare(org.axonframework.domain.EventMessage) prepared} yet.
     */
    public void prepareForInvocation(EventMessage eventMessage, EventListener member) {
        if (member instanceof EventProcessingMonitorSupport) {
            final Counter counter = eventCounters.get(eventMessage.getIdentifier());
            Assert.notNull(counter, "You must prepare a message before registering async invocations");
            counter.expectAsyncInvocation();
        }
    }

    @Override
    public void onEventProcessingCompleted(List<? extends EventMessage> eventMessages) {
        List<EventMessage> messagesToAck = new ArrayList<EventMessage>(eventMessages.size());
        for (EventMessage eventMessage : eventMessages) {
            final String eventIdentifier = eventMessage.getIdentifier();
            Counter counter = eventCounters.get(eventIdentifier);
            if (counter == null || counter.recordSuccess()) {
                if (counter != null && counter.hasFailed()) {
                    targetMonitor.onEventProcessingFailed(Arrays.asList(eventMessage), counter.failureCause());
                } else {
                    messagesToAck.add(eventMessage);
                }
                eventCounters.remove(eventIdentifier, counter);
            }
        }
        if (!messagesToAck.isEmpty()) {
            targetMonitor.onEventProcessingCompleted(messagesToAck);
        }
    }

    @Override
    public void onEventProcessingFailed(List<? extends EventMessage> eventMessages, Throwable cause) {
        List<EventMessage> messagesToReport = new ArrayList<EventMessage>(eventMessages.size());
        for (EventMessage eventMessage : eventMessages) {
            final String eventIdentifier = eventMessage.getIdentifier();
            Counter counter = eventCounters.get(eventIdentifier);
            if (counter == null || counter.recordFailure(cause)) {
                messagesToReport.add(eventMessage);
                eventCounters.remove(eventIdentifier, counter);
            }
        }
        if (!messagesToReport.isEmpty()) {
            targetMonitor.onEventProcessingFailed(messagesToReport, cause);
        }
    }

    private static class Counter {

        private final AtomicInteger eventCounter = new AtomicInteger(1);
        private final AtomicInteger failureCounter = new AtomicInteger(0);
        private volatile Throwable cause;


        public void expectAsyncInvocation() {
            eventCounter.incrementAndGet();
        }

        public boolean recordSuccess() {
            return eventCounter.decrementAndGet() == 0;
        }

        public boolean recordFailure(Throwable cause) {
            // we care about the last cause recorded
            this.cause = cause;
            failureCounter.incrementAndGet();
            return eventCounter.decrementAndGet() == 0;
        }

        public Throwable failureCause() {
            return cause;
        }

        private boolean hasFailed() {
            return cause != null;
        }
    }
}
