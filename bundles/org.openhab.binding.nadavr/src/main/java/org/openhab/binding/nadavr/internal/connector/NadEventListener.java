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
 * The {@link NadEventListener.java} class to listens for incoming messages from the NAD device and catches diagnostic
 * messages related to connection errors..
 *
 * @author Dave J Schoepel - Initial contribution
 */
@NonNullByDefault
public interface NadEventListener {

    /**
     * The {@link NadIpConnector} client has received a line from the NAD Device.
     *
     * @param msg - Is as state message received from the NAD device in format described in NadProtocol
     */
    void receivedMessage(NadMessage msg);

    /**
     * The {@link NadIpConnector} client has problems connecting to the NAD Device.
     *
     * @param errorMsg - Reason for the communication/connection error
     */
    void connectionError(@Nullable String errorMsg);
}
