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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.openhab.binding.nadavr.internal.NADAvrConfiguration;
import org.openhab.binding.nadavr.internal.NADAvrState;
import org.openhab.binding.nadavr.internal.NADAvrStateChangedListener;
import org.openhab.binding.nadavr.internal.NADAvrStateDescriptionProvider;
import org.openhab.binding.nadavr.internal.NADModel;
import org.openhab.binding.nadavr.internal.SourceName;
import org.openhab.binding.nadavr.internal.UnsupportedCommandTypeException;
import org.openhab.binding.nadavr.internal.connector.NADAvrConnector;
import org.openhab.binding.nadavr.internal.factory.NADAvrConnectorFactory;
import org.openhab.binding.nadavr.internal.nadcp.NADCommand;
import org.openhab.binding.nadavr.internal.nadcp.NADCommand.Prefix;
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
// @NonNullByDefault
public class NADAvrHandler extends BaseThingHandler implements NADAvrStateChangedListener {

    private final Logger logger = LoggerFactory.getLogger(NADAvrHandler.class);
    private static final int RETRY_TIME_SECONDS = 30;

    private NADAvrConfiguration config;
    private NADAvrConnector connector;

    private SourceName avrSourceName = new SourceName();

    private NADAvrState nadavrState;
    private final NADAvrStateDescriptionProvider stateDescriptionProvider;
    private NADAvrConnectorFactory connectorFactory = new NADAvrConnectorFactory();
    private ScheduledFuture<?> retryJob;
    // private CommandStates avrCommandStates = new CommandStates();

    public NADAvrHandler(Thing thing, NADAvrStateDescriptionProvider stateDescriptionProvider) {
        super(thing);
        this.stateDescriptionProvider = stateDescriptionProvider;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (connector == null) {
            return;
        }
        logger.debug("handleCommand testing command = {}", command.toString());
        try {
            switch (channelUID.getId()) {
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

    public boolean checkConfiguration() {
        // Check that zone count is within the supported range 1 - max zones for this model
        int maxZones = getMaxZonesForModel(thing.getThingTypeUID().getId());
        if (config.getZoneCount() < 1 || config.getZoneCount() > maxZones) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "This binding supports 1 to " + maxZones + " zones. Please update the zone count.");
            return false;
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

        nadavrState = new NADAvrState(this);

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
        populateInputs();

        boolean thingReachable = true;
        if (thingReachable) {
            updateStatus(ThingStatus.ONLINE);
        } else {
            updateStatus(ThingStatus.OFFLINE);
        }
    }

    private void createConnection() {
        if (connector != null) {
            connector.dispose();
        }
        connector = connectorFactory.getConnector(config, nadavrState, stateDescriptionProvider, scheduler,
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

    // TODO this is where we need to set the source input names determined by processInfo method
    private void populateInputs() {
        logger.debug("NADAvrHandler - populateInputs() started....");
        List<StateOption> options = new ArrayList<>();

        for (int i = 1; i <= avrSourceName.size(); i++) {
            String key = String.valueOf(i);
            String name = avrSourceName.getAvrSourceName(key);
            // options.add(new StateOption(String.valueOf(i), avrSourceName.getAvrSourceName(String.valueOf(i))));
            options.add(new StateOption(String.valueOf(i), name));
        }
        logger.debug("Got Source Name input List from NAD Device {}", options);
        // TODO only update zones that are active?..

        for (int i = 1; i <= config.getZoneCount(); i++) {
            switch (i) {
                case 1:
                    stateDescriptionProvider.setStateOptions(new ChannelUID(getThing().getUID(), CHANNEL_MAIN_SOURCE),
                            options);
                    connector.sendCommand(Prefix.Main.toString(), NADCommand.INPUT_SOURCE_QUERY);
                    break;
                case 2:
                    stateDescriptionProvider.setStateOptions(new ChannelUID(getThing().getUID(), CHANNEL_ZONE2_SOURCE),
                            options);
                    connector.sendCommand(Prefix.Zone2.toString(), NADCommand.INPUT_SOURCE_QUERY);
                    break;
                case 3:
                    stateDescriptionProvider.setStateOptions(new ChannelUID(getThing().getUID(), CHANNEL_ZONE3_SOURCE),
                            options);
                    connector.sendCommand(Prefix.Zone3.toString(), NADCommand.INPUT_SOURCE_QUERY);
                    break;
                case 4:
                    stateDescriptionProvider.setStateOptions(new ChannelUID(getThing().getUID(), CHANNEL_ZONE4_SOURCE),
                            options);
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
        connector = null;
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
