package com.alibaba.wisp.engine;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;

enum WispAIOSupporter {
    INSTANCE;

    private ExecutorService executor;

    private ThreadGroup threadgroup;

    WispAIOSupporter() {
    }

    void startDaemon(ThreadGroup g) {
        threadgroup = g;
        ThreadPoolExecutor workPool;
        workPool = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(),
                Runtime.getRuntime().availableProcessors(), Long.MAX_VALUE,
                TimeUnit.SECONDS, new LinkedBlockingDeque<>(), new AIOThreadPoolFactory());
        workPool.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        this.executor = workPool;
        WispAsyncIO.wispAIOLoaded = true;
    }

    public <T> T invokeIOTask(Callable<T> command) throws IOException {
        Future<T> future;
        try {
            future = submitIOTask(command);
        } catch (RejectedExecutionException e) {
            throw new IOException("busy", e);
        }
        T result;
        while (true) {
            try {
                result = future.get();
                return result;
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                Class<?> causeClass = cause.getClass();
                if (IOException.class.isAssignableFrom(causeClass)) {
                    throw (IOException) cause;
                } else if (RuntimeException.class.isAssignableFrom(causeClass)) {
                    throw (RuntimeException) cause;
                } else if (Error.class.isAssignableFrom(causeClass)) {
                    throw (Error) cause;
                } else {
                    throw new Error(e);
                }
            } catch (InterruptedException e) {
                Thread.interrupted();
            }
        }
    }

    <T> Future<T> submitIOTask(Callable<T> command) {
        return executor.submit(command);
    }

    private class AIOThreadPoolFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final static String namePrefix = "AIO-worker-thread-";

        AIOThreadPoolFactory() {
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t;
            if (threadgroup != null) {
                t = new Thread(threadgroup, r, namePrefix + threadNumber.getAndIncrement());
            } else {
                t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
            }
            t.setDaemon(true);
            return t;
        }
    }
}
