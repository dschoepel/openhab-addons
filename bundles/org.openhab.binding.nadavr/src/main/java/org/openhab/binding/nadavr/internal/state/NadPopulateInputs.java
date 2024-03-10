/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.binding.nadavr.internal.state;

import static org.openhab.binding.nadavr.internal.NadAvrBindingConstants.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.nadavr.internal.NadAvrConfiguration;
import org.openhab.binding.nadavr.internal.NadException;
import org.openhab.binding.nadavr.internal.connector.NadIpConnector;
import org.openhab.binding.nadavr.internal.nadcp.NadCommand;
import org.openhab.binding.nadavr.internal.nadcp.NadCommand.Prefix;
import org.openhab.binding.nadavr.internal.nadcp.NadMessage;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.types.StateOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link NadAvrPopulateInputs.java} class starts a thread to monitor for and update input source names for related
 * channel's options. This captures user modified source names, and any changes that may be made to them
 * via the device's configuration menu outside of this binding.
 *
 * @author Dave J Schoepel - Initial contribution
 */
@NonNullByDefault
public class NadPopulateInputs {
    private final Logger logger = LoggerFactory.getLogger(NadPopulateInputs.class);
    private ScheduledExecutorService piExecutor = Executors.newSingleThreadScheduledExecutor();
    private volatile boolean sendSourceQuery = false;
    private volatile boolean isRunning = false;
    private int numberOfInputSources = 5;
    ThingUID thingUID;
    NadAvrConfiguration config;
    NadAvrStateDescriptionProvider stateDescriptionProvider;
    NadIpConnector connection;

    /**
     * Constructor for populating the NAD Input source names for each of the Devices zones.
     *
     * @param thingUID - thing UID of the device whose source input channel options will be updated
     * @param config - thing configuration settings
     * @param connection - details for the thing used to request source names
     * @param stateDescriptionProvider - used to dynamically update the channel options with input source names
     * @param sendSourceQuery - flag to indicate if a source query has been sent to prevent sending too many requests
     * @param numberOfInputSources - number of input sources associated with the things model
     */
    public NadPopulateInputs(ThingUID thingUID, NadAvrConfiguration config, NadIpConnector connection,
            NadAvrStateDescriptionProvider stateDescriptionProvider, boolean sendSourceQuery,
            int numberOfInputSources) {
        this.thingUID = thingUID;
        this.config = config;
        this.connection = connection;
        this.stateDescriptionProvider = stateDescriptionProvider;
        this.sendSourceQuery = sendSourceQuery;
        this.numberOfInputSources = numberOfInputSources;
    }

    /**
     * Method to query source input names and dynamically update them on each of the active zones channel for source
     * input options.
     *
     * @throws NadException -
     */
    private void populateInputs() throws NadException {
        isRunning = true;
        List<StateOption> options = new ArrayList<>();
        List<StateOption> optionsZ2to4 = new ArrayList<>();
        if (logger.isDebugEnabled()) {
            logger.debug("Number of input sources = {}", numberOfInputSources);
        }
        // Build list of source names to be used by Input Source channel (options)
        for (int i = 1; i <= numberOfInputSources; i++) {
            String name = NadAvrInputSourceList.getSourceName(i - 1);
            options.add(new StateOption(String.valueOf(i), name));
            optionsZ2to4.add(new StateOption(String.valueOf(i), name));
        }
        // Zones 2-4 will have one extra input called local that points back to the main source
        optionsZ2to4.add(new StateOption(String.valueOf(options.size() + 1), LOCAL));
        if (logger.isDebugEnabled()) {
            logger.debug("Got Source Name input List from NAD Device {}", options);
            logger.debug("Got Source Name input List from NAD Device {}", optionsZ2to4);
        }
        NadCommand srcQuery = NadCommand.INPUT_SOURCE_QUERY;
        for (int i = 1; i <= config.getZoneCount(); i++) {
            switch (i) {
                case 1:
                    stateDescriptionProvider.setStateOptions(new ChannelUID(thingUID, CHANNEL_MAIN_SOURCE), options);
                    if (sendSourceQuery) {
                        connection.sendCommand(new NadMessage.MessageBuilder().prefix(Prefix.Main.toString())
                                .variable(srcQuery.getVariable().toString()).operator(srcQuery.getOperator().toString())
                                .value(srcQuery.getValue()).build());
                    }
                    break;
                case 2:
                    stateDescriptionProvider.setStateOptions(new ChannelUID(thingUID, CHANNEL_ZONE2_SOURCE),
                            optionsZ2to4);
                    if (sendSourceQuery) {
                        connection.sendCommand(new NadMessage.MessageBuilder().prefix(Prefix.Zone2.toString())
                                .variable(srcQuery.getVariable().toString()).operator(srcQuery.getOperator().toString())
                                .value(srcQuery.getValue()).build());
                    }
                    break;
                case 3:
                    stateDescriptionProvider.setStateOptions(new ChannelUID(thingUID, CHANNEL_ZONE3_SOURCE),
                            optionsZ2to4);
                    if (sendSourceQuery) {
                        connection.sendCommand(new NadMessage.MessageBuilder().prefix(Prefix.Zone3.toString())
                                .variable(srcQuery.getVariable().toString()).operator(srcQuery.getOperator().toString())
                                .value(srcQuery.getValue()).build());
                    }
                    break;
                case 4:
                    stateDescriptionProvider.setStateOptions(new ChannelUID(thingUID, CHANNEL_ZONE4_SOURCE),
                            optionsZ2to4);
                    if (sendSourceQuery) {
                        connection.sendCommand(new NadMessage.MessageBuilder().prefix(Prefix.Zone4.toString())
                                .variable(srcQuery.getVariable().toString()).operator(srcQuery.getOperator().toString())
                                .value(srcQuery.getValue()).build());
                    }
                    break;
                default:
                    break;
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("NadAvrPopulateInputs - populateInputs() finished....");
        }
    }

    /**
     * Scheduled thread to poll for source input names and populate channel options with source names
     */
    Runnable scheduler = new Runnable() {
        @Override
        public void run() {
            Thread.currentThread().setName("OH-binding-" + thingUID.getAsString() + "-PopulateInputs");
            try {
                populateInputs();
            } catch (NadException e) {
                logger.error(
                        "Error requesting input source name information from the NAD device @{}, check for connection issues.  Error: {}",
                        connection.getConnectionName(), e.getLocalizedMessage());
            }
            if (sendSourceQuery) {
                sendSourceQuery = false;
                try {
                    logger.debug("polulateInputs - Sleeping 2 seconds...");
                    TimeUnit.SECONDS.sleep(2);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                try {
                    populateInputs();
                } catch (NadException e) {
                    logger.error(
                            "Error requesting input source name information from the NAD device @{}, check for connection issues.  Error: {}",
                            connection.getConnectionName(), e.getLocalizedMessage());
                }
            }
            isRunning = false;
            return;
        }
    };

    /**
     * Method to start the populateInputs thread
     */
    public void startPi() {
        if (logger.isDebugEnabled()) {
            logger.debug("PopulateInputs started...");
        }
        piExecutor.schedule(scheduler, 3, TimeUnit.SECONDS);
    }

    /**
     * Method to stop the populateInputs thread
     */
    public void stopPi() {
        if (logger.isDebugEnabled()) {
            logger.debug("PopulateInputs stopped...");
        }
        piExecutor.shutdown();
    }

    /**
     * Method to determine if the populateInputs thread is active
     *
     * @return true if this thread is running, false if not
     */
    public boolean isRunning() {
        return isRunning;
    }
}
