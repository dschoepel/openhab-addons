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
import java.util.stream.Collectors;

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
import org.openhab.binding.nadavr.internal.state.NadTunerPresetNameList;
import org.openhab.binding.nadavr.internal.xml.NadTunerPresets;
import org.openhab.core.library.types.StringType;
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
import org.openhab.core.types.StateOption;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link NadHandler} is responsible for initializing the NAD AVR Thing, validating the configuration, handling
 * commands sent to one of the channels, updating item states when changes are detected, and disposing of the thing.
 *
 * @author Dave J Schoepel - Initial contribution
 */
@NonNullByDefault
public class NadAvrHandler extends BaseThingHandler implements NadAvrStateChangedListener, NadEventListener {

    private final Logger logger = LoggerFactory.getLogger(NadAvrHandler.class);

    private static final long POLLING_INTERVAL = TimeUnit.SECONDS.toSeconds(60);

    private @Nullable ScheduledFuture<?> connectionJob;
    private @Nullable ScheduledFuture<?> requestDeviceDetailsJob;

    private NadAvrConfiguration config = new NadAvrConfiguration();
    private NadAvrStateDescriptionProvider stateDescriptionProvider;

    private NadIpConnector connector = new NadIpConnector("127.0.0.1", 23, "OH-Binding-nadavr");
    private NadPopulateInputs populateInputs = new NadPopulateInputs(thing.getUID(), config, connector,
            stateDescriptionProvider, true, 10);
    private NadAvrState nadavrState = new NadAvrState(this);
    private NadTunerMonitor tunerMonitor = new NadTunerMonitor(connector, config, nadavrState, "OH-Binding-nadavr");

    private Object sequenceLock = new Object();

    /**
     * Constructor for NAD Audio Receiver Handler
     *
     * @param thing - Surround sound audio receiver
     * @param stateDescriptionProvider - Dynamic provider of state options for user described zone and tuner channels
     */
    public NadAvrHandler(Thing thing, NadAvrStateDescriptionProvider stateDescriptionProvider) {
        super(thing);
        this.stateDescriptionProvider = stateDescriptionProvider;
    }

    /**
     * Method to initialize a NAD AV Receiver thing.
     * <ul>
     * <li>Validate configuration settings</li>
     * <li>Configure number of zones for the AV Receiver thing from 1 to up to max allowed for model</li>
     * <li>Connect to device and start thread to check for failed connections</li>
     * <li>Initialize channel states with settings retrieved from the receiver</li>
     * <li>Start threads to monitor for input name changes and tuner setting changes</li>
     * </ul>
     */
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
            logger.info("nadavr:NadHandler using configuration: {}", config.toString());
        }

        /* Initialize the list of commands this binding supports */
        NadCommand.initializeCommandList();

        /* Set up number of zones specified for this thing in the configuration */
        configureZoneChannels(config);

        if (config.enableTunerSupport) {
            /* Set the Tuner Preset option values, if active, update with the user provided preset descriptions */
            populateTunerPresets();

            /* Set the Tuner Band option values */
            populateTunerBands();
        }

        /* Initialize IP connector for the NAD device */
        String threadNamePrefix = "OH-Binding-" + getThing().getUID().getAsString();
        connector = new NadIpConnector(config.ipAddress, config.telnetPort, threadNamePrefix + "-Connection");

        updateStatus(ThingStatus.UNKNOWN);
        /* Start the IP connection to the NAD device */
        scheduleConnectionJob();

        // Start thread to populate names for the Source Input numbers
        if (!populateInputs.isRunning()) {
            int numberOfInputSources = getNumberOfInputSources(thing.getThingTypeUID().getId());
            if (numberOfInputSources > 0) {
                populateInputs = new NadPopulateInputs(thing.getUID(), config, connector, stateDescriptionProvider,
                        true, numberOfInputSources);
                populateInputs.startPi();
            }
        } // end if populatInputs

        // Start thread to capture state of the tuner input and capture RDS Stream if band is FM
        if (config.enableTunerSupport && !tunerMonitor.isTmStarted()) {
            tunerMonitor = new NadTunerMonitor(connector, config, nadavrState, threadNamePrefix);
            tunerMonitor.startTm();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Finished initializing handler for thing {}", getThing().getUID());
        }

        if (config.refreshInterval > 0) {
            // To be sure this job is not running try to cancel it before starting a new one
            cancelRequestDeviceDetailsJob();
            // Start state refresh updater
            requestDeviceDetailsJob = scheduler.scheduleWithFixedDelay(() -> {
                Thread.currentThread().setName("OH-binding-" + this.thing.getUID() + "-reqeustDeviceDetailsJob");
                try {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Send item state requests to NAD Receiver @{}", connector.getConnectionName());
                    }
                    requestDeviceDetails();
                } catch (LinkageError e) {
                    logger.warn("Failed to send item state refresh requests to NAD Receiver @{}. Cause: {}",
                            connector.getConnectionName(), e.getMessage());
                } catch (Exception ex) {
                    logger.warn("Exception in item state refresh Thread NAD Receiver @{}. Cause: {}",
                            connector.getConnectionName(), ex.getMessage());
                }
            }, config.refreshInterval, config.refreshInterval, TimeUnit.SECONDS);
        }
    }

    /**
     * Method to shutdown and remove NAD AV Receiver thing.
     * <ul>
     * <li>Close connection</li>
     * <li>Kill reconnection thread</li>
     * <li>Kill threads to monitor for input name changes and tuner setting changes</li>
     * <li>Remove handler</li>
     * </ul>
     */
    @Override
    public void dispose() {
        if (logger.isDebugEnabled()) {
            logger.debug("Disposing handler for thing {}", getThing().getUID());
        }
        closeConnection();
        cancelConnectionJob();
        cancelRequestDeviceDetailsJob();
        populateInputs.stopPi();
        tunerMonitor.stopTm();
        super.dispose();
    }

    /**
     * Method to validate configuration settings before bringing NAD AVR Thing online
     *
     * @param config - NAD AV Receiver thing configuration settings
     * @return true indicating all checks passed or false when there are errors to be addressed
     */
    public boolean checkConfiguration(NadAvrConfiguration config) {
        // Check that zone count is within the supported range 1 - max zones for this model
        int maxZones = getMaxZonesForModel(thing.getThingTypeUID().getId());
        if (config.getZoneCount() < 1 || config.getZoneCount() > maxZones) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "This binding supports 1 to " + maxZones + " zones. Please update the zone count.");
            return false;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("enableTunerSupport is {}", config.enableTunerSupport);
        }

        // Check for tuner preset details file being enabled
        if (config.arePresetNamesEnabled() && config.enableTunerSupport) {
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
     * Method to get the maximum number of zones for the NAD thing's model. The default is 2 zones if the model
     * is not found in {@link NadModel.java}
     *
     * @param model - NAD AVR thing model number
     * @return maximum physical zones this NAD model has
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

    /**
     * Method to retrieve the number of input sources for a given NAD Model, {@link NADPopulateInputs.java} uses
     * this number to update the input source channels for the NAD device zones
     *
     * @param model - NAD Model name to lookup the number of input sources
     * @return numberOfInputSources available on the given NAD Model
     */
    public int getNumberOfInputSources(String model) {
        /* Default number of input sources */
        int numberOfInputSources = 8;
        for (NadModel supportedModel : NadModel.values()) {
            if (supportedModel.getId().equals(model)) {
                numberOfInputSources = supportedModel.getNumberOfInputSources();
            }
        }
        return numberOfInputSources;
    }

    /**
     * Method to configure the number of zone channels for the NAD AV Receiver. This can be
     * specified on the thing configuration "zoneCount" between 1 and the max number of zones allowed
     * for the model. Channels will be dynamically added or removed depending increasing or decreasing the
     * zoneCount.
     *
     * @param config NAD AV Receiver thing configuration settings
     */
    private void configureZoneChannels(NadAvrConfiguration config) {
        logger.debug("Configuring zone channels");
        Integer zoneCount = config.getZoneCount();
        // current zone channels
        ArrayList<Channel> channels = new ArrayList<>(this.getThing().getChannels());
        boolean channelsUpdated = false;
        // construct a set with the existing channel type UIDs, to quickly check
        Set<String> currentChannels = new HashSet<>();
        channels.forEach(channel -> currentChannels.add(channel.getUID().getId()));
        // Make sure list of channels is clean by removing and adding them
        editThing().withoutChannels(channels);
        editThing().withChannels(channels);
        // Initialize empty List to hold channels to be removed
        Set<Entry<String, ChannelTypeUID>> channelsToRemove = new HashSet<>();
        // Process of adding or removing zones based on number set in the thing configuration
        if (zoneCount > 1) {
            // add channels for zone 2
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
                    String itemLabel = CHANNEL_ITEM_LABELS.get(entry.getKey());
                    if (itemLabel != null) {
                        Channel channel = ChannelBuilder
                                .create(new ChannelUID(this.getThing().getUID(), entry.getKey()), itemType)
                                .withType(entry.getValue()).withLabel(itemLabel).build();
                        channels.add(channel);
                    }
                }
                channelsUpdated = true;
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("No zone channels have been added");
                }
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
                    if (logger.isDebugEnabled()) {
                        logger.debug("Removed channel {}", entry.getKey());
                    }
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Could NOT remove channel {}", entry.getKey());
                    }
                }
            }
            channelsUpdated = true;
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("No zone channels have been removed");
            }
        }
        // update Thing if channels changed
        if (channelsUpdated) {
            updateThing(editThing().withChannels(channels).build());
        }
    }

    /**
     * Method to Schedule the reconnection job. If connection is dropped, will try to reconnect
     * or send diagnostic messages to troubleshoot.
     */
    private void scheduleConnectionJob() {
        if (logger.isDebugEnabled()) {
            logger.debug("Scheduling connection job");
        }
        // Make sure there is no active connection
        cancelConnectionJob();
        // Schedule connection job with initial delay and Polling Interval in seconds
        connectionJob = scheduler.scheduleWithFixedDelay(() -> {
            if (!connector.isConnected()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Trying to reconnect...");
                }
                closeConnection();
                String error = null;
                if (openConnection()) {
                    synchronized (sequenceLock) {
                        try {
                            // Send commands to Receiver to initialize channel states and verify connection
                            requestDeviceDetails();
                            connector.sendCommand(buidMsgFromCommand(NadCommand.POWER_QUERY));
                        } catch (NadException e) {
                            error = "@text/offline.comm-error-first-command-after-reconnection";
                            if (logger.isDebugEnabled()) {
                                logger.debug("First command after connection failed", e);
                            }
                            closeConnection();
                        }
                    }
                } else {
                    error = "@text/offline.comm-error-reconnection";
                }
                if (error != null) {
                    // Connection errors - set thing to offline and send diagnostic messages
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, error);
                } else {
                    // Connection is active, set thing to ONLINE
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
     * Cancel the checkStatus job
     */
    private void cancelRequestDeviceDetailsJob() {
        ScheduledFuture<?> requestDeviceDetailsJob = this.requestDeviceDetailsJob;
        if (requestDeviceDetailsJob != null && !requestDeviceDetailsJob.isCancelled()) {
            requestDeviceDetailsJob.cancel(true);
            this.requestDeviceDetailsJob = null;
        }
    }

    /**
     * Open the connection with the NAD AV Receiver
     *
     * @return true if the connection is opened successfully or false if not
     */
    private synchronized boolean openConnection() {
        connector.addEventListener(this);
        try {
            connector.open();
            // Send commands to Receiver to initialize channel states
            requestDeviceDetails();
        } catch (NadException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("openConnection() failed", e);
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("openConnection(): {}", connector.isConnected() ? "connected" : "disconnected");
        }
        return connector.isConnected();
    }

    /**
     * Close the connection with the NAD device
     */
    private synchronized void closeConnection() {
        connector.close();
        connector.removeEventListener(this);
        if (logger.isDebugEnabled()) {
            logger.debug("closeConnection(): disconnected");
        }
    }

    /**
     * Method to handle sending a command from a given channel to the NAD AV Receiver thing.
     */
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        try {
            switch (channelUID.getId()) {
                /**
                 * Tuner settings
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
                case CHANNEL_TUNER_XM_CHANNEL:
                    connector.sendTunerXMChannelCommand(command, Prefix.Tuner);
                    break;
                /**
                 * Main zone (Zone 1)
                 */
                case CHANNEL_MAIN_POWER:
                    connector.sendPowerCommand(command, Prefix.Main);
                    break;
                case CHANNEL_MAIN_MODEL:
                    connector.sendModelQueryCommand(command, Prefix.Main);
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
            if (logger.isDebugEnabled()) {
                logger.debug("Unsupported Command {} for channel {}", command, channelUID.getId());
            }
        } catch (NadException e) {
            logger.error("Sending command to channel \"{}\" failed.  Error: {}", channelUID.getId(),
                    e.getLocalizedMessage());
        }
    }

    /**
     *
     */
    @Override
    public void receivedMessage(NadMessage msg) {
        if (logger.isDebugEnabled()) {
            logger.debug("Received status update from NAD Device @{}: data={}", connector.getConnectionName(), msg);
        }
        // Connection is live, set status to ONLINE if it is not already set
        if (!ThingStatus.ONLINE.equals(thing.getStatus())) {
            updateStatus(ThingStatus.ONLINE);
        }
        // Initialize receivedCommand before validating the NadMesage
        NadCommand receivedCommand = NadCommand.EMPTY_COMMAND;
        try {
            receivedCommand = NadCommand.getCommandByVariableAndOperator(msg.getVariable(), msg.getOperator());
        } catch (IllegalArgumentException ex) {
            // Some messages are not configured/supported in this binding and are simply ignored. Use debug to see them.
            // The NaDCommand.java class defines the messages recognized by this binding //
            if (logger.isDebugEnabled()) {
                logger.debug("Received unknown status update from NAD Device @{}: data={}", config.ipAddress, msg);
            }
            return;
        }
        try {
            String commandPrefix = msg.getPrefix();
            // Look for input source names to be used to update the the input options for a zone
            if (commandPrefix.contains(NAD_PREFIX_SOURCE) && msg.getVariable().contains(NAD_VARIABLE_NAME)
                    && NAD_EQUALS_OPERATOR.equals(msg.getOperator())) {
                int index = (Integer.parseInt(commandPrefix.substring(6).stripTrailing()) - 1);
                NadAvrInputSourceList.updateSourceName(index, msg.getValue());
                if (logger.isDebugEnabled()) {
                    logger.debug("NadAvrInputSourceList updated with new name at index - {}, value = {}", index,
                            msg.getValue());
                }
            }
            // Process valid command to update the respective channel state.
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
                    case MODEL_NAME:
                        nadavrState.setModelName(msg.getPrefix(), msg.getValue());
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
                        // refreshTunerDetails();
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
                        String fileName = "";
                        if (config.enablePresetNames) {
                            fileName = config.getPresetNamesFilePath();
                        }
                        nadavrState.setTunerPreset(commandPrefix, msg.getValue().toString(), fileName);
                        break;
                    case TUNER_FM_RDS_TEXT_SET:
                        nadavrState.setTunerFMRdsText(commandPrefix, msg.getValue().toString());
                        break;
                    case TUNER_XM_CHANNEL_SET:
                        String xmChannel = "0";
                        if (msg.getValue().equals("None")) {
                            xmChannel = "1"; // Set to default Preview channel
                        } else {
                            xmChannel = msg.getValue();
                            nadavrState.setTunerXMChannel(commandPrefix, xmChannel);
                        }
                        break;
                    case TUNER_XM_CHANNEL_NAME_SET:
                        nadavrState.setTunerXMChannelName(commandPrefix, msg.getValue().toString());
                        break;
                    case TUNER_XM_NAME_SET:
                        nadavrState.setTunerXMName(commandPrefix, msg.getValue().toString());
                        break;
                    case TUNER_XM_SONG_TITLE_SET:
                        nadavrState.setTunerXMSongTitle(commandPrefix, msg.getValue().toString());
                        break;
                    case TUNER_DAB_DLS_TEXT_SET:
                        nadavrState.setTunerDABDlsText(commandPrefix, msg.getValue().toString());
                        break;
                    case TUNER_DAB_SERVICE_NAME_SET:
                        nadavrState.setTunerDABServiceName(commandPrefix, msg.getValue().toString());
                        break;
                    default:
                        break;
                }
            }
        } catch (Exception ex) {
            // Highlight invalid messages
            logger.warn("Exception in receivedMessage for NAD Device @{}. Cause: {}, message received: {}",
                    connector.getConnectionName(), ex.getMessage(), msg);
        }
    }

    /**
     * Method to send state query commands to the NAD AV Receiver to initialize/refresh channel states
     */
    private void requestDeviceDetails() {
        // Sends a series of state query commands over the connection
        int temp = getNumberOfInputSources(thing.getThingTypeUID().getId());
        if (logger.isDebugEnabled()) {
            logger.debug("NADAvrHandler - checkStatus() started.... connector is started = {}, numberOfInputs {}",
                    connector.isConnected(), temp);
        }
        // Start a one-shot thread to send the queries to the Receiver
        if (connector.isConnected()) {
            // scheduler.execute(() -> {
            // Initialize message with default "Main.Power=?" command
            NadMessage queryCmd = new NadMessage.MessageBuilder().prefix(Prefix.Main.toString())
                    .variable(NadCommand.POWER_QUERY.getVariable().toString())
                    .operator(NadCommand.POWER_QUERY.getOperator().toString())
                    .value(NadCommand.POWER_QUERY.getValue().toString()).build();

            // Refresh source input names if the number of inputs are greater than zero
            if (getNumberOfInputSources(thing.getThingTypeUID().getId()) > 0) {
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
            }

            // Refresh tuner state information
            refreshTunerDetails();

            // When adding new zone commands be sure to include in array to refresh states...
            List<NadCommand> nadZoneRefreshCmds = new ArrayList<>(
                    Arrays.asList(NadCommand.POWER_QUERY, NadCommand.MODEL_QUERY, NadCommand.INPUT_SOURCE_QUERY,
                            NadCommand.VOLUME_QUERY, NadCommand.LISTENING_MODE_QUERY, NadCommand.MUTE_QUERY,
                            NadCommand.VOLUME_CONTROL_QUERY, NadCommand.VOLUME_FIXED_QUERY));
            // Refresh zone state information
            for (NadCommand nadCmd : nadZoneRefreshCmds) {
                if (nadCmd.getOperator() == NAD_QUERY) {
                    logger.debug("------- >>> NADcmd = {}", nadCmd);
                    for (int zone = 1; zone <= config.getZoneCount(); zone++) {
                        switch (zone) {
                            case 1: // MainZone - 1
                                if (NadCommand.VOLUME_CONTROL_QUERY.equals(nadCmd)
                                        || NadCommand.VOLUME_FIXED_QUERY.equals(nadCmd)) {
                                    break; // not valid for main zone
                                }
                                queryCmd = new NadMessage.MessageBuilder().prefix(Prefix.Main.toString())
                                        .variable(nadCmd.getVariable().toString())
                                        .operator(nadCmd.getOperator().toString()).value(nadCmd.getValue().toString())
                                        .build();
                                break;
                            case 2: // Zone 2
                                if (NadCommand.LISTENING_MODE_QUERY.equals(nadCmd)) {
                                    break; // only valid for main zone
                                }
                                queryCmd = new NadMessage.MessageBuilder().prefix(Prefix.Zone2.toString())
                                        .variable(nadCmd.getVariable().toString())
                                        .operator(nadCmd.getOperator().toString()).value(nadCmd.getValue().toString())
                                        .build();
                                break;
                            case 3: // Zone 3
                                if (NadCommand.LISTENING_MODE_QUERY.equals(nadCmd)) {
                                    break; // only valid for main zone
                                }
                                queryCmd = new NadMessage.MessageBuilder().prefix(Prefix.Zone3.toString())
                                        .variable(nadCmd.getVariable().toString())
                                        .operator(nadCmd.getOperator().toString()).value(nadCmd.getValue().toString())
                                        .build();
                                break;
                            case 4: // Zone 4
                                if (NadCommand.LISTENING_MODE_QUERY.equals(nadCmd)) {
                                    break; // only valid for main zone
                                }
                                queryCmd = new NadMessage.MessageBuilder().prefix(Prefix.Zone4.toString())
                                        .variable(nadCmd.getVariable().toString())
                                        .operator(nadCmd.getOperator().toString()).value(nadCmd.getValue().toString())
                                        .build();
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
                        } catch (InterruptedException e) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Nadhandler refresh Error in sleep to receive status", e);
                            }
                        }
                    }
                }
            }
            // });
        } else {
            // Not Connected, set to OFFLINE
            updateStatus(ThingStatus.OFFLINE);
        }
    }

    /**
     * Method to update a changed state
     */
    @Override
    public void stateChanged(String channelID, State state) {
        /* Only update channels if they are linked to an item */
        if (isLinked(channelID)) {
            updateState(channelID, state);
        }
    }

    /**
     * Method to handle/report connection errors
     */
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

    /**
     * Method to format command components into an NadMessage
     *
     * @param command - ENUM {@link NadCommand.java} version of an NADCommand
     * @return formatted NadMessage (.e.g Main.Power=On)
     */
    private NadMessage buidMsgFromCommand(NadCommand command) {
        return new NadMessage.MessageBuilder().prefix(command.getPrefix()).variable(command.getVariable())
                .operator(command.getOperator()).value(command.getValue()).build();
    }

    /**
     * Method to update tuner band channel name options
     */
    private void populateTunerBands() {
        Set<String> bandSet = Arrays.stream(NadCommand.DefaultTunerBandNames.values())
                .map(prefix -> new String(prefix.name())).collect(Collectors.toSet());
        String[] bands = bandSet.toArray(new String[bandSet.size()]);
        Arrays.sort(bands);
        List<StateOption> options = new ArrayList<>();
        for (String band : bands) {
            options.add(new StateOption(band, band));
        }
        /* Update the tuner band channel options with broadcast band descriptions */
        stateDescriptionProvider.setStateOptions(new ChannelUID(this.getThing().getUID(), CHANNEL_TUNER_BAND), options);
    }

    /**
     * Method to populate/update tuner preset names with default values (P01-P40) or user defined descriptions
     * from a file specified in the NAD AV Receiver thing configuration
     */
    private void populateTunerPresets() {
        List<StateOption> options = new ArrayList<>();
        if (config.arePresetNamesEnabled()) {
            /* Build list of preset names to be used by Tuner Preset channel */
            for (int i = 1; i <= NadTunerPresetNameList.size(); i++) {
                /* Build key used for preset name to be located in the preset names file */
                String keyVal = String.valueOf(i);
                if (i <= 9) {
                    keyVal = "0" + keyVal;
                }
                StringType key = new StringType(String.valueOf(keyVal));
                /* Retrieve the default name for the preset preset names list array (index starts at zero) */
                String name = NadTunerPresetNameList.getTunerPreseteName(i - 1);
                /* Get the preset name from the user provided file. If value returned is "Not Set" use default. */
                StringType presetName = nadavrState.getPresetDetail(key, config.getPresetNamesFilePath());
                if (!StringType.valueOf(UnDefType.UNDEF.toString()).equals(presetName)) {
                    name = presetName.toString();
                }
                /* Build options for the Tuner Preset channel */
                options.add(new StateOption(String.valueOf(i), name));
            }
        } else {
            /* Return presets to default if there is no user provided preset names file */
            for (int i = 1; i <= NadTunerPresetNameList.size(); i++) {
                /* Retrieve the default name for the preset preset names list array (index starts at zero) */
                String name = NadTunerPresetNameList.getTunerPreseteName(i - 1);
                /* Build options for the Tuner Preset channel */
                options.add(new StateOption(String.valueOf(i), name));
            }
        }
        /* Update the tuner preset channel options with preset descriptions */
        stateDescriptionProvider.setStateOptions(new ChannelUID(this.getThing().getUID(), CHANNEL_TUNER_PRESET),
                options);
    }

    /**
     * Method to refresh tuner channel details when the broadcast band changes
     */
    protected void refreshTunerDetails() {
        // Initialize message with empty command
        NadMessage queryCmd = new NadMessage.MessageBuilder().prefix(NadCommand.EMPTY_COMMAND.getPrefix().toString())
                .variable(NadCommand.EMPTY_COMMAND.getVariable().toString())
                .operator(NadCommand.EMPTY_COMMAND.getOperator().toString())
                .value(NadCommand.EMPTY_COMMAND.getValue().toString()).build();

        // When adding new tuner commands be sure to include in array to refresh states...
        List<NadCommand> nadTunerRefreshCmds = new ArrayList<>(Arrays.asList(NadCommand.TUNER_BAND_QUERY,
                NadCommand.TUNER_AM_FREQUENCY_QUERY, NadCommand.TUNER_FM_FREQUENCY_QUERY,
                NadCommand.TUNER_FM_MUTE_QUERY, NadCommand.TUNER_FM_RDS_TEXT_QUERY, NadCommand.TUNER_PRESET_QUERY,
                NadCommand.TUNER_XM_CHANNEL_QUERY));

        // Refresh tuner state information
        for (NadCommand nadTunerCmd : nadTunerRefreshCmds) {
            if (nadTunerCmd.getOperator() == NAD_QUERY) {
                queryCmd = new NadMessage.MessageBuilder().prefix(Prefix.Tuner.toString())
                        .variable(nadTunerCmd.getVariable().toString()).operator(nadTunerCmd.getOperator().toString())
                        .value(nadTunerCmd.getValue().toString()).build();
                try {
                    connector.sendCommand(queryCmd);
                } catch (NadException e) {
                    logger.error(
                            "Error requesting tuner state information from the NAD device @{}, check for connection issues.  Error: {}",
                            connector.getConnectionName(), e.getLocalizedMessage());
                }
            }
        }
    }
}
