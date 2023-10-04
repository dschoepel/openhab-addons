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
package org.openhab.binding.nadavr.internal.nadcp;

import java.util.HashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link NadCommand} represents the binding supported NAD AVR Ethernet/RS232 Protocol Commands.
 * <ul>
 * <li>Format for a command is Prefix, dot(.), Variable, Operator(+,- =, ?), Value.</li>
 * <li>For example 'Power' is prefixed by Main, Zone2-4 and has operator and value "Main.Power=On" .</li>
 * </ul>
 *
 * @author Dave J Schoepel - Initial contribution
 */
@NonNullByDefault
public enum NadCommand {
    /**
     * Commands supported in this Binding
     */
    EMPTY_COMMAND("", "", "", ""),
    MODEL_QUERY("", "Model", "?", ""),
    MODEL_NAME("", "Model", "=", "%s"),
    POWER_QUERY("", "Power", "?", ""),
    POWER_SET("", "Power", "=", "%s"),
    SOURCE_NAME_QUERY("", "Name", "?", ""),
    SOURCE_NAME("", "Name", "=", "%s"),
    INPUT_SOURCE_QUERY("", "Source", "?", ""),
    SOURCE_SET("", "Source", "=", "%s"),
    TEMP_PSU("", "Temp.PSU", "=", "%s"),
    TEMP_FRONT("", "Temp.Front", "=", "%s"),
    TEMP_CENTER("", "Temp.Center", "=", "%s"),
    TEMP_BACK("", "Temp.Back", "=", "%s"),
    TEMP_SURROUND("", "Temp.Surround", "=", "%s"),
    TEMP_HEIGHT("", "Temp.Height", "=", "%s"),
    FAN_STATUS("", "Fan.Status", "=", "%s"),
    TUNER_FM_MUTE_QUERY("", "FM.Mute", "?", ""),
    TUNER_FM_MUTE_SET("", "FM.Mute", "=", "%s"),
    TUNER_FM_FREQUENCY_QUERY("", "FM.Frequency", "?", ""),
    TUNER_FM_FREQUENCY_SET("", "FM.Frequency", "=", "%.1f"),
    TUNER_AM_FREQUENCY_QUERY("", "AM.Frequency", "?", ""),
    TUNER_AM_FREQUENCY_SET("", "AM.Frequency", "=", "%d"),
    TUNER_BAND_QUERY("", "Band", "?", ""),
    TUNER_BAND_SET("", "Band", "=", "%s"),
    TUNER_PRESET_QUERY("", "Preset", "?", ""),
    TUNER_PRESET_SET("", "Preset", "=", "%s"),
    TUNER_FM_RDS_TEXT_QUERY("", "FM.RDSText", "?", ""),
    TUNER_FM_RDS_TEXT_SET("", "FM.RDSText", "=", "%s"),
    TUNER_XM_CHANNEL_QUERY("", "XM.Channel", "?", ""),
    TUNER_XM_CHANNEL_SET("", "XM.Channel", "=", "%d"),
    TUNER_XM_CHANNEL_NAME_QUERY("", "XM.ChannelName", "?", ""),
    TUNER_XM_CHANNEL_NAME_SET("", "XM.ChannelName", "=", "%s"),
    TUNER_XM_NAME_QUERY("", "XM.Name", "?", ""),
    TUNER_XM_NAME_SET("", "XM.Name", "=", "%s"),
    TUNER_XM_SONG_TITLE_QUERY("", "XM.Title", "?", ""),
    TUNER_XM_SONG_TITLE_SET("", "XM.Title", "=", "%s"),
    TUNER_DAB_DLS_TEXT_QUERY("", "DAB.DLS", "?", ""),
    TUNER_DAB_DLS_TEXT_SET("", "DAB.DLS", "=", "%s"),
    TUNER_DAB_SERVICE_NAME_QUERY("", "DAB.SERVICE", "?", ""),
    TUNER_DAB_SERVICE_NAME_SET("", "DAB.SERVICE", "=", "%s"),
    LISTENING_MODE_SET("", "ListeningMode", "=", ""),
    LISTENING_MODE_QUERY("", "ListeningMode", "?", ""),
    VOLUME_SET("", "Volume", "=", "%d"),
    VOLUME_QUERY("", "Volume", "?", ""),
    VOLUME_FIXED_SET("", "VolumeFixed", "=", "%d"),
    VOLUME_FIXED_QUERY("", "VolumeFixed", "?", ""),
    VOLUME_CONTROL_SET("", "VolumeControl", "=", "%s"),
    VOLUME_CONTROL_QUERY("", "VolumeControl", "?", ""),
    MUTE_QUERY("", "Mute", "?", ""),
    MUTE_SET("", "Mute", "=", "");

    /**
     * Supported command prefixes:
     * <ul>
     * <li>Zones are Main, Zone1-Zone4</li>
     * <li>Sources are Source1 - Source10, Ipod, Tuner</li>
     * </ul>
     */
    public static enum Prefix {
        Ipod,
        Main,
        Preset1,
        Preset2,
        Preset3,
        Preset4,
        Preset5,
        Source1,
        Source2,
        Source3,
        Source4,
        Source5,
        Source6,
        Source7,
        Source8,
        Source9,
        Source10,
        Source11,
        Tuner,
        Zone2,
        Zone3,
        Zone4;
    }

    /**
     * Default Source Names is used to initialize an array to hold input source names for the AVR
     */
    public static enum DefaultSourceNames {
        Source01,
        Source02,
        Source03,
        Source04,
        Source05,
        Source06,
        Source07,
        Source08,
        Source09,
        Source10;
    }

    /**
     * Default Preset Names is used to initialize an array to hold preset names for the AVR
     */
    public static enum DefaultPresetNames {
        P01,
        P02,
        P03,
        P04,
        P05,
        P06,
        P07,
        P08,
        P09,
        P10,
        P11,
        P12,
        P13,
        P14,
        P15,
        P16,
        P17,
        P18,
        P19,
        P20,
        P21,
        P22,
        P23,
        P24,
        P25,
        P26,
        P27,
        P28,
        P29,
        P30,
        P31,
        P32,
        P33,
        P34,
        P35,
        P36,
        P37,
        P38,
        P39,
        P40;
    }

    /**
     * Default Tuner Band Names is used to initialize an array to hold tuner band names for the AVR
     */
    public static enum DefaultTunerBandNames {
        AM,
        FM,
        XM,
        DAB;
    }

    private static HashMap<String, NadCommand> commandList = new HashMap<String, NadCommand>();

    private String prefix;
    private String variable;
    private String operator;
    private String value;

    /**
     *
     * Constructor for the NAD RS232/IP Command protocol format
     *
     * @param prefix - see enum {@link Prefix}
     * @param variable - see enum {@link NadCommand} variable part of command
     * @param operator - either of +, -, + or ?
     * @param value - the value of a channel (item) state or the value to be set for the channel (item)
     */
    private NadCommand(String prefix, String variable, String operator, String value) {
        this.prefix = prefix;
        this.variable = variable;
        this.operator = operator;
        this.value = value;
    }

    /**
     * @return the prefix string (example 'Main.')
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * @return the variable string (example 'Power')
     */
    public String getVariable() {
        return variable;
    }

    /**
     * @return the operator string (example '=')
     */
    public String getOperator() {
        return operator;
    }

    /**
     * @return the value string (example '?')
     */
    public String getValue() {
        return value;
    }

    // /**
    // * @param prefix
    // * @param baseCommand
    // * @return
    // * @throws IllegalArgumentException
    // */
    // public static NadCommand getCommandForPrefix(Prefix prefix, NadCommand baseCommand)
    // throws IllegalArgumentException {
    // return NadCommand.valueOf(prefix.toString() + "." + baseCommand);
    // }

    /**
     * The intializeCommandList() method builds the list of commands that this binding supports
     */
    public static void initializeCommandList() {
        for (NadCommand command : values()) {
            String key = command.getVariable() + command.getOperator();
            commandList.put(key, command);
        }
    }

    /**
     * Method to determine if the command received from the NAD device is supported
     *
     * @param variable - see enum {@link NadCommand} variable part of command
     * @param operator - either of +, -, + or ?
     * @return the {@link NadCommand} based on the variable and operator found in the list of supported commands
     * @throws IllegalArgumentException
     */
    public static @Nullable NadCommand getCommandByVariableAndOperator(String variable, String operator)
            throws IllegalArgumentException {
        String key = variable + operator;
        if (commandList.containsKey(key)) {
            NadCommand candidate = commandList.get(key);
            return candidate;
        }
        throw new IllegalArgumentException(
                "There is no matching command name for the variable '" + variable + "' and operator '" + operator);
    }
}
