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
public enum NADModel {

    // Please also remember to add supported models to the README.md
    // T187, T777, T787, T778, T753
    // Format is model name, max number of zones for that specific model

    T_187("T187", 4),
    T_777("T777", 4),
    T_778("T778", 2),
    T_787("T787", 4),
    T_758("T758", 2);

    private final String id;
    private final int maxZones;

    private NADModel(String id, int maxZones) {
        this.id = id;
        this.maxZones = maxZones;
    }

    public String getId() {
        return id;
    }

    public int getMaxZones() {
        return maxZones;
    }
}
