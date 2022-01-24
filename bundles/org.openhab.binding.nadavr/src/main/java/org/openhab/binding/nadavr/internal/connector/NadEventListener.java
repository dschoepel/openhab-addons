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
package org.openhab.binding.nadavr.internal.connector;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.nadavr.internal.nadcp.NadMessage;

/**
 * The {@link NadEventListener.java} class contains fields mapping thing configuration parameters.
 *
 * @author Dave J Schoepel - Initial contribution
 */
@NonNullByDefault
public interface NadEventListener {

    /**
     * The telnet client has received a line.
     *
     * @param ip the Ethernet address of the NAD device
     * @param msg the received message
     */
    void receivedMessage(String ip, NadMessage msg);

    /**
     * The telnet client has successfully connect to the receiver.
     *
     * @param ip the Ethernet address of the NAD device
     * @param errorMsg reson for the error
     */
    void connectionError(String ip, @Nullable String errorMsg);
}
