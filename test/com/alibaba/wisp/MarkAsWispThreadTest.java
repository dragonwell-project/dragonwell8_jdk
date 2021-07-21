/*
 * Copyright (c) 2020 Alibaba Group Holding Limited. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Alibaba designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

/*
 * @test
 * @library /lib/testlibrary
 * @summary Test ability to mark thread as wispThread
 * @requires os.family == "linux"
 * @run main/othervm -XX:+UnlockExperimentalVMOptions  -XX:+EnableCoroutine -XX:+UseWisp2 -Dcom.alibaba.wisp.allThreadAsWisp=false -XX:ActiveProcessorCount=5 MarkAsWispThreadTest
 */

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import sun.misc.SharedSecrets;
import static jdk.testlibrary.Asserts.*;


public class MarkAsWispThreadTest{
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        ThreadFactory threadFactory = new WispThreadFactory();
        ExecutorService executors = Executors.newFixedThreadPool(1, threadFactory);
        executors.submit(new Runnable() {
            @Override
            public void run() {
                assertTrue(SharedSecrets.getWispEngineAccess().runningAsCoroutine(Thread.currentThread()));
            }
        }).get();

//         Threads are not coroutine by default
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                assertFalse(SharedSecrets.getWispEngineAccess().runningAsCoroutine(Thread.currentThread()));
            }
        });
        t.start();
        t.join();
    }
}

class WispThreadFactory implements ThreadFactory{
    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r);
        SharedSecrets.getWispEngineAccess().markAsWispThread(t);
        return t;
    }
}
