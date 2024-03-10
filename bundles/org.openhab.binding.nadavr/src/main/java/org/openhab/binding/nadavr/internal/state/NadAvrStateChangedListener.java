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
package org.openhab.binding.nadavr.internal.state;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.types.State;

/**
 * The {@link NadAvrStateChangedListener.java} class listens for state changes/errors and passes along to Handler.
 *
 * @author Dave J Schoepel - Initial contribution
 */
@NonNullByDefault
public interface NadAvrStateChangedListener {
    /**
     * Update was received.
     *
     * @param channelID the channel for which its state changed
     * @param state the new state of the channel
     */
    void stateChanged(String channelID, State state);

    /**
     * A connection error occurred
     *
     * @param errorMessage the error message
     */
    void connectionError(String errorMessage);
}
