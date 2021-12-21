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
package org.openhab.binding.nadavr.internal.discovery;

import static org.openhab.binding.nadavr.internal.NADAvrBindingConstants.*;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jmdns.ServiceInfo;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.nadavr.internal.NADModel;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.config.discovery.mdns.MDNSDiscoveryParticipant;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link NADDiscoveryParticipant} class contains fields mapping thing configuration parameters.
 *
 * @author Dave J Schoepel - Initial contribution
 */
@NonNullByDefault
@Component(service = MDNSDiscoveryParticipant.class)
public class NADAvrDiscoveryParticipant implements MDNSDiscoveryParticipant {

    private Logger logger = LoggerFactory.getLogger(NADAvrDiscoveryParticipant.class);

    // Service type for LAN enabled NAD receivers
    private static final String TELNET_SERVICE_TYPE = "_telnet._tcp.local.";

    private boolean isAutoDiscoveryEnabled;
    private Set<ThingTypeUID> supportedThingTypes;

    /**
     * Match the serial number, vendor and model of the discovered AVR.
     * Input is like "NAD T787 (824F01F2)._telnet._tcp.local."
     * Vendor is group 1, Model is group 2, and Serial number (last 8 digits of MAC address)
     * Alternate: "^([a-zA-Z]+) (T[0-9]+) \\(([^)]*)\\)\\._telnet\\._tcp\\.local\\.$"
     */
    private static final Pattern NAD_AVR_PATTERN = Pattern
            .compile("^(NAD) (T[0-9]+) \\(([^)]*)\\)\\._telnet\\._tcp\\.local\\.$");

    private static final Pattern NAD_AVR_HOSTNAME_PATTERN = Pattern.compile("^([a-zA-Z0-9-]+)\\.local\\.$");

    public NADAvrDiscoveryParticipant() {
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
    public Set<@NonNull ThingTypeUID> getSupportedThingTypeUIDs() {
        return supportedThingTypes;
    }

    @Override
    public String getServiceType() {
        return TELNET_SERVICE_TYPE;
    }

    @Override
    public @Nullable DiscoveryResult createResult(ServiceInfo serviceInfo) {
        if (!serviceInfo.hasData()) {
            logger.debug("ServiceInfo does not have data");
            return null;
        }
        String qualifiedName = serviceInfo.getQualifiedName();
        logger.debug("AVR found: {}", qualifiedName);

        String server = serviceInfo.getServer();

        int port = serviceInfo.getPort();

        InetAddress[] ipAddresses = serviceInfo.getInetAddresses();

        logger.debug("NAD mDNS service qualifiedName: {}, server: {}, port: {}, ipAddresses: {} ({})", qualifiedName,
                server, port, ipAddresses, ipAddresses.length);

        ThingUID thingUID = getThingUID(serviceInfo);
        logger.debug("ThingUID = {}", thingUID);
        if (thingUID != null) {
            Matcher matcher = NAD_AVR_PATTERN.matcher(qualifiedName);
            matcher.matches(); // we already know it matches, it was matched in getThingUID
            String serial = matcher.group(3).toLowerCase();
            String vendor = matcher.group(1).trim();
            String model = matcher.group(2).trim();
            if (serviceInfo.getHostAddresses().length == 0) {
                logger.debug("Could not determine IP address for the NAD AVR");
                return null;
            }
            Matcher matchHostName = NAD_AVR_HOSTNAME_PATTERN.matcher(server);
            String hostName = "";
            if (matchHostName.matches()) {
                hostName = matchHostName.group(1);

            } else {
                logger.debug("Could not match hostname: {}", server);
            }

            String ipAddress = serviceInfo.getHostAddresses()[0];
            logger.debug("IP Address: {}", ipAddress);

            Map<String, Object> properties = new HashMap<>(2);

            properties.put(PARAMETER_HOST, hostName);
            properties.put(PARAMETER_IP_ADDRESS, ipAddress);
            properties.put(Thing.PROPERTY_SERIAL_NUMBER, serial);
            properties.put(Thing.PROPERTY_VENDOR, vendor);
            properties.put(Thing.PROPERTY_MODEL_ID, model);
            String maxZones = String.valueOf(getMaxZonesForModel(model));
            properties.put(PARAMETER_MAX_ZONES, maxZones);

            String label = vendor + " " + model;

            DiscoveryResult result = DiscoveryResultBuilder.create(thingUID).withProperties(properties).withLabel(label)
                    .withRepresentationProperty(Thing.PROPERTY_SERIAL_NUMBER).build();
            return result;
        } else {
            return null;
        }
    }

    @Override
    public @Nullable ThingUID getThingUID(ServiceInfo service) {
        // if (isAutoDiscoveryEnabled) {
        Matcher matcher = NAD_AVR_PATTERN.matcher(service.getQualifiedName());
        if (matcher.matches()) {
            logger.debug("This seems like a supported NAD A/V Receiver!");
            String serial = matcher.group(3).toLowerCase();
            ThingTypeUID thingTypeUID = findThingType(matcher.group(2));
            // TODO remove serial number?
            return new ThingUID(thingTypeUID, serial);
        } else {
            logger.debug("This discovered device is not supported by the NAD binding: {}", service.getQualifiedName());
        }
        // }
        return null;
    }

    private ThingTypeUID findThingType(@Nullable String deviceModel) {
        ThingTypeUID thingTypeUID = THING_TYPE_NAD_UNSUPPORTED;

        for (ThingTypeUID thingType : SUPPORTED_THING_TYPE_UIDS) {
            if (thingType.getId().equalsIgnoreCase(deviceModel)) {
                return thingType;
            }
        }
        if (isSupportedDeviceModel(deviceModel)) {
            thingTypeUID = THING_TYPE_NADAVR;
        }

        return thingTypeUID;
    }

    /**
     * Return true only if the given device model is supported.
     *
     * @param deviceModel
     * @return
     */
    private boolean isSupportedDeviceModel(final @Nullable String deviceModel) {
        boolean isSupported = false;
        if (deviceModel != null && !deviceModel.isBlank()) {
            // List<NADModel> models = Arrays.asList(NADModel.values());
            List<NADModel> models = new ArrayList<NADModel>(EnumSet.allOf(NADModel.class));
            ListIterator<NADModel> modelIterator = models.listIterator();
            while (modelIterator.hasNext() && !isSupported) {
                isSupported = deviceModel.equalsIgnoreCase(modelIterator.toString());
            }
        }
        return isSupported;
        // return deviceModel != null && !deviceModel.isBlank() && Arrays.stream(NADModel.values())
        // .anyMatch(model -> StringUtils.startsWithIgnoreCase(deviceModel, model.getId()));
    }

    /**
     * @param model
     * @return
     */
    private int getMaxZonesForModel(String model) {
        int maxZones = 2;
        for (NADModel supportedModel : NADModel.values()) {
            if (supportedModel.getId().equals(model)) {
                maxZones = supportedModel.getMaxZones();
            }
        }
        return maxZones;
    }
}
