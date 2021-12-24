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

import java.math.BigDecimal;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link NADAvrConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Dave J Schoepel - Initial contribution
 */
@NonNullByDefault
public class NADAvrConfiguration {

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
     * Telnet is enabled vs serial
     *
     * @return telnetEnabled
     */
    public boolean telnetEnabled = true;

    // private NADAvrConnector connector;

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

    public boolean isTelnet() {
        return telnetEnabled;
    }

    public void setTelnet(boolean telnetEnabled) {
        this.telnetEnabled = telnetEnabled;
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

    @Override
    public String toString() {
        return "NADAvrConfiguration [hostname=" + hostname + ", ipAddress=" + ipAddress + ", telnetPort=" + telnetPort
                + ", telnetEnabled=" + telnetEnabled + ", zoneCount=" + zoneCount + "]";
    }
}
