/********************************************************************************
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.tda.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.jifa.tda.enums.JavaThreadState;
import org.eclipse.jifa.tda.enums.OSTreadState;
import org.eclipse.jifa.tda.model.Frame;
import org.eclipse.jifa.tda.model.JavaThread;
import org.eclipse.jifa.tda.model.Thread;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class SearchQuery {
    
    private List<String> terms = new ArrayList<>();

    private boolean regex = false;

    private boolean searchStack = true;

    private boolean searchName = true;

    private boolean searchState = true;

    private boolean matchCase = true;

    private Set<JavaThreadState> allowedJavaStates = EnumSet.allOf(JavaThreadState.class);

    private Set<OSTreadState> allowedOSStates = EnumSet.allOf(OSTreadState.class);

    public Predicate<Thread> build() {
        Predicate<String> stringMatcher = regex ? compileRegexMatcher() : compileStringMatcher();
        
        Predicate<Thread> p = t -> false;
        if(searchName) {
            p = p.or(t -> stringMatcher.test(t.getName()));
        }
        if(searchState) {
            p = p.or(t -> {
                if(stringMatcher.test(t.getOsThreadState().toString())) {
                    return true;
                }
                if(t instanceof JavaThread) {
                    JavaThreadState state = ((JavaThread)t).getJavaThreadState();
                    return stringMatcher.test(state.toString());
                }
                return false;
            });
        }
        if(searchStack) {
            p = p.or(t -> {
                if(t instanceof JavaThread) {
                    JavaThread jt = (JavaThread)t;
                    if(jt.getTrace() != null) {
                        Frame[] frames = jt.getTrace().getFrames();
                        for (Frame frame : frames) {
                            if(stringMatcher.test(frame.getClazz()) || stringMatcher.test(frame.getMethod()) || stringMatcher.test(frame.getModule())) {
                                return true;
                            }
                        }
                    }
                }
                return false;
            });
        }
        Predicate<Thread> filter = t -> {
            if(!allowedOSStates.contains(t.getOsThreadState())) {
                return false;
            }
            if(t instanceof JavaThread) {
                JavaThreadState state = ((JavaThread)t).getJavaThreadState();
                if(!allowedJavaStates.contains(state)) {
                    return false;
                }
            }
            return true;
        };
        Predicate<Thread> overall = filter.and(p);
        return overall;
    }
    
    private Predicate<String> compileStringMatcher() {
        if(!matchCase) {
            terms = terms.stream().map(t -> t.toLowerCase(Locale.ROOT)).collect(Collectors.toList());
        }
        return s -> {
            if(s == null) {
                return false;
            }
            if(!matchCase) {
                s = s.toLowerCase(Locale.ROOT);
            }
            for (String term : terms) {
                if(s.contains(term)) {
                    return true;   
                }
            }
            return false;
        };
    }

    private Predicate<String> compileRegexMatcher() {
        List<Pattern> patterns = new ArrayList<>();
        for (String term : terms) {
            patterns.add(Pattern.compile(term));
        }
        return s -> {
            if(s == null)
                return false;
            for (Pattern pattern : patterns) {
                if(pattern.matcher(s).find()) {
                    return true;   
                }
            }
            return false;
        };
    }

    public static SearchQuery forTerms(String... terms) {
        return forTerms(Arrays.asList(terms));
    }

    public static SearchQuery forTerms(List<String> terms) {
        SearchQuery query = new SearchQuery();
        query.terms = terms;
        return query;
    }

    public SearchQuery withRegex(boolean regex) {
        setRegex(regex);
        return this;
    }

    public SearchQuery withMatchCase(boolean matchCase) {
        setMatchCase(matchCase);
        return this;
    }

    public SearchQuery withSearchStack(boolean searchStack) {
        setSearchStack(searchStack);
        return this;
    }

    public SearchQuery withSearchName(boolean searchName) {
        setSearchName(searchName);
        return this;
    }

    public SearchQuery withSearchState(boolean searchState) {
        setSearchState(searchState);
        return this;
    }

    public SearchQuery withAllowedJavaStates(Set<JavaThreadState> states) {
        setAllowedJavaStates(states);
        return this;
    }

    public SearchQuery withAllowedOSStates(Set<OSTreadState> states) {
        setAllowedOSStates(states);
        return this;
    }

}
