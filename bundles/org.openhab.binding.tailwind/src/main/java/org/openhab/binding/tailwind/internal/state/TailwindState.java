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
package org.openhab.binding.tailwind.internal.state;

import static org.openhab.binding.tailwind.internal.TailwindBindingConstants.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link TailwindState.java} class represents/sets the state of channels (associated items) of a TailWind thing.
 *
 * @author Dave J Schoepel - Initial contribution
 */
@NonNullByDefault
public class TailwindState {

    private final Logger logger = LoggerFactory.getLogger(TailwindState.class);

    // ----- General (Controller Specific) -----
    private State doorNum = DecimalType.ZERO;
    private State nightModeEnabled = DecimalType.ZERO;
    private State ledBrightness = DecimalType.ZERO;
    private State routerRSSI = DecimalType.ZERO;
    private State productID = StringType.EMPTY;
    private State deviceID = StringType.EMPTY;
    private State firmwareVersion = StringType.EMPTY;

    // ----- Door One Specific Channels -----
    private State doorOneIndex = DecimalType.ZERO;
    private State doorOneStatus = StringType.EMPTY;
    private State doorOneLockup = DecimalType.ZERO;
    private State doorOneDisabled = DecimalType.ZERO;

    // ----- Door Two Specific Channels -----
    private State doorTwoIndex = DecimalType.ZERO;
    private State doorTwoStatus = StringType.EMPTY;
    private State doorTwoLockup = DecimalType.ZERO;
    private State doorTwoDisabled = DecimalType.ZERO;

    // ----- Door Three Specific Channels -----
    private State doorThreeIndex = DecimalType.ZERO;
    private State doorThreeStatus = StringType.EMPTY;
    private State doorThreeLockup = DecimalType.ZERO;
    private State doorThreeDisabled = DecimalType.ZERO;

    private TailwindStateChangedListener handler;

    /**
     * TailwindState constructor associates the state changes to the handler for a thing
     *
     * @param handler - handler for this Tailwind thing
     */
    public TailwindState(TailwindStateChangedListener handler) {
        this.handler = handler;
    }

    /**
     * @param errorMessage
     */
    public void connectionError(String errorMessage) {
        handler.connectionError(errorMessage);
    }

    /**
     * Method to get state for NAD AVR thing channels
     *
     * @param channelID - thing channels supported by this binding
     * @return - current state for the channel
     */
    public State getStateForChannelID(String channelID) {
        switch (channelID) {
            /**
             * General (Controller Channels)
             */
            case CHANNEL_DOOR_NUM:
                return doorNum;
            case CHANNEL_NIGHT_MODE_ENABLED:
                return nightModeEnabled;
            case CHANNEL_LED_BRIGHTNESS:
                return ledBrightness;
            case CHANNEL_ROUTER_RSSI:
                return routerRSSI;
            case CHANNEL_PRODUCT_ID:
                return productID;
            case CHANNEL_DEVICE_ID:
                return deviceID;
            case CHANNEL_FIRMWARE_VERSION:
                return firmwareVersion;
            /**
             * Door One Channels
             */
            case CHANNEL_DOOR_1_CONTROLS_INDEX:
                return doorOneIndex;
            case CHANNEL_DOOR_1_CONTROLS_STATUS:
                return doorOneStatus;
            case CHANNEL_DOOR_1_CONTROLS_LOCKUP:
                return doorOneLockup;
            case CHANNEL_DOOR_1_CONTROLS_DISABLED:
                return doorOneDisabled;
            /**
             * Door Two Channels
             */
            case CHANNEL_DOOR_2_CONTROLS_INDEX:
                return doorTwoIndex;
            case CHANNEL_DOOR_2_CONTROLS_STATUS:
                return doorTwoStatus;
            case CHANNEL_DOOR_2_CONTROLS_LOCKUP:
                return doorTwoLockup;
            case CHANNEL_DOOR_2_CONTROLS_DISABLED:
                return doorTwoDisabled;
            /**
             * Door Three Channels
             */
            case CHANNEL_DOOR_3_CONTROLS_INDEX:
                return doorThreeIndex;
            case CHANNEL_DOOR_3_CONTROLS_STATUS:
                return doorThreeStatus;
            case CHANNEL_DOOR_3_CONTROLS_LOCKUP:
                return doorThreeLockup;
            case CHANNEL_DOOR_3_CONTROLS_DISABLED:
                return doorThreeDisabled;
            default:
                return UnDefType.UNDEF;
        } // Switch/case for ChannelID
    }

    /**
     * Method to set Number of Doors state for the TailWind Controller
     *
     * @param doorNum - Active listening mode to be set
     */
    public void setDoorNum(String doorNum) {
        DecimalType newVal = DecimalType.valueOf(doorNum);
        if (!newVal.equals(this.doorNum)) {
            this.doorNum = newVal;
            handler.stateChanged(CHANNEL_DOOR_NUM, this.doorNum);

        } // If doorNum changed
    }

}
