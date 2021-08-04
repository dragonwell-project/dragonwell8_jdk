/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

/* @test
 * @bug 8195160
 * @summary Unit test for ServerSocketChannel setOption/getOption/options
 *          methods.
 * @modules jdk.net
 * @requires !vm.graal.enabled
 * @requires (os.family == "linux")
 * @library .. /test/lib
 * @build RsocketTest
 * @run main/othervm SocketOptionTests
 */

import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.SocketOption;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.util.Set;
import jdk.net.RdmaSockets;
import static java.net.StandardSocketOptions.*;
import static jdk.net.RdmaSocketOptions.*;

import jtreg.SkippedException;

public class SocketOptionTests {

    static void checkOption(ServerSocketChannel ssc, SocketOption name, Object expectedValue)
        throws IOException
    {
        Object value = ssc.getOption(name);
        if (!value.equals(expectedValue))
            throw new RuntimeException("value not as expected");
    }

    public static void main(String[] args) throws IOException {
        if (!RsocketTest.isRsocketAvailable())
            throw new SkippedException("rsocket is not available");

        ServerSocketChannel ssc = RdmaSockets.openServerSocketChannel(
            StandardProtocolFamily.INET);

        // check supported options
        Set<SocketOption<?>> options = ssc.supportedOptions();
        if (!options.contains(SO_REUSEADDR))
            throw new RuntimeException("SO_REUSEADDR should be supported");
        if (!options.contains(SO_RCVBUF))
            throw new RuntimeException("SO_RCVBUF should be supported");
        if (!options.contains(RDMA_SQSIZE))
            throw new RuntimeException("RDMA_SQSIZE should be supported");
        if (!options.contains(RDMA_RQSIZE))
            throw new RuntimeException("RDMA_RQSIZE should be supported");
        if (!options.contains(RDMA_INLINE))
            throw new RuntimeException("RDMA_INLINE should be supported");

        // allowed to change when not bound
        ssc.setOption(SO_RCVBUF, 256*1024);     // can't check
        int before = ssc.getOption(SO_RCVBUF);
        int after = ssc.setOption(SO_RCVBUF, Integer.MAX_VALUE).getOption(SO_RCVBUF);
        if (after < before)
            throw new RuntimeException("setOption caused SO_RCVBUF to decrease");
        ssc.setOption(SO_REUSEADDR, true);
        checkOption(ssc, SO_REUSEADDR, true);
        ssc.setOption(SO_REUSEADDR, false);
        checkOption(ssc, SO_REUSEADDR, false);

        // NullPointerException
        try {
            ssc.setOption(null, "value");
            throw new RuntimeException("NullPointerException not thrown");
        } catch (NullPointerException x) {
        }
        try {
            ssc.getOption(null);
            throw new RuntimeException("NullPointerException not thrown");
        } catch (NullPointerException x) {
        }

        // ClosedChannelException
        ssc.close();
        try {
            ssc.setOption(SO_REUSEADDR, true);
            throw new RuntimeException("ClosedChannelException not thrown");
        } catch (ClosedChannelException x) {
        }
    }
}
