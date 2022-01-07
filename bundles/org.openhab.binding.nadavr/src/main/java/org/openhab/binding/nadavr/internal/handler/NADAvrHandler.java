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
package org.openhab.binding.nadavr.internal.handler;

import static org.openhab.binding.nadavr.internal.NADAvrBindingConstants.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.nadavr.internal.NADAvrConfiguration;
import org.openhab.binding.nadavr.internal.NADAvrInputSourceList;
import org.openhab.binding.nadavr.internal.NADAvrPopulateInputs;
import org.openhab.binding.nadavr.internal.NADAvrState;
import org.openhab.binding.nadavr.internal.NADAvrStateChangedListener;
import org.openhab.binding.nadavr.internal.NADAvrStateDescriptionProvider;
import org.openhab.binding.nadavr.internal.NADModel;
import org.openhab.binding.nadavr.internal.UnsupportedCommandTypeException;
import org.openhab.binding.nadavr.internal.connector.NADAvrConnector;
import org.openhab.binding.nadavr.internal.factory.NADAvrConnectorFactory;
import org.openhab.binding.nadavr.internal.nadcp.NADCommand;
import org.openhab.binding.nadavr.internal.nadcp.NADCommand.Prefix;
import org.openhab.binding.nadavr.internal.xml.TunerPresets;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link NADAvrHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Dave J Schoepel - Initial contribution
 */
@NonNullByDefault
public class NADAvrHandler extends BaseThingHandler implements NADAvrStateChangedListener {

    private final Logger logger = LoggerFactory.getLogger(NADAvrHandler.class);
    private static final int RETRY_TIME_SECONDS = 30;

    private NADAvrConfiguration config = new NADAvrConfiguration() {
    };
    private NADAvrConnector connector;
    private NADAvrStateDescriptionProvider stateDescriptionProvider;

    private TunerPresets tunerPresets = new TunerPresets();

    private NADAvrState nadavrState = new NADAvrState(this);

    private NADAvrConnectorFactory connectorFactory = new NADAvrConnectorFactory();
    private @Nullable ScheduledFuture<?> retryJob;

    public NADAvrHandler(Thing thing, NADAvrStateDescriptionProvider stateDescriptionProvider,
            NADAvrConnector connector) {
        super(thing);
        this.stateDescriptionProvider = stateDescriptionProvider;
        this.connector = connector;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // if (connector == null) {
        // return;
        // }
        logger.debug("handleCommand testing command = {} and not string {}", command.toString(), command);
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
                    throw new UnsupportedCommandTypeException();
            }
        } catch (UnsupportedCommandTypeException e) {
            logger.debug("Unsupported Command {} for channel {}", command, channelUID.getId());
        }
    }

    /**
     * Validate configuration settings before bringing NAD Avr Thing online
     *
     * @return true indicating all checks passed or false there are errors to be addressed
     */
    public boolean checkConfiguration() {
        // Check that zone count is within the supported range 1 - max zones for this model
        int maxZones = getMaxZonesForModel(thing.getThingTypeUID().getId());
        if (config.getZoneCount() < 1 || config.getZoneCount() > maxZones) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "This binding supports 1 to " + maxZones + " zones. Please update the zone count.");
            return false;
        }

        // Check for tuner preset details file being enabled
        if (config.arePresetNamesEnabled()) {
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

    @Override
    public void initialize() {
        logger.debug("NADAvrHandler - initialize() started....");
        cancelRetry();
        config = getConfigAs(NADAvrConfiguration.class);

        if (!checkConfiguration()) {
            return;
        }

        // nadavrState = new NADAvrState(this);

        NADCommand.initializeCommandList();

        configureZoneChannels();
        // ThingStatus ONLINE/OFFLINE is set when AVR status is known.
        updateStatus(ThingStatus.UNKNOWN);

        // create Telnet connection
        createConnection();
        // Wait for connection to be established before requesting source names
        try {
            Thread.sleep(3000); // 3 seconds
        } catch (InterruptedException e) {
            logger.debug("Error in sleep to let telnet connection start", e);
        }
        // Request Source names and populate source input channel with names
        querySourceNames();
        // populateInputs();
        NADAvrPopulateInputs populateInput = new NADAvrPopulateInputs(thing.getUID(), config, connector,
                stateDescriptionProvider, true);
        if (!populateInput.isRunning()) {
            populateInput.start();
        }

        boolean thingReachable = true;
        if (thingReachable) {
            updateStatus(ThingStatus.ONLINE);
        } else {
            updateStatus(ThingStatus.OFFLINE);
        }
    }

    private void createConnection() {
        connector.dispose();
        // connector = connectorFactory.getConnector(config, nadavrState, stateDescriptionProvider, scheduler,
        // this.getThing().getUID());
        connector = connectorFactory.getConnector(config, nadavrState, stateDescriptionProvider,
                this.getThing().getUID());
        connector.connect();
        logger.debug("NADAvrHandler - createConnection() connector.connect executed ");
    }

    private void cancelRetry() {
        ScheduledFuture<?> localRetryJob = retryJob;
        if (localRetryJob != null && !localRetryJob.isDone()) {
            localRetryJob.cancel(false);
        }
    }

    private void configureZoneChannels() {
        logger.debug("Configuring zone channels");
        Integer zoneCount = config.getZoneCount();

        ArrayList<Channel> channels = new ArrayList<>(this.getThing().getChannels());

        boolean channelsUpdated = false;

        // construct a set with the existing channel type UIDs, to quickly check
        Set<String> currentChannels = new HashSet<>();
        channels.forEach(channel -> currentChannels.add(channel.getUID().getId()));

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
                    Channel channel = ChannelBuilder
                            .create(new ChannelUID(this.getThing().getUID(), entry.getKey()), itemType)
                            .withType(entry.getValue()).build();
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

    private void querySourceNames() {
        logger.debug("NADAvrHandler - querySourceNames() started....");

        for (int input = 1; input <= 10; input++) {
            String prefix = "Source" + input;
            connector.sendCommand(prefix, NADCommand.SOURCE_NAME_QUERY);
        }
    }

    private void populateInputs() {
        logger.debug("NADAvrHandler - populateInputs() started....");
        logger.debug("----> sourceNameList length = {} and contents are {}", NADAvrInputSourceList.size(),
                NADAvrInputSourceList.getSourceNameList());
        List<StateOption> options = new ArrayList<>();
        List<StateOption> optionsZ2to4 = new ArrayList<>();

        for (int i = 1; i <= NADAvrInputSourceList.size(); i++) {
            String name = NADAvrInputSourceList.getSourceName(i - 1);
            options.add(new StateOption(String.valueOf(i), name));
            optionsZ2to4.add(new StateOption(String.valueOf(i), name));
        }
        logger.debug("Value of i = {}", optionsZ2to4.size());
        optionsZ2to4.add(new StateOption(String.valueOf(options.size() + 1), LOCAL));
        logger.debug("Got Source Name input List from NAD Device {}", options);

        for (int i = 1; i <= config.getZoneCount(); i++) {
            switch (i) {
                case 1:
                    stateDescriptionProvider.setStateOptions(new ChannelUID(getThing().getUID(), CHANNEL_MAIN_SOURCE),
                            options);
                    connector.sendCommand(Prefix.Main.toString(), NADCommand.INPUT_SOURCE_QUERY);
                    break;
                case 2:
                    stateDescriptionProvider.setStateOptions(new ChannelUID(getThing().getUID(), CHANNEL_ZONE2_SOURCE),
                            optionsZ2to4);
                    connector.sendCommand(Prefix.Zone2.toString(), NADCommand.INPUT_SOURCE_QUERY);
                    break;
                case 3:
                    stateDescriptionProvider.setStateOptions(new ChannelUID(getThing().getUID(), CHANNEL_ZONE3_SOURCE),
                            optionsZ2to4);
                    connector.sendCommand(Prefix.Zone3.toString(), NADCommand.INPUT_SOURCE_QUERY);
                    break;
                case 4:
                    stateDescriptionProvider.setStateOptions(new ChannelUID(getThing().getUID(), CHANNEL_ZONE4_SOURCE),
                            optionsZ2to4);
                    connector.sendCommand(Prefix.Zone4.toString(), NADCommand.INPUT_SOURCE_QUERY);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void channelLinked(ChannelUID channelUID) {
        super.channelLinked(channelUID);
        String channelID = channelUID.getId();
        if (isLinked(channelID)) {
            State state = nadavrState.getStateForChannelID(channelID);
            updateState(channelID, state);
        }
    }

    @Override
    public void dispose() {
        connector.dispose();
        // connector = null;
        cancelRetry();
        super.dispose();
    }

    @Override
    public void stateChanged(String channelID, State state) {
        logger.debug("Received state {} for channelID {}", state, channelID);

        // Don't flood the log with thing 'updated: ONLINE' each time a single channel changed
        if (this.getThing().getStatus() != ThingStatus.ONLINE) {
            updateStatus(ThingStatus.ONLINE);
        }
        updateState(channelID, state);
    }

    @Override
    public void connectionError(String errorMessage) {
        if (this.getThing().getStatus() != ThingStatus.OFFLINE) {
            // Don't flood the log with thing 'updated: OFFLINE' when already offline
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, errorMessage);
        }
        connector.dispose();
        retryJob = scheduler.schedule(this::createConnection, RETRY_TIME_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * @param model
     * @return
     */
    public int getMaxZonesForModel(String model) {
        int maxZones = 2;
        for (NADModel supportedModel : NADModel.values()) {
            if (supportedModel.getId().equals(model)) {
                maxZones = supportedModel.getMaxZones();
            }
        }
        return maxZones;
    }
}
