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
package org.openhab.binding.nadavr.internal.connector;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.nadavr.internal.NadException;
import org.openhab.binding.nadavr.internal.nadcp.NadCommand;
import org.openhab.binding.nadavr.internal.nadcp.NadCommand.Prefix;
import org.openhab.binding.nadavr.internal.nadcp.NadMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link NadAvrFMRdsTextStream.java} class is used to capture the tuner FM RDS text stream using a separate thread.
 *
 * @author Dave J Schoepel - Initial contribution
 */
@NonNullByDefault
public class NadFMRdsTextStream {

    Logger logger = LoggerFactory.getLogger(NadFMRdsTextStream.class);
    private ScheduledExecutorService rdsExecutor = Executors.newSingleThreadScheduledExecutor();
    private String threadHostName = "";
    private volatile boolean isRdsPaused;
    private volatile boolean isRdsStarted;
    NadIpConnector connection;

    /**
     * Constructor for FM RDS Text Stream thread
     *
     * @param connection to NAD Device to retrieve the text stream from the tuner
     */
    public NadFMRdsTextStream(NadIpConnector connection) {
        this.connection = connection;
    }

    /**
     * Method to send an FM RDS Text Stream query to the NAD Device whose reply will return the
     * current text stream data. This response will be received by the NadAvrHandler and
     * used to update the channel.
     *
     * @throws NadException
     */
    public void getRdsStream() throws NadException {
        if (logger.isDebugEnabled()) {
            logger.debug("getRdsStream is using connector at {}", connection.msgReaderThreadName);
        }
        connection.sendCommand(new NadMessage.MessageBuilder().prefix(Prefix.Tuner.toString())
                .variable(NadCommand.TUNER_FM_RDS_TEXT_QUERY.getVariable().toString())
                .operator(NadCommand.TUNER_FM_RDS_TEXT_QUERY.getOperator().toString())
                .value(NadCommand.TUNER_FM_RDS_TEXT_QUERY.getValue()).build());
    }

    /**
     * Runnable used by the scheduler to give the thread a name and start the {@link getRdsStream}
     */
    Runnable rdsFMStreamThread = new Runnable() {
        @Override
        public void run() {
            Thread.currentThread().setName(threadHostName + "-RdsTextStream");
            if (!isRdsPaused) {
                try {
                    getRdsStream();
                } catch (NadException e) {
                    logger.error(
                            "Error sending FM RDS text query to the NAD device @{}, check for connection issues.  Error: {}",
                            connection.getConnectionName(), e.getLocalizedMessage());
                }
            }
        }
    };

    /**
     * This method starts the thread to capture FM band RDS Text Streams from the NAD Tuner Source
     *
     * @param threadHost is the NAD Device host name to be associated with this thread
     */
    public void start(String threadHost) {
        boolean isRdsShutdown = false;
        threadHostName = threadHost;
        if (!isRdsStarted()) {
            isRdsShutdown = true;
        }

        if (isRdsShutdown) {
            logger.debug("getRdsStream is starting...");
            rdsExecutor.scheduleWithFixedDelay(rdsFMStreamThread, getRdsInitialDelay(), getRdsPeriodDelay(),
                    getRdsTimeUnits());
            isRdsStarted = true;
        } else {
            logger.debug("Resumed getRdsStream...");
            resumeRds();
        }
    }

    /**
     * Method to pause the running of {@link rdsFMStreamThread} after it has been started
     */
    public void pauseRds() {
        if (logger.isDebugEnabled()) {
            logger.debug("getRdsStream is paused...");
        }
        isRdsPaused = true;
    }

    /**
     * Method to resume the paused {@link rdsFMStreamThread}
     */
    public void resumeRds() {
        if (logger.isDebugEnabled()) {
            logger.debug("getRdsStream is resumed...");
        }
        isRdsPaused = false;
    }

    /**
     * Method to kill the {@link rdsFMStreamThread}
     */
    public void stopRds() {
        isRdsStarted = false;
        rdsExecutor.shutdownNow();
        if (logger.isDebugEnabled()) {
            logger.debug("getRdsStream is stopped...");
        }
    }

    /**
     * @return true if {@link rdsFMStreamThread} is running, false if {@link rdsFMStreamThread} is not running
     */
    public boolean isRdsStarted() {
        return isRdsStarted;
    }

    /**
     * Method to check if the {@link rdsFMStreamThread} is paused when the tuner band is not set to FM
     *
     * @return true if {@link rdsFMStreamThread} is paused, false if {@link rdsFMStreamThread} is running
     */
    public boolean isRdsPaused() {
        return isRdsPaused;
    }

    /**
     * Method to set the startup delay when scheduling the {@link rdsFMStreamThread}
     *
     * @return Thread startup initial delay in {@link getRdsTimeUnits}
     */
    public int getRdsInitialDelay() {
        return 0;
    }

    /**
     * Method to set the scheduling delay for the thread {@link rdsFMStreamThread}
     *
     * @return Time to pause between running the {@link getRdsStream} in {@link getRDSTimeUnits}
     */
    public int getRdsPeriodDelay() {
        return 25;
    }

    /**
     * Method to set the time units for scheduling
     *
     * @return the time units to be used when scheduling the {@link rdsFMStreamThread}
     */
    public TimeUnit getRdsTimeUnits() {
        return TimeUnit.SECONDS;
    }
}
