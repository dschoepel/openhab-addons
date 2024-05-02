/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.binding.tailwind.internal.connector;

import static org.openhab.binding.tailwind.internal.TailwindBindingConstants.*;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class TailwindUdpConnector {

    /** Buffer for incoming UDP packages. */
    private static final int MAX_PACKET_SIZE = 1024;

    private final Logger logger = LoggerFactory.getLogger(TailwindUdpConnector.class);

    /** The device IP this connector is listening to / sends to. */
    private final String host;

    /** The port this connector is listening to. */
    private final int receivePort;

    /** Service to spawn new threads for handling status updates. */
    private final ExecutorService executorService;

    /** Thread factory for UDP listening thread. */
    private final NamedThreadFactory listeningThreadFactory = new NamedThreadFactory(TAILWIND_UDP_CONNECTOR_THREAD_NAME,
            true);

    /** Socket for receiving UDP packages. */
    private @Nullable DatagramSocket receivingSocket = null;
    /** Socket for sending UDP packages. */
    private @Nullable DatagramSocket sendingSocket = null;

    /** The listener that gets notified upon newly received messages. */
    private @Nullable Consumer<String> listener;

    private int receiveFailures = 0;
    private boolean listenerActive = false;

    /**
     * @throws UnknownHostException
     *
     */
    public TailwindUdpConnector(int udpReceivePort, ExecutorService executorService) throws UnknownHostException {
        InetAddress address1 = InetAddress.getLocalHost();
        String hostAddress = address1.getHostAddress();
        if (udpReceivePort <= 0) {
            throw new IllegalArgumentException("Invalid udpReceivePort: " + udpReceivePort);
        }
        this.host = hostAddress;
        this.receivePort = udpReceivePort;

        this.executorService = executorService;
    }

    /**
     * Initialize socket connection to the UDP receive port for the given listener.
     *
     * @throws SocketException Is only thrown if <code>logNotTHrowException = false</code>.
     * @throws InterruptedException Typically happens during shutdown.
     */
    public void connect(Consumer<String> listener, boolean logNotThrowExcpetion)
            throws SocketException, InterruptedException {
        if (receivingSocket == null) {
            try {
                receivingSocket = new DatagramSocket(receivePort);
                sendingSocket = new DatagramSocket();
                this.listener = listener;
                listeningThreadFactory.newThread(this::listen).start();

                // wait for the listening thread to be active
                for (int i = 0; i < 20 && !listenerActive; i++) {
                    Thread.sleep(100); // wait at most 20 * 100ms = 2sec for the listener to be active
                }
                if (!listenerActive) {
                    logger.warn(
                            "Listener thread started but listener is not yet active after 2sec; something seems to be wrong with the JVM thread handling?!");
                }
            } catch (SocketException e) {
                if (logNotThrowExcpetion) {
                    logger.warn(
                            "Failed to open socket connection on port {} (maybe there is already another socket listener on that port?)",
                            receivePort, e);
                }

                disconnect();

                if (!logNotThrowExcpetion) {
                    throw e;
                }
            }
        } else if (!Objects.equals(this.listener, listener)) {
            throw new IllegalStateException("A listening thread is already running");
        }
    } // End connect(...)

    private void listen() {
        try {
            listenUnhandledInterruption();
        } catch (InterruptedException e) {
            // OH shutdown - don't log anything, just quit
        }
    } // End listen()

    private void listenUnhandledInterruption() throws InterruptedException {
        logger.info("TailWind UPD listener started for: '{}:{}'", host, receivePort);

        final Consumer<String> listener2 = listener;
        final DatagramSocket socket2 = receivingSocket;
        while (listener2 != null && socket2 != null && receivingSocket != null) {
            try {
                final DatagramPacket packet = new DatagramPacket(new byte[MAX_PACKET_SIZE], MAX_PACKET_SIZE);

                listenerActive = true;
                socket2.receive(packet); // receive packet (blocking call)
                listenerActive = false;

                final byte[] data = Arrays.copyOfRange(packet.getData(), 0, packet.getLength() - 1);

                if (data == null || data.length == 0) {
                    if (isConnected()) {
                        logger.debug("Nothing received, this may happen during shutdown or some unknown error");
                    }
                    continue;
                }
                receiveFailures = 0; // message successfully received, unset failure counter

                /* useful for debugging without logger */
                // System.out.println(String.format("%s [%s] received: %s", getClass().getSimpleName(),
                // new SimpleDateFormat("HH:mm:ss.SSS").format(new Date()), new String(data).trim()));

                // log & notify listener in new thread (so that listener loop continues immediately)
                executorService.execute(() -> {
                    final String message = new String(data);
                    // if (logger.isDebugEnabled()) {
                    // logger.debug("Received data on port {}: {}", receivePort, message);
                    // }
                    listener2.accept(message);
                });
            } catch (Exception e) {
                listenerActive = false;

                if (receivingSocket == null) {
                    logger.debug("Socket closed; stopping listener on port {}.", receivePort);
                } else {
                    // if we get 3 errors in a row, we should better add a delay to stop spamming the log!
                    if (receiveFailures++ > ATTEMPTS_WITH_COMMUNICATION_ERRORS) {
                        logger.debug(
                                "Unexpected error while listening on port {}; waiting 10sec before the next attempt to listen on that port.",
                                receivePort, e);
                        for (int i = 0; i < 50 && receivingSocket != null; i++) {
                            Thread.sleep(200); // 50 * 200ms = 10sec
                        }
                    } else {
                        logger.warn("Unexpected error while listening on port {}", receivePort, e);
                    }
                }
            }
        }
    } // End listenUnhandledInterruption()

    /** Close the socket connection. */
    public void disconnect() {
        logger.debug("Tailwind UDP listener stopped for: '{}:{}'", host, receivePort);
        listener = null;
        final DatagramSocket receivingSocket2 = receivingSocket;
        if (receivingSocket2 != null) {
            receivingSocket = null;
            if (!receivingSocket2.isClosed()) {
                receivingSocket2.close(); // this interrupts and terminates the listening thread
            }
        }
        final DatagramSocket sendingSocket2 = sendingSocket;
        if (sendingSocket2 != null) {
            synchronized (this) {
                if (Objects.equals(sendingSocket, sendingSocket2)) {
                    sendingSocket = null;
                    if (!sendingSocket2.isClosed()) {
                        sendingSocket2.close();
                    }
                }
            }
        }
    } // End disconnect()

    public boolean isConnected() {
        return receivingSocket != null;
    } // End isConnected()

    public String getUdpConnectionName() {
        if (host != null) {
            return host + ":" + String.valueOf(receivePort);
        } else {
            return "tailwind:" + String.valueOf(receivePort);
        }
    }
}
