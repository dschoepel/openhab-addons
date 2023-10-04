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
 * The {@link NadDABDlsTextStream.java} class is used to capture tuner DAB DLS text stream using a separate thread.
 *
 * @author Dave J Schoepel - Initial contribution
 */
@NonNullByDefault
public class NadDABDlsTextStream {

    Logger logger = LoggerFactory.getLogger(NadDABDlsTextStream.class);
    private ScheduledExecutorService dlsExecutor = Executors.newSingleThreadScheduledExecutor();
    private String threadHostName = "";
    private volatile boolean isDlsPaused;
    private volatile boolean isDlsStarted;
    NadIpConnector connection;

    /**
     * Constructor for DAB DLS Text Stream thread
     *
     * @param connection to NAD Device to retrieve the DLS text stream from the tuner
     */
    public NadDABDlsTextStream(NadIpConnector connection) {
        this.connection = connection;
    }

    /**
     * Method to send an DAB DLS Text Stream query to the NAD Device whose reply will return the
     * current text stream data. This response will be received by the NadAvrHandler and
     * used to update the channel.
     *
     * @throws NadException
     */
    public void getDlsStream() throws NadException {
        if (logger.isDebugEnabled()) {
            logger.debug("getDlsStream is using connector at {}", connection.msgReaderThreadName);
        }
        connection.sendCommand(new NadMessage.MessageBuilder().prefix(Prefix.Tuner.toString())
                .variable(NadCommand.TUNER_DAB_DLS_TEXT_QUERY.getVariable().toString())
                .operator(NadCommand.TUNER_DAB_DLS_TEXT_QUERY.getOperator().toString())
                .value(NadCommand.TUNER_DAB_DLS_TEXT_QUERY.getValue()).build());
    }

    /**
     * Runnable used by the scheduler to give the thread a name and start the {@link getDlsStream}
     */
    Runnable dlsDABStreamThread = new Runnable() {
        @Override
        public void run() {
            Thread.currentThread().setName(threadHostName + "-DlsTextStream");
            if (!isDlsPaused) {
                try {
                    getDlsStream();
                } catch (NadException e) {
                    logger.error(
                            "Error sending DAB DLS text query to the NAD device @{}, check for connection issues.  Error: {}",
                            connection.getConnectionName(), e.getLocalizedMessage());
                }
            }
        }
    };

    /**
     * This method starts the thread to capture DAB band DLS Text Streams from the NAD Tuner Source
     *
     * @param threadHost is the NAD Device host name to be associated with this thread
     */
    public void start(String threadHost) {
        boolean isDlsShutdown = false;
        threadHostName = threadHost;
        if (!isDlsStarted()) {
            isDlsShutdown = true;
        }

        if (isDlsShutdown) {
            logger.debug("getDlsStream is starting...");
            dlsExecutor.scheduleWithFixedDelay(dlsDABStreamThread, getDlsInitialDelay(), getDlsPeriodDelay(),
                    getDlsTimeUnits());
            isDlsStarted = true;
        } else {
            logger.debug("Resumed getDlsStream...");
            resumeDls();
        }
    }

    /**
     * Method to pause the running of {@link dlsDABtreamThread} after it has been started
     */
    public void pauseDls() {
        if (logger.isDebugEnabled()) {
            logger.debug("getDlsStream is paused...");
        }
        isDlsPaused = true;
    }

    /**
     * Method to resume the paused {@link dlsDABtreamThread}
     */
    public void resumeDls() {
        if (logger.isDebugEnabled()) {
            logger.debug("getDlsStream is resumed...");
        }
        isDlsPaused = false;
    }

    /**
     * Method to kill the {@link dlsDABStreamThread}
     */
    public void stopDls() {
        isDlsStarted = false;
        dlsExecutor.shutdownNow();
        if (logger.isDebugEnabled()) {
            logger.debug("getDlsStream is stopped...");
        }
    }

    /**
     * @return true if {@link dlsDABStreamThread} is running, false if {@link dlsDABStreamThread} is not running
     */
    public boolean isDlsStarted() {
        return isDlsStarted;
    }

    /**
     * Method to check if the {@link dlsDABStreamThread} is paused when the tuner band is not set to DAB
     *
     * @return true if {@link dlsDABStreamThread} is paused, false if {@link dlsDABStreamThread} is running
     */
    public boolean isDlsPaused() {
        return isDlsPaused;
    }

    /**
     * Method to set the startup delay when scheduling the {@link dlsDABStreamThread}
     *
     * @return Thread startup initial delay in {@link getDlsTimeUnits}
     */
    public int getDlsInitialDelay() {
        return 0;
    }

    /**
     * Method to set the scheduling delay for the thread {@link dlsDABStreamThread}
     *
     * @return Time to pause between running the {@link getDlsStream} in {@link getDlsTimeUnits}
     */
    public int getDlsPeriodDelay() {
        return 10;
    }

    /**
     * Method to set the time units for scheduling
     *
     * @return the time units to be used when scheduling the {@link dlsDABStreamThread}
     */
    public TimeUnit getDlsTimeUnits() {
        return TimeUnit.SECONDS;
    }
}
