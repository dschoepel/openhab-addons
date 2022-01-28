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
package org.openhab.binding.nadavr.internal.handler;

import static org.openhab.binding.nadavr.internal.NadAvrBindingConstants.*;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.nadavr.internal.NadAvrConfiguration;
import org.openhab.binding.nadavr.internal.NadException;
import org.openhab.binding.nadavr.internal.NadModel;
import org.openhab.binding.nadavr.internal.connector.NadEventListener;
import org.openhab.binding.nadavr.internal.connector.NadIpConnector;
import org.openhab.binding.nadavr.internal.connector.NadTunerMonitor;
import org.openhab.binding.nadavr.internal.nadcp.NadCommand;
import org.openhab.binding.nadavr.internal.nadcp.NadCommand.Prefix;
import org.openhab.binding.nadavr.internal.nadcp.NadMessage;
import org.openhab.binding.nadavr.internal.nadcp.NadUnsupportedCommandTypeException;
import org.openhab.binding.nadavr.internal.state.NadAvrInputSourceList;
import org.openhab.binding.nadavr.internal.state.NadAvrState;
import org.openhab.binding.nadavr.internal.state.NadAvrStateChangedListener;
import org.openhab.binding.nadavr.internal.state.NadAvrStateDescriptionProvider;
import org.openhab.binding.nadavr.internal.state.NadPopulateInputs;
import org.openhab.binding.nadavr.internal.xml.NadTunerPresets;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link NadHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Dave J Schoepel - Initial contribution
 */
@NonNullByDefault
public class NadAvrHandler extends BaseThingHandler implements NadAvrStateChangedListener, NadEventListener {

    private final Logger logger = LoggerFactory.getLogger(NadAvrHandler.class);

    private static final long POLLING_INTERVAL = TimeUnit.SECONDS.toSeconds(60);

    private @Nullable ScheduledFuture<?> connectionJob;

    private NadAvrConfiguration config = new NadAvrConfiguration();
    private NadAvrStateDescriptionProvider stateDescriptionProvider;

    private NadIpConnector connector = new NadIpConnector("127.0.0.1", 23, "OH-Binding-nadavr");
    private NadPopulateInputs populateInput = new NadPopulateInputs(thing.getUID(), config, connector,
            stateDescriptionProvider, true);
    private NadAvrState nadavrState = new NadAvrState(this);
    private NadTunerMonitor tunerMonitor = new NadTunerMonitor(connector, config, nadavrState, "OH-Binding-nadavr");

    private Object sequenceLock = new Object();

    public NadAvrHandler(Thing thing, NadAvrStateDescriptionProvider stateDescriptionProvider) {
        super(thing);
        this.stateDescriptionProvider = stateDescriptionProvider;
    }

    @Override
    public void initialize() {
        if (logger.isDebugEnabled()) {
            logger.debug("Start initializing handler for thing {}", getThing().getUID());
        }

        // Get configuration settings
        config = getConfigAs(NadAvrConfiguration.class);

        // Validate configuration settings
        if (!checkConfiguration(config)) {
            return;
        } else {
            logger.info("NadHandler using configuration: {}", config.toString());
        }

        /* Initialize the list of commands this binding supports */
        NadCommand.initializeCommandList();

        /* Set up number of zones specified for this thing in the configuration */
        configureZoneChannels(config);

        /* Initialize IP connector for the NAD device */
        String threadNamePrefix = "OH-Binding-" + getThing().getUID().getAsString();
        connector = new NadIpConnector(config.ipAddress, config.telnetPort, threadNamePrefix + "-Connection");

        updateStatus(ThingStatus.UNKNOWN);
        /* Start the IP connection to the NAD device */
        scheduleConnectionJob();

        // Start thread to populate names for the Source Input numbers
        if (!populateInput.isRunning()) {
            populateInput = new NadPopulateInputs(thing.getUID(), config, connector, stateDescriptionProvider, true);
            populateInput.startPi();
            logger.info("NadHandler - Populate Inputs Started ....");
        }
        // Start thread to capture state of the tuner input and capture RDS Stream if band is FM
        if (!tunerMonitor.isTmStarted()) {
            tunerMonitor = new NadTunerMonitor(connector, config, nadavrState, threadNamePrefix);
            tunerMonitor.startTm();
            logger.info("NadHandler - Tuner Monitor Started ....");
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Finished initializing handler for thing {}", getThing().getUID());
        }
    }

    @Override
    public void dispose() {
        if (logger.isDebugEnabled()) {
            logger.debug("Disposing handler for thing {}", getThing().getUID());
        }
        closeConnection();
        populateInput.stopPi();
        tunerMonitor.stopTm();
        super.dispose();
    }

    /**
     * Validate configuration settings before bringing NAD AVR Thing online
     *
     * @return true indicating all checks passed or false there are errors to be addressed
     */
    public boolean checkConfiguration(NadAvrConfiguration config) {
        // Check that zone count is within the supported range 1 - max zones for this model
        int maxZones = getMaxZonesForModel(thing.getThingTypeUID().getId());
        if (config.getZoneCount() < 1 || config.getZoneCount() > maxZones) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "This binding supports 1 to " + maxZones + " zones. Please update the zone count.");
            return false;
        }
        // Check for tuner preset details file being enabled
        if (config.arePresetNamesEnabled()) {
            NadTunerPresets tunerPresets = new NadTunerPresets();
            if (config.getPresetNamesFilePath().isBlank()) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        "The tuner presets file name '" + config.getPresetNamesFilePath()
                                + "' is blank!  Please update name that inlcudes a valid path and file name...");
                return false;
            }
            String presetFileName = config.getPresetNamesFilePath();
            File tempFile = new File(presetFileName);
            boolean exists = tempFile.exists();
            if (!exists) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        "The tuner presets file name '" + config.getPresetNamesFilePath()
                                + "' was not found!  Please update name that inlcudes a valid path and file name...");
                return false;
            } else if (!tunerPresets.presetFileIsValid(presetFileName)) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        "The tuner presets file is not formated correctly! Please check the OPENHAB log for details and correct errors in '"
                                + config.getPresetNamesFilePath() + "'");
                return false;
            }
        }
        return true;
    }

    /**
     * @param model
     * @return
     */
    public int getMaxZonesForModel(String model) {
        int maxZones = 2;
        for (NadModel supportedModel : NadModel.values()) {
            if (supportedModel.getId().equals(model)) {
                maxZones = supportedModel.getMaxZones();
            }
        }
        return maxZones;
    }

    private void configureZoneChannels(NadAvrConfiguration config) {
        logger.debug("Configuring zone channels");
        Integer zoneCount = config.getZoneCount();

        ArrayList<Channel> channels = new ArrayList<>(this.getThing().getChannels());

        boolean channelsUpdated = false;

        // construct a set with the existing channel type UIDs, to quickly check
        Set<String> currentChannels = new HashSet<>();
        channels.forEach(channel -> currentChannels.add(channel.getUID().getId()));

        editThing().withoutChannels(channels);
        editThing().withChannels(channels);

        Set<Entry<String, ChannelTypeUID>> channelsToRemove = new HashSet<>();

        if (zoneCount > 1) {
            List<Entry<String, ChannelTypeUID>> channelsToAdd = new ArrayList<>(ZONE2_CHANNEL_TYPES.entrySet());

            if (zoneCount > 2) {
                // add channels for zone 3
                channelsToAdd.addAll(ZONE3_CHANNEL_TYPES.entrySet());
                if (zoneCount > 3) {
                    // add channels for zone 4 (more zones currently not supported)
                    channelsToAdd.addAll(ZONE4_CHANNEL_TYPES.entrySet());
                } else {
                    channelsToRemove.addAll(ZONE4_CHANNEL_TYPES.entrySet());
                }
            } else {
                channelsToRemove.addAll(ZONE3_CHANNEL_TYPES.entrySet());
                channelsToRemove.addAll(ZONE4_CHANNEL_TYPES.entrySet());
            }
            // filter out the already existing channels
            channelsToAdd.removeIf(c -> currentChannels.contains(c.getKey()));
            // add the channels that were not yet added
            if (!channelsToAdd.isEmpty()) {
                for (Entry<String, ChannelTypeUID> entry : channelsToAdd) {
                    String itemType = CHANNEL_ITEM_TYPES.get(entry.getKey());
                    logger.debug("Adding channel ThingUID = {}, key = {}, item type = {}, item value = {}",
                            this.getThing().getUID(), entry.getKey(), itemType, entry.getValue());
                    Channel channel = ChannelBuilder.create(new ChannelUID(this.getThing().getUID(), entry.getKey()))
                            .withType(entry.getValue()).build();
                    logger.debug("adding channel Label = {}, Description = {}, UID = {}", channel.getLabel(),
                            channel.getDescription(), channel.getUID());
                    channels.add(channel);
                }
                channelsUpdated = true;
            } else {
                logger.debug("No zone channels have been added");
            }
        } else {
            channelsToRemove.addAll(ZONE2_CHANNEL_TYPES.entrySet());
            channelsToRemove.addAll(ZONE3_CHANNEL_TYPES.entrySet());
            channelsToRemove.addAll(ZONE4_CHANNEL_TYPES.entrySet());
        }
        // filter out the non-existing channels
        channelsToRemove.removeIf(c -> !currentChannels.contains(c.getKey()));
        // remove the channels that were not yet added
        if (!channelsToRemove.isEmpty()) {
            for (Entry<String, ChannelTypeUID> entry : channelsToRemove) {
                if (channels.removeIf(c -> (entry.getKey()).equals(c.getUID().getId()))) {
                    logger.debug("Removed channel {}", entry.getKey());
                } else {
                    logger.debug("Could NOT remove channel {}", entry.getKey());
                }
            }
            channelsUpdated = true;
        } else {
            logger.debug("No zone channels have been removed");
        }
        // update Thing if channels changed
        if (channelsUpdated) {
            updateThing(editThing().withChannels(channels).build());
        }
    }

    /**
     * Schedule the reconnection job
     */
    private void scheduleConnectionJob() {
        if (logger.isDebugEnabled()) {
            logger.debug("Schedule connection job");
        }

        cancelConnectionJob();
        connectionJob = scheduler.scheduleWithFixedDelay(() -> {
            if (!connector.isConnected()) {
                logger.debug("Trying to reconnect...");
                closeConnection();
                String error = null;
                if (openConnection()) {
                    synchronized (sequenceLock) {
                        try {
                            checkStatus();
                            connector.sendCommand(buidMsgFromCommand(NadCommand.POWER_QUERY));
                        } catch (NadException e) {
                            error = "@text/offline.comm-error-first-command-after-reconnection";
                            logger.debug("First command after connection failed", e);
                            closeConnection();
                        }
                    }
                } else {
                    error = "@text/offline.comm-error-reconnection";
                }
                if (error != null) {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, error);
                } else {
                    updateStatus(ThingStatus.ONLINE);
                }
            }
        }, 1, POLLING_INTERVAL, TimeUnit.SECONDS);
    }

    /**
     * Cancel the reconnection job
     */
    private void cancelConnectionJob() {
        ScheduledFuture<?> connectionJob = this.connectionJob;
        if (connectionJob != null && !connectionJob.isCancelled()) {
            connectionJob.cancel(true);
            this.connectionJob = null;
        }
    }

    /**
     * Open the connection with the Rotel device
     *
     * @return true if the connection is opened successfully or flase if not
     */
    private synchronized boolean openConnection() {
        connector.addEventListener(this);
        try {
            connector.open();
            checkStatus();
        } catch (NadException e) {
            logger.debug("openConnection() failed", e);
        }
        logger.debug("openConnection(): {}", connector.isConnected() ? "connected" : "disconnected");
        return connector.isConnected();
    }

    /**
     * Close the connection with the Rotel device
     */
    private synchronized void closeConnection() {
        connector.close();
        connector.removeEventListener(this);
        logger.debug("closeConnection(): disconnected");
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        try {
            switch (channelUID.getId()) {
                /**
                 * General settings
                 */
                case CHANNEL_TUNER_BAND:
                    connector.sendTunerBandCommand(command, Prefix.Tuner);
                    break;
                case CHANNEL_TUNER_AM_FREQUENCY:
                    connector.sendTunerAmFrequencyCommand(command, Prefix.Tuner);
                    break;
                case CHANNEL_TUNER_FM_FREQUENCY:
                    connector.sendTunerFmFrequencyCommand(command, Prefix.Tuner);
                    break;
                case CHANNEL_TUNER_FM_MUTE:
                    connector.sendTunerFmMuteCommand(command, Prefix.Tuner);
                    break;
                case CHANNEL_TUNER_PRESET:
                    connector.sendTunerPresetCommand(command, Prefix.Tuner);
                    break;
                case CHANNEL_TUNER_FM_RDS_TEXT:
                    connector.sendTunerFmRdsTextCommand(command, Prefix.Tuner);
                    break;
                /**
                 * Main zone
                 */
                case CHANNEL_MAIN_POWER:
                    connector.sendPowerCommand(command, Prefix.Main);
                    break;
                case CHANNEL_MAIN_LISTENING_MODE:
                    connector.sendListeningModeCommand(command, Prefix.Main);
                    break;
                case CHANNEL_MAIN_MUTE:
                    connector.sendMuteCommand(command, Prefix.Main);
                    break;
                case CHANNEL_MAIN_VOLUME:
                    connector.sendVolumeCommand(command, Prefix.Main);
                    break;
                case CHANNEL_MAIN_VOLUME_DB:
                    connector.sendVolumeCommand(command, Prefix.Main);
                    break;
                case CHANNEL_MAIN_SOURCE:
                    connector.sendSourceCommand(command, Prefix.Main);
                    break;
                /**
                 * Zone 2
                 */
                case CHANNEL_ZONE2_POWER:
                    connector.sendPowerCommand(command, Prefix.Zone2);
                    break;
                case CHANNEL_ZONE2_MUTE:
                    connector.sendMuteCommand(command, Prefix.Zone2);
                    break;
                case CHANNEL_ZONE2_VOLUME:
                    connector.sendVolumeCommand(command, Prefix.Zone2);
                    break;
                case CHANNEL_ZONE2_VOLUME_DB:
                    connector.sendVolumeCommand(command, Prefix.Zone2);
                    break;
                case CHANNEL_ZONE2_SOURCE:
                    connector.sendSourceCommand(command, Prefix.Zone2);
                    break;
                case CHANNEL_ZONE2_VOLUME_FIXED:
                    connector.sendVolumeFixedCommand(command, Prefix.Zone2);
                    break;
                case CHANNEL_ZONE2_VOLUME_FIXED_DB:
                    connector.sendVolumeFixedDBCommand(command, Prefix.Zone2);
                    break;
                case CHANNEL_ZONE2_VOLUME_CONTROL:
                    connector.sendVolumeControlCommand(command, Prefix.Zone2);
                    break;
                /**
                 * Zone 3
                 */
                case CHANNEL_ZONE3_POWER:
                    connector.sendPowerCommand(command, Prefix.Zone3);
                    break;
                case CHANNEL_ZONE3_MUTE:
                    connector.sendMuteCommand(command, Prefix.Zone3);
                    break;
                case CHANNEL_ZONE3_VOLUME:
                    connector.sendVolumeCommand(command, Prefix.Zone3);
                    break;
                case CHANNEL_ZONE3_VOLUME_DB:
                    connector.sendVolumeCommand(command, Prefix.Zone3);
                    break;
                case CHANNEL_ZONE3_SOURCE:
                    connector.sendSourceCommand(command, Prefix.Zone3);
                    break;
                case CHANNEL_ZONE3_VOLUME_FIXED:
                    connector.sendVolumeFixedCommand(command, Prefix.Zone3);
                    break;
                case CHANNEL_ZONE3_VOLUME_FIXED_DB:
                    connector.sendVolumeFixedDBCommand(command, Prefix.Zone3);
                    break;
                case CHANNEL_ZONE3_VOLUME_CONTROL:
                    connector.sendVolumeControlCommand(command, Prefix.Zone3);
                    break;
                /**
                 * Zone 4
                 */
                case CHANNEL_ZONE4_POWER:
                    connector.sendPowerCommand(command, Prefix.Zone4);
                    break;
                case CHANNEL_ZONE4_MUTE:
                    connector.sendMuteCommand(command, Prefix.Zone4);
                    break;
                case CHANNEL_ZONE4_VOLUME:
                    connector.sendVolumeCommand(command, Prefix.Zone4);
                    break;
                case CHANNEL_ZONE4_VOLUME_DB:
                    connector.sendVolumeCommand(command, Prefix.Zone4);
                    break;
                case CHANNEL_ZONE4_SOURCE:
                    connector.sendSourceCommand(command, Prefix.Zone4);
                    break;
                case CHANNEL_ZONE4_VOLUME_FIXED:
                    connector.sendVolumeFixedCommand(command, Prefix.Zone4);
                    break;
                case CHANNEL_ZONE4_VOLUME_FIXED_DB:
                    connector.sendVolumeFixedDBCommand(command, Prefix.Zone4);
                    break;
                case CHANNEL_ZONE4_VOLUME_CONTROL:
                    connector.sendVolumeControlCommand(command, Prefix.Zone4);
                    break;
                default:
                    throw new NadUnsupportedCommandTypeException();
            }
        } catch (NadUnsupportedCommandTypeException e) {
            logger.debug("Unsupported Command {} for channel {}", command, channelUID.getId());
        } catch (NadException e) {
            logger.error("Sending command to channel \"{}\" failed.  Error: {}", channelUID.getId(),
                    e.getLocalizedMessage());
        }
    }

    @Override
    public void receivedMessage(NadMessage msg) {
        logger.debug("Received status update from NAD Device @{}: data={}", connector.getConnectionName(), msg);

        updateStatus(ThingStatus.ONLINE);
        NadCommand receivedCommand = NadCommand.EMPTY_COMMAND;
        try {
            receivedCommand = NadCommand.getCommandByVariableAndOperator(msg.getVariable(), msg.getOperator());
        } catch (IllegalArgumentException ex) {
            logger.debug("Received unknown status update from NAD Device @{}: data={}",
                    config.hostname + ":" + config.ipAddress, msg);
            return;
        }
        try {
            String commandPrefix = msg.getPrefix();

            if (commandPrefix.contains(NAD_PREFIX_SOURCE) && msg.getVariable().contains(NAD_VARIABLE_NAME)
                    && NAD_EQUALS_OPERATOR.equals(msg.getOperator())) {
                int index = (Integer.parseInt(commandPrefix.substring(6).stripTrailing()) - 1);
                NadAvrInputSourceList.updateSourceName(index, msg.getValue());

                logger.debug("NadAvrInputSourceList updated with new name at index - {}, value = {}", index,
                        msg.getValue());
            }
            if (receivedCommand != null) {
                switch (receivedCommand) {
                    case SOURCE_SET:
                        if (NadAvrInputSourceList.size() > 0) {
                            String sourceName = "";
                            int index = (Integer.parseInt(msg.getValue()));
                            if (index >= 11 && !ZONE1.equals(commandPrefix)) { // Zones2-3 include Local source
                                sourceName = LOCAL;
                            } else {
                                sourceName = NadAvrInputSourceList.getSourceName(index - 1);
                            }
                            nadavrState.setSourceName(msg.getPrefix().toString(), sourceName);
                        }
                        break;
                    case POWER_SET:
                        nadavrState.setPower(msg.getPrefix(), NAD_ON.equalsIgnoreCase(msg.getValue()));
                        break;
                    case VOLUME_CONTROL_SET:
                        nadavrState.setVolumeControl(msg.getPrefix(), msg.getValue());
                        break;
                    case VOLUME_FIXED_SET:
                        BigDecimal volumeFixed = new BigDecimal(msg.getValue());
                        nadavrState.setVolumeFixed(msg.getPrefix(), volumeFixed);
                        break;
                    case VOLUME_SET:
                        BigDecimal volume = new BigDecimal(msg.getValue().toString());
                        nadavrState.setVolume(msg.getPrefix(), volume);
                        break;
                    case MUTE_SET:
                        nadavrState.setMute(msg.getPrefix(), NAD_ON.equalsIgnoreCase(msg.getValue()));
                        break;
                    case LISTENING_MODE_SET:
                        nadavrState.setListeningMode(commandPrefix, msg.getValue().toString());
                        break;
                    case TUNER_BAND_SET:
                        nadavrState.setTunerBand(commandPrefix, msg.getValue().toString());
                        break;
                    case TUNER_FM_FREQUENCY_SET:
                        BigDecimal fmFrequency = new BigDecimal(msg.getValue());
                        nadavrState.setTunerFMFrequency(commandPrefix, fmFrequency);
                        break;
                    case TUNER_AM_FREQUENCY_SET:
                        BigDecimal amFrequency = new BigDecimal(msg.getValue());
                        nadavrState.setTunerAMFrequency(commandPrefix, amFrequency);
                        break;
                    case TUNER_FM_MUTE_SET:
                        nadavrState.setTunerFMMute(commandPrefix, NAD_ON.equalsIgnoreCase(msg.getValue()));
                        break;
                    case TUNER_PRESET_SET:
                        BigDecimal preset = new BigDecimal(msg.getValue());
                        String fileName = config.getPresetNamesFilePath();
                        nadavrState.setTunerPreset(commandPrefix, preset, fileName);
                        break;
                    case TUNER_FM_RDS_TEXT_SET:
                        nadavrState.setTunerFMRdsText(commandPrefix, msg.getValue().toString());
                        break;
                    default:
                        break;
                }
            }
        } catch (Exception ex) {
            logger.warn("Exception in receivedMessage for NAD Device @{}. Cause: {}, message received: {}",
                    connector.getConnectionName(), ex.getMessage(), msg);
        }
    }

    private void checkStatus() {
        // Sends a series of state query commands over the connection
        logger.debug("NADAvrTelnetConnector - refreshState() started.... connector is started = {}",
                connector.isConnected());
        // ScheduledExecutorService refresh = Executors.newSingleThreadScheduledExecutor();
        if (connector.isConnected()) {
            scheduler.execute(() -> {
                // Initialize message with default "Main.Power=?" command
                NadMessage queryCmd = new NadMessage.MessageBuilder().prefix(Prefix.Main.toString())
                        .variable(NadCommand.POWER_QUERY.getVariable().toString())
                        .operator(NadCommand.POWER_QUERY.getOperator().toString())
                        .value(NadCommand.POWER_QUERY.getValue().toString()).build();

                // Refresh source input names
                for (int input = 1; input <= 10; input++) {
                    String prefix = "Source" + input;
                    NadCommand nadCmd = NadCommand.SOURCE_NAME_QUERY;
                    queryCmd = new NadMessage.MessageBuilder().prefix(prefix).variable(nadCmd.getVariable().toString())
                            .operator(nadCmd.getOperator().toString()).value(nadCmd.getValue().toString()).build();

                    try {
                        connector.sendCommand(queryCmd);
                    } catch (NadException e) {
                        logger.error(
                                "Error requesting source name refresh from the NAD device @{}, check for connection issues.  Error: {}",
                                connector.getConnectionName(), e.getLocalizedMessage());

                    }
                }
                // When adding new general commands be sure to include in array to refresh states...
                List<NadCommand> nadGeneralRefreshCmds = new ArrayList<>(
                        Arrays.asList(NadCommand.TUNER_BAND_QUERY, NadCommand.TUNER_AM_FREQUENCY_QUERY,
                                NadCommand.TUNER_FM_FREQUENCY_QUERY, NadCommand.TUNER_FM_MUTE_QUERY,
                                NadCommand.TUNER_FM_RDS_TEXT_QUERY, NadCommand.TUNER_PRESET_QUERY));

                // Refresh general state information
                for (NadCommand nadGeneralCmd : nadGeneralRefreshCmds) {
                    if (nadGeneralCmd.getOperator() == NAD_QUERY) {
                        queryCmd = new NadMessage.MessageBuilder().prefix(Prefix.Tuner.toString())
                                .variable(nadGeneralCmd.getVariable().toString())
                                .operator(nadGeneralCmd.getOperator().toString())
                                .value(nadGeneralCmd.getValue().toString()).build();
                        try {
                            connector.sendCommand(queryCmd);
                        } catch (NadException e) {
                            logger.error(
                                    "Error requesting general state informatin from the NAD device @{}, check for connection issues.  Error: {}",
                                    connector.getConnectionName(), e.getLocalizedMessage());

                        }
                    }
                }
                // When adding new zone commands be sure to include in array to refresh states...
                List<NadCommand> nadZoneRefreshCmds = new ArrayList<>(Arrays.asList(NadCommand.POWER_QUERY,
                        NadCommand.INPUT_SOURCE_QUERY, NadCommand.VOLUME_QUERY, NadCommand.LISTENING_MODE_QUERY,
                        NadCommand.MUTE_QUERY, NadCommand.VOLUME_CONTROL_QUERY, NadCommand.VOLUME_FIXED_QUERY));
                // Refresh zone state information
                for (NadCommand nadCmd : nadZoneRefreshCmds) {
                    if (nadCmd.getOperator() == NAD_QUERY) {
                        logger.debug("------- >>> NADcmd = {}", nadCmd);
                        for (int zone = 1; zone <= config.getZoneCount(); zone++) {
                            switch (zone) {
                                case 1: // MainZone - 1
                                    queryCmd = new NadMessage.MessageBuilder().prefix(Prefix.Main.toString())
                                            .variable(nadCmd.getVariable().toString())
                                            .operator(nadCmd.getOperator().toString())
                                            .value(nadCmd.getValue().toString()).build();
                                    break;
                                case 2: // Zone 2
                                    queryCmd = new NadMessage.MessageBuilder().prefix(Prefix.Zone2.toString())
                                            .variable(nadCmd.getVariable().toString())
                                            .operator(nadCmd.getOperator().toString())
                                            .value(nadCmd.getValue().toString()).build();
                                    break;
                                case 3: // Zone 3
                                    queryCmd = new NadMessage.MessageBuilder().prefix(Prefix.Zone3.toString())
                                            .variable(nadCmd.getVariable().toString())
                                            .operator(nadCmd.getOperator().toString())
                                            .value(nadCmd.getValue().toString()).build();
                                    break;
                                case 4: // Zone 4
                                    queryCmd = new NadMessage.MessageBuilder().prefix(Prefix.Zone4.toString())
                                            .variable(nadCmd.getVariable().toString())
                                            .operator(nadCmd.getOperator().toString())
                                            .value(nadCmd.getValue().toString()).build();
                                    break;
                                default: // General commands
                                    break;
                            }
                            try {
                                connector.sendCommand(queryCmd);
                            } catch (NadException e1) {
                                logger.error(
                                        "Error requesting zone state information from the NAD device @{}, check for connection issues.  Error: {}",
                                        connector.getConnectionName(), e1.getLocalizedMessage());

                            }
                            // pause enough for command status to be returned...
                            try {
                                TimeUnit.MILLISECONDS.sleep(50);
                                // TimeUnit.SECONDS.sleep(3); // 3 seconds
                            } catch (InterruptedException e) {
                                logger.debug("Nadhandler refresh Error in sleep to receive status", e);
                            }
                        }
                    }
                }
            });
        } else {
            updateStatus(ThingStatus.OFFLINE);
        }
    }

    @Override
    public void stateChanged(String channelID, State state) {
        // Don't flood the log with thing 'updated: ONLINE' each time a single channel changed
        // TODO do we need this?
        // if (this.getThing().getStatus() != ThingStatus.ONLINE) {
        // updateStatus(ThingStatus.ONLINE);
        // }
        // Only update channels if they are linked to an item
        if (isLinked(channelID)) {
            updateState(channelID, state);
        }
    }

    @Override
    public void connectionError(@Nullable String errorMessage) {
        if (logger.isDebugEnabled()) {
            logger.debug("Connection error occurred to NAD Device @{}", config.ipAddress + ":" + config.telnetPort);
        }
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, errorMessage);
        // To be sure, in case something is still running, try to close the connection..
        if (connector.isConnected()) {
            connector.close();
            ;
        }
    }

    private NadMessage buidMsgFromCommand(NadCommand command) {
        return new NadMessage.MessageBuilder().prefix(command.getPrefix()).variable(command.getVariable())
                .operator(command.getOperator()).value(command.getValue()).build();
    }
}
