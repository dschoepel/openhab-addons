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
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.json.JSONObject;
import org.openhab.binding.tailwind.internal.TailwindConfiguration;
import org.openhab.binding.tailwind.internal.Utils.Utilites;
import org.openhab.binding.tailwind.internal.connector.JSONPost;
import org.openhab.binding.tailwind.internal.connector.TailwindCommunicationException;
import org.openhab.binding.tailwind.internal.dto.TailwindControllerData;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link TailwindHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Dave J. Schoepel - Initial contribution
 */
@NonNullByDefault
public class TailwindHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(TailwindHandler.class);

    private TailwindConfiguration config = new TailwindConfiguration();
    // private TailwindHttpResponse response = new TailwindHttpResponse();
    // private HttpClient httpClient = new HttpClient();
    private JSONPost tailwindHttpRequest = new JSONPost();
    private Utilites utilities = new Utilites();

    /**
     * Constructor for TailWind Device Handler
     *
     * @param thing - TailWind Controller
     */
    public TailwindHandler(Thing thing) {
        super(thing);
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
    @SuppressWarnings("null")
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
         * TODO: Send command to TaiWind controller to set the status report URL
         * Also update the controller and configured door states from the response
         * from sending this command to the controller.
         */

        /* TODO: Scheduled job to listen for UDP messages from the TailWind controller */

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
    }

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
        }

        // Check for a valid Authorization token
        if (logger.isDebugEnabled()) {
            logger.debug("Authorization Token is {}", config.authToken);
        }
        // Token must be 6 characters long, TailWind HTTP server responds with {"result":"OK"} when used
        // to request a status.
        if (config.getAuthToken().length() == 6) {
            // Check for a valid authorization token. HTTP response code should be 200 and response contains
            // {"result": "OK"} vs. {"result":"token fail"}
            // String response = "";
            String server = "tailwind-08d1f91202ec.local";
            String url = TAILWIND_BASE_URL_PART_1 + server + TAILWIND_BASE_URL_PART_2;

            JSONObject tailwindCommandString = new JSONObject(TAILWIND_CMD_DEVICE_STATUS);
            // tailwindCommandString.put(TAILWIND_JSON_KEY_VERSION, TAILWIND_JSON_VALUE_VER_02);
            String body = tailwindCommandString.toString();

            TailwindControllerData response = tailwindHttpRequest.postJson(url, body, config.authToken);
            /* TODO: Use response from OK device status set initial states for this thing */
            if (response != null) {
                if (!response.getResult().contentEquals("OK")) {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                            "The authorization token was invalid. Please check that the Token was entered correctly or obtain another one from the mobile app.");
                    return false;
                } else {
                    // Update thing properties to include number of doors connected
                    thing.setProperty(PARAMETER_DOOR_NUM_CONNECTED, String.valueOf(response.getDoorNum()));
                }
            }
            // TailwindControllerData responseData = parseResponse(response, TailwindControllerData.class);

            if (logger.isDebugEnabled()) {
                logger.debug("Response to validate token {} is: {}", config.authToken, response);
            }
            if (response != null) {

                int connectedDoors = (int) response.getDoorNum();
                if (config.getDoorCount() > connectedDoors) {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                            "This garage controller has " + connectedDoors
                                    + " doors connected. The number of controlled doors can be 1-" + connectedDoors
                                    + "!  Please update the door count.");
                    return false;
                }
            }
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "The authorization token should be 6 charactes long, its " + config.getAuthToken().length()
                            + " characters long. Please update the authorization code.");
            return false;
        } // If the authorization token length is equal to 6

        return true;
    }

    /**
     * Method to shutdown and remove Tailwind thing.
     * <ul>
     * <li>Remove handler</li>
     * </ul>
     */
    @Override
    public void dispose() {
        if (logger.isDebugEnabled()) {
            logger.debug("Disposing handler for thing {}", getThing().getUID());
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
}
