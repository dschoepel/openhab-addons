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
package org.openhab.binding.nadavr.internal.factory;

import java.util.concurrent.ScheduledExecutorService;

import org.openhab.binding.nadavr.internal.NADAvrConfiguration;
import org.openhab.binding.nadavr.internal.NADAvrState;
import org.openhab.binding.nadavr.internal.NADAvrStateDescriptionProvider;
import org.openhab.binding.nadavr.internal.connector.NADAvrConnector;
import org.openhab.binding.nadavr.internal.connector.NADAvrTelnetConnector;
import org.openhab.core.thing.ThingUID;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link NADAvrConnectorFactory.java} class contains fields mapping thing configuration parameters.
 *
 * @author Dave J Schoepel - Initial contribution
 */
// @NonNullByDefault
public class NADAvrConnectorFactory {

    private NADAvrStateDescriptionProvider stateDescriptionProvider;

    /**
     *
     */
    public NADAvrConnector getConnector(NADAvrConfiguration config, NADAvrState state,
            NADAvrStateDescriptionProvider stateDescriptionProvider, ScheduledExecutorService scheduler,
            ThingUID thingUID) {
        return new NADAvrTelnetConnector(config, state, stateDescriptionProvider, scheduler, thingUID);
    }

    @Reference
    protected void setDynamicStateDescriptionProvider(NADAvrStateDescriptionProvider provider) {
        this.stateDescriptionProvider = provider;
    }

    // protected void unsetDynamicStateDescriptionProvider(NADAvrStateDescriptionProvider provider) {
    // this.stateDescriptionProvider = null;
    // }
}
