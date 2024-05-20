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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.json.JSONObject;
import org.openhab.binding.tailwind.internal.TailwindConfiguration;
import org.openhab.binding.tailwind.internal.TailwindUnsupportedCommandTypeException;
import org.openhab.binding.tailwind.internal.connector.TailwindCommunicationException;
import org.openhab.binding.tailwind.internal.connector.TailwindConnectApi;
import org.openhab.binding.tailwind.internal.connector.TailwindUdpConnector;
import org.openhab.binding.tailwind.internal.connector.TailwindUdpEventListener;
import org.openhab.binding.tailwind.internal.dto.TailwindControllerData;
import org.openhab.binding.tailwind.internal.state.TailwindState;
import org.openhab.binding.tailwind.internal.state.TailwindStateChangedListener;
import org.openhab.binding.tailwind.internal.utils.Utilities;
import org.openhab.core.config.core.Configuration;
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
    private TailwindControllerData response = new TailwindControllerData();
    private TailwindState tailwindState = new TailwindState(this);
    private TailwindConnectApi tailwindApi;
    private @Nullable TailwindUdpConnector udpConnector;
    private Utilities utilities = new Utilities();
    private int updateStateFailures = 0;
    private Gson gson = new Gson();
    private @Nullable ScheduledFuture<?> requestControllerStatusJob;

    /**
     * Constructor for TailWind Device Handler
     *
     * @param thing - TailWind Controller
     * @param httpClient
     */
    public TailwindHandler(Thing thing, HttpClient httpClient) {
        super(thing);
        this.tailwindApi = new TailwindConnectApi(httpClient);
    }

    @Override
    public void channelLinked(ChannelUID channelUID) {
        String channel = channelUID.getId();
        if (logger.isDebugEnabled()) {
            logger.debug("Channel Id: {}, linked: {}, state: {}", channel, isLinked(channel),
                    tailwindState.getStateForChannelID(channel));
        }
        updateTailwindDetails(sendCommand(TAILWIND_CMD_DEVICE_STATUS));
        State state = tailwindState.getStateForChannelID(channel);
        updateState(channel, state);
    }

    @Override
    public void channelUnlinked(ChannelUID channelUID) {
        String channel = channelUID.getId();
        if (logger.isDebugEnabled()) {
            logger.debug("Channel was Unlinked: {}, current state: {}", channelUID,
                    tailwindState.getStateForChannelID(channel));
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            String channel = channelUID.getId();
            logger.debug("Channel Id: {}, linked: {}", channel, isLinked(channel));
            updateTailwindDetails(sendCommand(TAILWIND_CMD_DEVICE_STATUS));
        } else {
            //
            try {
                String cmdBody = "";
                switch (channelUID.getId()) {
                    /**
                     * Controller Channels
                     */
                    case CHANNEL_LED_BRIGHTNESS:
                        cmdBody = buildSetLEDBrightnessCommand(Integer.parseInt(command.toString()));
                        response = tailwindApi.getTailwindControllerData(thing, config.getAuthToken(), cmdBody);
                        if (response.getResult().contentEquals(JSON_RESPONSE_RESULT_OK)) {
                            tailwindState.setLedBrighness(Integer.parseInt(command.toString()));
                        } else {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Error updating LED brightness: {}, detail: {}", response.getResult(),
                                        response.getInfo());
                            }
                        } // If response from TailWind was OK
                        break;
                    case CHANNEL_SUPPORT_COMMAND:
                        String cmd = command.toString();
                        switch (cmd) {
                            case TAILWIND_JSON_VALUE_NAME_IDENTIFY:
                                cmdBody = TAILWIND_CMD_IDENTIFY_DEVICE;
                                break;
                            case TAILWIND_JSON_VALUE_NAME_REBOOT:
                                cmdBody = TAILWIND_CMD_REBOOT_DEVICE;
                                break;
                        }
                        response = tailwindApi.getTailwindControllerData(thing, config.getAuthToken(), cmdBody);
                        if (response.getResult().contentEquals(JSON_RESPONSE_RESULT_OK)) {
                            tailwindState.setSupportCommand(command.toString());
                            ;
                        } else {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Error updating LED brightness: {}, detail: {}", response.getResult(),
                                        response.getInfo());
                            }
                        }
                        break;
                    /**
                     * Door 1 Channels
                     */
                    case CHANNEL_DOOR_1_CONTROLS_STATUS:
                        tailwindState.setDoorStatus(0, response.getDoorData().getDoor1().getStatus());
                        break;
                    case CHANNEL_DOOR_1_CONTROLS_OPEN_CLOSE:
                        cmdBody = buildDoorOpenCloseCommand(command.toString(), 0);
                        response = tailwindApi.getTailwindControllerData(thing, config.authToken, cmdBody);
                        tailwindState.setDoorOpenClose(0, command.toString());
                        break;
                    case CHANNEL_DOOR_1_CONTROLS_PARTIAL_OPEN:
                        tailwindState.setPartialOpen(0, config.getDoorOnePartialOpen());
                        break;
                    /**
                     * Door 2 Channels
                     */
                    case CHANNEL_DOOR_2_CONTROLS_STATUS:
                        tailwindState.setDoorStatus(1, response.getDoorData().getDoor2().getStatus());
                        break;
                    case CHANNEL_DOOR_2_CONTROLS_OPEN_CLOSE:
                        cmdBody = buildDoorOpenCloseCommand(command.toString(), 1);
                        response = tailwindApi.getTailwindControllerData(thing, config.authToken, cmdBody);
                        tailwindState.setDoorOpenClose(1, command.toString());
                        break;
                    case CHANNEL_DOOR_2_CONTROLS_PARTIAL_OPEN:
                        tailwindState.setPartialOpen(1, config.getDoorTwoPartialOpen());
                        break;
                    /**
                     * Door 3 Channels
                     */
                    case CHANNEL_DOOR_3_CONTROLS_STATUS:
                        tailwindState.setDoorStatus(2, response.getDoorData().getDoor3().getStatus());
                        break;
                    case CHANNEL_DOOR_3_CONTROLS_OPEN_CLOSE:
                        cmdBody = buildDoorOpenCloseCommand(command.toString(), 2);
                        response = tailwindApi.getTailwindControllerData(thing, config.authToken, cmdBody);
                        tailwindState.setDoorOpenClose(2, command.toString());
                        break;
                    case CHANNEL_DOOR_3_CONTROLS_PARTIAL_OPEN:
                        tailwindState.setPartialOpen(2, config.getDoorThreePartialOpen());
                        break;
                    default:
                        throw new TailwindUnsupportedCommandTypeException();
                }
            } catch (TailwindUnsupportedCommandTypeException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Unsupported Command {} for channel {}", command.toString(), channelUID.getId());
                }
            } catch (TailwindCommunicationException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("TailWindAPI connection error sending command {} for channel {}", command.toString(),
                            channelUID.getId());
                }
            }
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
        // Initialize the handler.
        if (logger.isDebugEnabled()) {
            logger.debug("Start initializing handler for thing {}", getThing().getUID());
        }
        // Initialize partialOpenDoorStates
        initializePartialOpenStates();
        // Get configuration settings
        config = getConfigAs(TailwindConfiguration.class);
        // Validate configuration settings
        try {
            if (!checkConfiguration(config)) {
                return;
            } else {
                if (logger.isDebugEnabled()) {
                    logger.info("tailwind:TailwindHandler using configuration: {}", config.toString());
                }
            }
        } catch (TailwindCommunicationException e) {
            // Error requesting state details from the TailWind controller
            logger.warn("There was an error communicating to the TailWind controller! Error msg: {}", e.getMessage());
        } catch (Exception e) {
            // Error Catch all for any unknown errors
            logger.warn(
                    "There was an unknown exception validating the configuration for the TailWind controller! Error msg: {}",
                    e.getMessage());

        }

        /* Set up number of garage doors specified for this thing in the configuration */
        configureDoorChannels(config);

        /* Schedule job to update controller detail status periodically */
        if (TAILWIND_STATUS_REQUEST_JOB_INTERVAL > 0) {
            // To be sure job is not running, cancel it before starting a new one.
            cancelControllerStatusJob();
            // Start a new job to update controller status.
            requestControllerStatusJob = scheduler.scheduleWithFixedDelay(() -> {
                Thread.currentThread().setName("OH-binding-" + this.thing.getUID() + "-requestControllerStatusJob");
                try {
                    updateTailwindDetails(sendCommand(TAILWIND_CMD_DEVICE_STATUS));
                } catch (Exception e) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Error starting job to refresh controller status: {}", e.getMessage());
                    }
                }
            }, TAILWIND_STATUS_REQUEST_JOB_INTERVAL, TAILWIND_STATUS_REQUEST_JOB_INTERVAL, TimeUnit.SECONDS);
        }

        /* Schedule job to listen for UDP messages from the TailWind controller */
        scheduler.execute(this::initializeConnection);

        updateStatus(ThingStatus.UNKNOWN);

        scheduler.execute(() -> {
            boolean thingReachable = true; // <background task with long running initialization here>
            // when done do:
            if (thingReachable) {
                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE);
            }
        });
    } // End initialize()

    private void initializeConnection() {

        try {
            final TailwindUdpConnector newUdpConnector = new TailwindUdpConnector(thing, config, scheduler,
                    tailwindApi);
            udpConnector = newUdpConnector;
            // establish connection and register listener
            newUdpConnector.connect(this::eventReceived, true, utilities.getThreadName(thing));
        } catch (InterruptedException e) {
            // OH shutdown - don't log anything, Framework will call dispose()
        } catch (Exception e) {
            logger.debug("Connection to '{}' failed", config, e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.HANDLER_INITIALIZING_ERROR, "Connection to '" + config
                    + "' failed unexpectedly with " + e.getClass().getSimpleName() + ": " + e.getMessage());
            dispose();
        }
        if (udpConnector != null) {
            if (!udpConnector.isConnected()) {
                logger.debug("****> UdpConnector was not connected on port: {}!", TAILWIND_OPENHAB_HOST_UDP_PORT);
            }
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

        /**
         * Check that door count is within the supported range 1 - max doors for this model
         */
        int maxDoors = utilities.getMaxDoors(thing.getThingTypeUID().getId());
        if (config.getDoorCount() < 1 || config.getDoorCount() > maxDoors) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "This binding supports 1 to " + maxDoors + " garage doors. Please update the door count.");
            return false;
        } // If the configured number of doors is out of range

        /*
         * Make sure a valid webServerAddress was specified for the TailWind controller
         * If item was discovered, add the httpServerUrl to the configuration webServerAddress.
         * If item was created manually, validate the address and update the properties with
         * the address.
         * If the configured value is different from the properties, use manual steps to validate.
         */
        String addressCheckResult = webServerAddressCheck();
        if (!addressCheckResult.isBlank()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    addressCheckResult + ": Address error!");
            return false;
        }

        /**
         * Check for OH Server IPV4 non-blank IP address
         */
        // Get default OH Server IP if the configuration is blan
        List<String> openHabHostIPAddresses = new ArrayList<>(utilities.getOHServerIP());
        String addressList = "";
        String[] addresses = openHabHostIPAddresses.toArray(new String[0]);
        Integer lastElement = openHabHostIPAddresses.size() - 1;
        for (int i = 0; i <= lastElement; i++) {
            if (i != lastElement) {
                addressList = addressList + addresses[i] + ", ";
            } else {
                addressList = addressList + addresses[i];
            }
        }
        if (config.getOpenHabHostAddress().isBlank()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "The OH Server IPV4 Address " + config.getOpenHabHostAddress()
                            + " is blank! Valid addresses for the OH server are " + addressList
                            + ". Please use the Primary address in Settings, Network Settings.");
            return false;
        }

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
            response = sendCommand(TAILWIND_CMD_DEVICE_STATUS);

            /* Use response from OK device status set initial states for this thing */
            if (!response.getResult().contentEquals(JSON_RESPONSE_RESULT_OK)) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        "Unable to verify authorization token. Please check that the Token and Web Address were entered correctly. If web address is ok, obtain another token from the web app (web.gotailwind.com) and try again.");
                return false;
            } else {
                // Update thing properties to include number of doors connected
                thing.setProperty(PARAMETER_DOOR_NUM_CONNECTED, String.valueOf(response.getDoorNum()));
            } // If result was OK

            if (logger.isDebugEnabled()) {
                logger.debug("Response to validate token {} is: {}", config.authToken, response);
            }
            // Ensure configured doors do not exceed the number of doors connected
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
         * Verify the authorization token is valid by sending a status request to the controller.
         * If OK is returned, use the values to update the thing properties if this thing was manually
         * created.
         */
        response = sendCommand(TAILWIND_CMD_DEVICE_STATUS);
        if (!response.getResult().contentEquals(JSON_RESPONSE_RESULT_OK)) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "The authorization token was invalid. Please check that the Token was entered correctly or obtain another one from the mobile app.");
            if (logger.isDebugEnabled()) {
                logger.debug("Command to set status report URL failed with result code: {}", response.getResult());
            }
            return false;
        } else {
            // Get property details from response and update if there are changes
            Map<String, String> properties = new HashMap<String, String>(thing.getProperties());
            String currentMacAddress = properties.get(TAILWIND_PROPERTY_MAC_ADDRESS);
            if (currentMacAddress != null) {
                if (currentMacAddress.isBlank() || currentMacAddress.contains("000000000000")
                        || !currentMacAddress.equals(utilities.convertDeviceIdToMac(response.getDevID()))) {
                    properties.put(TAILWIND_PROPERTY_MAC_ADDRESS, utilities.convertDeviceIdToMac(response.getDevID()));
                }
            } // If MAC address sent from controller is not equal to what is stored in properties
            String currentSftwrVer = properties.get(TAILWIND_PROPERTY_SOFTWARE_VERSION);
            if (currentSftwrVer != null) {
                if (currentSftwrVer.isBlank() || !currentSftwrVer.equals(response.getFwVer())) {
                    properties.put(TAILWIND_PROPERTY_SOFTWARE_VERSION, response.getFwVer());
                }
            } // If software address sent from controller is not equal to what is stored in properties
            String currentModelId = properties.get(TAILWIND_PROPERTY_MODEL_ID);
            if (currentModelId != null) {
                if (currentModelId.isBlank() || !currentModelId.equals(response.getProduct())) {
                    properties.put(TAILWIND_PROPERTY_MODEL_ID, response.getProduct());
                }
            } // If model number sent from controller is not equal to what is stored in properties

            thing.setProperties(properties);
        } // If command response result was not "OK"

        /**
         * Update controller with the UDP host address to receive notifications of changes to the controller
         * If it is blank, get a suggested address. If not blank, make sure it is a valid IP.
         * Once a valid IP address is found, send an update to the TailWind controller to use the
         * openHabHostAddress to send status updates to..
         */

        if (Utilities.isValidIPAddress(config.getOpenHabHostAddress())) {
            if (openHabHostIPAddresses.contains(config.getOpenHabHostAddress())) {
                logger.info("Initializing TailWind handler for thingUID {} using {} for the OH Server IPV4 Address.",
                        thing.getUID(), config.getOpenHabHostAddress());
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        "The OH Server IPV4 Address " + config.getOpenHabHostAddress()
                                + " was not found in the list of the servers available IP addresses " + addressList
                                + "! Please use the Primary address in Settings, Network Settings.");
                return false;
            } // If configured IP address is found in list of OH servers network interface addresses

            // Ensure the configuration address for the OH server is in list of server IP addresses
            logger.debug("Configured address {} was found in list: {} is: {}", config.getOpenHabHostAddress(),
                    openHabHostIPAddresses.toString(), openHabHostIPAddresses.contains(config.getOpenHabHostAddress()));
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "The OH Server IPV4 Address " + config.getOpenHabHostAddress()
                            + " is not in a valid IPV4 address format! Valid addresses for the OH server are "
                            + addressList + ". Please use the Primary address in Settings, Network Settings.");
            return false;
        } // If configured IP address is valid format
        logger.debug("number of IP addresses assigned to openHab host: {}, address list: {}",
                openHabHostIPAddresses.size(), openHabHostIPAddresses.toString());

        /**
         * Check for duplicate door names
         */
        String doorResult = duplicateNameFound(config);
        if (!doorResult.isBlank()) {
            logger.debug("Duplicate door name found. The value '{}' was used more than once!", doorResult);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Duplicate door name found. The value '" + doorResult + "' was used more than once!");
            return false;
        } // If duplicate doorName was found

        /*
         * No configuration errors found, refresh the states for any items linked to channels
         */
        updateTailwindDetails(sendCommand(TAILWIND_CMD_DEVICE_STATUS));
        return true;
    }

    private String webServerAddressCheck() {
        String addressCheckResult = "";
        Configuration tailwindConfiguration = editConfiguration();
        String serverAddress = (String) tailwindConfiguration.get(TAILWIND_CONFIG_WEB_SERVER_ADDRESSS_KEY);
        Map<String, String> properties = new HashMap<String, String>(thing.getProperties());
        String httpServerUrl = properties.get(TAILWIND_HTTP_SERVER_URL);
        // Check for both blank configuration and properties values
        if (httpServerUrl != null) {
            // Check for blank webServerAddress, replace with httpServerUrl
            if (serverAddress.isBlank()) {
                if (!httpServerUrl.isBlank()) {
                    tailwindConfiguration.put(TAILWIND_CONFIG_WEB_SERVER_ADDRESSS_KEY,
                            properties.get(TAILWIND_HTTP_SERVER_URL));
                    updateConfiguration(tailwindConfiguration);
                } else {
                    // Error: Both serverAddress and httpServerUrl are blank
                    addressCheckResult = "BLANK";
                } // If httlServerUrl has a value (not blank)
            } else {
                // Check to see if the two are different
                if (!serverAddress.equalsIgnoreCase(httpServerUrl)) {
                    if (!serverAddress.isBlank()) {
                        // Validate Server address can be an IP address or Url (ends in .local)
                        if (Utilities.isValidIPAddress(serverAddress) || serverAddress.contains(".local")) {
                            // Valid address to try
                            properties.put(TAILWIND_HTTP_SERVER_URL, serverAddress);
                            thing.setProperties(properties);
                        } else {
                            // Error address is not formatted correctly
                            addressCheckResult = "ADDRESS FORMAT ERROR";
                        }
                    } else {
                        // Error Server address was not entered yet.
                        addressCheckResult = "BLANK";
                    }
                } // If serverAddress is not equal to httpServerUrl
            } // If serverAddress is blank
        }

        return addressCheckResult;
    }

    /**
     * Method to send a command to the TailWind controller
     *
     * @param commandString - JSON formated command string
     * @return TailwindControllerData object response from the TailWind controller
     */
    private TailwindControllerData sendCommand(String commandString) {
        JSONObject tailwindCommandString = new JSONObject(commandString);
        String body = tailwindCommandString.toString();
        try {
            response = tailwindApi.getTailwindControllerData(thing, config.authToken, body);
        } catch (TailwindCommunicationException e) {
            // Error trying to connect to the TailWind controller possible configuration settings changes needed
            logger.warn("There was an error communicating to the TailWind controller! Error msg: {}", e.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "The TailWind controller did not respond to configured URL/Host address. Please ensure URL/Host address is correct.");
        }
        if (!response.getResult().contentEquals(JSON_RESPONSE_RESULT_OK)) {
            logger.debug("Get Status request failed with result: {}", response.getResult());
        }
        return response;
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
        cancelControllerStatusJob();
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

    /**
     * Method to configure the door group/channels associated with this TailWind device
     * <ul>
     * <li>Get current list of channels and remove existing door channels</li>
     * <li>Use configuration "door...Name" to build labels for door channels</li>
     * <li>Use configuration "doorCount" to build list of door group/channels to add</li>
     * <li>Update TailWind thing with door group/channels to be added</li>
     * </ul>
     *
     * @param config - TailWind thing configuration settings
     */
    private void configureDoorChannels(TailwindConfiguration config) {
        if (logger.isDebugEnabled()) {
            logger.debug("Configuring garage door groups/channels");
        }
        boolean channelsUpdated = false;
        // Get a list of current configured channels
        ArrayList<Channel> channels = new ArrayList<>(this.getThing().getChannels());

        // Construct a backup of the existing channel type UIDs prior to making changes
        Set<String> currentChannelsBackup = new HashSet<>();
        channels.forEach(channel -> currentChannelsBackup.add(channel.getUID().getId()));

        // Clear out the door channels from the thing, keep remaining channels
        ArrayList<Channel> channelsToKeep = new ArrayList<Channel>();
        for (Channel channel : channels) {
            String cGroup = channel.getUID().getGroupId();
            if (cGroup != null && cGroup.equals(CHANNEL_GROUP_CONTROLLER)) {
                channelsToKeep.add(channel);
            } // If channel belongs to controller group
        } // Loop to find controller group channels

        /*
         * Make sure the list of channels is clean by removing and
         * adding just the controller channels
         */
        editThing().withoutChannels(channels);
        editThing().withChannels(channelsToKeep);
        channels.clear();
        channels.addAll(channelsToKeep);

        // Create a list to hold door channels to be added to the TailWind thing
        List<Entry<String, ChannelTypeUID>> channelsToAdd = new ArrayList<>();

        /*
         * Create a list of channel item labels using the garage door names from the
         * TailWind thing configuration
         */
        Map<String, @Nullable String> channelItemLabelsNew = buildChannelLabels();

        // Add garage doors channels based on doorCount set in the TailWind configuration
        Integer doorCount = config.getDoorCount();
        switch (doorCount) {
            case 1: // 1 door
                channelsToAdd.addAll(DOOR_1_CHANNEL_TYPES.entrySet());
                break;
            case 2: // 2 doors
                channelsToAdd.addAll(DOOR_1_CHANNEL_TYPES.entrySet());
                channelsToAdd.addAll(DOOR_2_CHANNEL_TYPES.entrySet());
                break;
            case 3: // 3 doors
                channelsToAdd.addAll(DOOR_1_CHANNEL_TYPES.entrySet());
                channelsToAdd.addAll(DOOR_2_CHANNEL_TYPES.entrySet());
                channelsToAdd.addAll(DOOR_3_CHANNEL_TYPES.entrySet());
                break;
        }

        /*
         * Process the list of channels to add. Create a new channel using the
         * channel UID from the channel entry in the list. Retrieve the channel type and label
         * using the channel Id (i.e doorOne#Index) from the lists:
         * -> CHANNEL_ITEM_TYPES - defines type (Number, String..) for each of the door group channels
         * -> channelItemLabelsNew - holds labels created using the doorName for each of door group channels
         */
        if (doorCount >= 1) {
            // Add the door channels to the TailWind thing
            if (!channelsToAdd.isEmpty()) {
                for (Entry<String, ChannelTypeUID> entry : channelsToAdd) {
                    String itemType = CHANNEL_ITEM_TYPES.get(entry.getKey());
                    String itemLabel = channelItemLabelsNew.get(entry.getKey());
                    String itemDescription = CHANNEL_ITEM_DESCRIPTION.get(entry.getKey());
                    if (itemLabel != null && itemDescription != null) {
                        Channel channel = ChannelBuilder
                                .create(new ChannelUID(this.getThing().getUID(), entry.getKey()), itemType)
                                .withType(entry.getValue()).withDescription(itemDescription).withLabel(itemLabel)
                                .build();
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
        }
        // Update the TailWind thing with the correct number of doors/channels
        if (channelsUpdated) {
            updateThing(editThing().withChannels(channels).build());
        }
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

    /**
     * Cancel the checkStatus job
     */
    private void cancelControllerStatusJob() {
        ScheduledFuture<?> requestControllerStatusJob = this.requestControllerStatusJob;
        if (requestControllerStatusJob != null) {
            if (!requestControllerStatusJob.isCancelled()) {
                requestControllerStatusJob.cancel(true);
                this.requestControllerStatusJob = null;
            }
        }
    }

    private void updateTailwindDetails(TailwindControllerData tailwindControllerData) {
        for (Channel channel : getThing().getChannels()) {
            ChannelUID channelUID = channel.getUID();
            if (ChannelKind.STATE.equals(channel.getKind()) && channelUID.isInGroup() && channelUID.getGroupId() != null
                    && isLinked(channelUID)) {
                updateTailwindChannel(channelUID, tailwindControllerData);
            } // end if channel is linked, update its state
        }
    }

    private void updateTailwindChannel(ChannelUID channelUID, TailwindControllerData channelState) {
        // logger.debug("Update requested for channel: {}, channelState: {}", channelUID, channelState);
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
                case CHANNEL_GROUP_DOOR_THREE:
                    updateTailwindDoorThreeStates(channelUID, channelState);
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
            case CHANNEL_DOOR_1_CONTROLS_OPEN_CLOSE:
                tailwindState.setDoorOpenClose(doorIndex, channelState.getDoorData().getDoor1().getStatus());
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
        // Always update the partialOpen setting from the configuration settings
        tailwindState.setPartialOpen(doorIndex, config.getDoorOnePartialOpen());
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
            case CHANNEL_DOOR_2_CONTROLS_OPEN_CLOSE:
                tailwindState.setDoorOpenClose(doorIndex, channelState.getDoorData().getDoor2().getStatus());
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
        // Always update the partialOpen setting from the configuration settings
        tailwindState.setPartialOpen(doorIndex, config.getDoorTwoPartialOpen());
    }

    public void updateTailwindDoorThreeStates(ChannelUID channelUID, TailwindControllerData channelState) {
        String channelId = channelUID.getIdWithoutGroup();
        String channelGroup = channelUID.getGroupId();
        String switchCase = channelGroup + "#" + channelId;
        int doorIndex = (int) channelState.getDoorData().getDoor3().getIndex();
        switch (switchCase) {
            case CHANNEL_DOOR_3_CONTROLS_INDEX:
                tailwindState.setDoorIndex(doorIndex, channelState.getDoorData().getDoor3().getIndex());
                break;
            case CHANNEL_DOOR_3_CONTROLS_STATUS:
                tailwindState.setDoorStatus(doorIndex, channelState.getDoorData().getDoor3().getStatus());
                break;
            case CHANNEL_DOOR_3_CONTROLS_OPEN_CLOSE:
                tailwindState.setDoorOpenClose(doorIndex, channelState.getDoorData().getDoor3().getStatus());
                break;
            case CHANNEL_DOOR_3_CONTROLS_LOCKUP:
                tailwindState.setLockup(doorIndex, channelState.getDoorData().getDoor3().getLockup());
                break;
            case CHANNEL_DOOR_3_CONTROLS_DISABLED:
                tailwindState.setDisabled(doorIndex, channelState.getDoorData().getDoor3().getDisabled());
                break;
            default:
                break;
        }
        // Always update the partialOpen setting from the configuration settings
        tailwindState.setPartialOpen(doorIndex, config.getDoorThreePartialOpen());
    }

    @Override
    public void connectionError(@Nullable String errorMsg) {
        if (logger.isDebugEnabled()) {
            logger.debug("Error connecting to tailwind controller API: {}", errorMsg);
        }
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
        if (isLinked(channelID)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Updating state for Channel Id: {}, linked: {}, State: {}", channelID, isLinked(channelID),
                        state);
            }
            updateState(channelID, state);
        } else {
            logger.debug("Tried to update State but channel {} is not linked!", channelID);
        }
    }

    private String duplicateNameFound(TailwindConfiguration config) {
        String result = "";
        List<String> doorNames = new ArrayList<String>();
        for (int i = 1; i <= 3; i++) {
            switch (i) {
                case 1:
                    doorNames.add(config.getDoorOneName());
                    break;
                case 2:
                    doorNames.add(config.getDoorTwoName());
                    break;
                case 3:
                    doorNames.add(config.getDoorThreeName());
                    break;
            }
        } // For loop to build doorNames set
          // Find the value that is duplicate
        Object[] nameArray = doorNames.toArray();
        int length = doorNames.size();
        for (int i = 0; i < length - 1; i++) {
            for (int j = i + 1; j < length; j++) {
                // Check if two string elements are equal and not the same element.
                if ((nameArray[i].equals(nameArray[j])) && (i != j)) {
                    // If a duplicate is found, print the duplicate element.
                    result = nameArray[j].toString();
                }
            }
        }

        return result;
    }

    private Map<String, @Nullable String> buildChannelLabels() {
        Map<String, @Nullable String> channelLabels = new HashMap<>();
        // Set door labels based on key value containing Id for each door
        String doorName = "";
        for (Map.Entry<String, String> entry : CHANNEL_ITEM_LABELS.entrySet()) {
            String key = entry.getKey();
            if (key.contains("doorOne")) {
                doorName = config.getDoorOneName();
            } else if (key.contains("doorTwo")) {
                doorName = config.getDoorTwoName();
            } else if (key.contains("doorThree")) {
                doorName = config.getDoorThreeName();
            } else {
                // Key name not found.
                doorName = "";
            }
            if (!doorName.isBlank()) {
                channelLabels.put(entry.getKey(), String.join(" ", doorName, entry.getValue()));
            } // If the doorName was not blank
        }

        return channelLabels;
    }

    /**
     * @param command - String value of "open" or "close" the door
     * @param index - Integer door index value (0=Door 1, 1=Door 2, 2=Door 3)
     * @return Body string for command to send to TailWind controller
     */
    private String buildDoorOpenCloseCommand(String command, long index) {
        JSONObject cmdToOpenOrClose = new JSONObject(TAILWIND_CMD_DOOR_OPEN_OR_CLOSE);
        String cmdKeyFound = cmdToOpenOrClose.getJSONObject(TAILWIND_JSON_KEY_DATA)
                .getJSONObject(TAILWIND_JSON_KEY_VALUE).getString(TAILWIND_JSON_KEY_CMD);
        if (cmdKeyFound != null) {
            if (command.equalsIgnoreCase(TAILWIND_JSON_VALUE_CMD_PARTIAL_TIME)) {
                cmdToOpenOrClose.getJSONObject(TAILWIND_JSON_KEY_DATA).getJSONObject(TAILWIND_JSON_KEY_VALUE)
                        .put(TAILWIND_JSON_KEY_DOOR_IDX, index).put(TAILWIND_JSON_KEY_CMD, TAILWIND_JSON_VALUE_CMD_OPEN)
                        .put(TAILWIND_JSON_KEY_PARTIAL, getPartialOpenValue((int) index));
            } else {
                cmdToOpenOrClose.getJSONObject(TAILWIND_JSON_KEY_DATA).getJSONObject(TAILWIND_JSON_KEY_VALUE)
                        .put(TAILWIND_JSON_KEY_DOOR_IDX, index).put(TAILWIND_JSON_KEY_CMD, command);
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug(
                        "Command to open or close door was not formatted correctly for door index: {}, command: {}",
                        index, cmdToOpenOrClose);
            }
        }

        return cmdToOpenOrClose.toString();
    }

    private String buildSetLEDBrightnessCommand(Integer command) {
        JSONObject cmdToSetLEDBrightness = new JSONObject(TAILWIND_CMD_SET_LED_BRIGHTNESS);
        cmdToSetLEDBrightness.getJSONObject(TAILWIND_JSON_KEY_DATA).getJSONObject(TAILWIND_JSON_KEY_VALUE)
                .put(TAILWIND_JSON_KEY_BRIGHTNESS, command);
        return cmdToSetLEDBrightness.toString();
    }

    private Long getPartialOpenValue(Integer doorIndex) {
        Long partialOpenValue = 0l;
        switch (doorIndex) {
            case 0:
                partialOpenValue = utilities.getSecondsToMilliseconds(config.getDoorOnePartialOpen());
                break;
            case 1:
                partialOpenValue = utilities.getSecondsToMilliseconds(config.getDoorTwoPartialOpen());
                break;
            case 2:
                partialOpenValue = utilities.getSecondsToMilliseconds(config.getDoorThreePartialOpen());
                break;
        }
        return partialOpenValue;
    }

    private void initializePartialOpenStates() {
        for (int doorIndex = 0; doorIndex <= 2; doorIndex++) {
            tailwindState.setPartialOpen(doorIndex, 0);
        }
    }
}
