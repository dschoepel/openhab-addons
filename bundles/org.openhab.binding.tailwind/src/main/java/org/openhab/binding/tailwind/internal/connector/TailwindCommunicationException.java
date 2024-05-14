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
package org.openhab.binding.tailwind.internal.connector;

import java.io.IOException;

import org.eclipse.jdt.annotation.Nullable;

public class TailwindCommunicationException extends IOException {

    private static final long serialVersionUID = 1L;
    private int statusCode = -1;

    public TailwindCommunicationException(Exception e) {
        super(e);
    }

    public TailwindCommunicationException(int statusCode, Exception e) {
        super(e);
        this.statusCode = statusCode;
    }

    public TailwindCommunicationException(int statusCode) {
        this.statusCode = statusCode;
    }

    public TailwindCommunicationException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public TailwindCommunicationException(String message, Exception e) {
        super(message, e);
    }

    public TailwindCommunicationException(String message) {
        super(message);
    }

    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public @Nullable String getMessage() {
        String message = super.getMessage();
        return message == null ? null : "Rest call failed: statusCode=" + statusCode + ", message=" + message;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": statusCode=" + statusCode + ", message=" + super.getMessage()
                + ", cause: " + getCause();
    }
}
