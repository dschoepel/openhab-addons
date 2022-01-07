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
package org.openhab.binding.nadavr.internal.connector;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.nadavr.internal.nadcp.NADCommand;
import org.openhab.binding.nadavr.internal.nadcp.NADCommand.Prefix;
import org.openhab.binding.nadavr.internal.nadcp.NADMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link NADAvrRdsTextThread} requests updates to the tuner FM RDS Text stream .
 *
 * @author Dave J Schoepel - Initial contribution
 */
@NonNullByDefault
public class NADAvrRdsTextThread extends Thread {

    private Logger logger = LoggerFactory.getLogger(NADAvrRdsTextThread.class);

    NADAvrConnector connector;

    private volatile boolean flag = true;

    /**
     * @param connector
     */
    public NADAvrRdsTextThread(NADAvrConnector connector) {
        this.connector = connector;
    }

    public void stopRunning() {
        flag = false;
    }

    @Override
    public void run() {
        if (logger.isDebugEnabled()) {
            logger.debug(">>>>> Running RdsTextThread");
        }

        // while (flag) {
        connector.internalSendCommand(new NADMessage.MessageBuilder().prefix(Prefix.Tuner.toString())
                .variable(NADCommand.TUNER_FM_RDS_TEXT_QUERY.getVariable().toString())
                .operator(NADCommand.TUNER_FM_RDS_TEXT_QUERY.getOperator().toString())
                .value(NADCommand.TUNER_FM_RDS_TEXT_QUERY.getValue()).build());
        // try {
        // Thread.sleep(10000);
        // } catch (InterruptedException e) {
        // Thread.currentThread().interrupt();
        // logger.debug("RdsText Thread interrupted");
        // return;
        // }
        // }

        // try {
        // // TODO build in way to end this thread from AVR handler
        // boolean exit = false;
        // while (!isInterrupted() && !exit) {
        // connector.internalSendCommand(new NADMessage.MessageBuilder().prefix(Prefix.Tuner.toString())
        // .variable(NADCommand.TUNER_FM_RDS_TEXT_QUERY.getVariable().toString())
        // .operator(NADCommand.TUNER_FM_RDS_TEXT_QUERY.getOperator().toString())
        // .value(NADCommand.TUNER_FM_RDS_TEXT_QUERY.getValue()).build());
        // try {
        // Thread.sleep(10000);
        // } catch (InterruptedException e) {
        // logger.debug("RdsText Thread interrupted");
        // exit = true;
        // }
        // ;
        // }
        // } finally {
        // }
        if (logger.isDebugEnabled()) {
            logger.debug(">>>>> RdsTextThread STOPPED running...");
        }
        // return;
    }
}
