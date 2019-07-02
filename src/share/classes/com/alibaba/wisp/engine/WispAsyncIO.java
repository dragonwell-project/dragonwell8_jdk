package com.alibaba.wisp.engine;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.Properties;

import sun.misc.SharedSecrets;
import sun.misc.WispAsyncIOAccess;

class WispAsyncIO {
    // AIO
    static boolean wispAIOLoaded = false;

    static {
        Properties p = java.security.AccessController.doPrivileged(
                new java.security.PrivilegedAction<Properties>() {
                    public Properties run() {
                        return System.getProperties();
                    }
                }
        );
    }

    static void setWispAsyncIOAccess() {
        // initialize TenantAccess
        if (SharedSecrets.getWispAsyncIOAccess() == null) {
            SharedSecrets.setWispAsyncIOAccess(new WispAsyncIOAccess() {
                @Override
                public boolean usingAsyncIO() {
                    return wispAIOLoaded
                            && SharedSecrets.getWispEngineAccess() != null
                            && SharedSecrets.getWispEngineAccess().runningAsCoroutine(Thread.currentThread());
                }

                @Override
                public <T> T executeAsyncIO(Callable<T> command) throws IOException {
                    return WispAIOSupporter.INSTANCE.invokeIOTask(command);
                }
            });
        }
    }

    /*
     * Initialize the WispAsyncIO class, called after System.initializeSystemClass by VM.
     **/
    static void initializeAsyncIOClass() {
        try {
            Class.forName(WispAIOSupporter.class.getName());
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    static void startAsyncIODaemon() {
        WispAIOSupporter.INSTANCE.startDaemon(WispEngine.DAEMON_THREAD_GROUP);
        setWispAsyncIOAccess();
    }
}
