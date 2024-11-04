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

import org.eclipse.jifa.tda.enums.JavaThreadState;
import org.eclipse.jifa.tda.model.JavaThread;
import org.eclipse.jifa.tda.model.Snapshot;
import org.eclipse.jifa.tda.parser.ParserException;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class TestJStackParser extends TestBase {

    @Test
    public void testTime() throws ParserException, ParseException, IOException {
        String time = "2021-06-12 23:07:17";
        Snapshot snapshot = parseString(time);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Assert.assertEquals(sdf.parse(time).getTime(), snapshot.getTimestamp());

        time = "2021-06-12 23:07:18\n";
        snapshot = parseString(time);
        Assert.assertEquals(sdf.parse(time).getTime(), snapshot.getTimestamp());
    }

    @Test
    public void testVersion() throws ParserException, IOException {
        String version = "Full thread dump OpenJDK 64-Bit Server VM (15.0.1+9-18 mixed mode, sharing):";
        Snapshot snapshot = parseString(version);
        Assert.assertEquals(-1, snapshot.getTimestamp());

        Assert.assertEquals("OpenJDK 64-Bit Server VM (15.0.1+9-18 mixed mode, sharing)", snapshot.getVmInfo());
    }

    @Test
    public void testJDK8Log() throws ParserException, URISyntaxException {
        Snapshot snapshot = parseFile("jstack_8.log");
        Assert.assertTrue(snapshot.getErrors().isEmpty());
    }

    @Test
    public void testJDK11Log() throws ParserException, URISyntaxException {
        Snapshot snapshot = parseFile("jstack_11_with_deadlocks.log");
        Assert.assertTrue(snapshot.getErrors().isEmpty());
    }

      @Test
    public void testJDK17WithPidLog() throws ParserException, URISyntaxException {
        Snapshot snapshot = parseFile("jstack_17_log_with_pid.log");
        Assert.assertTrue(snapshot.getErrors().isEmpty());
        assertEquals(2, snapshot.getJavaThreads().size());
        assertEquals(7692, snapshot.getPid());
    }

      @Test
    public void testJDK21WithPidLog() throws ParserException, URISyntaxException {
        Snapshot snapshot = parseFile("jstack_21.log");
        Assert.assertTrue(snapshot.getErrors().isEmpty());
        assertEquals(10, snapshot.getJavaThreads().size());
        assertEquals("main", snapshot.getJavaThreads().get(0).getName());
        assertFalse(snapshot.getJavaThreads().get(0).isDaemon());

        JavaThread thread = snapshot.getJavaThreads().get(8);
        assertEquals("Common-Cleaner", thread.getName());
        assertEquals(746.47, thread.getCpu(),0.1);
        assertEquals(826770670.0, thread.getElapsed(),0.1);
        assertEquals(JavaThreadState.PARKED_TIMED, thread.getJavaThreadState());
        assertEquals(18, thread.getJid());
        assertTrue(thread.isDaemon());
        assertEquals(1561, thread.getNid());
        assertEquals(8, thread.getPriority());
        assertEquals(0, thread.getOsPriority());
        assertEquals(139744483852208l, thread.getTid());
    }
}
