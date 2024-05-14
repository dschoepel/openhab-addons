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
package org.openhab.binding.tailwind.internal.not_used_archive;

import static org.openhab.binding.tailwind.internal.TailwindBindingConstants.TAILWIND_OPENHAB_HOST_UDP_PORT;

import java.util.ArrayList;
import java.util.List;

import org.openhab.binding.tailwind.internal.dto.TailwindControllerData;
import org.openhab.binding.tailwind.internal.not_used_archive.TailwindUdpServer.Event;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 *
 */
public class TailwindUdpEventReceiver implements TailwindUdpEventListenerService {

    Logger logger = LoggerFactory.getLogger(TailwindUdpEventReceiver.class);
    List<TailwindUdpEventListener> listeners = new ArrayList<>();
    TailwindUdpServer udpServer;
    final int openHabUdpServerPort = Integer.parseInt(TAILWIND_OPENHAB_HOST_UDP_PORT);

    public void TailwindUdpEventListenerService() {
        logger.warn("Starting Tailwind UDP listener on port: {}", openHabUdpServerPort);
    }

    void setupUdpServer() {
        udpServer = new TailwindUdpServer();
        udpServer.setPort(openHabUdpServerPort);
        udpServer.addUdpServerListener(new TailwindUdpServer.Listener() {

            @Override
            public void packetReceived(Event evt) {
                if (logger.isDebugEnabled()) {
                    logger.debug("UDP Server Received message {}.", evt.getPacketAsString());
                }
                processMessage(evt.getPacketAsString());
            }
        });
    }

    @Activate
    public void start() {
        setupUdpServer();
        udpServer.start();
    }

    @Deactivate
    public void stop() {
        udpServer.stop();
    }

    Gson gson = new Gson();

    private void processMessage(String data) {
        TailwindControllerData message = null;
        try {
            message = gson.fromJson(data, TailwindControllerData.class);
        } catch (Exception e) {
            logger.error("Unable to parse message from Tailwind controller {}", e);
        }
        if (message == null) {
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Sending message {} for {} listeners.", message, listeners.size());
        }
        for (TailwindUdpEventListener listener : listeners) {
            // listener.eventReceived(message);
        }
    }

    @Override
    public void registerListener(TailwindUdpEventListener listener) {
        listeners.add(listener);

    }

    @Override
    public void unRegisterListener(TailwindUdpEventListener listener) {
        listeners.remove(listener);
    }

}
