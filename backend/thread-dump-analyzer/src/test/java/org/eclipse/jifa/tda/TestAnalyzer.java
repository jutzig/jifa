/********************************************************************************
 * Copyright (c) 2022 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.eclipse.jifa.tda;

import static org.junit.Assert.assertEquals;

import java.nio.file.Path;
import java.util.List;

import org.eclipse.jifa.common.listener.DefaultProgressListener;
import org.eclipse.jifa.common.request.PagingRequest;
import org.eclipse.jifa.common.vo.PageView;
import org.eclipse.jifa.tda.enums.JavaThreadState;
import org.eclipse.jifa.tda.enums.OSTreadState;
import org.eclipse.jifa.tda.enums.ThreadType;
import org.eclipse.jifa.tda.vo.Content;
import org.eclipse.jifa.tda.vo.Overview;
import org.eclipse.jifa.tda.vo.VBlockingThread;
import org.eclipse.jifa.tda.vo.VFrame;
import org.eclipse.jifa.tda.vo.VMonitor;
import org.eclipse.jifa.tda.vo.VThread;
import org.junit.Assert;
import org.junit.Test;

public class TestAnalyzer extends TestBase {

    @Test
    public void test() throws Exception {
        ThreadDumpAnalyzer tda =
            new ThreadDumpAnalyzer(pathOfResource("jstack_8.log"), new DefaultProgressListener());
        Overview o1 = tda.overview();
        Overview o2 = tda.overview();
        Assert.assertEquals(o1, o2);
        Assert.assertEquals(o1.hashCode(), o2.hashCode());

        PageView<VThread> threads = tda.threads("main", ThreadType.JAVA, null, new PagingRequest(1, 1));
        Assert.assertEquals(1, threads.getTotalSize());

        PageView<VFrame> frames = tda.callSiteTree(0, new PagingRequest(1, 16));
        Assert.assertTrue(frames.getTotalSize() > 0);
        Assert.assertNotEquals(frames.getData().get(0), frames.getData().get(1));

        PageView<VMonitor> monitors = tda.monitors(new PagingRequest(1, 8));
        Assert.assertTrue(monitors.getTotalSize() > 0);

        Content line2 = tda.content(2, 1);
        Assert.assertEquals("Full thread dump OpenJDK 64-Bit Server VM (18-internal+0-adhoc.denghuiddh.my-jdk mixed " +
                            "mode, sharing):", line2.getContent().get(0));
    }

    @Test
    public void testFilter() throws Exception {
        ThreadDumpAnalyzer tda =
            new ThreadDumpAnalyzer(pathOfResource("jstack_8.log"), new DefaultProgressListener());
        Overview o1 = tda.overview();
        Overview o2 = tda.overview();
        Assert.assertEquals(o1, o2);
        Assert.assertEquals(o1.hashCode(), o2.hashCode());

        PageView<VThread> threads = tda.threads(null, null, JavaThreadState.RUNNABLE.toString(), new PagingRequest(1, 100));
        Assert.assertEquals(18, threads.getTotalSize());

        threads = tda.threads(null, ThreadType.GC, JavaThreadState.RUNNABLE.toString(), new PagingRequest(1, 100));
        Assert.assertEquals(10, threads.getTotalSize());

        threads = tda.threads(null, ThreadType.JAVA, JavaThreadState.RUNNABLE.toString(), new PagingRequest(1, 100));
        Assert.assertEquals(3, threads.getTotalSize());

        threads = tda.threads(null, ThreadType.JAVA, JavaThreadState.IN_OBJECT_WAIT.toString(), new PagingRequest(1, 100));
        Assert.assertEquals(2, threads.getTotalSize());

        threads = tda.threads(null, null, OSTreadState.OBJECT_WAIT.toString(), new PagingRequest(1, 100));
        Assert.assertEquals(2, threads.getTotalSize());

        threads = tda.threads("Refer", ThreadType.JAVA, JavaThreadState.IN_OBJECT_WAIT.toString(), new PagingRequest(1, 100));
        Assert.assertEquals(1, threads.getTotalSize());
    }

    @Test
    public void testBlockingThreads() throws Exception {
        ThreadDumpAnalyzer tda = new ThreadDumpAnalyzer(pathOfResource("jstack_11_large_with_blocked.log"), new DefaultProgressListener());
        List<VBlockingThread> blockingThreads = tda.blockingThreads();
        assertEquals(4,blockingThreads.size());

        VBlockingThread t = blockingThreads.get(0);
        assertEquals("Thread-7728157", t.getBlockingThread().getName());
        assertEquals(19, t.getBlockedThreads().size());
        assertEquals("Thread-7716893", t.getBlockedThreads().get(0).getName());
        assertEquals(0x00000001d3af89c0L, t.getHeldLock().getAddress());
        assertEquals("org.example.jmsadapter.listener.CrossInstanceLock", t.getHeldLock().getClazz());

        t = blockingThreads.get(1);
        assertEquals("Thread-7728079", t.getBlockingThread().getName());
        assertEquals(6, t.getBlockedThreads().size());
        assertEquals("Thread-7728084", t.getBlockedThreads().get(0).getName());
        assertEquals(0x00000001d3b1f158L, t.getHeldLock().getAddress());
        assertEquals("org.example.jmsadapter.listener.CrossInstanceLock", t.getHeldLock().getClazz());
    }
}
