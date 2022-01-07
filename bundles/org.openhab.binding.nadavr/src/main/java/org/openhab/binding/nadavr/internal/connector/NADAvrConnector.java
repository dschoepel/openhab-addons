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
package org.openhab.binding.nadavr.internal.connector;

import static org.openhab.binding.nadavr.internal.NADAvrBindingConstants.NAD_QUERY;

import java.math.BigDecimal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.nadavr.internal.NADAvrConfiguration;
import org.openhab.binding.nadavr.internal.NADAvrState;
import org.openhab.binding.nadavr.internal.NADAvrStateChangedListener;
import org.openhab.binding.nadavr.internal.UnsupportedCommandTypeException;
import org.openhab.binding.nadavr.internal.nadcp.NADCommand;
import org.openhab.binding.nadavr.internal.nadcp.NADCommand.Prefix;
import org.openhab.binding.nadavr.internal.nadcp.NADMessage;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.IncreaseDecreaseType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link NADAvrConnector} class contains fields mapping thing configuration parameters.
 *
 * @author Dave J Schoepel - Initial contribution
 */
@NonNullByDefault
public abstract class NADAvrConnector {

    private final Logger logger = LoggerFactory.getLogger(NADAvrConnector.class);

    private NADAvrStateChangedListener stateChangedListener = new NADAvrStateChangedListener() {
        @Override
        public void stateChanged(String channelID, State state) {
        }

        @Override
        public void connectionError(String errorMessage) {
        }
    };

    private static final BigDecimal VOLUME_RANGE = new BigDecimal("118");
    private static final BigDecimal VOLUME_DB_MIN = new BigDecimal("-99");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    protected NADAvrState state = new NADAvrState(stateChangedListener);
    protected NADAvrConfiguration config = new NADAvrConfiguration() {
    };;

    public abstract void connect();

    public abstract void dispose();

    protected abstract void internalSendCommand(NADMessage msg);

    /**
     * Sends Power (ON/OFF/Status) commands for a specific zone
     *
     * @param command POWER_QUERY, POWER_SET)
     * @param zone (see Prefix eNum for command prefixes)
     * @throws UnsupportedCommandTypeException
     */
    public void sendPowerCommand(Command command, Prefix zone) throws UnsupportedCommandTypeException {
        String cmdValue = "";
        if (command == OnOffType.ON) {
            cmdValue = "On";
        } else if (command == OnOffType.OFF) {
            cmdValue = "Off";
        } else if (command instanceof RefreshType) {
            cmdValue = NAD_QUERY; // "?"
        } else {
            throw new UnsupportedCommandTypeException();
        }

        internalSendCommand(new NADMessage.MessageBuilder().prefix(zone.toString())
                .variable(NADCommand.POWER_SET.getVariable().toString())
                .operator(NADCommand.POWER_SET.getOperator().toString()).value(cmdValue).build());
    }

    /**
     * Sends Listening Mode (Set/Status) command for a specific zone
     *
     * @param command
     * @param zone
     * @throws UnsupportedCommandTypeException
     */
    public void sendListeningModeCommand(Command command, Prefix zone) throws UnsupportedCommandTypeException {
        String cmdValue = "";
        if (command instanceof StringType) {
            cmdValue = command.toString();
        } else if (command instanceof RefreshType) {
            cmdValue = NAD_QUERY; // "?"else {
        } else {
            throw new UnsupportedCommandTypeException();
        }

        internalSendCommand(new NADMessage.MessageBuilder().prefix(zone.toString())
                .variable(NADCommand.LISTENING_MODE_SET.getVariable().toString())
                .operator(NADCommand.LISTENING_MODE_SET.getOperator().toString()).value(cmdValue).build());
    }

    /**
     * Sends Tuner Band (Set/Status) command
     *
     * @param command
     * @param zone
     * @throws UnsupportedCommandTypeException
     */
    public void sendTunerBandCommand(Command command, Prefix tuner) throws UnsupportedCommandTypeException {
        String cmdValue = "";
        if (command instanceof StringType) {
            cmdValue = command.toString();
        } else if (command instanceof RefreshType) {
            cmdValue = NAD_QUERY; // "?"else {
        } else {
            throw new UnsupportedCommandTypeException();
        }

        internalSendCommand(new NADMessage.MessageBuilder().prefix(tuner.toString())
                .variable(NADCommand.TUNER_BAND_SET.getVariable().toString())
                .operator(NADCommand.TUNER_BAND_SET.getOperator().toString()).value(cmdValue).build());
    }

    public void sendSourceCommand(Command command, Prefix zone) throws UnsupportedCommandTypeException {
        String cmdValue = "";
        if (command instanceof StringType) {
            cmdValue = command.toString();
        } else if (command instanceof RefreshType) {
            cmdValue = NAD_QUERY; // "?"else {
        } else {
            throw new UnsupportedCommandTypeException();
        }
        internalSendCommand(new NADMessage.MessageBuilder().prefix(zone.toString())
                .variable(NADCommand.SOURCE_SET.getVariable().toString())
                .operator(NADCommand.SOURCE_SET.getOperator().toString()).value(cmdValue).build());
    }

    public void sendMuteCommand(Command command, Prefix zone) throws UnsupportedCommandTypeException {
        String cmdValue = "";
        if (command == OnOffType.ON) {
            cmdValue = "On";
        } else if (command == OnOffType.OFF) {
            cmdValue = "Off";
        } else if (command instanceof RefreshType) {
            cmdValue = NAD_QUERY; // "?"
        } else {
            throw new UnsupportedCommandTypeException();
        }
        internalSendCommand(new NADMessage.MessageBuilder().prefix(zone.toString())
                .variable(NADCommand.MUTE_SET.getVariable().toString())
                .operator(NADCommand.MUTE_SET.getOperator().toString()).value(cmdValue).build());
    }

    // Command represents a % from the dimmer type control, need to convert to dB
    public void sendVolumeCommand(Command command, Prefix zone) throws UnsupportedCommandTypeException {
        String cmdValue = "";
        if (command instanceof RefreshType) {
            cmdValue = NAD_QUERY;
        } else if (command == IncreaseDecreaseType.INCREASE) {
            cmdValue += "+";
        } else if (command == IncreaseDecreaseType.DECREASE) {
            cmdValue += "-";
        } else if (command instanceof PercentType) {
            cmdValue = percentToDenonValue(((PercentType) command).toBigDecimal());
        } else if (command instanceof DecimalType) {
            cmdValue = toDenonValue(((DecimalType) command));
        } else {
            throw new UnsupportedCommandTypeException();
        }
        internalSendCommand(new NADMessage.MessageBuilder().prefix(zone.toString())
                .variable(NADCommand.VOLUME_SET.getVariable().toString())
                .operator(NADCommand.VOLUME_SET.getOperator().toString()).value(cmdValue).build());
    }

    public void sendVolumeFixedCommand(Command command, Prefix zone) throws UnsupportedCommandTypeException {
        String cmdValue = "";
        if (command instanceof RefreshType) {
            cmdValue = NAD_QUERY;
        } else if (command == IncreaseDecreaseType.INCREASE) {
            cmdValue += "+";
        } else if (command == IncreaseDecreaseType.DECREASE) {
            cmdValue += "-";
        } else if (command instanceof PercentType) {
            cmdValue = percentToDenonValue(((PercentType) command).toBigDecimal());
        } else if (command instanceof DecimalType) {
            cmdValue = toDenonValue(((DecimalType) command));
        } else {
            throw new UnsupportedCommandTypeException();
        }
        internalSendCommand(new NADMessage.MessageBuilder().prefix(zone.toString())
                .variable(NADCommand.VOLUME_FIXED_SET.getVariable().toString())
                .operator(NADCommand.VOLUME_FIXED_SET.getOperator().toString()).value(cmdValue).build());
    }

    public void sendTunerFmFrequencyCommand(Command command, Prefix zone) throws UnsupportedCommandTypeException {
        String cmdValue = "";
        if (command instanceof RefreshType) {
            cmdValue = NAD_QUERY;
        } else if (command == IncreaseDecreaseType.INCREASE) {
            cmdValue += "+";
        } else if (command == IncreaseDecreaseType.DECREASE) {
            cmdValue += "-";
        } else if (command instanceof DecimalType) {
            cmdValue = toDenonFloatValue(((DecimalType) command));
        } else {
            throw new UnsupportedCommandTypeException();
        }
        internalSendCommand(new NADMessage.MessageBuilder().prefix(zone.toString())
                .variable(NADCommand.TUNER_FM_FREQUENCY_SET.getVariable().toString())
                .operator(NADCommand.TUNER_FM_FREQUENCY_SET.getOperator().toString()).value(cmdValue).build());
    }

    public void sendTunerAmFrequencyCommand(Command command, Prefix zone) throws UnsupportedCommandTypeException {
        String cmdValue = "";
        if (command instanceof RefreshType) {
            cmdValue = NAD_QUERY;
        } else if (command == IncreaseDecreaseType.INCREASE) {
            cmdValue += "+";
        } else if (command == IncreaseDecreaseType.DECREASE) {
            cmdValue += "-";
        } else if (command instanceof DecimalType) {
            cmdValue = toDenonValue(((DecimalType) command));
        } else {
            throw new UnsupportedCommandTypeException();
        }
        internalSendCommand(new NADMessage.MessageBuilder().prefix(zone.toString())
                .variable(NADCommand.TUNER_AM_FREQUENCY_SET.getVariable().toString())
                .operator(NADCommand.TUNER_AM_FREQUENCY_SET.getOperator().toString()).value(cmdValue).build());
    }

    public void sendTunerPresetCommand(Command command, Prefix zone) throws UnsupportedCommandTypeException {
        String cmdValue = "";
        if (command instanceof RefreshType) {
            cmdValue = NAD_QUERY;
        } else if (command == IncreaseDecreaseType.INCREASE) {
            cmdValue += "+";
        } else if (command == IncreaseDecreaseType.DECREASE) {
            cmdValue += "-";
        } else if (command instanceof DecimalType) {
            cmdValue = toDenonValue(((DecimalType) command));
        } else {
            throw new UnsupportedCommandTypeException();
        }
        internalSendCommand(new NADMessage.MessageBuilder().prefix(zone.toString())
                .variable(NADCommand.TUNER_PRESET_SET.getVariable().toString())
                .operator(NADCommand.TUNER_PRESET_SET.getOperator().toString()).value(cmdValue).build());
    }

    /**
     * Sends Tuner (ON/OFF/Status) commands
     *
     * @param command TUNER_FM_MUTE_QUERY, TUNER_FM_MUTE_SET)
     * @param zone (see Prefix eNum for command prefixes)
     * @throws UnsupportedCommandTypeException
     */
    public void sendTunerFmMuteCommand(Command command, Prefix zone) throws UnsupportedCommandTypeException {
        String cmdValue = "";
        if (command == OnOffType.ON) {
            cmdValue = "On";
        } else if (command == OnOffType.OFF) {
            cmdValue = "Off";
        } else if (command instanceof RefreshType) {
            cmdValue = NAD_QUERY; // "?"
        } else {
            throw new UnsupportedCommandTypeException();
        }
        internalSendCommand(new NADMessage.MessageBuilder().prefix(zone.toString())
                .variable(NADCommand.TUNER_FM_MUTE_SET.getVariable().toString())
                .operator(NADCommand.TUNER_FM_MUTE_SET.getOperator().toString()).value(cmdValue).build());
    }

    /**
     * Sends TunerFM RDS Text Query command
     *
     * @param command TUNER_FM_RDS_TEXT_QUERY)
     * @param zone (see Prefix eNum for command prefixes)
     * @throws UnsupportedCommandTypeException
     */
    public void sendTunerFmRdsTextCommand(Command command, Prefix zone) throws UnsupportedCommandTypeException {
        String cmdValue = "";
        if (command instanceof RefreshType) {
            cmdValue = "?"; // Only option is to query this setting
        } else {
            throw new UnsupportedCommandTypeException();
        }
        internalSendCommand(new NADMessage.MessageBuilder().prefix(zone.toString())
                .variable(NADCommand.TUNER_FM_RDS_TEXT_SET.getVariable().toString())
                .operator(NADCommand.TUNER_FM_RDS_TEXT_SET.getOperator().toString()).value(cmdValue).build());
    }

    public void sendVolumeFixedDBCommand(Command command, Prefix zone) throws UnsupportedCommandTypeException {
        Command dbCommand = command;
        if (dbCommand instanceof PercentType) {
            throw new UnsupportedCommandTypeException();
        }
        sendVolumeFixedCommand(dbCommand, zone);
    }

    public void sendVolumeControlCommand(Command command, Prefix zone) throws UnsupportedCommandTypeException {
        String cmdValue = "";
        if (command instanceof StringType) {
            cmdValue = command.toString();
        } else if (command instanceof RefreshType) {
            cmdValue = NAD_QUERY; // "?"else {
        } else {
            throw new UnsupportedCommandTypeException();
        }
        internalSendCommand(new NADMessage.MessageBuilder().prefix(zone.toString())
                .variable(NADCommand.VOLUME_CONTROL_SET.getVariable().toString())
                .operator(NADCommand.VOLUME_CONTROL_SET.getOperator().toString()).value(cmdValue).build());
    }

    public void sendVolumeDbCommand(Command command, Prefix zone) throws UnsupportedCommandTypeException {
        Command dbCommand = command;
        if (dbCommand instanceof PercentType) {
            throw new UnsupportedCommandTypeException();
        }
        sendVolumeCommand(dbCommand, zone);
    }

    protected String toDenonFloatValue(DecimalType number) {
        String dbString = String.valueOf(number.floatValue());
        return dbString;
    }

    protected String toDenonValue(DecimalType number) {
        String dbString = String.valueOf(number.intValue());
        // BigDecimal num = number.toBigDecimal();
        // if (num.compareTo(BigDecimal.TEN) == -1) {
        // dbString = "0" + dbString;
        // }
        // if (num.remainder(BigDecimal.ONE).equals(ONESTEP)) {
        // dbString = dbString + "0";
        // }
        return dbString;
    }

    protected String percentToDenonValue(BigDecimal pct) {
        BigDecimal percent = pct.multiply(VOLUME_RANGE).divide(ONE_HUNDRED).add(VOLUME_DB_MIN);
        return toDenonValue(new DecimalType(percent));
    }

    public void sendCommand(String prefix, NADCommand deviceCommand) {
        logger.debug("NADAvrConnector - sendCommand was called with prefix = {} and deviceCommand = {}", prefix,
                deviceCommand);

        internalSendCommand(
                new NADMessage.MessageBuilder().prefix(prefix).variable(deviceCommand.getVariable().toString())
                        .operator(deviceCommand.getOperator().toString()).value(deviceCommand.getValue()).build());
    }

    public String getConnectionName() {
        return config.ipAddress + ":" + config.telnetPort;
    }
}
