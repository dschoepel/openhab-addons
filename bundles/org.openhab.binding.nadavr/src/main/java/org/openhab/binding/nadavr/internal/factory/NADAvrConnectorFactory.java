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
package org.openhab.binding.nadavr.internal.factory;

import org.eclipse.jdt.annotation.NonNullByDefault;
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
@NonNullByDefault
public class NADAvrConnectorFactory {

    private NADAvrStateDescriptionProvider stateDescriptionProvider = new NADAvrStateDescriptionProvider();

    /**
     *
     */
    // public NADAvrConnector getConnector(NADAvrConfiguration config, NADAvrState state,
    // NADAvrStateDescriptionProvider stateDescriptionProvider, ScheduledExecutorService scheduler,
    // ThingUID thingUID) {
    // return new NADAvrTelnetConnector(config, state, stateDescriptionProvider, scheduler, thingUID);
    // }

    public NADAvrConnector getConnector(NADAvrConfiguration config, NADAvrState state,
            NADAvrStateDescriptionProvider stateDescriptionProvider, ThingUID thingUID) {
        return new NADAvrTelnetConnector(config, state, stateDescriptionProvider, thingUID);
    }

    @Reference
    protected void setDynamicStateDescriptionProvider(NADAvrStateDescriptionProvider provider) {
        this.stateDescriptionProvider = provider;
    }

    public NADAvrStateDescriptionProvider getStateDescriptionProvider() {
        return stateDescriptionProvider;
    }

    // protected void unsetDynamicStateDescriptionProvider(NADAvrStateDescriptionProvider provider) {
    // this.stateDescriptionProvider = null;
    // }
}
