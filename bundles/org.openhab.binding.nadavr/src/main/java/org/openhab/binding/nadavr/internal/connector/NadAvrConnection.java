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

import static org.openhab.binding.nadavr.internal.NadAvrBindingConstants.NAD_QUERY;

import java.math.BigDecimal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.nadavr.internal.NadAvrConfiguration;
import org.openhab.binding.nadavr.internal.nadcp.NadCommand;
import org.openhab.binding.nadavr.internal.nadcp.NadCommand.Prefix;
import org.openhab.binding.nadavr.internal.nadcp.NadMessage;
import org.openhab.binding.nadavr.internal.nadcp.NadUnsupportedCommandTypeException;
import org.openhab.binding.nadavr.internal.state.NadAvrState;
import org.openhab.binding.nadavr.internal.state.NadAvrStateChangedListener;
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
 * The {@link NadAvrConnection} class contains fields mapping thing configuration parameters.
 *
 * @author Dave J Schoepel - Initial contribution
 */
@NonNullByDefault
public abstract class NadAvrConnection {

    private final Logger logger = LoggerFactory.getLogger(NadAvrConnection.class);

    private NadAvrStateChangedListener stateChangedListener = new NadAvrStateChangedListener() {
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
    protected NadAvrState state = new NadAvrState(stateChangedListener);
    protected NadAvrConfiguration config = new NadAvrConfiguration() {
    };;

    public abstract void openConnection();

    public abstract void closeConnection();

    public abstract boolean isConnected();
    //
    // public abstract boolean isTunerActive();

    public abstract void sendCommand(NadMessage msg);

    /**
     * Sends Power (ON/OFF/Status) commands for a specific zone
     *
     * @param command POWER_QUERY, POWER_SET)
     * @param zone (see Prefix eNum for command prefixes)
     * @throws NadUnsupportedCommandTypeException
     */
    public void sendPowerCommand(Command command, Prefix zone) throws NadUnsupportedCommandTypeException {
        String cmdValue = "";
        if (command == OnOffType.ON) {
            cmdValue = "On";
        } else if (command == OnOffType.OFF) {
            cmdValue = "Off";
        } else if (command instanceof RefreshType) {
            cmdValue = NAD_QUERY; // "?"
        } else {
            throw new NadUnsupportedCommandTypeException();
        }

        sendCommand(new NadMessage.MessageBuilder().prefix(zone.toString())
                .variable(NadCommand.POWER_SET.getVariable().toString())
                .operator(NadCommand.POWER_SET.getOperator().toString()).value(cmdValue).build());
    }

    /**
     * Sends Listening Mode (Set/Status) command for a specific zone
     *
     * @param command
     * @param zone
     * @throws NadUnsupportedCommandTypeException
     */
    public void sendListeningModeCommand(Command command, Prefix zone) throws NadUnsupportedCommandTypeException {
        String cmdValue = "";
        if (command instanceof StringType) {
            cmdValue = command.toString();
        } else if (command instanceof RefreshType) {
            cmdValue = NAD_QUERY; // "?"else {
        } else {
            throw new NadUnsupportedCommandTypeException();
        }

        sendCommand(new NadMessage.MessageBuilder().prefix(zone.toString())
                .variable(NadCommand.LISTENING_MODE_SET.getVariable().toString())
                .operator(NadCommand.LISTENING_MODE_SET.getOperator().toString()).value(cmdValue).build());
    }

    /**
     * Sends Tuner Band (Set/Status) command
     *
     * @param command
     * @param zone
     * @throws NadUnsupportedCommandTypeException
     */
    public void sendTunerBandCommand(Command command, Prefix tuner) throws NadUnsupportedCommandTypeException {
        String cmdValue = "";
        if (command instanceof StringType) {
            cmdValue = command.toString();
        } else if (command instanceof RefreshType) {
            cmdValue = NAD_QUERY; // "?"else {
        } else {
            throw new NadUnsupportedCommandTypeException();
        }

        sendCommand(new NadMessage.MessageBuilder().prefix(tuner.toString())
                .variable(NadCommand.TUNER_BAND_SET.getVariable().toString())
                .operator(NadCommand.TUNER_BAND_SET.getOperator().toString()).value(cmdValue).build());
    }

    public void sendSourceCommand(Command command, Prefix zone) throws NadUnsupportedCommandTypeException {
        String cmdValue = "";
        if (command instanceof StringType) {
            cmdValue = command.toString();
        } else if (command instanceof RefreshType) {
            cmdValue = NAD_QUERY; // "?"else {
        } else {
            throw new NadUnsupportedCommandTypeException();
        }
        sendCommand(new NadMessage.MessageBuilder().prefix(zone.toString())
                .variable(NadCommand.SOURCE_SET.getVariable().toString())
                .operator(NadCommand.SOURCE_SET.getOperator().toString()).value(cmdValue).build());
    }

    public void sendMuteCommand(Command command, Prefix zone) throws NadUnsupportedCommandTypeException {
        String cmdValue = "";
        if (command == OnOffType.ON) {
            cmdValue = "On";
        } else if (command == OnOffType.OFF) {
            cmdValue = "Off";
        } else if (command instanceof RefreshType) {
            cmdValue = NAD_QUERY; // "?"
        } else {
            throw new NadUnsupportedCommandTypeException();
        }
        sendCommand(new NadMessage.MessageBuilder().prefix(zone.toString())
                .variable(NadCommand.MUTE_SET.getVariable().toString())
                .operator(NadCommand.MUTE_SET.getOperator().toString()).value(cmdValue).build());
    }

    // Command represents a % from the dimmer type control, need to convert to dB
    public void sendVolumeCommand(Command command, Prefix zone) throws NadUnsupportedCommandTypeException {
        String cmdValue = "";
        if (command instanceof RefreshType) {
            cmdValue = NAD_QUERY;
        } else if (command == IncreaseDecreaseType.INCREASE) {
            cmdValue += "+";
        } else if (command == IncreaseDecreaseType.DECREASE) {
            cmdValue += "-";
        } else if (command instanceof PercentType) {
            cmdValue = percentToNadDbValue(((PercentType) command).toBigDecimal());
        } else if (command instanceof DecimalType) {
            cmdValue = toNadValue(((DecimalType) command));
        } else {
            throw new NadUnsupportedCommandTypeException();
        }
        sendCommand(new NadMessage.MessageBuilder().prefix(zone.toString())
                .variable(NadCommand.VOLUME_SET.getVariable().toString())
                .operator(NadCommand.VOLUME_SET.getOperator().toString()).value(cmdValue).build());
    }

    public void sendVolumeFixedCommand(Command command, Prefix zone) throws NadUnsupportedCommandTypeException {
        String cmdValue = "";
        if (command instanceof RefreshType) {
            cmdValue = NAD_QUERY;
        } else if (command == IncreaseDecreaseType.INCREASE) {
            cmdValue += "+";
        } else if (command == IncreaseDecreaseType.DECREASE) {
            cmdValue += "-";
        } else if (command instanceof PercentType) {
            cmdValue = percentToNadDbValue(((PercentType) command).toBigDecimal());
        } else if (command instanceof DecimalType) {
            cmdValue = toNadValue(((DecimalType) command));
        } else {
            throw new NadUnsupportedCommandTypeException();
        }
        sendCommand(new NadMessage.MessageBuilder().prefix(zone.toString())
                .variable(NadCommand.VOLUME_FIXED_SET.getVariable().toString())
                .operator(NadCommand.VOLUME_FIXED_SET.getOperator().toString()).value(cmdValue).build());
    }

    public void sendTunerFmFrequencyCommand(Command command, Prefix zone) throws NadUnsupportedCommandTypeException {
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
            throw new NadUnsupportedCommandTypeException();
        }
        sendCommand(new NadMessage.MessageBuilder().prefix(zone.toString())
                .variable(NadCommand.TUNER_FM_FREQUENCY_SET.getVariable().toString())
                .operator(NadCommand.TUNER_FM_FREQUENCY_SET.getOperator().toString()).value(cmdValue).build());
    }

    public void sendTunerAmFrequencyCommand(Command command, Prefix zone) throws NadUnsupportedCommandTypeException {
        String cmdValue = "";
        if (command instanceof RefreshType) {
            cmdValue = NAD_QUERY;
        } else if (command == IncreaseDecreaseType.INCREASE) {
            cmdValue += "+";
        } else if (command == IncreaseDecreaseType.DECREASE) {
            cmdValue += "-";
        } else if (command instanceof DecimalType) {
            cmdValue = toNadValue(((DecimalType) command));
        } else {
            throw new NadUnsupportedCommandTypeException();
        }
        sendCommand(new NadMessage.MessageBuilder().prefix(zone.toString())
                .variable(NadCommand.TUNER_AM_FREQUENCY_SET.getVariable().toString())
                .operator(NadCommand.TUNER_AM_FREQUENCY_SET.getOperator().toString()).value(cmdValue).build());
    }

    public void sendTunerPresetCommand(Command command, Prefix zone) throws NadUnsupportedCommandTypeException {
        String cmdValue = "";
        if (command instanceof RefreshType) {
            cmdValue = NAD_QUERY;
        } else if (command == IncreaseDecreaseType.INCREASE) {
            cmdValue += "+";
        } else if (command == IncreaseDecreaseType.DECREASE) {
            cmdValue += "-";
        } else if (command instanceof DecimalType) {
            cmdValue = toNadValue(((DecimalType) command));
        } else {
            throw new NadUnsupportedCommandTypeException();
        }
        sendCommand(new NadMessage.MessageBuilder().prefix(zone.toString())
                .variable(NadCommand.TUNER_PRESET_SET.getVariable().toString())
                .operator(NadCommand.TUNER_PRESET_SET.getOperator().toString()).value(cmdValue).build());
    }

    /**
     * Sends Tuner (ON/OFF/Status) commands
     *
     * @param command TUNER_FM_MUTE_QUERY, TUNER_FM_MUTE_SET)
     * @param zone (see Prefix eNum for command prefixes)
     * @throws NadUnsupportedCommandTypeException
     */
    public void sendTunerFmMuteCommand(Command command, Prefix zone) throws NadUnsupportedCommandTypeException {
        String cmdValue = "";
        if (command == OnOffType.ON) {
            cmdValue = "On";
        } else if (command == OnOffType.OFF) {
            cmdValue = "Off";
        } else if (command instanceof RefreshType) {
            cmdValue = NAD_QUERY; // "?"
        } else {
            throw new NadUnsupportedCommandTypeException();
        }
        sendCommand(new NadMessage.MessageBuilder().prefix(zone.toString())
                .variable(NadCommand.TUNER_FM_MUTE_SET.getVariable().toString())
                .operator(NadCommand.TUNER_FM_MUTE_SET.getOperator().toString()).value(cmdValue).build());
    }

    /**
     * Sends TunerFM RDS Text Query command
     *
     * @param command TUNER_FM_RDS_TEXT_QUERY)
     * @param zone (see Prefix eNum for command prefixes)
     * @throws NadUnsupportedCommandTypeException
     */
    public void sendTunerFmRdsTextCommand(Command command, Prefix zone) throws NadUnsupportedCommandTypeException {
        String cmdValue = "";
        if (command instanceof RefreshType) {
            cmdValue = "?"; // Only option is to query this setting
        } else {
            throw new NadUnsupportedCommandTypeException();
        }
        sendCommand(new NadMessage.MessageBuilder().prefix(zone.toString())
                .variable(NadCommand.TUNER_FM_RDS_TEXT_SET.getVariable().toString())
                .operator(NadCommand.TUNER_FM_RDS_TEXT_SET.getOperator().toString()).value(cmdValue).build());
    }

    public void sendVolumeFixedDBCommand(Command command, Prefix zone) throws NadUnsupportedCommandTypeException {
        Command dbCommand = command;
        if (dbCommand instanceof PercentType) {
            throw new NadUnsupportedCommandTypeException();
        }
        sendVolumeFixedCommand(dbCommand, zone);
    }

    public void sendVolumeControlCommand(Command command, Prefix zone) throws NadUnsupportedCommandTypeException {
        String cmdValue = "";
        if (command instanceof StringType) {
            cmdValue = command.toString();
        } else if (command instanceof RefreshType) {
            cmdValue = NAD_QUERY; // "?"else {
        } else {
            throw new NadUnsupportedCommandTypeException();
        }
        sendCommand(new NadMessage.MessageBuilder().prefix(zone.toString())
                .variable(NadCommand.VOLUME_CONTROL_SET.getVariable().toString())
                .operator(NadCommand.VOLUME_CONTROL_SET.getOperator().toString()).value(cmdValue).build());
    }

    public void sendVolumeDbCommand(Command command, Prefix zone) throws NadUnsupportedCommandTypeException {
        Command dbCommand = command;
        if (dbCommand instanceof PercentType) {
            throw new NadUnsupportedCommandTypeException();
        }
        sendVolumeCommand(dbCommand, zone);
    }

    protected String toDenonFloatValue(DecimalType number) {
        String dbString = String.valueOf(number.floatValue());
        return dbString;
    }

    protected String toNadValue(DecimalType number) {
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

    protected String percentToNadDbValue(BigDecimal pct) {
        BigDecimal percent = pct.multiply(VOLUME_RANGE).divide(ONE_HUNDRED).add(VOLUME_DB_MIN);
        return toNadValue(new DecimalType(percent));
    }

    public void sendCommand(String prefix, NadCommand deviceCommand) {
        logger.debug("NADAvrConnector - sendCommand was called with prefix = {} and deviceCommand = {}", prefix,
                deviceCommand);

        sendCommand(new NadMessage.MessageBuilder().prefix(prefix).variable(deviceCommand.getVariable().toString())
                .operator(deviceCommand.getOperator().toString()).value(deviceCommand.getValue()).build());
    }

    public String getConnectionName() {
        return config.ipAddress + ":" + config.telnetPort;
    }
}
