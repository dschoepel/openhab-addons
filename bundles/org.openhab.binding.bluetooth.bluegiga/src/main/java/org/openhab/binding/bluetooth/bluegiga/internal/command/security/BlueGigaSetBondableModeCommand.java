/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.binding.bluetooth.bluegiga.internal.command.security;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.bluetooth.bluegiga.internal.BlueGigaCommand;

/**
 * Class to implement the BlueGiga command <b>setBondableMode</b>.
 * <p>
 * This command is used to enter a passkey required for Man-in-the-Middle pairing. It should be
 * sent as a response to Passkey Request event.
 * <p>
 * This class provides methods for processing BlueGiga API commands.
 * <p>
 * Note that this code is autogenerated. Manual changes may be overwritten.
 *
 * @author Chris Jackson - Initial contribution of Java code generator
 */
@NonNullByDefault
public class BlueGigaSetBondableModeCommand extends BlueGigaCommand {
    public static final int COMMAND_CLASS = 0x05;
    public static final int COMMAND_METHOD = 0x01;

    /**
     * Enables or disables bonding mode. 0 : the device is not bondable. 1 : the device is bondable
     * <p>
     * BlueGiga API type is <i>boolean</i> - Java type is {@link boolean}
     */
    private boolean bondable;

    /**
     * Enables or disables bonding mode. 0 : the device is not bondable. 1 : the device is bondable
     *
     * @param bondable the bondable to set as {@link boolean}
     */
    public void setBondable(boolean bondable) {
        this.bondable = bondable;
    }

    @Override
    public int[] serialize() {
        // Serialize the header
        serializeHeader(COMMAND_CLASS, COMMAND_METHOD);

        // Serialize the fields
        serializeBoolean(bondable);

        return getPayload();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("BlueGigaSetBondableModeCommand [bondable=");
        builder.append(bondable);
        builder.append(']');
        return builder.toString();
    }
}
