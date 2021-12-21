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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openhab.binding.nadavr.internal.NADAvrConfiguration;
import org.openhab.binding.nadavr.internal.NADAvrStateChangedListener;
import org.openhab.binding.nadavr.internal.nadcp.NADMessage;
import org.openhab.binding.nadavr.internal.nadcp.NADProtocol;
import org.openhab.binding.nadavr.internal.nadcp.NADcpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link NADAvrTelnetClientThread} manages the connection to the NAD A/V Receiver.
 *
 * @author Dave J Schoepel - Initial contribution
 */
// @NonNullByDefault
public class NADAvrTelnetClientThread extends Thread {

    private Logger logger = LoggerFactory.getLogger(NADAvrTelnetClientThread.class);

    /**
     * REGEX to validate message and group components of the message
     * Group 1 = prefix,
     * Group 2 = variable,
     * Group 3 = operator,
     * Group 4 = value
     */
    private static final Pattern NAD_FULL_MESSAGE_PATTERN = Pattern
            .compile("^(.[^.]+)\\.(.*[^=\\?\\-\\+])([=\\?\\+\\-])(.*)$", Pattern.CASE_INSENSITIVE);

    /**
     * REGEX to validate message that is a query
     * Group 1 = prefix + .variable(optional),
     * Group 2 = operator is ?
     */
    private static final Pattern NAD_PREFIX_QUERY_PATTERN = Pattern.compile("^(.[^.]+)(\\?)", Pattern.CASE_INSENSITIVE);

    private static final Integer RECONNECT_DELAY = 60000; // 1 minute

    private static final Integer TIMEOUT = 60000; // 1 minute

    private NADAvrConfiguration config;

    private NADAvrTelnetListener listener;
    private List<NADAvrStateChangedListener> listeners = new ArrayList<>();

    private boolean connected = false;

    private int retryCount = 1;

    private Socket socket;

    private OutputStreamWriter out;

    private BufferedReader in;

    /**
     * @param config
     * @param listener
     */
    public NADAvrTelnetClientThread(NADAvrConfiguration config, NADAvrTelnetListener listener) {
        logger.debug("NAD Avr listener created.");
        this.config = config;
        this.listener = listener;
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            if (!connected) {
                connectTelnetSocket();
            }
            // Initialize protocol place holders
            String prefix = "";
            String variable = "";
            String operator = "";
            String value = "";
            do {
                try {
                    String line = in.readLine();
                    // logger.debug("Received from {} : {}", config.getHostname(), line);
                    if (line == null) {
                        logger.debug("No more data read from client. Disconnecting..");
                        listener.telnetClientConnected(false);
                        disconnect();
                        break;
                    }
                    logger.debug("Received from {} : {}", config.getHostname(), line);
                    if (!line.isBlank()) {
                        // verify that the line is valid - longer than 0, contains dot ".", contains operator
                        Matcher match = NAD_FULL_MESSAGE_PATTERN.matcher(line);
                        if (match.matches()) {
                            prefix = match.group(1); // get prefix - Group 1: anything before first dot
                            variable = match.group(2); // get variable - Group 2: between first dot and operator
                            operator = match.group(3); // Get operator - Group 3: '=, ?, + or -'
                            value = match.group(4).stripTrailing(); // Get value - Group 4: everything after the
                                                                    // operator
                            NADMessage recievedMessage = new NADMessage.MessageBuilder().prefix(prefix)
                                    .variable(variable).operator(operator).value(value).build();
                            listener.receivedLine(recievedMessage);
                        } else {
                            Matcher matchPrefix = NAD_PREFIX_QUERY_PATTERN.matcher(line);
                            if (matchPrefix.matches()) {
                                prefix = matchPrefix.group(1);
                                operator = matchPrefix.group(2);
                                NADMessage recievedMessage = new NADMessage.MessageBuilder().prefix(prefix)
                                        .variable(variable).operator(operator).value(value).build();
                                listener.receivedLine(recievedMessage);
                            } else {
                                throw new NADcpException(
                                        "Skipping NAD response message, it is not in a valid message format (<prefix> . <variable> <operator> <value>): "
                                                + line);
                            }
                        }
                    }
                } catch (SocketTimeoutException e) {
                    logger.trace("Socket timeout");
                    // Disconnects are not always detected unless you write to the socket.
                    try {
                        out.write('\r');
                        out.flush();
                    } catch (IOException e2) {
                        logger.debug("Error writing to socket");
                        connected = false;
                    }
                } catch (IOException e) {
                    if (!isInterrupted()) {
                        // only log if we don't stop this on purpose causing a SocketClosed
                        logger.debug("Error in telnet connection ", e);
                    }
                    connected = false;
                    listener.telnetClientConnected(false);
                } catch (NADcpException ncp) {
                    if (!isInterrupted()) {
                        // only log if we don't stop this on purpose causing a SocketClosed
                        logger.debug("Error message recieved over telnet connection ", ncp);
                    }
                }
            } while (!isInterrupted() && connected);
        }
        disconnect();
        logger.debug("Stopped client thread");
    }

    public void sendCommand(NADMessage msg) {
        logger.debug("Send command: {} to {}:{}", msg.toString(), config.ipAddress, config.telnetPort);
        sendCommand(msg, retryCount);
    }

    /**
     * Sends a command to NAD device.
     *
     * @param cmd NAD command to send
     */
    public void send(final String prefix, final String variable, final String operator, final String value) {
        try {
            sendCommand(new NADMessage.MessageBuilder().prefix(prefix).variable(variable).operator(operator)
                    .value(value).build());
        } catch (Exception e) {
            logger.warn("Could not send command to device {} on {}:{}: ", config.hostname, config.ipAddress,
                    config.telnetPort, e);
        }
    }

    /**
     * Sends to command to the NAD device.
     *
     * @param msg the NAD protocol command to send.
     * @param retry retry count when connection fails.
     */
    private void sendCommand(NADMessage msg, int retry) {
        if (connected) {
            try {
                String data = NADProtocol.createNADCommand(msg);
                if (logger.isTraceEnabled()) {
                    logger.trace("Sending {} length: {}", data, data.length());
                }
                logger.debug("Sending {} length: {}", data, data.length());
                out.write(data);
                out.flush();
            } catch (IOException ioException) {
                logger.warn("Error occurred when sending command: {}", ioException.getMessage());

                if (retry > 0) {
                    logger.debug("Retry {}...", retry);
                    disconnect();
                    sendCommand(msg, retry - 1);
                } else {
                    sendConnectionErrorEvent(ioException.getMessage());
                }
            }
        }
    }

    public void sendCommand(String command) {
        if (out != null) {
            try {
                out.write('\r' + command + '\r'); // Precede and follow command with carriage return
                out.flush();
            } catch (IOException e) {
                logger.debug("Error sending command", e);
            }
        } else {
            logger.debug("Cannot send command, no telnet connection");
        }
    }

    public void shutdown() {
        disconnect();
    }

    private void connectTelnetSocket() {
        disconnect();
        int delay = 0;

        while (!isInterrupted() && (socket == null || !socket.isConnected())) {
            try {
                Thread.sleep(delay);
                logger.debug("Connecting to {} {}", config.getHostname(), config.ipAddress);

                // Use raw socket instead of TelnetClient here because TelnetClient sends an
                // extra newline char after each write which causes the connection to become
                // unresponsive.
                socket = new Socket();
                socket.connect(new InetSocketAddress(config.getHostname(), config.getTelnetPort()), TIMEOUT);
                socket.setKeepAlive(true);
                socket.setSoTimeout(TIMEOUT);

                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new OutputStreamWriter(socket.getOutputStream(), "UTF-8");

                connected = true;
                logger.debug("Calling telnetClinetConnected(true)");
                listener.telnetClientConnected(true);
                logger.debug("NAD telnet client connected to {}", config.getHostname());
            } catch (IOException e) {
                logger.debug("Cannot connect to {}", config.getHostname(), e);
                listener.telnetClientConnected(false);
            } catch (InterruptedException e) {
                logger.debug("Interrupted while connecting to {}", config.getHostname(), e);
                Thread.currentThread().interrupt();
            }
            delay = RECONNECT_DELAY;
        }
    }

    public boolean isConnected() {
        return connected;
    }

    private void disconnect() {
        if (socket != null) {
            logger.debug("Disconnecting socket");
            try {
                socket.close();
            } catch (IOException e) {
                logger.debug("Error while disconnecting telnet client", e);
            } finally {
                socket = null;
                out = null;
                in = null;
                listener.telnetClientConnected(false);
            }
        }
    }

    private void sendConnectionErrorEvent(String errorMsg) {
        // send message to event listeners
        try {
            for (NADAvrStateChangedListener listener : listeners) {
                listener.connectionError(errorMsg);
            }
        } catch (Exception ex) {
            logger.debug("Event listener invoking error: {}", ex.getMessage());
        }
    }
}
