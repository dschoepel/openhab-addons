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
package org.openhab.binding.nadavr.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Enumerates all supported NAD models.
 *
 * @author Dave J Schoepel - Initial contribution
 */
@NonNullByDefault
public enum NadModel {

    /**
     * Please also remember to add supported models to the README.md
     * T187, T765, T777, T787, T753
     * Format is model name, max number of zones for that specific model, source inputs
     */

    T_765("T765", 4, 10),
    T_775("T775", 4, 10),
    T_785("T785", 4, 10),
    T_187("T187", 4, 10),
    T_777("T777", 4, 10),
    T_787("T787", 4, 10);

    private final String id;
    private final int maxZones;
    private final int numberOfInputSources;

    private NadModel(String id, int maxZones, int numberOfInputSources) {
        this.id = id;
        this.maxZones = maxZones;
        this.numberOfInputSources = numberOfInputSources;
    }

    public String getId() {
        return id;
    }

    public int getMaxZones() {
        return maxZones;
    }

    public int getNumberOfInputSources() {
        return numberOfInputSources;
    }
}
