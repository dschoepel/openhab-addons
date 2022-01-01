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
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.types.StateOption;
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

    NADAvrTelnetClientThread telnetClientThread = new NADAvrTelnetClientThread(config, this);

    protected boolean disposing = false;

    ScheduledExecutorService telnetStateRequest = Executors.newScheduledThreadPool(1);

    private ThingUID thingUID;

    private NADAvrStateDescriptionProvider stateDescriptionProvider;

    /**
     *
     */
    public NADAvrTelnetConnector(NADAvrConfiguration config, NADAvrState state,
            NADAvrStateDescriptionProvider stateDescriptionprovider, ScheduledExecutorService scheduler,
            ThingUID thingUID) {
        this.config = config;
        this.scheduler = scheduler;
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
                populateInputs();
            }
            if (receivedCommand != null) {
                switch (receivedCommand) {
                    case SOURCE_SET:
                        if (NADAvrInputSourceList.size() > 0) {
                            int index = (Integer.parseInt(data.getValue()));
                            String sourceName = NADAvrInputSourceList.getSourceName(index - 1);
                            if (state != null) {
                                state.setSourceName(data.getPrefix().toString(), sourceName);
                            }
                        }
                        break;
                    case POWER_SET:
                        if (state != null) {
                            state.setPower(data.getPrefix(), data.getValue().equalsIgnoreCase("On"));
                        }
                        break;
                    case VOLUME_CONTROL_SET:
                        if (state != null) {
                            state.setVolumeControl(data.getPrefix(), data.getValue());
                        }
                        break;
                    case VOLUME_FIXED_SET:
                        BigDecimal volumeFixed = new BigDecimal(data.getValue());
                        if (state != null) {
                            state.setVolumeFixed(data.getPrefix(), volumeFixed);
                        }
                        break;
                    case VOLUME_SET:
                        BigDecimal volume = new BigDecimal(data.getValue().toString());
                        if (state != null) {
                            state.setVolume(data.getPrefix(), volume);
                        }
                        break;
                    case MUTE_SET:
                        if (state != null) {
                            state.setMute(data.getPrefix(), data.getValue().equalsIgnoreCase("On"));
                        }
                        break;
                    case LISTENING_MODE_SET:
                        if (state != null) {
                            state.setListeningMode(commandPrefix, data.getValue().toString());
                        }
                        break;
                    case TUNER_BAND_SET:
                        if (state != null) {
                            state.setTunerBand(commandPrefix, data.getValue().toString());
                        }
                        break;
                    case TUNER_FM_FREQUENCY_SET:
                        BigDecimal fmFrequency = new BigDecimal(data.getValue());
                        if (state != null) {
                            state.setTunerFMFrequency(commandPrefix, fmFrequency);
                        }
                        break;
                    case TUNER_AM_FREQUENCY_SET:
                        BigDecimal amFrequency = new BigDecimal(data.getValue());
                        if (state != null) {
                            state.setTunerAMFrequency(commandPrefix, amFrequency);
                        }
                        break;
                    case TUNER_FM_MUTE_SET:
                        if (state != null) {
                            state.setTunerFMMute(commandPrefix, data.getValue().equalsIgnoreCase("On"));
                        }
                        break;
                    case TUNER_PRESET_SET:
                        BigDecimal preset = new BigDecimal(data.getValue());
                        if (state != null) {
                            state.setTunerPreset(commandPrefix, preset);
                        }
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
            if (!disposing && state != null) {
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
    }

    @Override
    public void dispose() {
        logger.debug("disposing connector");
        disposing = true;
        telnetStateRequest.shutdown();
        telnetClientThread.interrupt();
        telnetClientThread.shutdown();
    }

    private void refreshState() {
        // Sends a series of state query commands over the connection
        logger.debug("NADAvrTelnetConnector - refreshState() started....");
        ScheduledExecutorService s = Executors.newScheduledThreadPool(1);
        s.submit(() -> {

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
                    NADCommand.TUNER_FM_MUTE_QUERY, NADCommand.TUNER_PRESET_QUERY));

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

    private void populateInputs() {
        logger.debug("NADAvrHandler - populateInputs() started....");
        List<StateOption> options = new ArrayList<>();
        // Build list of source names to be used by Input Source channel (options)
        for (int i = 1; i <= NADAvrInputSourceList.size(); i++) {
            String name = NADAvrInputSourceList.getSourceName(i - 1);
            options.add(new StateOption(String.valueOf(i), name));
        }
        logger.debug("Got Source Name input List from NAD Device {}", options);

        for (int i = 1; i <= config.getZoneCount(); i++) {
            switch (i) {
                case 1:
                    stateDescriptionProvider.setStateOptions(new ChannelUID(thingUID, CHANNEL_MAIN_SOURCE), options);
                    break;
                case 2:
                    stateDescriptionProvider.setStateOptions(new ChannelUID(thingUID, CHANNEL_ZONE2_SOURCE), options);
                    break;
                case 3:
                    stateDescriptionProvider.setStateOptions(new ChannelUID(thingUID, CHANNEL_ZONE3_SOURCE), options);
                    break;
                case 4:
                    stateDescriptionProvider.setStateOptions(new ChannelUID(thingUID, CHANNEL_ZONE4_SOURCE), options);
                    break;
                default:
                    break;
            }
        }
    }
}
