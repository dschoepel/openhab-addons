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
package org.openhab.binding.tailwind.internal.discovery;

import static org.openhab.binding.tailwind.internal.TailwindBindingConstants.SUPPORTED_THING_TYPE_UIDS;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import javax.jmdns.ServiceInfo;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.mdns.MDNSDiscoveryParticipant;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
@Component(service = MDNSDiscoveryParticipant.class)
public class TailwindDiscoveryParticipant implements MDNSDiscoveryParticipant {

    private Logger logger = LoggerFactory.getLogger(TailwindDiscoveryParticipant.class);

    // Service type for HTTP enabled Tailwind garage controllers
    private static final String HTTP_SERVICE_TYPE = "_http._tcp.local.";

    private boolean isAutoDiscoveryEnabled;
    private Set<ThingTypeUID> supportedThingTypes;

    /**
     * Match the details of the discovered Tailwind controller.
     * Input is like "NAD T787 (824F01F2)._telnet._tcp.local."
     * Vendor is group 1, Model is group 2, and Serial number (last 8 digits of MAC address)
     * Alternate: "^([a-zA-Z]+) (T[0-9]+) \\(([^)]*)\\)\\._telnet\\._tcp\\.local\\.$"
     */

    private static final Pattern TAILWIND_CONTROLLER_PATTERN = Pattern
            .compile("^(Tailwind) (T[0-9]+) \\(([^)]*)\\)\\._telnet\\._tcp\\.local\\.$");

    private static final Pattern TAILWIND_HOSTNAME_PATTERN = Pattern.compile("^([a-zA-Z0-9-]+)\\.local\\.$");

    public TailwindDiscoveryParticipant() {
        this.isAutoDiscoveryEnabled = true;
        this.supportedThingTypes = SUPPORTED_THING_TYPE_UIDS;
    }

    @Activate
    protected void activate(ComponentContext componentContext) {
        if (componentContext.getProperties() != null) {
            String autoDiscoveryPropertyValue = (String) componentContext.getProperties().get("enableAutoDiscovery");
            if (autoDiscoveryPropertyValue != null && !autoDiscoveryPropertyValue.isEmpty()) {
                isAutoDiscoveryEnabled = Boolean.valueOf(autoDiscoveryPropertyValue);
            }
        }
        supportedThingTypes = isAutoDiscoveryEnabled ? SUPPORTED_THING_TYPE_UIDS : new HashSet<>();
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypeUIDs() {
        return supportedThingTypes;
    }

    @Override
    public String getServiceType() {
        return HTTP_SERVICE_TYPE;
    }

    @Override
    public @Nullable DiscoveryResult createResult(ServiceInfo serviceInfo) {
        // TODO Auto-generated method stub
        if (!serviceInfo.hasData()) {
            if (logger.isDebugEnabled()) {
                logger.debug("ServiceInfo does not have data");
            }
            return null;
        }
        // qualified name is in format like "???._http._tcp.local."
        String qualifiedName = serviceInfo.getQualifiedName();
        if (logger.isDebugEnabled()) {
            logger.debug("Tailwind controller found: {}", qualifiedName);
        }
        // The http server of the discovered device
        String server = serviceInfo.getServer();
        // port being listened to by http server
        int port = serviceInfo.getPort();
        // Internet Protocol address for discovered device
        InetAddress[] ipAddresses = serviceInfo.getInetAddresses();
        if (logger.isDebugEnabled()) {
            logger.debug("Tailwind mDNS service qualifiedName: {}, server: {}, port: {}, ipAddresses: {} ({})",
                    qualifiedName, server, port, ipAddresses, ipAddresses.length);
        }
        // TODO getThingUID steps
        return null;
    }

    @Override
    public @Nullable ThingUID getThingUID(ServiceInfo service) {
        // TODO Auto-generated method stub
        return null;
    }

}
