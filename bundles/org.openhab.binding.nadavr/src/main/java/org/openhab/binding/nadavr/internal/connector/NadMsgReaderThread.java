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

import java.io.InterruptedIOException;
import java.util.Arrays;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.nadavr.internal.NadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link NadMsgReaderThread} reads messages from the NAD device in a dedicated thread
 *
 * @author Dave J Schoepel - Initial contribution
 */
@NonNullByDefault
public class NadMsgReaderThread extends Thread {

    private final Logger logger = LoggerFactory.getLogger(NadMsgReaderThread.class);

    private static final int READ_BUFFER_SIZE = 32;

    private NadConnector connector;

    public NadMsgReaderThread(NadConnector connector, String threadName) {
        super(threadName);
        this.connector = connector;
    }

    @Override
    public void run() {
        logger.debug("Data listener started...");
        final int size = 128;
        byte[] readDataBuffer = new byte[READ_BUFFER_SIZE];
        byte[] dataBuffer = new byte[size];
        int index = 0;
        final char terminatingChar = '\n'; /* carriage return */

        try {
            while (!Thread.interrupted()) {
                int len = connector.readInput(readDataBuffer);
                if (len > 0) {
                    for (int i = 0; i < len; i++) {
                        if (index < size) {
                            dataBuffer[index++] = readDataBuffer[i];
                        }
                        if (readDataBuffer[i] == terminatingChar) {
                            if (index > +size) {
                                dataBuffer[index - 1] = (byte) terminatingChar;
                            }
                            byte[] msg = Arrays.copyOf(dataBuffer, index);
                            connector.handleIncomingMessage(msg);
                            index = 0;
                        }
                    }
                }
            }
        } catch (InterruptedIOException e) {
            Thread.currentThread().interrupt();
            logger.debug("Interrupted via InterruptedIOException");
        } catch (NadException e) {
            logger.debug("Reading failed: {}", e.getMessage(), e);
            connector.handleIncomingMessage(NadConnector.READ_ERROR);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Data listener stopped!");
        }
    }
}
