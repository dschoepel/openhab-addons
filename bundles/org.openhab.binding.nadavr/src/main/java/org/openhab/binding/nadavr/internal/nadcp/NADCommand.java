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
package org.openhab.binding.nadavr.internal.nadcp;

import java.util.HashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link NADCommand} represents NAD AVR Ethernet/RS232 Protocol Commands
 *
 * @author Dave J Schoepel - Initial contribution
 */
@NonNullByDefault
public enum NADCommand {
    /**
     * Format for a command is Prefix, dot(.), Variable, Operator(+,- =, =), Value. For example 'Power' is prefixed by
     * Main,
     * Zone2-4 and has operator and value "Main.Power=On" .
     */
    POWER_QUERY("", "Power", "?", ""),
    POWER_SET("", "Power", "=", "%s"),
    SOURCE_NAME_QUERY("", "Name", "?", ""),
    SOURCE_NAME("", "Name", "=", "%s"),
    INPUT_SOURCE_QUERY("", "Source", "?", ""),
    SOURCE_SET("", "Source", "=", "%s"),
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
     *
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
        Tuner,
        Zone2,
        Zone3,
        Zone4;
    }

    private static HashMap<String, NADCommand> commandList = new HashMap<String, NADCommand>();

    private String prefix;
    private String variable;
    private String operator;
    private String value;

    private NADCommand(String prefix, String variable, String operator, String value) {
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

    public static NADCommand getCommandForPrefix(Prefix prefix, NADCommand baseCommand)
            throws IllegalArgumentException {
        return NADCommand.valueOf(prefix.toString() + "." + baseCommand);
    }

    public static void initializeCommandList() {
        for (NADCommand command : values()) {
            String key = command.getVariable() + command.getOperator();
            commandList.put(key, command);
        }
    }

    public static @Nullable NADCommand getCommandByVariableAndOperator(String variable, String operator)
            throws IllegalArgumentException {
        String key = variable + operator;
        if (commandList.containsKey(key)) {
            NADCommand candidate = commandList.get(key);
            return candidate;
        }

        throw new IllegalArgumentException(
                "There is no matching command name for the variable '" + variable + "' and operator '" + operator);
    }
}
