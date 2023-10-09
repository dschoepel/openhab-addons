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

import static org.openhab.binding.nadavr.internal.NadAvrBindingConstants.SUPPORTED_THING_TYPE_UIDS;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.nadavr.internal.handler.NadAvrHandler;
import org.openhab.binding.nadavr.internal.state.NadAvrStateDescriptionProvider;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link NadAvrHandlerFactory} is responsible for creating NAD AVR things and thing handlers.
 *
 * @author Dave J Schoepel - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.nadavr", service = ThingHandlerFactory.class)
public class NadAvrHandlerFactory extends BaseThingHandlerFactory {

    private final Logger logger = LoggerFactory.getLogger(NadAvrHandlerFactory.class);
    private NadAvrStateDescriptionProvider stateDescriptionProvider = new NadAvrStateDescriptionProvider();

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        logger.debug("supportsThingType is using thingTypeUID: {}", thingTypeUID);
        if (SUPPORTED_THING_TYPE_UIDS.contains(thingTypeUID)) {
            if (logger.isDebugEnabled()) {
                logger.debug("supportsThingType found thingTypeUID: {} in SUPPORTED_THING_TYPE_UIDS.", thingTypeUID);
            }
        }
        return SUPPORTED_THING_TYPE_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();
        if (logger.isDebugEnabled()) {
            logger.debug("createHandler is comparing {} to list of supported thing types: {}", thingTypeUID,
                    SUPPORTED_THING_TYPE_UIDS);
        }
        if (SUPPORTED_THING_TYPE_UIDS.contains(thingTypeUID)) {
            return new NadAvrHandler(thing, stateDescriptionProvider);
        }
        return null;
    }

    @Reference
    protected void setDynamicStateDescriptionProvider(NadAvrStateDescriptionProvider provider) {
        this.stateDescriptionProvider = provider;
    }
}
