/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
import org.openhab.binding.nadavr.internal.nadcp.NadCommand;

/**
 * The {@link NadTunerPresetNameList.java} class for An Array that is used to store input source names.
 *
 * @author Dave J Schoepel - Initial contribution
 */
@NonNullByDefault
public class NadTunerPresetNameList {

    public static String[] presetNameList = presetNames();

    /**
     * Initialize source names list with default source names
     *
     * @return sourceNames array
     */
    public static String[] presetNames() {
        Set<String> nameSet = Arrays.stream(NadCommand.DefaultPresetNames.values())
                .map(prefix -> new String(prefix.name())).collect(Collectors.toSet());
        String[] presetNames = nameSet.toArray(new String[nameSet.size()]);
        Arrays.sort(presetNames);
        return presetNames;
    }

    public static String[] getTunerPresetNameList() {
        return presetNameList;
    }

    public static void setTunerPresetNameList(String[] sourceNameList) {
        NadTunerPresetNameList.presetNameList = sourceNameList;
    }

    public static int size() {
        return presetNameList.length;
    }

    public static void updateTunerPresetName(int index, String presetName) {
        presetNameList[index] = presetName;
    }

    public static String getTunerPreseteName(int index) {
        return presetNameList[index];
    }

    @Override
    public String toString() {
        return "TunerPresetNameList [toString()=" + super.toString() + "]";
    }
}
