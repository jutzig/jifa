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

import org.eclipse.jifa.tda.model.Snapshot;
import org.eclipse.jifa.tda.parser.BisParser;
import org.eclipse.jifa.tda.parser.JStackParser;
import org.eclipse.jifa.tda.parser.Parser;
import org.eclipse.jifa.tda.parser.ParserException;
import org.eclipse.jifa.tda.parser.ParserFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import static org.eclipse.jifa.common.listener.ProgressListener.NoOpProgressListener;

public class TestBisParser extends TestBase {

    @Before
    public void setup() {
        analyzer = new BisParser();
    }

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
    public void testBIS67Log() throws ParserException, URISyntaxException {
        Snapshot snapshot = parseFile("BIS67.txt");
        Assert.assertTrue(snapshot.getErrors().isEmpty());
        Assert.assertTrue(snapshot.getJavaThreads().size() > 0);
    }

    @Test
    public void testBISBlockingLog() throws ParserException, URISyntaxException {
        Path path = pathOfResource("BIS67Blocking.txt");
        Snapshot snapshot = ParserFactory.buildParser(path).parse(path, NoOpProgressListener);
        Assert.assertTrue(snapshot.getErrors().isEmpty());
        Assert.assertTrue(snapshot.getJavaThreads().size() > 0);
    }

    @Test
    public void testBIS652Log() throws ParserException, URISyntaxException {
        Snapshot snapshot = parseFile("BIS652.txt");
        Assert.assertTrue(snapshot.getErrors().isEmpty());
        Assert.assertTrue(snapshot.getJavaThreads().size() > 0);
        System.out.println(snapshot.getJavaThreads().size());
    }
}
