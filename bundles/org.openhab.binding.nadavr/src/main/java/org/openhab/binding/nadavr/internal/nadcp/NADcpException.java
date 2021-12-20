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
package org.openhab.binding.nadavr.internal.nadcp;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link NADcpException} handles exceptions for NAD command protocol errors.
 *
 * @author Dave J Schoepel - Initial contribution
 */
@NonNullByDefault
public class NADcpException extends Exception {

    private static final long serialVersionUID = -419559332711791571L;

    public NADcpException() {
        super();
    }

    public NADcpException(String message) {
        super(message);
    }

    public NADcpException(String message, Throwable cause) {
        super(message, cause);
    }

    public NADcpException(Throwable cause) {
        super(cause);
    }
}
