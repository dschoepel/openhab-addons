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

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.type.ChannelTypeUID;

/**
 * The {@link TailwindBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Dave J. Schoepel - Initial contributions
 */
@NonNullByDefault
public class TailwindBindingConstants {

    public static final String BINDING_ID = "tailwind";
    // private static final String SYSTEM_STATE_CHANNEL_TYPE = "system";

    // List of all Thing Type UIDs (TailwindPro may not be needed???)
    public static final ThingTypeUID THING_TYPE_TAILWIND = new ThingTypeUID(BINDING_ID, "tailwindPro");
    public static final ThingTypeUID THING_TYPE_TAILWIND_UNSUPPORTED = new ThingTypeUID(BINDING_ID,
            "tailwindUnsupported");

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPE_UIDS = Stream
            .concat(Stream.of(THING_TYPE_TAILWIND),
                    Arrays.stream(TailwindModel.values()).map(model -> new ThingTypeUID(BINDING_ID, model.getId())))
            .collect(Collectors.toSet());

    // List of Controller Channel ID's
    public static final String CHANNEL_DOOR_NUM = "numberOfDoorSupported";
    public static final String CHANNEL_NIGHT_MODE_ENABLED = "nightModeEnabled";
    public static final String CHANNEL_LED_BRIGHTNESS = "ledBrightness";
    public static final String CHANNEL_ROUTER_RSSI = "routerRssi";
    public static final String CHANNEL_PRODUCT_ID = "productID";
    public static final String CHANNEL_DEVICE_ID = "deviceID";
    public static final String CHANNEL_FIRMWARE_VERSION = "firmwareVersion";
    public static final String PARAMETER_DOOR_NUM_CONNECTED = "numberOfDoorsConnected";

    // List of all Channel ID's
    public static final String CHANNEL_DOOR_1_CONTROLS_INDEX = "doorOne#index";
    public static final String CHANNEL_DOOR_1_CONTROLS_STATUS = "doorOne#status";
    public static final String CHANNEL_DOOR_1_CONTROLS_LOCKUP = "doorOne#lockup";
    public static final String CHANNEL_DOOR_1_CONTROLS_DISABLED = "doorOne#disabled";

    public static final String CHANNEL_DOOR_2_CONTROLS_INDEX = "doorTwo#index";
    public static final String CHANNEL_DOOR_2_CONTROLS_STATUS = "doorTwo#status";
    public static final String CHANNEL_DOOR_2_CONTROLS_LOCKUP = "doorTwo#lockup";
    public static final String CHANNEL_DOOR_2_CONTROLS_DISABLED = "doorTwo#disabled";

    public static final String CHANNEL_DOOR_3_CONTROLS_INDEX = "doorThree#index";
    public static final String CHANNEL_DOOR_3_CONTROLS_STATUS = "doorThree#status";
    public static final String CHANNEL_DOOR_3_CONTROLS_LOCKUP = "doorThree#lockup";
    public static final String CHANNEL_DOOR_3_CONTROLS_DISABLED = "doorThree#disabled";

    // Map of Door 2 Channel Type UIDs (to be added to Thing later when needed)
    public static final Map<String, ChannelTypeUID> DOOR_2_CHANNEL_TYPES = new LinkedHashMap<>();
    static {
        DOOR_2_CHANNEL_TYPES.put(CHANNEL_DOOR_2_CONTROLS_INDEX, new ChannelTypeUID(BINDING_ID, "index"));
        DOOR_2_CHANNEL_TYPES.put(CHANNEL_DOOR_2_CONTROLS_STATUS, new ChannelTypeUID(BINDING_ID, "status"));
        DOOR_2_CHANNEL_TYPES.put(CHANNEL_DOOR_2_CONTROLS_LOCKUP, new ChannelTypeUID(BINDING_ID, "lockup"));
        DOOR_2_CHANNEL_TYPES.put(CHANNEL_DOOR_2_CONTROLS_DISABLED, new ChannelTypeUID(BINDING_ID, "disabled"));
    }

    // Map of Door 3 Channel Type UIDs (to be added to Thing later when needed)
    public static final Map<String, ChannelTypeUID> DOOR_3_CHANNEL_TYPES = new LinkedHashMap<>();
    static {
        DOOR_3_CHANNEL_TYPES.put(CHANNEL_DOOR_3_CONTROLS_INDEX, new ChannelTypeUID(BINDING_ID, "index"));
        DOOR_3_CHANNEL_TYPES.put(CHANNEL_DOOR_3_CONTROLS_STATUS, new ChannelTypeUID(BINDING_ID, "status"));
        DOOR_3_CHANNEL_TYPES.put(CHANNEL_DOOR_3_CONTROLS_LOCKUP, new ChannelTypeUID(BINDING_ID, "lockup"));
        DOOR_3_CHANNEL_TYPES.put(CHANNEL_DOOR_3_CONTROLS_DISABLED, new ChannelTypeUID(BINDING_ID, "disabled"));
    }

    /**
     * Static mapping of ChannelType-to-ItemType
     * The following items belong to the core:
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

    /**
     * Miscellaneous constants
     */
    public static final String TAILWIND_VENDOR_NAME = "tailwind";
    public static final String TAILWIND_THING_LABEL_NAME = "TailWind";
    public static final String TAILWIND_HTTP_SERVER_URL = "httpServerUrl";
    public static final String TAILWIND_BASE_URL_PART_1 = "http://";
    public static final String TAILWIND_BASE_URL_PART_2 = "/json";
    public static final String TAILWIND_HTTP_HEADER_TOKEN = "TOKEN";
    public static final String TAILWIND_OPENHAB_HOST_UDP_PORT = "50904";
    public static final String NOT_FOUND_ERROR = "Not_Found";
    public static final String JSON_RESPONSE_RESULT_OK = "OK";

    /**
     * TailWind device commands. To be parsed into JSON object and modified to reflect
     * the command that is to be sent to the TailWind controller HTTP server API.
     */

    /* ------- Keys for JSON values ----------------- */
    public static String TAILWIND_JSON_KEY_VERSION = "version";
    public static String TAILWIND_JSON_KEY_PRODUCT = "product";
    public static String TAILWIND_JSON_KEY_DATA = "data";
    public static String TAILWIND_JSON_KEY_TYPE = "type";
    public static String TAILWIND_JSON_KEY_NAME = "name";
    public static String TAILWIND_JSON_KEY_VALUE = "value";
    public static String TAILWIND_JSON_KEY_URL = "url";
    public static String TAILWIND_JSON_KEY_PROTO = "proto";
    public static String TAILWIND_JSON_KEY_ENABLE = "enable";
    public static String TAILWIND_JSON_KEY_DOOR_IDX = "door_idx";
    public static String TAILWIND_JSON_KEY_CMD = "cmd";
    public static String TAILWIND_JSON_KEY_BRIGHTNESS = "brightness";

    /* ------- Values for JSON keys ----------------- */
    public static String TAILWIND_JSON_VALUE_PRODUCT = "iQ3";
    public static String TAILWIND_JSON_VALUE_VER_01 = "0.1";
    public static String TAILWIND_JSON_VALUE_VER_02 = "0.2";
    public static String TAILWIND_JSON_VALUE_TYPE_GET = "get";
    public static String TAILWIND_JSON_VALUE_TYPE_SET = "set";
    public static String TAILWIND_JSON_VALUE_NAME_DEV_ST = "dev_st";
    public static String TAILWIND_JSON_VALUE_NAME_NOTIFY_URL = "notify_url";
    public static String TAILWIND_JSON_VALUE_NAME_DOOR_OP = "door_op";
    public static String TAILWIND_JSON_VALUE_NAME_STATUS_LED = "status_led";
    public static String TAILWIND_JSON_VALUE_NAME_IDENTIFY = "identify";
    public static String TAILWIND_JSON_VALUE_NAME_REBOOT = "reboot";
    public static String TAILWIND_JSON_VALUE_NAME_PROTO_HTTP = "http";
    public static String TAILWIND_JSON_VALUE_NAME_PROTPO_UDP = "udp";
    public static int TAILWIND_JSON_VALUE_ENABLE_ON = 1;
    public static int TAILWIND_JSON_VALUE_ENABLE_OFF = 0;
    public static String TAILWIND_JSON_VALUE_CMD_OPEN = "open";
    public static String TAILWIND_JSON_VALUE_CMD_CLOSE = "close";

    public static String TAILWIND_CMD_DEVICE_STATUS_1 = "{\"version\": \"0.1\",\"data\": {\"type\": \"get\",\"name\": \"dev_st\"}}";
    public static String TAILWIND_CMD_SET_STATUS_REPORT = "{\"product\": \"iQ3\",\"version\": \"0.1\",\"data\": {\"type\": \"set\",\"name\": \"notify_url\",\"value\": {\"url\": \"http://192.168.1.1:8888/report\",\"proto\": \"http\",\"enable\": 1}}}";
    public static String TAILWIND_CMD_DOOR_OPEN_OR_CLOSE = "{\"product\": \"iQ3\",\"version\": \"0.1\",\"data\": {\"type\": \"set\",\"name\": \"door_op\",\"value\": {\"door_idx\": 1,\"cmd\": \"open\",}}}";
    public static String TAILWIND_CMD_SET_LED_BRIGHTNESS = "{\"product\": \"iQ3\",\"version\": \"0.1\",\"data\": {\"type\": \"set\",\"name\": \"status_led\",\"value\": {\"brightness\": 100}}}";
    public static String TAILWIND_CMD_IDENTIFY_DEVICE = "{\"product\": \"iQ3\",\"version\": \"0.2\",\"data\": {\"type\": \"set\",\"name\": \"identify\"}}";
    public static String TAILWIND_CMD_REBOOT_DEVICE = "{\"product\": \"iQ3\",\"version\": \"0.2\",\"data\": {\"type\": \"set\",\"name\": \"reboot\"}}";

    public static final Map<String, Object> TAILWIND_CMD_DEVICE_STATUS = new LinkedHashMap<>();
    static {
        TAILWIND_CMD_DEVICE_STATUS.put(TAILWIND_JSON_KEY_VERSION, TAILWIND_JSON_VALUE_VER_01);
        /*---- Nested Data object ------------*/
        Map<String, Object> TAILWIND_CMD_DEVICE_STATUS_DATA = new LinkedHashMap<String, Object>();
        TAILWIND_CMD_DEVICE_STATUS_DATA.put(TAILWIND_JSON_KEY_TYPE, TAILWIND_JSON_VALUE_TYPE_GET);
        TAILWIND_CMD_DEVICE_STATUS_DATA.put(TAILWIND_JSON_KEY_NAME, TAILWIND_JSON_VALUE_NAME_DEV_ST);
        /*---- Status JSON object with nested data object ------------*/
        TAILWIND_CMD_DEVICE_STATUS.put(TAILWIND_JSON_KEY_DATA, TAILWIND_CMD_DEVICE_STATUS_DATA);
    }

    public static final Map<String, Object> TAILWIND_CMD_SET_STATUS_REPORT_URL = new LinkedHashMap<>();
    static {
        TAILWIND_CMD_SET_STATUS_REPORT_URL.put(TAILWIND_JSON_KEY_PRODUCT, TAILWIND_JSON_VALUE_PRODUCT);
        TAILWIND_CMD_SET_STATUS_REPORT_URL.put(TAILWIND_JSON_KEY_VERSION, TAILWIND_JSON_VALUE_VER_01);
        /*---- Nested Data object ------------*/
        Map<String, Object> TAILWIND_CMD_SET_STATUS_REPORT_DATA = new LinkedHashMap<String, Object>();
        TAILWIND_CMD_SET_STATUS_REPORT_DATA.put(TAILWIND_JSON_KEY_TYPE, TAILWIND_JSON_VALUE_TYPE_SET);
        TAILWIND_CMD_SET_STATUS_REPORT_DATA.put(TAILWIND_JSON_KEY_NAME, TAILWIND_JSON_VALUE_NAME_NOTIFY_URL);

        /*---- Nested Value object ------------*/
        Map<String, Object> TAILWIND_CMD_SET_STATUS_REPORT_VALUE = new LinkedHashMap<String, Object>();
        TAILWIND_CMD_SET_STATUS_REPORT_VALUE.put(TAILWIND_JSON_KEY_PROTO, TAILWIND_JSON_VALUE_NAME_PROTPO_UDP);
        TAILWIND_CMD_SET_STATUS_REPORT_VALUE.put(TAILWIND_JSON_KEY_ENABLE, TAILWIND_JSON_VALUE_ENABLE_ON);
        TAILWIND_CMD_SET_STATUS_REPORT_VALUE.put(TAILWIND_JSON_KEY_URL, "http://openHabServerURL");

        /*---- Status JSON data object with nested value object ------------*/
        TAILWIND_CMD_SET_STATUS_REPORT_DATA.put(TAILWIND_JSON_KEY_VALUE, TAILWIND_CMD_SET_STATUS_REPORT_VALUE);
        /*---- Status JSON object with nested data object ------------*/
        TAILWIND_CMD_SET_STATUS_REPORT_URL.put(TAILWIND_JSON_KEY_DATA, TAILWIND_CMD_SET_STATUS_REPORT_DATA);
    }
}
