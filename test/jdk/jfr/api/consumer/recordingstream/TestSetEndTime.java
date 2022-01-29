/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package jdk.jfr.api.consumer.recordingstream;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import jdk.jfr.Event;
import jdk.jfr.Name;
import jdk.jfr.Recording;
import jdk.jfr.StackTrace;
import jdk.jfr.consumer.EventStream;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import jdk.jfr.consumer.RecordingStream;

/**
 * @test
 * @summary Tests EventStream::setEndTime
 * @key jfr
 * @library /lib /
 * @run main/othervm jdk.jfr.api.consumer.recordingstream.TestSetEndTime
 */
public final class TestSetEndTime {

    @Name("Mark")
    @StackTrace(false)
    public final static class Mark extends Event {
        public boolean include;
        public int id;
    }

    public static void main(String... args) throws Exception {
        testEventStream();
        testRecordingStream();
    }

    private static void testRecordingStream() throws Exception {
        while (true) {
            CountDownLatch closed = new CountDownLatch(1);
            AtomicInteger count = new AtomicInteger();
            try (RecordingStream rs = new RecordingStream()) {
                rs.setFlushInterval(Duration.ofSeconds(1));
                rs.onEvent(e -> {
                    count.incrementAndGet();
                });
                // when end is reached stream is closed
                rs.onClose(() -> {
                    closed.countDown();
                });
                Instant endTime = Instant.now().plus(Duration.ofMillis(100));
                System.out.println("Setting end time: " + endTime);
                rs.setEndTime(endTime);
                rs.startAsync();
                for (int i = 0; i < 50; i++) {
                    Mark m = new Mark();
                    m.commit();
                    Thread.sleep(10);
                }
                closed.await();
                System.out.println("Found events: " + count.get());
                if (count.get() < 50) {
                    return;
                }
                System.out.println("Found 50 events. Retrying");
                System.out.println();
            }
        }
    }

    static void testEventStream() throws InterruptedException, IOException, Exception {
        while (true) {
            try (Recording r = new Recording()) {
                r.start();

                Mark event1 = new Mark();
                event1.id = 1;
                event1.include = false;
                event1.commit(); // start time

                nap();

                Mark event2 = new Mark();
                event2.id = 2;
                event2.include = true;
                event2.commit();

                nap();

                Mark event3 = new Mark();
                event3.id = 3;
                event3.include = false;
                event3.commit(); // end time

                Path p = Paths.get("recording.jfr");
                r.dump(p);
                Instant start = null;
                Instant end = null;
                System.out.println("Find start and end time as instants:");
                for (RecordedEvent e : RecordingFile.readAllEvents(p)) {
                    if (e.getInt("id") == 1) {
                        start = e.getEndTime();
                        System.out.println("Start  : " + start);
                    }
                    if (e.getInt("id") == 2) {
                        Instant middle = e.getEndTime();
                        System.out.println("Middle : " + middle);
                    }
                    if (e.getInt("id") == 3) {
                        end = e.getEndTime();
                        System.out.println("End    : " + end);
                    }
                }
                System.out.println();
                System.out.println("Opening stream between " + start + " and " + end);
                AtomicBoolean success = new AtomicBoolean(false);
                AtomicInteger eventsCount = new AtomicInteger();
                try (EventStream d = EventStream.openRepository()) {
                    d.setStartTime(start.plusNanos(1));
                    // Stream should close when end is reached
                    d.setEndTime(end.minusNanos(1));
                    d.onEvent(e -> {
                        eventsCount.incrementAndGet();
                        boolean include = e.getBoolean("include");
                        System.out.println("Event " + e.getEndTime() + " include=" + include);
                        if (include) {
                            success.set(true);
                        }
                    });
                    d.start();
                    if (eventsCount.get() == 1 && success.get()) {
                        return;
                    }
                }
            }
        }

    }

    private static void nap() throws InterruptedException {
        // Ensure we advance at least 1 ns with fast time
        Thread.sleep(1);
    }

}
