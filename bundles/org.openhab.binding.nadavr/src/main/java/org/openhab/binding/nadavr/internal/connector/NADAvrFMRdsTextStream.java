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
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.nadavr.internal.nadcp.NADCommand;
import org.openhab.binding.nadavr.internal.nadcp.NADCommand.Prefix;
import org.openhab.binding.nadavr.internal.nadcp.NADMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link NADAvrFMRdsTextStream.java} class is used to capture tuner FM RDS text stream.
 *
 * @author Dave J Schoepel - Initial contribution
 */
@NonNullByDefault
public class NADAvrFMRdsTextStream {

    Logger logger = LoggerFactory.getLogger(NADAvrFMRdsTextStream.class);
    private @Nullable ScheduledExecutorService executor = null;
    private volatile boolean isPaused = false;
    private volatile boolean isStarted = false;
    NADAvrConnector connector;

    public NADAvrFMRdsTextStream(NADAvrConnector connector) {
        this.connector = connector;
    }

    public void getRdsStream() {
        connector.internalSendCommand(new NADMessage.MessageBuilder().prefix(Prefix.Tuner.toString())
                .variable(NADCommand.TUNER_FM_RDS_TEXT_QUERY.getVariable().toString())
                .operator(NADCommand.TUNER_FM_RDS_TEXT_QUERY.getOperator().toString())
                .value(NADCommand.TUNER_FM_RDS_TEXT_QUERY.getValue()).build());
    }

    Runnable scheduler = new Runnable() {
        @Override
        public void run() {
            if (!isPaused) {
                getRdsStream();
            }
        }
    };

    public void start() {
        boolean isShutdown = false;
        if (executor != null) {
            if (executor.isShutdown()) {
                isShutdown = true;
            }
        } else if (executor == null) {
            isShutdown = true;
        }

        if (isShutdown) {
            logger.debug("getRdsStream is starting...");
            executor = Executors.newSingleThreadScheduledExecutor();
            executor.scheduleWithFixedDelay(scheduler, getInitialDelay(), getPeriodDelay(), getTimeUnits());
            isStarted = true;
        } else {
            logger.debug("Resumed getRdsStream...");
            resume();
        }
    }

    public void pause() {
        logger.debug("getRdsStream is paused...");
        isPaused = true;
    }

    public void resume() {
        logger.debug("getRdsStream is resumed...");
        isPaused = false;
    }

    public void stop() {
        logger.debug("getRdsStream is stopped...");
        isStarted = false;
        if (executor != null) {
            executor.shutdownNow();
        }
        executor = null;
    }

    public boolean isStarted() {
        return isStarted;
    }

    public int getInitialDelay() {
        return 0;
    }

    public int getPeriodDelay() {
        return 5;
    }

    public TimeUnit getTimeUnits() {
        return TimeUnit.SECONDS;
    }
}
