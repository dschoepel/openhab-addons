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
package org.openhab.binding.nadavr.internal.xml;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link NadPreset.java} class contains fields mapping for xml NadPreset Name Details.
 *
 * @author Dave J Schoepel - Initial contribution
 */

@NonNullByDefault
public class NadPreset {

    protected String id = "";

    protected String band = "";

    protected String frequency = "";

    protected String name = "";

    public NadPreset(String id, String band, String frequency, String name) {
        super();
        this.id = id;
        this.band = band;
        this.frequency = frequency;
        this.name = name;
    }

    public String getBand() {
        return band;
    }

    public String getFrequency() {
        return frequency;
    }

    public String getName() {
        return name;
    }

    public String getID() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setBand(String band) {
        this.band = band;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public void setName(String name) {
        this.name = name;
    }
}
