/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.binding.tailwind.internal.handler;

import static org.openhab.binding.tailwind.internal.TailwindBindingConstants.*;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.json.JSONObject;
import org.openhab.binding.tailwind.internal.TailwindConfiguration;
import org.openhab.binding.tailwind.internal.Utils.Utilites;
//import org.openhab.binding.tailwind.internal.connector.JSONPost;
import org.openhab.binding.tailwind.internal.connector.TailwindCommunicationException;
import org.openhab.binding.tailwind.internal.connector.TailwindConnectApi;
import org.openhab.binding.tailwind.internal.connector.TailwindUdpConnector;
import org.openhab.binding.tailwind.internal.connector.TailwindUdpEventListener;
import org.openhab.binding.tailwind.internal.dto.TailwindControllerData;
import org.openhab.binding.tailwind.internal.state.TailwindState;
import org.openhab.binding.tailwind.internal.state.TailwindStateChangedListener;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.type.ChannelKind;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * The {@link TailwindHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Dave J. Schoepel - Initial contribution
 */
@NonNullByDefault
public class TailwindHandler extends BaseThingHandler
        implements TailwindStateChangedListener, TailwindUdpEventListener {

    private final Logger logger = LoggerFactory.getLogger(TailwindHandler.class);

    private TailwindConfiguration config = new TailwindConfiguration();
    // private TailwindHttpResponse response = new TailwindHttpResponse();
    // private HttpClient httpClient = new HttpClient();
    // private HttpClient httpClient;
    private TailwindControllerData response = new TailwindControllerData();

    private TailwindState tailwindState = new TailwindState(this);

    private TailwindConnectApi tailwindApi;
    // private TailwindState state;
    private @Nullable TailwindUdpConnector udpConnector;
    private Utilites utilities = new Utilites();

    private int updateStateFailures = 0;
    private Gson gson = new Gson();

    /**
     * Constructor for TailWind Device Handler
     *
     * @param thing - TailWind Controller
     * @param httpClient
     */
    public TailwindHandler(Thing thing, HttpClient httpClient) {
        super(thing);
        // this.httpClient = httpClient;
        this.tailwindApi = new TailwindConnectApi(httpClient);
        // if (httpClient.isRunning()) {
        // logger.debug("Http client is running");
        // }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (CHANNEL_DOOR_1_CONTROLS_INDEX.equals(channelUID.getId())) {
            if (command instanceof RefreshType) {
                // TODO: handle data refresh
            }

            // TODO: handle command

            // Note: if communication with thing fails for some reason,
            // indicate that by setting the status with detail information:
            // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
            // "Could not control device at IP address x.x.x.x");
        }
    }

    /**
     * Method to initialize a TailWind thing.
     * <ul>
     * <li>Validate configuration settings</li>
     * <li>Configure number of doors for the TailWind garage controller thing from 1 to up to max allowed for model</li>
     * <li>Connect to device and start thread to check for failed connections</li>
     * <li>Initialize channel states with settings retrieved from the controller</li>
     * <li>Start threads to monitor for door notifications and status changes</li>
     * </ul>
     */
    @Override
    public void initialize() {
        // TODO: Initialize the handler.
        // The framework requires you to return from this method quickly, i.e. any network access must be done in
        // the background initialization below.
        // Also, before leaving this method a thing status from one of ONLINE, OFFLINE or UNKNOWN must be set. This
        // might already be the real thing status in case you can decide it directly.
        // In case you can not decide the thing status directly (e.g. for long running connection handshake using WAN
        // access or similar) you should set status UNKNOWN here and then decide the real status asynchronously in the
        // background.

        // set the thing status to UNKNOWN temporarily and let the background task decide for the real status.
        // the framework is then able to reuse the resources from the thing handler initialization.
        // we set this upfront to reliably check status updates in unit tests.

        if (logger.isDebugEnabled()) {
            logger.debug("Start initializing handler for thing {}", getThing().getUID());
        }

        // Get configuration settings
        config = getConfigAs(TailwindConfiguration.class);
        // Validate configuration settings
        try {
            if (!checkConfiguration(config)) {
                return;
            } else {
                logger.info("tailwind:TailwindHandler using configuration: {}", config.toString());
            }
        } catch (TailwindCommunicationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        /* Set up number of garage doors specified for this thing in the configuration */
        configureZoneChannels(config);

        /**
         * TODO
         * update the controller and configured door states from the response
         * from sending this command to the controller.
         */
        scheduler.execute(this::initializeConnection);
        // logger.debug("Tailwind last response was: {}", response.toString());
        /* TODO: Scheduled job to listen for UDP messages from the TailWind controller */
        // TailwindUdpServer receiver = new TailwindUdpServer(Integer.parseInt(TAILWIND_OPENHAB_HOST_UDP_PORT));
        // receiver.start();
        //
        // logger.debug("UdpServer State: {}", receiver.getState().toString());

        updateStatus(ThingStatus.UNKNOWN);

        // Example for background initialization:
        scheduler.execute(() -> {
            boolean thingReachable = true; // <background task with long running initialization here>
            // when done do:
            if (thingReachable) {
                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE);
            }
        });

        // These logging types should be primarily used by bindings
        // logger.trace("Example trace message");
        // logger.debug("Example debug message");
        // logger.warn("Example warn message");
        //
        // Logging to INFO should be avoided normally.
        // See https://www.openhab.org/docs/developer/guidelines.html#f-logging

        // Note: When initialization can NOT be done set the status with more details for further
        // analysis. See also class ThingStatusDetail for all available status details.
        // Add a description to give user information to understand why thing does not work as expected. E.g.
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
        // "Can not access device as username and/or password are invalid");
    } // End initialize()

    private void initializeConnection() {
        try {
            final TailwindUdpConnector newUdpConnector = new TailwindUdpConnector(
                    Integer.parseInt(TAILWIND_OPENHAB_HOST_UDP_PORT), scheduler);
            udpConnector = newUdpConnector;

            // establish connection and register listener
            newUdpConnector.connect(this::eventReceived, true);

        } catch (InterruptedException e) {
            // OH shutdown - don't log anything, Framework will call dispose()
        } catch (Exception e) {
            logger.debug("Connection to '{}' failed", config, e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.HANDLER_INITIALIZING_ERROR, "Connection to '" + config
                    + "' failed unexpectedly with " + e.getClass().getSimpleName() + ": " + e.getMessage());
            dispose();
        }
    } // End intializeConnection()

    /**
     * Method to validate configuration settings before bringing TailWind Thing online
     *
     * @param config - TailWindthing configuration settings
     * @return true indicating all checks passed or false when there are errors to be addressed
     * @throws Exception
     */
    public boolean checkConfiguration(TailwindConfiguration config) throws Exception {
        // Check that door count is within the supported range 1 - max doors for this model
        int maxDoors = utilities.GetMaxDoors(thing.getThingTypeUID().getId());
        if (config.getDoorCount() < 1 || config.getDoorCount() > maxDoors) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "This binding supports 1 to " + maxDoors + " garage doors. Please update the door count.");
            return false;
        } // If the configured number of doors is out of range

        /**
         * Check for a valid Authorization token
         */
        if (logger.isDebugEnabled()) {
            logger.debug("Authorization Token is {}", config.authToken);
        }
        // Token must be 6 characters long, TailWind HTTP server responds with {"result":"OK"}
        if (config.getAuthToken().length() == 6) {
            // Check for a valid authorization token. HTTP response code should be 200 and response contains
            // {"result": "OK"} vs. {"result":"token fail"}
            JSONObject tailwindCommandString = new JSONObject(TAILWIND_CMD_DEVICE_STATUS);
            String body = tailwindCommandString.toString();
            response = tailwindApi.getTailwindControllerData(thing, config.authToken, body);

            /* TODO: Use response from OK device status set initial states for this thing */
            if (!response.getResult().contentEquals(JSON_RESPONSE_RESULT_OK)) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        "The authorization token was invalid. Please check that the Token was entered correctly or obtain another one from the mobile app.");
                return false;
            } else {
                // Update thing properties to include number of doors connected
                thing.setProperty(PARAMETER_DOOR_NUM_CONNECTED, String.valueOf(response.getDoorNum()));
            } // If result was OK

            if (logger.isDebugEnabled()) {
                logger.debug("Response to validate token {} is: {}", config.authToken, response);
            }
            // Ensure configured doors does not exceed the number of doors connected
            int connectedDoors = (int) response.getDoorNum();
            if (config.getDoorCount() > connectedDoors) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        "This garage controller has " + connectedDoors
                                + " doors connected. The number of controlled doors can be 1-" + connectedDoors
                                + "!  Please update the door count.");
                return false;
            } // If configured doors > connected doors

        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "The authorization token should be 6 charactes long, its " + config.getAuthToken().length()
                            + " characters long. Please update the authorization code.");
            return false;
        } // If the authorization token length is equal to 6

        /**
         * Update controller with the UDP host address to receive notifications of changes to the controller
         */
        String serverUDPReportingUrl = buildOpenHabUdpUrl();
        if (logger.isDebugEnabled()) {
            logger.debug("UDP server URL is: {}", serverUDPReportingUrl);
        } // If logger enabled
          // Update the command string map with the actual URL to use....
        JSONObject tailwindCommandString = new JSONObject(TAILWIND_CMD_SET_STATUS_REPORT_URL);
        String urlKeyFound = tailwindCommandString.getJSONObject(TAILWIND_JSON_KEY_DATA)
                .getJSONObject(TAILWIND_JSON_KEY_VALUE).getString(TAILWIND_JSON_KEY_URL);
        if (urlKeyFound != null) {
            tailwindCommandString.getJSONObject(TAILWIND_JSON_KEY_DATA).getJSONObject(TAILWIND_JSON_KEY_VALUE)
                    .put(TAILWIND_JSON_KEY_URL, serverUDPReportingUrl);
            String body = tailwindCommandString.toString();
            response = tailwindApi.getTailwindControllerData(thing, config.authToken,
                    body); /* TODO: Use response from OK device status set initial states for this thing */
            if (!response.getResult().contentEquals(JSON_RESPONSE_RESULT_OK)) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        "The authorization token was invalid. Please check that the Token was entered correctly or obtain another one from the mobile app.");
                return false;

            } // If result was OK
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Key for UDP server URL is missing from the body string used to set the URL: {}",
                        TAILWIND_CMD_SET_STATUS_REPORT_URL);
            } // If logger enabled
        } // If urlKeyFound is not null

        tailwindCommandString = new JSONObject(TAILWIND_CMD_DEVICE_STATUS);
        String body = tailwindCommandString.toString();
        response = tailwindApi.getTailwindControllerData(thing, config.authToken, body);
        // TailwindControllerData tailwindControllerData = gson.fromJson(response, TailwindControllerData.class);

        updateTailwindDetails(response);
        return true;
    }

    /**
     * Method to shutdown and remove TailWind thing.
     * <ul>
     * <li>Remove handler</li>
     * </ul>
     */
    @Override
    public void dispose() {
        if (logger.isDebugEnabled()) {
            logger.debug("Disposing handler for thing {}", getThing().getUID());
        }
        final TailwindUdpConnector connector = udpConnector;
        if (connector != null) {
            udpConnector = null;
            try {
                connector.disconnect();
            } catch (Exception e) {
                logger.debug("Failed to close socket connection for: {}", config, e);
            }
        }
        // updateStatus(ThingStatus.REMOVED);
        super.dispose();
    }

    private void configureZoneChannels(TailwindConfiguration config) {
        logger.debug("Configuring garage door groups/channels");
        Integer doorCount = config.getDoorCount();
        // current door channels
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
        // Process of adding or removing garage doors based on number set in the thing configuration
        if (doorCount > 1) {
            // add channels for Door 2
            List<Entry<String, ChannelTypeUID>> channelsToAdd = new ArrayList<>(DOOR_2_CHANNEL_TYPES.entrySet());
            if (doorCount > 2) {
                // add channels for door 3
                channelsToAdd.addAll(DOOR_3_CHANNEL_TYPES.entrySet());
            } else {
                channelsToRemove.addAll(DOOR_3_CHANNEL_TYPES.entrySet());
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
                        if (logger.isDebugEnabled()) {
                            logger.debug("Door channel has been added: {} and label {}", entry.getKey(), itemLabel);
                        }
                    }
                }
                channelsUpdated = true;
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("No door channels have been added");
                }
            }
        } else {
            channelsToRemove.addAll(DOOR_2_CHANNEL_TYPES.entrySet());
            channelsToRemove.addAll(DOOR_3_CHANNEL_TYPES.entrySet());
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
                logger.debug("No door channels have been removed");
            }
        }
        // update Thing if channels changed
        if (channelsUpdated) {
            updateThing(editThing().withChannels(channels).build());
        }
    }

    /**
     * Method to set the URL used by the TailWind controller to send notifications
     *
     * @return urlString - OpenHab host IP:Port to use for set status report URL command
     * @throws IOException
     */
    private String buildOpenHabUdpUrl() throws IOException {
        InetAddress address1 = InetAddress.getLocalHost();
        String hostAddress = address1.getHostAddress();
        String urlString = hostAddress + ":" + TAILWIND_OPENHAB_HOST_UDP_PORT;
        return urlString;
    }

    @Override
    public void eventReceived(String msg) {
        if (logger.isDebugEnabled()) {
            logger.debug("Received status update from TailWind Device @{}: data={}", getOpenHabHost(), msg);
        }
        // Connection is live, set status to ONLINE if it is not already set
        if (!ThingStatus.ONLINE.equals(thing.getStatus())) {
            updateStatus(ThingStatus.ONLINE);
        }
        try {
            synchronized (this) { // to make sure state is fully processed before replacing it
                TailwindControllerData tailwindControllerData = gson.fromJson(msg, TailwindControllerData.class);
                if (tailwindControllerData != null) {
                    logger.debug("Received status update from TailWind Device @{}: data={}", getOpenHabHost(),
                            tailwindControllerData);
                    updateTailwindDetails(tailwindControllerData);
                }

                // if(newStatus != null && recentState != null && newStatus.equals(recentState.))
            }
        } catch (Exception e) {
            if (getThing().getStatus() == ThingStatus.ONLINE) {
                if (updateStateFailures++ == ATTEMPTS_WITH_COMMUNICATION_ERRORS) {
                    final String errorMsg = "Setting thing offline because status updated failed " + updateStateFailures
                            + " times in a row for: " + config;
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, errorMsg);
                } else if (updateStateFailures < ATTEMPTS_WITH_COMMUNICATION_ERRORS) {
                    logger.warn("Status update failed for: {}", config, e);
                }
            } // else: ignore exception for offline things
        }
    }

    private void updateTailwindDetails(TailwindControllerData tailwindControllerData) {

        // for (TailwindControllerData channelState : tailwindControllerData) {
        for (Channel channel : getThing().getChannels()) {
            ChannelUID channelUID = channel.getUID();
            if (ChannelKind.STATE.equals(channel.getKind()) && channelUID.isInGroup() && channelUID.getGroupId() != null
                    && isLinked(channelUID)) {
                updateTailwindChannel(channelUID, tailwindControllerData);
            } // end if channel is linked, update its state
        }
        // }
    }

    private void updateTailwindChannel(ChannelUID channelUID, TailwindControllerData channelState) {
        logger.debug("Update requested for channel: {}, channelState: {}", channelUID, channelState);
        String channelGroupId = channelUID.getGroupId();
        if (channelGroupId == null) {
            logger.error("Channel group Id is null, unable to update TailWind controller channel");
        } else {
            switch (channelGroupId) {
                case CHANNEL_GROUP_CONTROLLER:
                    updateTailwindControllerStates(channelUID, channelState);
                    break;
                case CHANNEL_GROUP_DOOR_ONE:
                    updateTailwindDoorOneStates(channelUID, channelState);
                    break;
                case CHANNEL_GROUP_DOOR_TWO:
                    updateTailwindDoorTwoStates(channelUID, channelState);
                    break;
            }
        }
    } // Update TailWind Channel

    private void updateTailwindControllerStates(ChannelUID channelUID, TailwindControllerData channelState) {
        String channelId = channelUID.getIdWithoutGroup();
        String channelGroup = channelUID.getGroupId();
        String switchCase = channelGroup + "#" + channelId;
        switch (switchCase) {
            case CHANNEL_DOOR_NUM:
                tailwindState.setDoorNum(channelState.getDoorNum());
                break;
            case CHANNEL_NIGHT_MODE_ENABLED:
                tailwindState.setNightModeEnabled(channelState.getNightModeEn());
                break;
            case CHANNEL_LED_BRIGHTNESS:
                tailwindState.setLedBrighness(channelState.getLEDBrightness());
                break;
            case CHANNEL_ROUTER_RSSI:
                tailwindState.setRouterRSSI(channelState.getRouterRssi());
                break;
            case CHANNEL_PRODUCT_ID:
                tailwindState.setProductID(channelState.getProduct());
                break;
            case CHANNEL_DEVICE_ID:
                tailwindState.setDeviceID(channelState.getDevID());
                break;
            case CHANNEL_FIRMWARE_VERSION:
                tailwindState.setFirmwareVersion(channelState.getFwVer());
                break;
            default:
                break;
        } // Switch/case on controller channels
    }

    public void updateTailwindDoorOneStates(ChannelUID channelUID, TailwindControllerData channelState) {
        String channelId = channelUID.getIdWithoutGroup();
        String channelGroup = channelUID.getGroupId();
        String switchCase = channelGroup + "#" + channelId;
        int doorIndex = (int) channelState.getDoorData().getDoor1().getIndex();
        switch (switchCase) {
            case CHANNEL_DOOR_1_CONTROLS_INDEX:
                tailwindState.setDoorIndex(doorIndex, channelState.getDoorData().getDoor1().getIndex());
                break;
            case CHANNEL_DOOR_1_CONTROLS_STATUS:
                tailwindState.setDoorStatus(doorIndex, channelState.getDoorData().getDoor1().getStatus());
                break;
            case CHANNEL_DOOR_1_CONTROLS_LOCKUP:
                tailwindState.setLockup(doorIndex, channelState.getDoorData().getDoor1().getLockup());
                break;
            case CHANNEL_DOOR_1_CONTROLS_DISABLED:
                tailwindState.setDisabled(doorIndex, channelState.getDoorData().getDoor1().getDisabled());
                break;
            default:
                break;
        }
    }

    public void updateTailwindDoorTwoStates(ChannelUID channelUID, TailwindControllerData channelState) {
        String channelId = channelUID.getIdWithoutGroup();
        String channelGroup = channelUID.getGroupId();
        String switchCase = channelGroup + "#" + channelId;
        int doorIndex = (int) channelState.getDoorData().getDoor2().getIndex();
        switch (switchCase) {
            case CHANNEL_DOOR_2_CONTROLS_INDEX:
                tailwindState.setDoorIndex(doorIndex, channelState.getDoorData().getDoor2().getIndex());
                break;
            case CHANNEL_DOOR_2_CONTROLS_STATUS:
                tailwindState.setDoorStatus(doorIndex, channelState.getDoorData().getDoor2().getStatus());
                break;
            case CHANNEL_DOOR_2_CONTROLS_LOCKUP:
                tailwindState.setLockup(doorIndex, channelState.getDoorData().getDoor2().getLockup());
                break;
            case CHANNEL_DOOR_2_CONTROLS_DISABLED:
                tailwindState.setDisabled(doorIndex, channelState.getDoorData().getDoor2().getDisabled());
                break;
            default:
                break;
        }
    }

    @Override
    public void connectionError(@Nullable String errorMsg) {
        // TODO Auto-generated method stub

    }

    private String getOpenHabHost() {
        Map<String, String> thingProperties = thing.getProperties();
        String server = thingProperties.get(TAILWIND_HTTP_SERVER_URL);
        if (server != null) {
            return server;
        } else {
            return "Tailwind Device";
        }
    }

    @Override
    public void stateChanged(String channelID, State state) {
        /* Only update channels if they are linked to an item */

        ChannelUID channelUID = new ChannelUID(this.getThing().getUID(), channelID);
        updateState(channelID, state);
        // if (isLinked(channelID)) {
        // updateState(channelID, state);
        // } else {
        // logger.debug("Tried to update State but channel {} is not linked!", channelID);
        // }
    }
}
