/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.nadavr.internal.state;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.nadavr.internal.nadcp.NadCommand.DefaultSourceNames;

/**
 * The {@link NadAvrInputSourceList.java} class for An Array that is used to store/update input source names
 * dynamically.
 *
 * @author Dave J Schoepel - Initial contribution
 */
@NonNullByDefault
public class NadAvrInputSourceList {

    public static String[] sourceNameList = sourceNames();

    /**
     * Initialize source names list with default source names
     *
     * @return sourceNames - array
     */
    public static String[] sourceNames() {
        Set<String> nameSet = Arrays.stream(DefaultSourceNames.values()).map(prefix -> new String(prefix.name()))
                .collect(Collectors.toSet());
        String[] sourceNames = nameSet.toArray(new String[nameSet.size()]);
        Arrays.sort(sourceNames);
        return sourceNames;
    }

    public static String[] getSourceNameList() {
        return sourceNameList;
    }

    public static void setSourceNameList(String[] sourceNameList) {
        NadAvrInputSourceList.sourceNameList = sourceNameList;
    }

    public static int size() {
        return sourceNameList.length;
    }

    public static void updateSourceName(int index, String sourceName) {
        sourceNameList[index] = sourceName;
    }

    public static String getSourceName(int index) {
        return sourceNameList[index];
    }

    @Override
    public String toString() {
        return "InputSourceList [toString()=" + super.toString() + "]";
    }
}
