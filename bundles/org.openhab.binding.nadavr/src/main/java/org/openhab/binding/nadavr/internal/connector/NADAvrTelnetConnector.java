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
package org.openhab.binding.nadavr.internal.connector;

import static org.openhab.binding.nadavr.internal.NADAvrBindingConstants.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.nadavr.internal.NADAvrConfiguration;
import org.openhab.binding.nadavr.internal.NADAvrInputSourceList;
import org.openhab.binding.nadavr.internal.NADAvrState;
import org.openhab.binding.nadavr.internal.NADAvrStateDescriptionProvider;
import org.openhab.binding.nadavr.internal.nadcp.NADCommand;
import org.openhab.binding.nadavr.internal.nadcp.NADCommand.Prefix;
import org.openhab.binding.nadavr.internal.nadcp.NADMessage;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.thing.ThingUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link NADAvrTelnetConnector.java} class contains fields mapping thing configuration parameters.
 *
 * @author Dave J Schoepel - Initial contribution
 */
@NonNullByDefault
public class NADAvrTelnetConnector extends NADAvrConnector implements NADAvrTelnetListener {

    private final Logger logger = LoggerFactory.getLogger(NADAvrTelnetConnector.class);

    private NADAvrFMRdsTextStream rdsText = new NADAvrFMRdsTextStream(this);

    NADAvrTelnetClientThread telnetClientThread = new NADAvrTelnetClientThread(config, this);
    NADAvrConnector connector = (this);
    protected boolean disposing = false;

    ScheduledExecutorService telnetStateRequest = Executors.newScheduledThreadPool(1);
    ScheduledExecutorService rdsTextRequest = Executors.newScheduledThreadPool(1);

    private ThingUID thingUID;

    NADAvrStateDescriptionProvider stateDescriptionProvider;

    /**
     *
     */
    // public NADAvrTelnetConnector(NADAvrConfiguration config, NADAvrState state,
    // NADAvrStateDescriptionProvider stateDescriptionprovider, ScheduledExecutorService scheduler,
    // ThingUID thingUID) {
    // this.config = config;
    // this.scheduler = scheduler;
    // this.state = state;
    // this.stateDescriptionProvider = stateDescriptionprovider;
    // this.thingUID = thingUID;
    // }

    /**
    *
    */
    public NADAvrTelnetConnector(NADAvrConfiguration config, NADAvrState state,
            NADAvrStateDescriptionProvider stateDescriptionprovider, ThingUID thingUID) {
        this.config = config;
        // this.scheduler = scheduler;
        this.state = state;
        this.stateDescriptionProvider = stateDescriptionprovider;
        this.thingUID = thingUID;
    }

    @Override
    public void receivedLine(NADMessage data) {
        try {
            NADCommand receivedCommand = NADCommand.EMPTY_COMMAND;
            try {
                receivedCommand = NADCommand.getCommandByVariableAndOperator(data.getVariable(), data.getOperator());
            } catch (IllegalArgumentException ex) {
                logger.debug("Received unknown status update from NAD Device @{}: data={}",
                        config.hostname + ":" + config.ipAddress, data);
                return;
            }
            logger.debug("Received line = {}", receivedCommand);
            String commandPrefix = data.getPrefix();

            if (commandPrefix.contains(NAD_PREFIX_SOURCE) && data.getVariable().contains(NAD_VARIABLE_NAME)
                    && data.getOperator().equals(NAD_EQUALS_OPERATOR)) {
                int index = (Integer.parseInt(commandPrefix.substring(6).stripTrailing()) - 1);
                NADAvrInputSourceList.updateSourceName(index, data.getValue());

                logger.debug("NADAvrInputSourceList updated with new name at index - {}, value = {}", index,
                        data.getValue());
                // if (!populateInput.isRunning()) {
                // populateInput.start();
                // } else {
                // logger.debug("TelnetConnector - populateInput is running.....");
                // }
                // populateInputs();
            }
            if (receivedCommand != null) {

                switch (receivedCommand) {
                    case SOURCE_SET:
                        if (NADAvrInputSourceList.size() > 0) {
                            String sourceName = "";
                            int index = (Integer.parseInt(data.getValue()));
                            if (index >= 11 && !commandPrefix.equals(ZONE1)) { // Zones2-3 include Local source
                                sourceName = LOCAL;
                            } else {
                                sourceName = NADAvrInputSourceList.getSourceName(index - 1);
                            }
                            state.setSourceName(data.getPrefix().toString(), sourceName);
                        }
                        break;
                    case POWER_SET:
                        state.setPower(data.getPrefix(), data.getValue().equalsIgnoreCase("On"));
                        break;
                    case VOLUME_CONTROL_SET:
                        state.setVolumeControl(data.getPrefix(), data.getValue());
                        break;
                    case VOLUME_FIXED_SET:
                        BigDecimal volumeFixed = new BigDecimal(data.getValue());
                        state.setVolumeFixed(data.getPrefix(), volumeFixed);
                        break;
                    case VOLUME_SET:
                        BigDecimal volume = new BigDecimal(data.getValue().toString());
                        state.setVolume(data.getPrefix(), volume);
                        break;
                    case MUTE_SET:
                        state.setMute(data.getPrefix(), data.getValue().equalsIgnoreCase("On"));
                        break;
                    case LISTENING_MODE_SET:
                        state.setListeningMode(commandPrefix, data.getValue().toString());
                        break;
                    case TUNER_BAND_SET:
                        state.setTunerBand(commandPrefix, data.getValue().toString());
                        String band = data.getValue();
                        switch (band) {
                            case "FM":
                                if (isTunerActive()) {
                                    logger.debug("Is the Tuner Active? Anser: {}", isTunerActive());
                                } else {
                                    logger.debug("Is the Tuner Active? Anser: {}", isTunerActive());
                                }
                                if (!rdsText.isStarted()) {
                                    rdsText.start();
                                } else {
                                    rdsText.resume();
                                }
                                break;
                            default:
                                rdsText.pause();
                                break;
                        }
                        break;
                    case TUNER_FM_FREQUENCY_SET:
                        BigDecimal fmFrequency = new BigDecimal(data.getValue());
                        state.setTunerFMFrequency(commandPrefix, fmFrequency);
                        break;
                    case TUNER_AM_FREQUENCY_SET:
                        BigDecimal amFrequency = new BigDecimal(data.getValue());
                        state.setTunerAMFrequency(commandPrefix, amFrequency);
                        break;
                    case TUNER_FM_MUTE_SET:
                        state.setTunerFMMute(commandPrefix, data.getValue().equalsIgnoreCase("On"));
                        break;
                    case TUNER_PRESET_SET:
                        BigDecimal preset = new BigDecimal(data.getValue());
                        state.setTunerPreset(commandPrefix, preset);
                        break;
                    case TUNER_FM_RDS_TEXT_SET:
                        state.setTunerFMRdsText(commandPrefix, data.getValue().toString());
                        break;
                    default:
                        break;
                }
            }
        } catch (Exception ex) {
            logger.warn("Exception in statusUpdateReceived for NAD device @{}. Cause: {}, data received: {}",
                    config.hostname, ex.getMessage(), data);
        }
    }

    @Override
    public void telnetClientConnected(boolean connected) {
        if (!connected) {
            if (!disposing) {
                state.connectionError("Error connecting to the telnet port.");
            }
            logger.debug("Telnet client disconnected.");
        } else {
            refreshState();
        }
    }

    @Override
    public void connect() {
        telnetClientThread = new NADAvrTelnetClientThread(config, this);
        telnetClientThread.setName("OH-binding-" + thingUID);
        telnetClientThread.start();
        logger.debug("NADAvrTelnetConnector - telnetClientThread started....");
        if (!rdsText.isStarted()) {
            rdsText.start();
        }
    }

    @Override
    public void dispose() {
        logger.debug("disposing connector");
        disposing = true;
        telnetStateRequest.shutdown();
        telnetClientThread.interrupt();
        telnetClientThread.shutdown();
        rdsText.stop();
    }

    private void refreshState() {
        // Sends a series of state query commands over the connection
        logger.debug("NADAvrTelnetConnector - refreshState() started....");
        ScheduledExecutorService refresh = Executors.newSingleThreadScheduledExecutor();
        refresh.submit(() -> {

            // Initialize message with default "Main.Power=?" command
            NADMessage queryCmd = new NADMessage.MessageBuilder().prefix(Prefix.Main.toString())
                    .variable(NADCommand.POWER_QUERY.getVariable().toString())
                    .operator(NADCommand.POWER_QUERY.getOperator().toString())
                    .value(NADCommand.POWER_QUERY.getValue().toString()).build();

            // When adding new zone commands be sure to include in array to refresh states...
            List<NADCommand> nadZoneRefreshCmds = new ArrayList<>(Arrays.asList(NADCommand.POWER_QUERY,
                    NADCommand.INPUT_SOURCE_QUERY, NADCommand.VOLUME_QUERY, NADCommand.LISTENING_MODE_QUERY,
                    NADCommand.MUTE_QUERY, NADCommand.VOLUME_CONTROL_QUERY, NADCommand.VOLUME_FIXED_QUERY));

            // When adding new general commands be sure to include in array to refresh states...
            List<NADCommand> nadGeneralRefreshCmds = new ArrayList<>(Arrays.asList(NADCommand.TUNER_BAND_QUERY,
                    NADCommand.TUNER_AM_FREQUENCY_QUERY, NADCommand.TUNER_FM_FREQUENCY_QUERY,
                    NADCommand.TUNER_FM_MUTE_QUERY, NADCommand.TUNER_FM_RDS_TEXT_QUERY, NADCommand.TUNER_PRESET_QUERY));

            // Refresh general state information
            for (NADCommand nadGeneralCmd : nadGeneralRefreshCmds) {
                if (nadGeneralCmd.getOperator() == NAD_QUERY) {
                    queryCmd = new NADMessage.MessageBuilder().prefix(Prefix.Tuner.toString())
                            .variable(nadGeneralCmd.getVariable().toString())
                            .operator(nadGeneralCmd.getOperator().toString()).value(nadGeneralCmd.getValue().toString())
                            .build();
                    internalSendCommand(queryCmd);
                }
            }
            // Refresh zone state information
            for (NADCommand nadCmd : nadZoneRefreshCmds) {
                if (nadCmd.getOperator() == NAD_QUERY) {
                    logger.debug("------- >>> NADcmd = {}", nadCmd);
                    for (int zone = 1; zone <= config.getZoneCount(); zone++) {
                        switch (zone) {
                            case 1: // MainZone - 1
                                queryCmd = new NADMessage.MessageBuilder().prefix(Prefix.Main.toString())
                                        .variable(nadCmd.getVariable().toString())
                                        .operator(nadCmd.getOperator().toString()).value(nadCmd.getValue().toString())
                                        .build();
                                break;
                            case 2: // Zone 2
                                queryCmd = new NADMessage.MessageBuilder().prefix(Prefix.Zone2.toString())
                                        .variable(nadCmd.getVariable().toString())
                                        .operator(nadCmd.getOperator().toString()).value(nadCmd.getValue().toString())
                                        .build();
                                break;
                            case 3: // Zone 3
                                queryCmd = new NADMessage.MessageBuilder().prefix(Prefix.Zone3.toString())
                                        .variable(nadCmd.getVariable().toString())
                                        .operator(nadCmd.getOperator().toString()).value(nadCmd.getValue().toString())
                                        .build();
                                break;
                            case 4: // Zone 4
                                queryCmd = new NADMessage.MessageBuilder().prefix(Prefix.Zone4.toString())
                                        .variable(nadCmd.getVariable().toString())
                                        .operator(nadCmd.getOperator().toString()).value(nadCmd.getValue().toString())
                                        .build();
                                break;
                            default: // General commands
                                break;
                        }
                        internalSendCommand(queryCmd);
                    }
                }
            }
            // Refresh source input names
            for (int input = 1; input <= 10; input++) {
                String prefix = "Source" + input;
                NADCommand nadCmd = NADCommand.SOURCE_NAME_QUERY;
                queryCmd = new NADMessage.MessageBuilder().prefix(prefix).variable(nadCmd.getVariable().toString())
                        .operator(nadCmd.getOperator().toString()).value(nadCmd.getValue().toString()).build();

                internalSendCommand(queryCmd);
            }
        });
    }

    @Override
    protected void internalSendCommand(NADMessage msg) {
        logger.debug("Sending command '{}'", msg);
        // if (msg == null || msg.toString().isBlank()) {
        if (msg.toString().isBlank()) {
            logger.warn("Trying to send empty command");
            return;
        }
        telnetClientThread.sendCommand(msg);
    }

    public boolean isTunerActive() {
        boolean tunerIsActive = false;
        boolean mainPowerOn = false;
        boolean zone2PowerOn = false;
        boolean zone3PowerOn = false;
        boolean zone4PowerOn = false;
        mainPowerOn = state.getStateForChannelID(CHANNEL_MAIN_POWER).equals(OnOffType.ON);
        zone2PowerOn = state.getStateForChannelID(CHANNEL_ZONE2_POWER).equals(OnOffType.ON);
        zone3PowerOn = state.getStateForChannelID(CHANNEL_ZONE3_POWER).equals(OnOffType.ON);
        zone4PowerOn = state.getStateForChannelID(CHANNEL_ZONE4_POWER).equals(OnOffType.ON);
        if (mainPowerOn || zone2PowerOn || zone3PowerOn || zone4PowerOn) {
            logger.debug("Zones powered on - main {}, Z2 {}, Z3 {}, Z4 {}", mainPowerOn, zone2PowerOn, zone3PowerOn,
                    zone4PowerOn);
            // TODO now check to see if a source is set to the tuner
        } else {
            logger.debug("Zones powered on - main {}, Z2 {}, Z3 {}, Z4 {}", mainPowerOn, zone2PowerOn, zone3PowerOn,
                    zone4PowerOn);
        }
        return tunerIsActive;
    }
}
