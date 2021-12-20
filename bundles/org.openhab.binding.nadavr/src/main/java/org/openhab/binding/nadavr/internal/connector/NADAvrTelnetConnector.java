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
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import org.openhab.binding.nadavr.internal.NADAvrConfiguration;
import org.openhab.binding.nadavr.internal.NADAvrState;
import org.openhab.binding.nadavr.internal.NADAvrStateDescriptionProvider;
import org.openhab.binding.nadavr.internal.SourceName;
import org.openhab.binding.nadavr.internal.nadcp.NADCommand;
import org.openhab.binding.nadavr.internal.nadcp.NADCommand.Prefix;
import org.openhab.binding.nadavr.internal.nadcp.NADMessage;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.types.State;
import org.openhab.core.types.StateOption;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link NADAvrTelnetConnector.java} class contains fields mapping thing configuration parameters.
 *
 * @author Dave J Schoepel - Initial contribution
 */
public class NADAvrTelnetConnector extends NADAvrConnector implements NADAvrTelnetListener {

    private final Logger logger = LoggerFactory.getLogger(NADAvrTelnetConnector.class);

    // All regular commands. Example: Main.Power=on, Main.Speaker.Front.Config=Small, Source2.Name=Apple TV
    // private static final Pattern COMMAND_PATTERN = Pattern.compile("([\\w\\[.\\]\\\\]+)=(.*)");
    private NADAvrTelnetClientThread telnetClientThread;

    protected boolean disposing = false;

    private Future<?> telnetStateRequest;

    private SourceName avrSourceName = new SourceName();
    // private static final BigDecimal NINETYNINE = new BigDecimal("99");

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

    // TODO modify this area for received line like statusUpdateReceived in NADHandler
    @Override
    public void receivedLine(NADMessage data) {
        NADCommand receivedCommand = null;
        try {
            receivedCommand = NADCommand.getCommandByVariableAndOperator(data.getVariable(), data.getOperator());
        } catch (IllegalArgumentException ex) {
            logger.debug("Received unknown status update from NAD Device @{}: data={}",
                    config.hostname + ":" + config.ipAddress, data);
            return;
        }
        logger.debug("Received line = {}", receivedCommand);
        // TODO May want to validate command prefix before using it below...

        String commandPrefix = data.getPrefix();

        if (commandPrefix.contains(NAD_PREFIX_SOURCE) && data.getVariable().contains(NAD_VARIABLE_NAME)
                && data.getOperator().equals(NAD_EQUALS_OPERATOR)) {
            String key = commandPrefix.substring(6).stripTrailing();
            if (avrSourceName.containsKeySourceName(key.toString())) {
                avrSourceName.replaceAvrSourceName(key, data.getValue());
            } else {
                avrSourceName.setAvrSourceName(key, data.getValue());
            }
            populateInputs();
        }

        switch (receivedCommand) {
            case SOURCE_SET:
                if (avrSourceName.size() > 0) {
                    String key = data.getValue().toString();
                    String sourceName = avrSourceName.getAvrSourceName(key);
                    state.setSourceName(data.getPrefix().toString(), sourceName);
                }
                break;
            case POWER_SET:
                state.setPower(data.getPrefix().toString(), data.getValue().equalsIgnoreCase("On"));
                break;
            case VOLUME_CONTROL_SET:
                state.setVolumeControl(data.getPrefix().toString(), data.getValue().toString());
                break;
            case VOLUME_FIXED_SET:
                BigDecimal volumeFixed = new BigDecimal(data.getValue().toString());
                state.setVolumeFixed(data.getPrefix().toString(), volumeFixed);
                break;
            case VOLUME_SET:
                BigDecimal volume = new BigDecimal(data.getValue().toString());
                state.setVolume(data.getPrefix().toString(), volume);
                break;
            case MUTE_SET:
                state.setMute(data.getPrefix().toString(), data.getValue().equalsIgnoreCase("On"));
                break;
            case LISTENING_MODE_SET:
                state.setListeningMode(commandPrefix, data.getValue().toString());
                break;
            default:
                break;
        }
    }

    @Override
    public void telnetClientConnected(boolean connected) {
        if (!connected) {
            if (!disposing) {
                logger.debug("Telnet client disconnected.");
                state.connectionError("Error connecting to the telnet port.");
            }
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
        // try { // allow connection to start up before sending/receiving status commands
        // Thread.sleep(2000);
        // } catch (InterruptedException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }
    }

    @Override
    public void dispose() {
        logger.debug("disposing connector");
        disposing = true;

        if (telnetStateRequest != null) {
            telnetStateRequest.cancel(true);
            telnetStateRequest = null;
        }

        if (telnetClientThread != null) {
            telnetClientThread.interrupt();
            // Invoke a shutdown after interrupting the thread to close the socket immediately,
            // otherwise the client keeps running until a line was received from the telnet connection
            telnetClientThread.shutdown();
            telnetClientThread = null;
        }
    }

    private void refreshState() {
        // Sends a series of state query commands over the telnet connection
        logger.debug("NADAvrTelnetConnector - refreshState() started....");
        telnetStateRequest = scheduler.submit(() -> {
            // When adding new commands be sure to include in array to refresh states...
            NADMessage queryCmd = null;
            List<NADCommand> NADRefreshCmds = new ArrayList<>(Arrays.asList(NADCommand.POWER_QUERY,
                    NADCommand.INPUT_SOURCE_QUERY, NADCommand.VOLUME_QUERY, NADCommand.LISTENING_MODE_QUERY,
                    NADCommand.MUTE_QUERY, NADCommand.VOLUME_CONTROL_QUERY, NADCommand.VOLUME_FIXED_QUERY));

            // Refresh zone state information
            for (NADCommand NADcmd : NADRefreshCmds) {
                if (NADcmd.getOperator() == NAD_QUERY) {
                    logger.debug("------- >>> NADcmd = {}", NADcmd);
                    for (int zone = 1; zone <= config.getZoneCount(); zone++) {
                        switch (zone) {
                            case 1: // MainZone - 1
                                queryCmd = new NADMessage.MessageBuilder().prefix(Prefix.Main.toString())
                                        .variable(NADcmd.getVariable().toString())
                                        .operator(NADcmd.getOperator().toString()).value(NADcmd.getValue().toString())
                                        .build();
                                break;
                            case 2: // Zone 2
                                queryCmd = new NADMessage.MessageBuilder().prefix(Prefix.Zone2.toString())
                                        .variable(NADcmd.getVariable().toString())
                                        .operator(NADcmd.getOperator().toString()).value(NADcmd.getValue().toString())
                                        .build();
                                break;
                            case 3: // Zone 3
                                queryCmd = new NADMessage.MessageBuilder().prefix(Prefix.Zone3.toString())
                                        .variable(NADcmd.getVariable().toString())
                                        .operator(NADcmd.getOperator().toString()).value(NADcmd.getValue().toString())
                                        .build();
                                break;
                            case 4: // Zone 4
                                queryCmd = new NADMessage.MessageBuilder().prefix(Prefix.Zone4.toString())
                                        .variable(NADcmd.getVariable().toString())
                                        .operator(NADcmd.getOperator().toString()).value(NADcmd.getValue().toString())
                                        .build();
                                break;
                            default:
                                break;
                        }
                        internalSendCommand(queryCmd);
                    }
                }
            }
            // Refresh source input names
            for (int input = 1; input <= 10; input++) {
                String prefix = "Source" + input;
                NADCommand NADCmd = NADCommand.SOURCE_NAME_QUERY;
                queryCmd = new NADMessage.MessageBuilder().prefix(prefix).variable(NADCmd.getVariable().toString())
                        .operator(NADCmd.getOperator().toString()).value(NADCmd.getValue().toString()).build();

                internalSendCommand(queryCmd);
            }
        });
    }

    // TODO rewrite this to use the NAD protocol commands NADMessage
    @Override
    protected void internalSendCommand(NADMessage msg) {
        logger.debug("Sending command '{}'", msg);
        if (msg == null || msg.toString().isBlank()) {
            logger.warn("Trying to send empty command");
            return;
        }
        telnetClientThread.sendCommand(msg);
    }

    private State convertDeviceValueToOpenHabState(String data, Class<?> classToConvert) {
        State state = UnDefType.UNDEF;

        try {
            int index;

            if (data.contentEquals("N/A")) {
                state = UnDefType.UNDEF;

            } else if (classToConvert == OnOffType.class) {
                index = Integer.parseInt(data, 10);
                state = index == 0 ? OnOffType.OFF : OnOffType.ON;

            } else if (classToConvert == DecimalType.class) {
                index = Integer.parseInt(data, 16);
                state = new DecimalType(index);

            } else if (classToConvert == PercentType.class) {
                index = Integer.parseInt(data, 16);
                state = new PercentType(index);

            } else if (classToConvert == StringType.class) {
                state = new StringType(data);

            }
        } catch (Exception e) {
            logger.debug("Cannot convert value '{}' to data type {}", data, classToConvert);
        }

        logger.debug("Converted data '{}' to openHAB state '{}' ({})", data, state, classToConvert);
        return state;
    }

    // TODO this is where we need to set the source input names determined by processInfo method
    private void populateInputs() {
        logger.debug("NADAvrHandler - populateInputs() started....");
        List<StateOption> options = new ArrayList<>();
        // Build list of source names to be used by Input Source channel (options)
        for (int i = 1; i <= avrSourceName.size(); i++) {
            String key = String.valueOf(i);
            String name = avrSourceName.getAvrSourceName(key);
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
