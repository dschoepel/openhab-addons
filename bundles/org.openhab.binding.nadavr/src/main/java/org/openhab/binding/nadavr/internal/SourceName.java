/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.binding.nadavr.internal;

import java.util.HashMap;
import java.util.Map;

/**
 * The {@link SourceName.java} class for An Array list that is used to store input source names.
 *
 * @author Dave J Schoepel - Initial contribution
 */

public class SourceName {

    private static Map<String, String> avrSourceNames = new HashMap<String, String>();

    public String getAvrSourceName(String source) {
        return avrSourceNames.get(source);
    }

    public int size() {
        return avrSourceNames.size();
    }

    public void setAvrSourceName(String source, String name) {
        avrSourceNames.put(source, name);
    }

    public void replaceAvrSourceName(String source, String name) {
        avrSourceNames.replace(source, name);
    }

    public boolean containsKeySourceName(String key) {
        return avrSourceNames.containsKey(key);
    }
}
