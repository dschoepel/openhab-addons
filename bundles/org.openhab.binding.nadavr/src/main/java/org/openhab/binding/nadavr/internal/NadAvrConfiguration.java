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

import java.math.BigDecimal;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link NadAvrConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Dave J Schoepel - Initial contribution
 */
@NonNullByDefault
public class NadAvrConfiguration {

    /**
     * The refresh interval in seconds for the NAD A/V Receiver
     */
    public int refreshInterval = 0; // 0=default (disabled)

    /**
     * The IP Address for the NAD A/V Receiver
     */
    public String ipAddress = "";

    /**
     * The telnet port used for connecting the telnet session for the NAD A/V Receiver
     */
    public int telnetPort = 23;

    /**
     * Enable user defined external tuner preset name detail file
     *
     * @return enable NadPreset Names
     */
    public boolean enablePresetNames = false;

    /**
     * Path including file name to user defined tuner preset name details file
     */
    public String presetNamesFilePath = "";

    // Default zone count
    private Integer zoneCount = 4;

    // Enable Tuner support
    public boolean enableTunerSupport = false;

    // Default maximum volume
    public static final BigDecimal MAX_VOLUME = new BigDecimal("19");

    private BigDecimal mainVolumeMax = MAX_VOLUME;

    /**
     * @return zoneCount to be used to dynamically configure the zone channels for the NAD AVR thing
     */
    public int getRefreshInterval() {
        return refreshInterval;
    }

    /**
     * @return zoneCount to be used to dynamically configure the zone channels for the NAD AVR thing
     */
    public int getZoneCount() {
        return zoneCount;
    }

    /**
     * @return zoneCount to be used to dynamically configure the zone channels for the NAD AVR thing
     */
    public boolean getEnableTunerSupport() {
        return enableTunerSupport;
    }

    /**
     * @return enablePresetNames true if a file has been provided for the tuner preset names assigned to the numeric
     *         preset numbers
     */
    public boolean arePresetNamesEnabled() {
        return enablePresetNames;
    }

    /**
     * @return presetNamesFilePath for xml file that defines the tuner preset details
     */
    public String getPresetNamesFilePath() {
        return presetNamesFilePath;
    }

    @Override
    public String toString() {
        return "NadAvrConfiguration [ipAddress=" + ipAddress + ", telnetPort=" + telnetPort + ", enablePresetNames="
                + enablePresetNames + ", presetNamesFilePath=" + presetNamesFilePath + ", zoneCount=" + zoneCount
                + ", enableTunerSupport=" + enableTunerSupport + ", mainVolumeMax=" + mainVolumeMax + "]";
    }
}
