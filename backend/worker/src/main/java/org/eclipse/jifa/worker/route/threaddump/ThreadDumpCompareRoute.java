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
package org.eclipse.jifa.worker.route.threaddump;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jifa.common.enums.FileType;
import org.eclipse.jifa.common.vo.FileInfo;
import org.eclipse.jifa.tda.ThreadDumpAnalyzer;
import org.eclipse.jifa.tda.vo.Comparison;
import org.eclipse.jifa.worker.route.HttpMethod;
import org.eclipse.jifa.worker.route.ParamKey;
import org.eclipse.jifa.worker.route.RouteMeta;
import org.eclipse.jifa.worker.support.Analyzer;
import org.eclipse.jifa.worker.support.FileSupport;

import io.vertx.core.Promise;

/**
 * allows to compare a sequence of thread dumps with each ohter
 */
public class ThreadDumpCompareRoute extends ThreadDumpBaseRoute {

    /**
     * creates a comparison between severel thread dumps
     * @param promise
     * @param files
     */
    @RouteMeta(path = "/compare/summary", method = HttpMethod.GET )
    public void summary(Promise<Comparison> promise, @ParamKey(value = "files", mandatory = false) List<String> files) {
        Comparison result = new Comparison();
        for (String file : files) {
            result.getFileInfos().add(FileSupport.info(FileType.THREAD_DUMP, file));
        }
        Collections.sort(result.getFileInfos(), Comparator.comparing(FileInfo::getCreationTime));
        for (FileInfo info : result.getFileInfos()) {
            ThreadDumpAnalyzer analyzer = Analyzer.threadDumpAnalyzerOf(info.getName());
            result.getOverviews().add(analyzer.overview());
        }
        promise.complete(result);
    }
}
