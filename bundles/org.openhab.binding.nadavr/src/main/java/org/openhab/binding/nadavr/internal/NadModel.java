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
package org.openhab.binding.nadavr.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Enumerates all supported NAD Audio Video Receiver models.
 *
 * @author Dave J Schoepel - Initial contribution
 */
@NonNullByDefault
public enum NadModel {

    /**
     * Please also remember to add supported models to the README.md
     * T765, T775, T785, T187, T777, T778, T787
     * Format is model name, max number of zones for that specific model, source inputs
     */

    C_427("C427", 1, 0),
    T_765("T765", 4, 10),
    T_775("T775", 4, 10),
    T_785("T785", 4, 10),
    T_187("T187", 4, 10),
    T_777("T777", 4, 10),
    T_778("T778", 2, 8),
    T_787("T787", 4, 10);

    private final String id;
    private final int maxZones;
    private final int numberOfInputSources;

    /**
     * Constructor for the NAD model class used to identify models during discovery and
     * create the NAD Avr thing.
     *
     * @param id - Model number
     * @param maxZones - maximum number of zones that can be configured for the model
     * @param numberOfInputSources - number of input sources available for this model
     */
    private NadModel(String id, int maxZones, int numberOfInputSources) {
        this.id = id;
        this.maxZones = maxZones;
        this.numberOfInputSources = numberOfInputSources;
    }

    /**
     * Method to get the model id from the NadModel enum
     *
     * @return id - NAD model
     */
    public String getId() {
        return id;
    }

    /**
     * Method to get the maximum zones allowed for the device from the NadModel enum
     *
     * @return maxZones - maximum number of zones that can be configured for the model
     */
    public int getMaxZones() {
        return maxZones;
    }

    /**
     * Method to get the input sources available for the device from the NadModel enum
     *
     * @return numberOfInputSources - number of input sources available for this model
     */
    public int getNumberOfInputSources() {
        return numberOfInputSources;
    }
}
