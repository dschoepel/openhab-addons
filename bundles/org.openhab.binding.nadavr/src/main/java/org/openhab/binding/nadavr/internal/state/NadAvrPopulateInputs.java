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
package org.openhab.binding.nadavr.internal.state;

import static org.openhab.binding.nadavr.internal.NadAvrBindingConstants.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.nadavr.internal.NadAvrConfiguration;
import org.openhab.binding.nadavr.internal.connector.NadConnection;
import org.openhab.binding.nadavr.internal.nadcp.NadCommand;
import org.openhab.binding.nadavr.internal.nadcp.NadCommand.Prefix;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.types.StateOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link NadAvrPopulateInputs.java} class contains fields mapping thing configuration parameters.
 *
 * @author Dave J Schoepel - Initial contribution
 */
@NonNullByDefault
public class NadAvrPopulateInputs {
    private final Logger logger = LoggerFactory.getLogger(NadAvrPopulateInputs.class);
    private ScheduledExecutorService piExecutor = Executors.newSingleThreadScheduledExecutor();
    private volatile boolean sendSourceQuery = false;
    private volatile boolean isRunning = false;
    ThingUID thingUID;
    NadAvrConfiguration config;
    NadAvrStateDescriptionProvider stateDescriptionProvider;
    NadConnection connection;

    public NadAvrPopulateInputs(ThingUID thingUID, NadAvrConfiguration config, NadConnection connection,
            NadAvrStateDescriptionProvider stateDescriptionProvider, boolean sendSourceQuery) {
        this.thingUID = thingUID;
        this.config = config;
        this.connection = connection;
        this.stateDescriptionProvider = stateDescriptionProvider;
        this.sendSourceQuery = sendSourceQuery;
    }

    private void populateInputs() {
        logger.debug("NadAvrPopulateInputs - populateInputs() started with sourceQuery = {} ....", sendSourceQuery);
        isRunning = true;
        List<StateOption> options = new ArrayList<>();
        List<StateOption> optionsZ2to4 = new ArrayList<>();
        // Build list of source names to be used by Input Source channel (options)
        for (int i = 1; i <= NadAvrInputSourceList.size(); i++) {
            String name = NadAvrInputSourceList.getSourceName(i - 1);
            options.add(new StateOption(String.valueOf(i), name));
            optionsZ2to4.add(new StateOption(String.valueOf(i), name));
        }
        logger.debug("Value of i = {}", optionsZ2to4.size());
        optionsZ2to4.add(new StateOption(String.valueOf(options.size() + 1), LOCAL));
        logger.debug("Got Source Name input List from NAD Device {}", options);
        logger.debug("Got Source Name input List from NAD Device {}", optionsZ2to4);

        for (int i = 1; i <= config.getZoneCount(); i++) {
            switch (i) {
                case 1:
                    stateDescriptionProvider.setStateOptions(new ChannelUID(thingUID, CHANNEL_MAIN_SOURCE), options);
                    if (sendSourceQuery) {
                        connection.sendCommand(Prefix.Main.toString(), NadCommand.INPUT_SOURCE_QUERY);
                    }
                    break;
                case 2:
                    stateDescriptionProvider.setStateOptions(new ChannelUID(thingUID, CHANNEL_ZONE2_SOURCE),
                            optionsZ2to4);
                    if (sendSourceQuery) {
                        connection.sendCommand(Prefix.Zone2.toString(), NadCommand.INPUT_SOURCE_QUERY);
                    }
                    break;
                case 3:
                    stateDescriptionProvider.setStateOptions(new ChannelUID(thingUID, CHANNEL_ZONE3_SOURCE),
                            optionsZ2to4);
                    if (sendSourceQuery) {
                        connection.sendCommand(Prefix.Zone3.toString(), NadCommand.INPUT_SOURCE_QUERY);
                    }
                    break;
                case 4:
                    stateDescriptionProvider.setStateOptions(new ChannelUID(thingUID, CHANNEL_ZONE4_SOURCE),
                            optionsZ2to4);
                    if (sendSourceQuery) {
                        connection.sendCommand(Prefix.Zone4.toString(), NadCommand.INPUT_SOURCE_QUERY);
                    }
                    break;
                default:
                    break;
            }
        }

        logger.debug("NadAvrPopulateInputs - populateInputs() finished....");
    }

    Runnable scheduler = new Runnable() {
        @Override
        public void run() {
            Thread.currentThread().setName(thingUID.getAsString() + "-PopulateInputs");
            populateInputs();
            if (sendSourceQuery) {
                sendSourceQuery = false;
                try {
                    logger.debug("polulateInputs - Sleeping 2 seconds...");
                    TimeUnit.SECONDS.sleep(2);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                populateInputs();
            }
            isRunning = false;
            return;
        }
    };

    public void startPi() {
        logger.debug("PopulateInputs started...");
        piExecutor.schedule(scheduler, 0, TimeUnit.SECONDS);
    }

    public void stopPi() {
        logger.debug("PopulateInputs stopped...");
        piExecutor.shutdown();
    }

    public boolean isRunning() {
        return isRunning;
    }
}
