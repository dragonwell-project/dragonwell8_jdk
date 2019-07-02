package sun.misc;


import com.alibaba.wisp.engine.WispEngine;
import com.alibaba.wisp.engine.WispTask;

import java.io.IOException;
import java.util.concurrent.Callable;

public interface WispAsyncIOAccess {
    boolean usingAsyncIO();

    <T> T executeAsyncIO(Callable<T> command) throws IOException;
}
