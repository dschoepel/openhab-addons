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
package org.openhab.binding.tailwind.internal.not_used_archive;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.tailwind.internal.connector.TailwindUdpConnector;

/**
 *
 */
@NonNullByDefault
public interface TailwindUdpEventListener {
    /**
     * The {@link TailwindUdpConnector} client has received a notification from the Tailwind thing.
     *
     * @param msg - Is as state message received from the Tailwind device in format described in TailwindControllerData
     */
    void eventReceived(String msg);

    /**
     * The {@link TailwindUdpConnector} client has problems connecting to the NAD Device.
     *
     * @param errorMsg - Reason for the communication/connection error
     */
    void connectionError(@Nullable String errorMsg);
}
