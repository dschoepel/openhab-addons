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

import static org.openhab.binding.nadavr.internal.NADAvrBindingConstants.SUPPORTED_THING_TYPE_UIDS;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.nadavr.internal.NADAvrStateDescriptionProvider;
import org.openhab.binding.nadavr.internal.handler.NADAvrHandler;
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
 * The {@link NADAvrHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Dave J Schoepel - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.nadavr", service = ThingHandlerFactory.class)
public class NADAvrHandlerFactory extends BaseThingHandlerFactory {

    private final Logger logger = LoggerFactory.getLogger(NADAvrHandlerFactory.class);
    // private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Set.of(THING_TYPE_NADAVR);
    private NADAvrStateDescriptionProvider stateDescriptionProvider = new NADAvrStateDescriptionProvider();

    // @Activate
    // public NADAvrHandlerFactory(NADAvrStateDescriptionProvider stateDescriptionProvider) {
    // super();
    // this.stateDescriptionProvider = stateDescriptionProvider;
    // }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        logger.debug("supportsThingType is using thingTypeUID: {}", thingTypeUID);
        if (SUPPORTED_THING_TYPE_UIDS.contains(thingTypeUID)) {
            logger.debug("supportsThingType found thingTypeUID: {} in SUPPORTED_THING_TYPE_UIDS.", thingTypeUID);
        }
        return SUPPORTED_THING_TYPE_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();
        logger.debug("createHandler is comparing {} to list of supported thing types: {}", thingTypeUID,
                SUPPORTED_THING_TYPE_UIDS);
        if (SUPPORTED_THING_TYPE_UIDS.contains(thingTypeUID)) {
            return new NADAvrHandler(thing, stateDescriptionProvider);
            // return new NADHandler(thing, stateDescriptionProvider);
        }

        return null;
    }

    @Reference
    protected void setDynamicStateDescriptionProvider(NADAvrStateDescriptionProvider provider) {
        this.stateDescriptionProvider = provider;
    }

    // protected void unsetDynamicStateDescriptionProvider(NADAvrStateDescriptionProvider provider) {
    // this.stateDescriptionProvider = null;
    // }
}
