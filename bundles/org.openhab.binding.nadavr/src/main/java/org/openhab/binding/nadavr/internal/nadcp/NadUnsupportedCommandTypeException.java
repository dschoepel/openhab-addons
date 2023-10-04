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
package org.openhab.binding.nadavr.internal.nadcp;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link NadUnsupportedCommandTypeException.java} class handles command exceptions for the the NAD AVR thing.
 *
 * @author Dave J Schoepel - Initial contribution
 */
@NonNullByDefault
public class NadUnsupportedCommandTypeException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Method to send error when an unsupported command is encountered
     */
    public NadUnsupportedCommandTypeException() {
        super();
    }

    /**
     * Method to send error when an unsupported command is encountered with additional diagnostic information
     *
     * @param message - include diagnostic information for troubleshooting errors
     */
    public NadUnsupportedCommandTypeException(String message) {
        super(message);
    }
}
