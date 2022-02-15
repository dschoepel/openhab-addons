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
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.nadavr.internal.NadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link NadIpConnector} is responsible for establishing/managing the IP telnet connection to
 * the NAD Device.
 *
 * @author Dave J Schoepel - Initial contribution
 */
@NonNullByDefault
public class NadIpConnector extends NadConnector {

    private final Logger logger = LoggerFactory.getLogger(NadIpConnector.class);

    private String address;
    private int port;

    private @Nullable Socket clientSocket;

    /**
     * Constructor for the NAD device thing's IP connection used to send and receive messages (commands).
     *
     * @param address - the IP address of the device
     * @param port - the TCP port to be used
     * @param readerThreadName - the name of thread to be created
     */
    public NadIpConnector(String address, Integer port, String msgReaderThreadName) {
        super(msgReaderThreadName);
        this.address = address;
        this.port = port;
    }

    @Override
    public void open() throws NadException {
        logger.debug("Opening IP connection on IP {} port {}", this.address, this.port);
        try {
            // Create the IP connection socket using IP address and port specified in the thing configuration
            Socket clientSocket = new Socket(this.address, this.port);
            clientSocket.setSoTimeout(100);
            // Create input and output streams for the IP connection
            dataOut = new DataOutputStream(clientSocket.getOutputStream());
            dataIn = new DataInputStream(clientSocket.getInputStream());
            // Create and start a thread to be used to send and receive messages with the NAD device
            Thread thread = new NadMsgReaderThread(this, msgReaderThreadName);
            setMsgReaderThread(thread);
            thread.start();
            // Assign the new socket to this connection
            this.clientSocket = clientSocket;
            // Indicate connection succeeded if now errors...
            setConnected(true);
            if (logger.isDebugEnabled()) {
                logger.debug("IP connection opened");
            }
        } catch (IOException | SecurityException | IllegalArgumentException e) {
            // Connection errors forwarded on to event listeners
            setConnected(false);
            throw new NadException("Opening IP connection failed", e);
        }
    }

    @Override
    public void close() {
        if (logger.isDebugEnabled()) {
            logger.debug("Closing IP connection");
        }
        super.cleanup();
        Socket clientSocket = this.clientSocket;
        if (clientSocket != null) {
            try {
                clientSocket.close();
            } catch (IOException e) {
            }
            this.clientSocket = null;
        }
        setConnected(false);
        if (logger.isDebugEnabled()) {
            logger.debug("IP connection closed");
        }
    }

    /**
     * Reads some number of bytes from the input stream and stores them into the buffer array b. The number of bytes
     * actually read is returned as an integer. In case of socket timeout, the returned value is 0.
     *
     * @param dataBuffer - the buffer into which the data is read.
     *
     * @return the total number of bytes read into the buffer, or -1 if there is no more data because the end of the
     *         stream has been reached.
     *
     * @throws NadException - If the input stream is null, if the first byte cannot be read for any reason
     *             other than the end of the file, if the input stream has been closed, or if some other I/O error
     *             occurs.
     * @throws InterruptedIOException - if the thread was interrupted during the reading of the input stream
     */
    @Override
    protected int readInput(byte[] dataBuffer) throws NadException, InterruptedIOException {
        InputStream dataIn = this.dataIn;
        if (dataIn == null) {
            throw new NadException("readInput failed: input stream is null");
        }
        try {
            return dataIn.read(dataBuffer);
        } catch (SocketTimeoutException e) {
            return 0;
        } catch (IOException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("readInput failed: {}", e.getMessage());
            }
            throw new NadException("readInput failed", e);
        }
    }

    /**
     * Method to return NAD Device connection name in the form of its IP address and port mostly used in diagnostic
     * messages.
     *
     * @return - ip:port as connection name
     */
    public String getConnectionName() {
        return address + ":" + port;
    }
}
