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
package org.openhab.binding.tailwind.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link tailwindConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Dave J. Schoepel - Initial contribution
 */
@NonNullByDefault
public class tailwindConfiguration {

    /**
     * The number of garage doors connected to the controller
     */
    public int doorCount = 1; // 1=default (can be 1-3)

    /**
     * Authorization token for the local API server on the controller
     */
    public String authToken = ""; // token from TailWind mobile application

    /**
     * @return doorCount to be used to dynamically configure the door channels
     */
    public int getDoorCount() {
        return doorCount;
    }

    /**
     * @return authToken to be used with the requests to the API server on the controller
     */
    public String getAuthToken() {
        return authToken;
    }

    @Override
    public String toString() {
        return "tailwindConfiguration [doorCount=" + doorCount + ", authToken=" + authToken + "]";
    }

}
