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
 * The {@link NadXmRefreshChannelInfo.java} class is used to refresh the details for the
 * selected XM channel.
 *
 * @author Dave J Schoepel - Initial contribution
 */
@NonNullByDefault
public class NadXmRefreshChannelInfo {

    Logger logger = LoggerFactory.getLogger(NadXmRefreshChannelInfo.class);
    private ScheduledExecutorService xmExecutor = Executors.newSingleThreadScheduledExecutor();
    private String threadHostName = "";
    private volatile boolean isXmPaused;
    private volatile boolean isXmStarted;
    NadIpConnector connection;

    /**
     * Constructor for XM Refresh Channel Info thread
     *
     * @param connection - to NAD Device to retrieve the XM Channel Info from the tuner
     */
    public NadXmRefreshChannelInfo(NadIpConnector connection) {
        this.connection = connection;
    }

    /**
     * Method to send an XM Channel Info Queries to the NAD Device whose reply will return the
     * current Channel name, song and title playing on the XM channel. This response will be received by the
     * NadAvrHandler and used to update the channels.
     *
     * @throws NadException - Send diagnostic messages to help troubleshoot errors
     */
    public void getXmChannelInfo() throws NadException {
        if (logger.isDebugEnabled()) {
            logger.debug("getXmChannelInfo is using connector at {}", connection.msgReaderThreadName);
        }
        connection.sendCommand(new NadMessage.MessageBuilder().prefix(Prefix.Tuner.toString())
                .variable(NadCommand.TUNER_XM_CHANNEL_NAME_QUERY.getVariable().toString())
                .operator(NadCommand.TUNER_XM_CHANNEL_NAME_QUERY.getOperator().toString())
                .value(NadCommand.TUNER_XM_CHANNEL_NAME_QUERY.getValue()).build());

        connection.sendCommand(new NadMessage.MessageBuilder().prefix(Prefix.Tuner.toString())
                .variable(NadCommand.TUNER_XM_SONG_TITLE_QUERY.getVariable().toString())
                .operator(NadCommand.TUNER_XM_SONG_TITLE_QUERY.getOperator().toString())
                .value(NadCommand.TUNER_XM_SONG_TITLE_QUERY.getValue()).build());

        connection.sendCommand(new NadMessage.MessageBuilder().prefix(Prefix.Tuner.toString())
                .variable(NadCommand.TUNER_XM_NAME_QUERY.getVariable().toString())
                .operator(NadCommand.TUNER_XM_NAME_QUERY.getOperator().toString())
                .value(NadCommand.TUNER_XM_NAME_QUERY.getValue()).build());
    }

    /**
     * Runnable used by the scheduler to give the thread a name and start the {@link getXmChannelInfo}
     */
    Runnable xmRefreshChannelInfoThread = new Runnable() {
        @Override
        public void run() {
            Thread.currentThread().setName(threadHostName + "-NadXmRefreshChannelInfo");
            if (!isXmPaused) {
                try {
                    getXmChannelInfo();
                } catch (NadException e) {
                    logger.error(
                            "Error sending XM Channel Info queries to the NAD device @{}, check for connection issues.  Error: {}",
                            connection.getConnectionName(), e.getLocalizedMessage());
                }
            }
        }
    };

    /**
     * This method starts the thread to refresh XM channel info from the NAD Tuner Source
     *
     * @param threadHost - is the NAD Device host name to be associated with this thread
     */
    public void start(String threadHost) {
        boolean isXmShutdown = false;
        threadHostName = threadHost;
        if (!isXmStarted()) {
            isXmShutdown = true;
        }
        if (isXmShutdown) {
            logger.debug("xmRefreshChannelInfo is starting...");
            xmExecutor.scheduleWithFixedDelay(xmRefreshChannelInfoThread, getXmInitialDelay(), getXmPeriodDelay(),
                    getXmTimeUnits());
            isXmStarted = true;
        } else {
            logger.debug("Resumed getXmChannelInfo...");
            resumeXmRefreshChannelInfo();
        }
    }

    /**
     * Method to pause the running of {@link xmRefreshChannelInfoThread} after it has been started
     */
    public void pauseXmRefreshChannleInfo() {
        if (logger.isDebugEnabled()) {
            logger.debug("getXmChannelInfo is paused...");
        }
        isXmPaused = true;
    }

    /**
     * Method to resume the paused {@link xmRefreshChannelInfoThread}
     */
    public void resumeXmRefreshChannelInfo() {
        if (logger.isDebugEnabled()) {
            logger.debug("getXmChannelInfo is resumed...");
        }
        isXmPaused = false;
    }

    /**
     * Method to kill the {@link xmRefreshChannelInfoThread}
     */
    public void stopXmRefreshChannelInfo() {
        isXmStarted = false;
        xmExecutor.shutdownNow();
        if (logger.isDebugEnabled()) {
            logger.debug("getXmChannelInfo is stopped...");
        }
    }

    /**
     * @return true if {@link xmRefreshChannelInfoThread} is running, false if {@link xmRefreshChannelInfoThread} is not
     *         running
     */
    public boolean isXmStarted() {
        return isXmStarted;
    }

    /**
     * Method to check if the {@link xmRefreshChannelInfoThread} is paused when the tuner band is not set to XM
     *
     * @return true if {@link xmRefreshChannelInfoThread} is paused, false if {@link xmRefreshChannelInfoThread} is
     *         running
     */
    public boolean isXmPaused() {
        return isXmPaused;
    }

    /**
     * Method to set the startup delay when scheduling the {@link xmRefreshChannelInfoThread}
     *
     * @return Thread startup initial delay in {@link getXmTimeUnits}
     */
    public int getXmInitialDelay() {
        return 5;
    }

    /**
     * Method to set the scheduling delay for the thread {@link xmRefreshChannelInfoThread}
     *
     * @return Time to pause between running the {@link getXmChannelInfo} in {@link getXmTimeUnits}
     */
    public int getXmPeriodDelay() {
        return 10;
    }

    /**
     * Method to set the time units for scheduling
     *
     * @return the time units to be used when scheduling the {@link xmRefreshChannelInfoThread}
     */
    public TimeUnit getXmTimeUnits() {
        return TimeUnit.SECONDS;
    }
}
