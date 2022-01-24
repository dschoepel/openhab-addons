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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.nadavr.internal.NadAvrConfiguration;
import org.openhab.binding.nadavr.internal.nadcp.NadCommand;
import org.openhab.binding.nadavr.internal.nadcp.NadCommand.Prefix;
import org.openhab.binding.nadavr.internal.nadcp.NadMessage;
import org.openhab.binding.nadavr.internal.nadcp.NadProtocol;
import org.openhab.binding.nadavr.internal.nadcp.NadcpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link NadConnection.java} class contains fields mapping thing configuration parameters.
 *
 * @author Dave J Schoepel - Initial contribution
 */
@NonNullByDefault
public class NadConnection extends NadAvrConnection {
    private Logger logger = LoggerFactory.getLogger(NadConnection.class);

    /** default Telnet port. **/
    public static final int DEFAULT_TELNET_PORT = 23;

    /** Connection timeout in milliseconds **/
    private static final int CONNECTION_TIMEOUT = 5000;

    /** Connection test interval in milliseconds **/
    private static final int CONNECTION_TEST_INTERVAL = 60000;

    /** Socket timeout in milliseconds **/
    private static final int SOCKET_TIMEOUT = CONNECTION_TEST_INTERVAL + 10000;

    /** Connection retry count on error situations **/
    private static final int FAST_CONNECTION_RETRY_COUNT = 3;

    /** Connection retry delays in milliseconds **/
    private static final int FAST_CONNECTION_RETRY_DELAY = 1000;
    private static final int SLOW_CONNECTION_RETRY_DELAY = 60000;

    private String ip;
    private int port;
    private NadAvrConfiguration configuration;
    private Socket nadSocket;
    private @Nullable DataListener dataListener;
    private @Nullable DataOutputStream outStream;
    private @Nullable DataInputStream inStream;
    private boolean connected;
    private List<NadEventListener> listeners = new ArrayList<>();
    private int retryCount = 1;
    private @Nullable ConnectionSupervisor connectionSupervisor;

    /**
     * Constructor for connecting to Ethernet NAD device using default port
     *
     * @param ip
     */
    public NadConnection(String ip, NadAvrConfiguration configuration) {
        this.ip = ip;
        this.configuration = configuration;
        nadSocket = new Socket();
    }

    /**
     * Constructor for connecting to Ethernet NAD device
     *
     * @param ip
     * @param port
     */
    public NadConnection(String ip, int port, NadAvrConfiguration configuration) {
        this.ip = ip;
        this.port = port;
        this.configuration = configuration;
        nadSocket = new Socket();
    }

    /**
     * Open connection to the NAD device.
     */
    @Override
    public void openConnection() {
        connectSocket();
    }

    /**
     * Closes the connection to the NAD device.
     */
    @Override
    public void closeConnection() {
        closeSocket();
    }

    public void addEventListener(NadEventListener listener) {
        this.listeners.add(listener);
    }

    public void removeEventListener(NadEventListener listener) {
        this.listeners.remove(listener);
    }

    @Override
    public String getConnectionName() {
        return ip + ":" + port;
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    /**
     * Sends a command to NAD device.
     *
     * @param cmd NAD command to send
     */
    public void send(final String prefix, final String variable, final String operator, final String value) {
        try {
            sendCommand(new NadMessage.MessageBuilder().prefix(prefix).variable(variable).operator(operator)
                    .value(value).build());
        } catch (Exception e) {
            logger.warn("Could not send command to device on {}:{}: ", ip, port, e);
        }
    }

    @Override
    public void sendCommand(NadMessage msg) {
        logger.debug("Send command: {} to {}:{} ({})", msg.toString(), ip, port, nadSocket);
        sendCommand(msg, retryCount);
    }

    /**
     * Sends to command to the receiver.
     *
     * @param msg the NAD command to send.
     * @param retry retry count when connection fails.
     */
    private void sendCommand(NadMessage msg, int retry) {
        if (connectSocket()) {
            try {
                String data = NadProtocol.createNADCommand(msg);
                if (logger.isTraceEnabled()) {
                    logger.debug("Sending {} bytes: {}", data, data.length());
                }
                if (outStream != null) {
                    outStream.writeBytes(data);
                }
                if (outStream != null) {
                    outStream.flush();
                }
            } catch (IOException ioException) {
                logger.warn("Error occurred when sending command: {}", ioException.getMessage());

                if (retry > 0) {
                    logger.debug("Retry {}...", retry);
                    closeSocket();
                    sendCommand(msg, retry - 1);
                } else {
                    sendConnectionErrorEvent(ioException.getMessage());
                }
            }
        }
    }

    /**
     * Connects to the receiver by opening a socket connection through the
     * IP and port.
     */
    private synchronized boolean connectSocket() {
        // Socket nadSocket = this.nadSocket;
        if (!connected || !nadSocket.isConnected()) {
            try {
                // Creating a socket to connect to the server
                nadSocket = new Socket();

                // start connection tester
                if (connectionSupervisor == null) {
                    connectionSupervisor = new ConnectionSupervisor(CONNECTION_TEST_INTERVAL);
                }
                nadSocket.connect(new InetSocketAddress(ip, port), CONNECTION_TIMEOUT);
                logger.debug("Connected to {}:{}", ip, port);
                // Get Input and Output streams

                outStream = new DataOutputStream(nadSocket.getOutputStream());
                inStream = new DataInputStream(nadSocket.getInputStream());
                nadSocket.setSoTimeout(SOCKET_TIMEOUT);
                if (outStream != null) {
                    outStream.flush();
                }
                connected = true;
                // start status update listener
                if (dataListener == null) {
                    dataListener = new DataListener();
                    dataListener.setName(configuration.getHostname() + "-DataListener");
                    if (dataListener != null) {
                        dataListener.start();
                    }
                }
            } catch (UnknownHostException unknownHost) {
                logger.debug("You are trying to connect to an unknown host: {}", unknownHost.getMessage());
                sendConnectionErrorEvent(unknownHost.getMessage());
            } catch (IOException ioException) {
                logger.debug("Can't connect: {}", ioException.getMessage());
                sendConnectionErrorEvent(ioException.getMessage());
            }
        }

        return connected;
    }

    /**
     * Closes the socket connection.
     *
     * @return true if the closed successfully
     */
    private boolean closeSocket() {
        try {
            if (dataListener != null) {
                dataListener.setInterrupted(true);
                dataListener = null;
                logger.debug("closed data listener!");
            }
            if (connectionSupervisor != null) {
                connectionSupervisor.stopConnectionTester();
                connectionSupervisor = null;
                logger.debug("closed connection tester!");
            }
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (IOException e) {
                }
                inStream = null;
                logger.debug("closed input stream!");
            }
            if (outStream != null) {
                try {
                    outStream.close();
                } catch (IOException e) {
                }
                outStream = null;
                logger.debug("closed output stream!");
            }
            if (nadSocket.isConnected()) {
                try {
                    nadSocket.close();
                } catch (IOException e) {
                }
                logger.debug("closed socket!");
            }
            connected = false;
        } catch (Exception e) {
            logger.debug("Closing connection throws an exception, {}", e.getMessage());
        }

        return connected;
    }

    /**
     * This method waits for any state messages from NAD device.
     *
     * @throws IOException
     * @throws InterruptedException
     * @throws NadcpException
     */
    private void waitStateMessages() throws NumberFormatException, IOException, InterruptedException, NadcpException {
        if (connected) {
            logger.debug("Waiting status messages");

            while (true) {
                if (inStream != null) {
                    NadMessage message = NadProtocol.getNextMessage(inStream);
                    logger.debug("Message received is: {}", message.toString());
                    // Make sure this message (i.e. status update) is supported by the binding
                    try {
                        NadCommand receivedCommand = NadCommand.EMPTY_COMMAND;
                        try {
                            receivedCommand = NadCommand.getCommandByVariableAndOperator(message.getVariable(),
                                    message.getOperator());
                        } catch (IllegalArgumentException ex) {
                            logger.warn("Received unknown status update from NAD Device @{}: data={}", ip + ":" + port,
                                    message);
                            return;
                        }
                        logger.debug("Received line = {}", receivedCommand);
                        sendMessageEvent(message);
                    } catch (Exception e) {
                        logger.warn(
                                "Exception in statusUpdateReceived for NAD device @{}. Cause: {}, data received: {}",
                                ip, e.getMessage(), message);
                    }
                }
            }
        } else {
            throw new IOException("Not Connected to Receiver");
        }
    }

    private class DataListener extends Thread {
        private boolean interrupted = false;

        DataListener() {
        }

        public void setInterrupted(boolean interrupted) {
            this.interrupted = interrupted;
            this.interrupt();
        }

        @Override
        public void run() {
            logger.debug("Data listener started");
            boolean restartConnection = false;
            long connectionAttempts = 0;
            // as long as no interrupt is requested, continue running
            while (!interrupted) {
                try {
                    waitStateMessages();
                    connectionAttempts = 0;
                } catch (SocketException | SocketTimeoutException e) {
                    logger.warn("No data received during supervision interval ({} ms)!", SOCKET_TIMEOUT);
                    restartConnection = true;
                } catch (NadcpException e) {
                    logger.error("Error occurred during message waiting: {}", e.getMessage());
                } catch (Exception e) {
                    if (!interrupted && !this.isInterrupted()) {
                        logger.error("Error occurred during message waiting: {}", e.getMessage());
                        restartConnection = true;
                        // sleep a while, to prevent fast looping if error situation is permanent
                        if (++connectionAttempts < FAST_CONNECTION_RETRY_COUNT) {
                            mysleep(FAST_CONNECTION_RETRY_DELAY);
                        } else {
                            // slow down after few faster attempts
                            if (connectionAttempts == FAST_CONNECTION_RETRY_COUNT) {
                                logger.debug(
                                        "Connection failed {} times to {}:{}, slowing down automatic connection to {} seconds.",
                                        FAST_CONNECTION_RETRY_COUNT, ip, port, SLOW_CONNECTION_RETRY_DELAY / 1000);
                            }
                            mysleep(SLOW_CONNECTION_RETRY_DELAY);
                        }
                    }
                }

                if (restartConnection && !interrupted) {
                    restartConnection = false;
                    // reopen connection
                    logger.debug("Reconnecting...");
                    try {
                        connected = false;
                        connectSocket();
                        logger.debug("Test connection to {}:{}", ip, port);
                        sendCommand(new NadMessage.MessageBuilder().prefix(Prefix.Main.toString())
                                .variable(NadCommand.POWER_QUERY.getVariable())
                                .operator(NadCommand.POWER_QUERY.getOperator()).value(NadCommand.POWER_QUERY.getValue())
                                .build());
                    } catch (Exception ex) {
                        logger.error("Reconnection invoking error: {}", ex.getMessage());

                        sendConnectionErrorEvent(ex.getMessage());
                    }
                }
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Data listener stopped");
            }
        }

        private void mysleep(long milli) {
            try {
                sleep(milli);
            } catch (InterruptedException e) {
                interrupted = true;
            }
        }
    }

    private class ConnectionSupervisor {
        private Timer timer;

        public ConnectionSupervisor(int milliseconds) {
            logger.debug("Connection supervisor started, interval {} milliseconds", milliseconds);
            timer = new Timer(configuration.getHostname() + "-ConnectionSupervisor");
            timer.schedule(new Task(), milliseconds, milliseconds);
        }

        public void stopConnectionTester() {
            timer.cancel();
        }

        class Task extends TimerTask {
            @Override
            public void run() {
                logger.debug("Test connection to {}:{}", ip, port);
                sendCommand(new NadMessage.MessageBuilder().prefix(Prefix.Main.toString())
                        .variable(NadCommand.POWER_QUERY.getVariable()).operator(NadCommand.POWER_QUERY.getOperator())
                        .value(NadCommand.POWER_QUERY.getValue()).build());
            }
        }
    }

    private void sendConnectionErrorEvent(@Nullable String errorMsg) {
        // send message to event listeners
        try {
            for (NadEventListener listener : listeners) {
                listener.connectionError(ip, errorMsg);
            }
        } catch (Exception ex) {
            logger.debug("Event listener invoking error: {}", ex.getMessage());
        }
    }

    private void sendMessageEvent(NadMessage message) {
        // send message to event listeners
        try {
            for (NadEventListener listener : listeners) {
                listener.receivedMessage(ip, message);
            }
        } catch (Exception e) {
            logger.debug("Event listener invoking error: {}", e.getMessage());
        }
    }
}
