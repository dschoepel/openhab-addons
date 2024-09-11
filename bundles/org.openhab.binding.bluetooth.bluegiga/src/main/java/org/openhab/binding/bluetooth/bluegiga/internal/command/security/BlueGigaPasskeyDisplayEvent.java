/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
import org.openhab.binding.bluetooth.bluegiga.internal.BlueGigaResponse;

/**
 * Class to implement the BlueGiga command <b>passkeyDisplayEvent</b>.
 * <p>
 * This event tells a passkey should be printed to the user for bonding. This passkey must be
 * entered in the remote device for bonding to be successful.
 * <p>
 * This class provides methods for processing BlueGiga API commands.
 * <p>
 * Note that this code is autogenerated. Manual changes may be overwritten.
 *
 * @author Chris Jackson - Initial contribution of Java code generator
 */
@NonNullByDefault
public class BlueGigaPasskeyDisplayEvent extends BlueGigaResponse {
    public static final int COMMAND_CLASS = 0x05;
    public static final int COMMAND_METHOD = 0x02;

    /**
     * Bluetooth connection handle
     * <p>
     * BlueGiga API type is <i>uint8</i> - Java type is {@link int}
     */
    private int handle;

    /**
     * Passkey range: 000000-999999
     * <p>
     * BlueGiga API type is <i>uint32</i> - Java type is {@link long}
     */
    private long passkey;

    /**
     * Event constructor
     */
    public BlueGigaPasskeyDisplayEvent(int[] inputBuffer) {
        // Super creates deserializer and reads header fields
        super(inputBuffer);

        event = (inputBuffer[0] & 0x80) != 0;

        // Deserialize the fields
        handle = deserializeUInt8();
        passkey = deserializeUInt32();
    }

    /**
     * Bluetooth connection handle
     * <p>
     * BlueGiga API type is <i>uint8</i> - Java type is {@link int}
     *
     * @return the current handle as {@link int}
     */
    public int getHandle() {
        return handle;
    }

    /**
     * Passkey range: 000000-999999
     * <p>
     * BlueGiga API type is <i>uint32</i> - Java type is {@link long}
     *
     * @return the current passkey as {@link long}
     */
    public long getPasskey() {
        return passkey;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("BlueGigaPasskeyDisplayEvent [handle=");
        builder.append(handle);
        builder.append(", passkey=");
        builder.append(passkey);
        builder.append(']');
        return builder.toString();
    }
}
