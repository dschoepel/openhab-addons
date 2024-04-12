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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.type.ChannelTypeUID;

/**
 * The {@link tailwindBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Dave J. Schoepel - Initial contribution
 */
@NonNullByDefault
public class tailwindBindingConstants {

    private static final String BINDING_ID = "tailwind";
    private static final String SYSTEM_STATE_CHANNEL_TYPE = "system";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_TAILWIND = new ThingTypeUID(BINDING_ID, "tailwindPro");

    // List of Controller ID's
    public static final String PARAMETER_DOOR_NUM = "doorNum";
    public static final String PARAMETER_NIGHT_MODE_ENABLED = "nightModeEnabled";
    public static final String PARAMETER_LED_BRIGHTNESS = "ledBrightness";
    public static final String PARAMETER_ROUTER_RSSI = "routerRssi";
    public static final String PARAMETER_PRODUCT_ID = "productID";
    public static final String PARAMETER_DEVICE_ID = "deviceID";

    // List of all Channel ID's
    public static final String CHANNEL_DOOR_1_CONTROLS_INDEX = "door1Controls#index";
    public static final String CHANNEL_DOOR_1_CONTROLS_STATUS = "door1Controls#status";
    public static final String CHANNEL_DOOR_1_CONTROLS_LOCKUP = "door1Controls#lockup";
    public static final String CHANNEL_DOOR_1_CONTROLS_DISABLED = "door1Controls#disabled";

    public static final String CHANNEL_DOOR_2_CONTROLS_INDEX = "door2Controls#index";
    public static final String CHANNEL_DOOR_2_CONTROLS_STATUS = "door2Controls#status";
    public static final String CHANNEL_DOOR_2_CONTROLS_LOCKUP = "door2Controls#lockup";
    public static final String CHANNEL_DOOR_2_CONTROLS_DISABLED = "door2Controls#disabled";

    public static final String CHANNEL_DOOR_3_CONTROLS_INDEX = "door3Controls#index";
    public static final String CHANNEL_DOOR_3_CONTROLS_STATUS = "door3Controls#status";
    public static final String CHANNEL_DOOR_3_CONTROLS_LOCKUP = "door3Controls#lockup";
    public static final String CHANNEL_DOOR_3_CONTROLS_DISABLED = "door3Controls#disabled";

    // Map of Door 2 Channel Type UIDs (to be added to Thing later when needed)
    public static final Map<String, ChannelTypeUID> DOOR_2_CHANNEL_TYPES = new LinkedHashMap<>();
    static {
        DOOR_2_CHANNEL_TYPES.put(CHANNEL_DOOR_2_CONTROLS_INDEX,
                new ChannelTypeUID(SYSTEM_STATE_CHANNEL_TYPE, "number"));
        DOOR_2_CHANNEL_TYPES.put(CHANNEL_DOOR_2_CONTROLS_STATUS,
                new ChannelTypeUID(SYSTEM_STATE_CHANNEL_TYPE, "string"));
        DOOR_2_CHANNEL_TYPES.put(CHANNEL_DOOR_2_CONTROLS_LOCKUP,
                new ChannelTypeUID(SYSTEM_STATE_CHANNEL_TYPE, "number"));
        DOOR_2_CHANNEL_TYPES.put(CHANNEL_DOOR_2_CONTROLS_DISABLED,
                new ChannelTypeUID(SYSTEM_STATE_CHANNEL_TYPE, "number"));
    }

    // Map of Door 3 Channel Type UIDs (to be added to Thing later when needed)
    public static final Map<String, ChannelTypeUID> DOOR_3_CHANNEL_TYPES = new LinkedHashMap<>();
    static {
        DOOR_3_CHANNEL_TYPES.put(CHANNEL_DOOR_3_CONTROLS_INDEX,
                new ChannelTypeUID(SYSTEM_STATE_CHANNEL_TYPE, "number"));
        DOOR_3_CHANNEL_TYPES.put(CHANNEL_DOOR_3_CONTROLS_STATUS,
                new ChannelTypeUID(SYSTEM_STATE_CHANNEL_TYPE, "string"));
        DOOR_3_CHANNEL_TYPES.put(CHANNEL_DOOR_3_CONTROLS_LOCKUP,
                new ChannelTypeUID(SYSTEM_STATE_CHANNEL_TYPE, "number"));
        DOOR_3_CHANNEL_TYPES.put(CHANNEL_DOOR_3_CONTROLS_DISABLED,
                new ChannelTypeUID(SYSTEM_STATE_CHANNEL_TYPE, "number"));
    }

    /**
     * Static mapping of ChannelType-to-ItemType
     * The following items belong to the core:
     * Switch, Rollershutter, Contact, String, Number, Dimmer, DateTime, Color, Image, Location, Player, Call.
     */
    public static final Map<String, String> CHANNEL_ITEM_TYPES = new HashMap<>();
    static {
        CHANNEL_ITEM_TYPES.put(CHANNEL_DOOR_2_CONTROLS_INDEX, "Number");
        CHANNEL_ITEM_TYPES.put(CHANNEL_DOOR_2_CONTROLS_STATUS, "String");
        CHANNEL_ITEM_TYPES.put(CHANNEL_DOOR_2_CONTROLS_LOCKUP, "Number");
        CHANNEL_ITEM_TYPES.put(CHANNEL_DOOR_2_CONTROLS_DISABLED, "Number");

        CHANNEL_ITEM_TYPES.put(CHANNEL_DOOR_3_CONTROLS_INDEX, "Number");
        CHANNEL_ITEM_TYPES.put(CHANNEL_DOOR_3_CONTROLS_STATUS, "String");
        CHANNEL_ITEM_TYPES.put(CHANNEL_DOOR_3_CONTROLS_LOCKUP, "Number");
        CHANNEL_ITEM_TYPES.put(CHANNEL_DOOR_3_CONTROLS_DISABLED, "Number");
    }

    /**
     * Static mapping of ChannelType-to-ItemLables
     */
    public static final Map<String, @Nullable String> CHANNEL_ITEM_LABELS = new HashMap<>();
    static {
        CHANNEL_ITEM_LABELS.put(CHANNEL_DOOR_2_CONTROLS_INDEX, "Index Door 2");
        CHANNEL_ITEM_LABELS.put(CHANNEL_DOOR_2_CONTROLS_STATUS, "Status Door 2");
        CHANNEL_ITEM_LABELS.put(CHANNEL_DOOR_2_CONTROLS_LOCKUP, "Locked Up Status Door 2");
        CHANNEL_ITEM_LABELS.put(CHANNEL_DOOR_2_CONTROLS_DISABLED, "Is Configured Door 2");

        CHANNEL_ITEM_LABELS.put(CHANNEL_DOOR_3_CONTROLS_INDEX, "Index Door 3");
        CHANNEL_ITEM_LABELS.put(CHANNEL_DOOR_3_CONTROLS_STATUS, "Status Door 3");
        CHANNEL_ITEM_LABELS.put(CHANNEL_DOOR_3_CONTROLS_LOCKUP, "Locked Up Status Door 3");
        CHANNEL_ITEM_LABELS.put(CHANNEL_DOOR_3_CONTROLS_DISABLED, "Is Configured Door 3");
    }
}
