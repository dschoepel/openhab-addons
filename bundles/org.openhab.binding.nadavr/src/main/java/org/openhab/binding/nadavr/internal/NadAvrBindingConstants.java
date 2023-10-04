/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.binding.nadavr.internal;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.type.ChannelTypeUID;

/**
 * The {@link NadAvrBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Dave J Schoepel - Initial contribution
 */
@NonNullByDefault
public class NadAvrBindingConstants {

    private static final String BINDING_ID = "nadavr";
    private static final String SYSTEM_STATE_CHANNEL_TYPE = "system";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_NADAVR = new ThingTypeUID(BINDING_ID, "nadAvr");
    public static final ThingTypeUID THING_TYPE_NAD_UNSUPPORTED = new ThingTypeUID(BINDING_ID, "nadUnsupported");

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPE_UIDS = Stream
            .concat(Stream.of(THING_TYPE_NADAVR, THING_TYPE_NAD_UNSUPPORTED),
                    Arrays.stream(NadModel.values()).map(model -> new ThingTypeUID(BINDING_ID, model.getId())))
            .collect(Collectors.toSet());

    // List of thing Parameter names
    public static final String PARAMETER_ZONE_COUNT = "zoneCount";
    public static final String PARAMETER_HOST = "hostname";
    public static final String PARAMETER_TELNET_PORT = "telnetPort";
    public static final String PARAMETER_IP_ADDRESS = "ipAddress";
    public static final String PARAMETER_MAX_ZONES = "maximumZones";
    public static final String PARAMETER_ENABLE_PRESET_NAMES = "enablePresetNames";
    public static final String PARAMETER_PRESET_NAMES_FILE_PATH = "presetNamesFilePath";

    // List of all Channel ids

    public static final String CHANNEL_TUNER_BAND = "tuner#band";
    public static final String CHANNEL_TUNER_PRESET = "tuner#preset";
    public static final String CHANNEL_TUNER_PRESET_DETAIL = "tuner#presetDetail";
    public static final String CHANNEL_TUNER_AM_FREQUENCY = "tuner#amFrequency";
    public static final String CHANNEL_TUNER_FM_FREQUENCY = "tuner#fmFrequency";
    public static final String CHANNEL_TUNER_FM_MUTE = "tuner#fmMute";
    public static final String CHANNEL_TUNER_FM_BLEND = "tuner#fmBlend";
    public static final String CHANNEL_TUNER_FM_RDS_TEXT = "tuner#fmRdsText";
    public static final String CHANNEL_TUNER_FM_RDS_NAME = "tuner#fmRdsName";
    public static final String CHANNEL_TUNER_XM_CHANNEL = "tuner#xmChannel";
    public static final String CHANNEL_TUNER_XM_CHANNEL_NAME = "tuner#xmChannelName";
    public static final String CHANNEL_TUNER_XM_NAME = "tuner#xmName";
    public static final String CHANNEL_TUNER_XM_SONG_TITLE = "tuner#xmSongTitle";
    public static final String CHANNEL_TUNER_DAB_DLS_TEXT = "tuner#dabDlsText";
    public static final String CHANNEL_TUNER_DAB_SERVICE_NAME = "tuner#dabServiceName";

    public static final String CHANNEL_MAIN_POWER = "zone1#power";
    public static final String CHANNEL_MAIN_LISTENING_MODE = "zone1#listeningMode";
    public static final String CHANNEL_MAIN_VOLUME = "zone1#volume";
    public static final String CHANNEL_MAIN_VOLUME_DB = "zone1#volumeDB";
    public static final String CHANNEL_MAIN_MUTE = "zone1#mute";
    public static final String CHANNEL_MAIN_SOURCE = "zone1#source";
    public static final String CHANNEL_MAIN_MODEL = "zone1#model";
    public static final String CHANNEL_MAIN_SLEEP = "zone1#sleep";
    public static final String CHANNEL_MAIN_VERSION = "zone1#version";

    public static final String CHANNEL_ZONE2_POWER = "zone2#power";
    public static final String CHANNEL_ZONE2_VOLUME = "zone2#volume";
    public static final String CHANNEL_ZONE2_VOLUME_DB = "zone2#volumeDB";
    public static final String CHANNEL_ZONE2_MUTE = "zone2#mute";
    public static final String CHANNEL_ZONE2_SOURCE = "zone2#source";
    public static final String CHANNEL_ZONE2_VOLUME_FIXED_DB = "zone2#volumeFixedDB";
    public static final String CHANNEL_ZONE2_VOLUME_FIXED = "zone2#volumeFixed";
    public static final String CHANNEL_ZONE2_VOLUME_CONTROL = "zone2#volumeControl";

    public static final String CHANNEL_ZONE3_VOLUME = "zone3#volume";
    public static final String CHANNEL_ZONE3_POWER = "zone3#power";
    public static final String CHANNEL_ZONE3_VOLUME_DB = "zone3#volumeDB";
    public static final String CHANNEL_ZONE3_MUTE = "zone3#mute";
    public static final String CHANNEL_ZONE3_SOURCE = "zone3#source";
    public static final String CHANNEL_ZONE3_VOLUME_FIXED_DB = "zone3#volumeFixedDB";
    public static final String CHANNEL_ZONE3_VOLUME_FIXED = "zone3#volumeFixed";
    public static final String CHANNEL_ZONE3_VOLUME_CONTROL = "zone3#volumeControl";

    public static final String CHANNEL_ZONE4_POWER = "zone4#power";
    public static final String CHANNEL_ZONE4_VOLUME = "zone4#volume";
    public static final String CHANNEL_ZONE4_VOLUME_DB = "zone4#volumeDB";
    public static final String CHANNEL_ZONE4_MUTE = "zone4#mute";
    public static final String CHANNEL_ZONE4_SOURCE = "zone4#source";
    public static final String CHANNEL_ZONE4_VOLUME_FIXED_DB = "zone4#volumeFixedDB";
    public static final String CHANNEL_ZONE4_VOLUME_FIXED = "zone4#volumeFixed";
    public static final String CHANNEL_ZONE4_VOLUME_CONTROL = "zone4#volumeControl";

    // Map of Zone2 Channel Type UIDs (to be added to Thing later when needed)
    public static final Map<String, ChannelTypeUID> ZONE2_CHANNEL_TYPES = new LinkedHashMap<>();
    static {
        ZONE2_CHANNEL_TYPES.put(CHANNEL_ZONE2_POWER, new ChannelTypeUID(SYSTEM_STATE_CHANNEL_TYPE, "power"));
        ZONE2_CHANNEL_TYPES.put(CHANNEL_ZONE2_VOLUME, new ChannelTypeUID(SYSTEM_STATE_CHANNEL_TYPE, "volume"));
        ZONE2_CHANNEL_TYPES.put(CHANNEL_ZONE2_VOLUME_DB, new ChannelTypeUID(BINDING_ID, "volumeDB"));
        ZONE2_CHANNEL_TYPES.put(CHANNEL_ZONE2_MUTE, new ChannelTypeUID(SYSTEM_STATE_CHANNEL_TYPE, "mute"));
        ZONE2_CHANNEL_TYPES.put(CHANNEL_ZONE2_SOURCE, new ChannelTypeUID(BINDING_ID, "source"));
        ZONE2_CHANNEL_TYPES.put(CHANNEL_ZONE2_VOLUME_FIXED_DB, new ChannelTypeUID(BINDING_ID, "volumeFixedDB"));
        ZONE2_CHANNEL_TYPES.put(CHANNEL_ZONE2_VOLUME_FIXED, new ChannelTypeUID(SYSTEM_STATE_CHANNEL_TYPE, "volume"));
        ZONE2_CHANNEL_TYPES.put(CHANNEL_ZONE2_VOLUME_CONTROL, new ChannelTypeUID(BINDING_ID, "volumeControl"));
    }

    // Map of Zone3 Channel Type UIDs (to be added to Thing later when needed)
    public static final Map<String, ChannelTypeUID> ZONE3_CHANNEL_TYPES = new LinkedHashMap<>();
    static {
        ZONE3_CHANNEL_TYPES.put(CHANNEL_ZONE3_POWER, new ChannelTypeUID(SYSTEM_STATE_CHANNEL_TYPE, "power"));
        ZONE3_CHANNEL_TYPES.put(CHANNEL_ZONE3_SOURCE, new ChannelTypeUID(BINDING_ID, "source"));
        ZONE3_CHANNEL_TYPES.put(CHANNEL_ZONE3_VOLUME, new ChannelTypeUID(SYSTEM_STATE_CHANNEL_TYPE, "volume"));
        ZONE3_CHANNEL_TYPES.put(CHANNEL_ZONE3_VOLUME_DB, new ChannelTypeUID(BINDING_ID, "volumeDB"));
        ZONE3_CHANNEL_TYPES.put(CHANNEL_ZONE3_MUTE, new ChannelTypeUID(SYSTEM_STATE_CHANNEL_TYPE, "mute"));
        ZONE3_CHANNEL_TYPES.put(CHANNEL_ZONE3_VOLUME_FIXED_DB, new ChannelTypeUID(BINDING_ID, "volumeFixedDB"));
        ZONE3_CHANNEL_TYPES.put(CHANNEL_ZONE3_VOLUME_FIXED, new ChannelTypeUID(SYSTEM_STATE_CHANNEL_TYPE, "volume"));
        ZONE3_CHANNEL_TYPES.put(CHANNEL_ZONE3_VOLUME_CONTROL, new ChannelTypeUID(BINDING_ID, "volumeControl"));

    }

    // Map of Zone4 Channel Type UIDs (to be added to Thing later when needed)
    public static final Map<String, ChannelTypeUID> ZONE4_CHANNEL_TYPES = new LinkedHashMap<>();
    static {
        ZONE4_CHANNEL_TYPES.put(CHANNEL_ZONE4_POWER, new ChannelTypeUID(SYSTEM_STATE_CHANNEL_TYPE, "power"));
        ZONE4_CHANNEL_TYPES.put(CHANNEL_ZONE4_VOLUME, new ChannelTypeUID(SYSTEM_STATE_CHANNEL_TYPE, "volume"));
        ZONE4_CHANNEL_TYPES.put(CHANNEL_ZONE4_VOLUME_DB, new ChannelTypeUID(BINDING_ID, "volumeDB"));
        ZONE4_CHANNEL_TYPES.put(CHANNEL_ZONE4_MUTE, new ChannelTypeUID(SYSTEM_STATE_CHANNEL_TYPE, "mute"));
        ZONE4_CHANNEL_TYPES.put(CHANNEL_ZONE4_SOURCE, new ChannelTypeUID(BINDING_ID, "source"));
        ZONE4_CHANNEL_TYPES.put(CHANNEL_ZONE4_VOLUME_FIXED_DB, new ChannelTypeUID(BINDING_ID, "volumeFixedDB"));
        ZONE4_CHANNEL_TYPES.put(CHANNEL_ZONE4_VOLUME_FIXED, new ChannelTypeUID(SYSTEM_STATE_CHANNEL_TYPE, "volume"));
        ZONE4_CHANNEL_TYPES.put(CHANNEL_ZONE4_VOLUME_CONTROL, new ChannelTypeUID(BINDING_ID, "volumeControl"));
    }

    /**
     * Static mapping of ChannelType-to-ItemType
     */
    public static final Map<String, String> CHANNEL_ITEM_TYPES = new HashMap<>();
    static {
        CHANNEL_ITEM_TYPES.put(CHANNEL_ZONE2_POWER, "Switch");
        CHANNEL_ITEM_TYPES.put(CHANNEL_ZONE2_VOLUME, "Dimmer");
        CHANNEL_ITEM_TYPES.put(CHANNEL_ZONE2_VOLUME_DB, "Number");
        CHANNEL_ITEM_TYPES.put(CHANNEL_ZONE2_MUTE, "Switch");
        CHANNEL_ITEM_TYPES.put(CHANNEL_ZONE2_SOURCE, "String");
        CHANNEL_ITEM_TYPES.put(CHANNEL_ZONE2_VOLUME_FIXED_DB, "Number");
        CHANNEL_ITEM_TYPES.put(CHANNEL_ZONE2_VOLUME_FIXED, "Dimmer");
        CHANNEL_ITEM_TYPES.put(CHANNEL_ZONE2_VOLUME_CONTROL, "String");

        CHANNEL_ITEM_TYPES.put(CHANNEL_ZONE3_POWER, "Switch");
        CHANNEL_ITEM_TYPES.put(CHANNEL_ZONE3_SOURCE, "String");
        CHANNEL_ITEM_TYPES.put(CHANNEL_ZONE3_VOLUME, "Dimmer");
        CHANNEL_ITEM_TYPES.put(CHANNEL_ZONE3_VOLUME_DB, "Number");
        CHANNEL_ITEM_TYPES.put(CHANNEL_ZONE3_MUTE, "Switch");
        CHANNEL_ITEM_TYPES.put(CHANNEL_ZONE3_VOLUME_FIXED_DB, "Number");
        CHANNEL_ITEM_TYPES.put(CHANNEL_ZONE3_VOLUME_FIXED, "Dimmer");
        CHANNEL_ITEM_TYPES.put(CHANNEL_ZONE3_VOLUME_CONTROL, "String");

        CHANNEL_ITEM_TYPES.put(CHANNEL_ZONE4_POWER, "Switch");
        CHANNEL_ITEM_TYPES.put(CHANNEL_ZONE4_VOLUME, "Dimmer");
        CHANNEL_ITEM_TYPES.put(CHANNEL_ZONE4_VOLUME_DB, "Number");
        CHANNEL_ITEM_TYPES.put(CHANNEL_ZONE4_MUTE, "Switch");
        CHANNEL_ITEM_TYPES.put(CHANNEL_ZONE4_SOURCE, "String");
        CHANNEL_ITEM_TYPES.put(CHANNEL_ZONE4_VOLUME_FIXED_DB, "Number");
        CHANNEL_ITEM_TYPES.put(CHANNEL_ZONE4_VOLUME_FIXED, "Dimmer");
        CHANNEL_ITEM_TYPES.put(CHANNEL_ZONE4_VOLUME_CONTROL, "String");
    }

    /**
     * Static mapping of ChannelType-to-ItemType
     */
    public static final Map<String, @Nullable String> CHANNEL_ITEM_LABELS = new HashMap<>();
    static {
        CHANNEL_ITEM_LABELS.put(CHANNEL_ZONE2_POWER, "Power Z2");
        CHANNEL_ITEM_LABELS.put(CHANNEL_ZONE2_VOLUME, "Volume Z2");
        CHANNEL_ITEM_LABELS.put(CHANNEL_ZONE2_VOLUME_DB, "VolumeDB Z2");
        CHANNEL_ITEM_LABELS.put(CHANNEL_ZONE2_MUTE, "Mute Z2");
        CHANNEL_ITEM_LABELS.put(CHANNEL_ZONE2_SOURCE, "Input Source Z2");
        CHANNEL_ITEM_LABELS.put(CHANNEL_ZONE2_VOLUME_FIXED_DB, "Fixed Volume(dB) Z2");
        CHANNEL_ITEM_LABELS.put(CHANNEL_ZONE2_VOLUME_FIXED, "Fixed Volume Z2");
        CHANNEL_ITEM_LABELS.put(CHANNEL_ZONE2_VOLUME_CONTROL, "Volume Control Z2");

        CHANNEL_ITEM_LABELS.put(CHANNEL_ZONE3_POWER, "Power Z3");
        CHANNEL_ITEM_LABELS.put(CHANNEL_ZONE3_VOLUME, "Volume Z3");
        CHANNEL_ITEM_LABELS.put(CHANNEL_ZONE3_VOLUME_DB, "VolumeDB Z3");
        CHANNEL_ITEM_LABELS.put(CHANNEL_ZONE3_MUTE, "Mute Z3");
        CHANNEL_ITEM_LABELS.put(CHANNEL_ZONE3_SOURCE, "Input Source Z3");
        CHANNEL_ITEM_LABELS.put(CHANNEL_ZONE3_VOLUME_FIXED_DB, "Fixed Volume(dB) Z3");
        CHANNEL_ITEM_LABELS.put(CHANNEL_ZONE3_VOLUME_FIXED, "Fixed Volume Z3");
        CHANNEL_ITEM_LABELS.put(CHANNEL_ZONE3_VOLUME_CONTROL, "Volume Control Z3");

        CHANNEL_ITEM_LABELS.put(CHANNEL_ZONE4_POWER, "Power Z4");
        CHANNEL_ITEM_LABELS.put(CHANNEL_ZONE4_VOLUME, "Volume Z4");
        CHANNEL_ITEM_LABELS.put(CHANNEL_ZONE4_VOLUME_DB, "VolumeDB Z4");
        CHANNEL_ITEM_LABELS.put(CHANNEL_ZONE4_MUTE, "Mute Z4");
        CHANNEL_ITEM_LABELS.put(CHANNEL_ZONE4_SOURCE, "Input Source Z4");
        CHANNEL_ITEM_LABELS.put(CHANNEL_ZONE4_VOLUME_FIXED_DB, "Fixed Volume(dB) Z4");
        CHANNEL_ITEM_LABELS.put(CHANNEL_ZONE4_VOLUME_FIXED, "Fixed Volume Z4");
        CHANNEL_ITEM_LABELS.put(CHANNEL_ZONE4_VOLUME_CONTROL, "Volume Control Z4");
    }

    // Used to calculate Volume percentage from dB (range is -99 to 19)
    public static final BigDecimal VOLUME_DB_MIN = new BigDecimal("-99");
    public static final BigDecimal VOLUME_DB_RANGE = new BigDecimal("118");
    public static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    public static final BigDecimal VOLUME_TO_INITIALIZE_STATE = new BigDecimal("-9999");

    // NAD protocol command elements
    public static final String NAD_PREFIX_SOURCE = "Source";
    public static final String NAD_VARIABLE_NAME = "Name";
    public static final String TUNER = "Tuner";
    public static final String C427 = "C427";
    public static final StringType FM = StringType.valueOf("FM");
    public static final StringType XM = StringType.valueOf("XM");
    public static final String NOT_SET = "Not Set";
    public static final String NAD_EQUALS_OPERATOR = "=";
    public static final String NAD_QUERY = "?";
    public static final String ZONE1 = "Main";
    public static final String ZONE2 = "Zone2";
    public static final String ZONE3 = "Zone3";
    public static final String ZONE4 = "Zone4";
    public static final String LOCAL = "Local";
    public static final String NAD_ON = "On";
    public static final String NAD_OFF = "Off";
    public static final String NAD_PRESET = "Preset";
}
