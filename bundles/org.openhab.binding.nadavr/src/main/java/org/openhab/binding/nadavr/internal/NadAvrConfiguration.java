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
     * The host name of the NAD A/V Receiver
     */
    public String hostname = "";

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

    // private NADAvrConnector connector;

    // Default zone count
    private Integer zoneCount = 2;

    // Default maximum volume
    public static final BigDecimal MAX_VOLUME = new BigDecimal("19");

    private BigDecimal mainVolumeMax = MAX_VOLUME;

    public BigDecimal getMainVolumeMax() {
        return mainVolumeMax;
    }

    public void setMainVolumeMax(BigDecimal mainVolumeMax) {
        this.mainVolumeMax = mainVolumeMax;
    }

    public Integer getZoneCount() {
        return zoneCount;
    }

    public void setZoneCount(Integer count) {
        Integer zoneCount = count;
        this.zoneCount = zoneCount;
    }

    public boolean arePresetNamesEnabled() {
        return enablePresetNames;
    }

    public void setenablePresetNames(boolean enablePresetNames) {
        this.enablePresetNames = enablePresetNames;
    }

    // public NADAvrConnector getConnector() {
    // return connector;
    // }
    //
    // public void setConnector(NADAvrConnector connector) {
    // this.connector = connector;
    // }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public int getTelnetPort() {
        return telnetPort;
    }

    public void setTelnetPort(int telnetPort) {
        this.telnetPort = telnetPort;
    }

    public String getPresetNamesFilePath() {
        return presetNamesFilePath;
    }

    public void setPresetNamesFilePath(String presetNamesFilePath) {
        this.presetNamesFilePath = presetNamesFilePath;
    }

    @Override
    public String toString() {
        return "NadAvrConfiguration [hostname=" + hostname + ", ipAddress=" + ipAddress + ", telnetPort=" + telnetPort
                + ", enablePresetNames=" + enablePresetNames + ", presetNamesFilePath=" + presetNamesFilePath
                + ", zoneCount=" + zoneCount + ", mainVolumeMax=" + mainVolumeMax + "]";
    }
}
