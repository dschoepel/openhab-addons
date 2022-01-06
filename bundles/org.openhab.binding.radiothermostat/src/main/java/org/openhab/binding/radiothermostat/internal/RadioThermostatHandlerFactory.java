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
package org.openhab.binding.radiothermostat.internal;

import static org.openhab.binding.radiothermostat.internal.RadioThermostatBindingConstants.THING_TYPE_RTHERM;

import java.util.Collections;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.binding.radiothermostat.internal.handler.RadioThermostatHandler;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link RadioThermostatHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Michael Lobstein - Initial contribution
 */
@NonNullByDefault
@Component(service = ThingHandlerFactory.class, configurationPid = "binding.radiothermostat")
public class RadioThermostatHandlerFactory extends BaseThingHandlerFactory {

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections.singleton(THING_TYPE_RTHERM);
    private final RadioThermostatStateDescriptionProvider stateDescriptionProvider;
    private final HttpClient httpClient;

    @Activate
    public RadioThermostatHandlerFactory(final @Reference RadioThermostatStateDescriptionProvider provider,
            final @Reference HttpClientFactory httpClientFactory) {
        this.stateDescriptionProvider = provider;
        this.httpClient = httpClientFactory.getCommonHttpClient();
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (thingTypeUID.equals(THING_TYPE_RTHERM)) {
            RadioThermostatHandler handler = new RadioThermostatHandler(thing, stateDescriptionProvider, httpClient);
            return handler;
        }

        return null;
    }
}
