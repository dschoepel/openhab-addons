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
package org.openhab.binding.nadavr.internal;

import static org.openhab.binding.nadavr.internal.NADAvrBindingConstants.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link NADAvrState.java} class contains fields mapping thing configuration parameters.
 *
 * @author Dave J Schoepel - Initial contribution
 */
@NonNullByDefault
public class NADAvrState {

    private final Logger logger = LoggerFactory.getLogger(NADAvrState.class);

    // ----- Main ------
    private State mainPower = UnDefType.UNDEF;
    private State listeningMode = StringType.EMPTY;
    private State mute = UnDefType.UNDEF;
    private State mainVolume = DecimalType.ZERO;
    private State mainVolumeDB = DecimalType.ZERO;
    private State mainSource = StringType.EMPTY;

    // ----- Zone2 ------
    private State zone2Power = UnDefType.UNDEF;
    private State zone2Volume = DecimalType.ZERO;
    private State zone2VolumeDB = DecimalType.ZERO;
    private State zone2Mute = UnDefType.UNDEF;
    private State zone2Source = StringType.EMPTY;
    private State zone2VolumeFixed = DecimalType.ZERO;
    private State zone2VolumeFixedDB = DecimalType.ZERO;
    private State zone2VolumeControl = StringType.EMPTY;

    // ----- Zone3 ------
    private State zone3Power = UnDefType.UNDEF;
    private State zone3Volume = DecimalType.ZERO;
    private State zone3VolumeDB = DecimalType.ZERO;
    private State zone3Mute = UnDefType.UNDEF;
    private State zone3Source = StringType.EMPTY;
    private State zone3VolumeFixed = DecimalType.ZERO;
    private State zone3VolumeFixedDB = DecimalType.ZERO;
    private State zone3VolumeControl = StringType.EMPTY;

    // ----- Zone4 ------
    private State zone4Power = UnDefType.UNDEF;
    private State zone4Volume = DecimalType.ZERO;
    private State zone4VolumeDB = DecimalType.ZERO;
    private State zone4Mute = UnDefType.UNDEF;
    private State zone4Source = StringType.EMPTY;
    private State zone4VolumeFixed = StringType.EMPTY;
    private State zone4VolumeFixedDB = DecimalType.ZERO;
    private State zone4VolumeControl = StringType.EMPTY;

    private NADAvrStateChangedListener handler;

    /**
     *
     */
    public NADAvrState(NADAvrStateChangedListener handler) {
        this.handler = handler;
    }

    /**
     * @param errorMessage
     */
    public void connectionError(String errorMessage) {
        handler.connectionError(errorMessage);
    }

    /**
     * @param channelID
     * @return
     */
    public State getStateForChannelID(String channelID) {
        switch (channelID) {
            /**
             * Main zone
             */
            case CHANNEL_MAIN_POWER:
                return mainPower;
            case CHANNEL_MAIN_LISTENING_MODE:
                return listeningMode;
            case CHANNEL_MAIN_MUTE:
                return mute;
            case CHANNEL_MAIN_VOLUME:
                return mainVolume;
            case CHANNEL_MAIN_VOLUME_DB:
                return mainVolumeDB;
            case CHANNEL_MAIN_SOURCE:
                return mainSource;
            /**
             * Zone2
             */
            case CHANNEL_ZONE2_POWER:
                return zone2Power;
            case CHANNEL_ZONE2_MUTE:
                return zone2Mute;
            case CHANNEL_ZONE2_VOLUME:
                return zone2Volume;
            case CHANNEL_ZONE2_VOLUME_DB:
                return zone2VolumeDB;
            case CHANNEL_ZONE2_SOURCE:
                return zone2Source;
            case CHANNEL_ZONE2_VOLUME_FIXED_DB:
                return zone2VolumeFixedDB;
            case CHANNEL_ZONE2_VOLUME_FIXED:
                return zone2VolumeFixed;
            case CHANNEL_ZONE2_VOLUME_CONTROL:
                return zone2VolumeControl;
            /**
             * Zone3
             */
            case CHANNEL_ZONE3_POWER:
                return zone3Power;
            case CHANNEL_ZONE3_MUTE:
                return zone3Mute;
            case CHANNEL_ZONE3_VOLUME:
                return zone3Volume;
            case CHANNEL_ZONE3_VOLUME_DB:
                return zone3VolumeDB;
            case CHANNEL_ZONE3_SOURCE:
                return zone3Source;
            case CHANNEL_ZONE3_VOLUME_FIXED_DB:
                return zone3VolumeFixedDB;
            case CHANNEL_ZONE3_VOLUME_FIXED:
                return zone3VolumeFixed;
            case CHANNEL_ZONE3_VOLUME_CONTROL:
                return zone3VolumeControl;
            /**
             * Zone4
             */
            case CHANNEL_ZONE4_POWER:
                return zone4Power;
            case CHANNEL_ZONE4_MUTE:
                return zone4Mute;
            case CHANNEL_ZONE4_VOLUME:
                return zone4Volume;
            case CHANNEL_ZONE4_VOLUME_DB:
                return zone4VolumeDB;
            case CHANNEL_ZONE4_SOURCE:
                return zone4Source;
            case CHANNEL_ZONE4_VOLUME_FIXED_DB:
                return zone4VolumeFixedDB;
            case CHANNEL_ZONE4_VOLUME_FIXED:
                return zone4VolumeFixed;
            case CHANNEL_ZONE4_VOLUME_CONTROL:
                return zone4VolumeControl;
            default:
                return UnDefType.UNDEF;
        }
    }

    /**
     * @param prefix
     * @param powerSetting
     */
    public void setPower(String prefix, boolean powerSetting) {
        OnOffType newVal = powerSetting ? OnOffType.ON : OnOffType.OFF;
        switch (prefix) {
            case ZONE1:
                if (newVal != mainPower) {
                    this.mainPower = newVal;
                    handler.stateChanged(CHANNEL_MAIN_POWER, this.mainPower);
                }
                break;
            case ZONE2:
                if (newVal != zone2Power) {
                    this.zone2Power = newVal;
                    handler.stateChanged(CHANNEL_ZONE2_POWER, this.zone2Power);
                }
                break;
            case ZONE3:
                if (newVal != zone3Power) {
                    this.zone3Power = newVal;
                    handler.stateChanged(CHANNEL_ZONE3_POWER, this.zone3Power);
                }
                break;
            case ZONE4:
                if (newVal != zone4Power) {
                    this.zone4Power = newVal;
                    handler.stateChanged(CHANNEL_ZONE4_POWER, this.zone4Power);
                }
                break;
            default:
                break;
        }
    }

    /**
     * @param listeningMode
     */
    public void setListeningMode(String prefix, String listeningMode) {
        StringType newVal = StringType.valueOf(listeningMode);
        switch (prefix) {
            case ZONE1:
                if (!newVal.equals(this.listeningMode)) {
                    this.listeningMode = newVal;
                    handler.stateChanged(CHANNEL_MAIN_LISTENING_MODE, this.listeningMode);
                }
                break;
            default:
                break;
        }
    }

    /**
     * @param prefix
     * @param source
     */
    public void setSourceName(String prefix, String source) {
        StringType newVal = StringType.valueOf(source);
        logger.debug("The source name is: {} for zone: {}", source, prefix);
        switch (prefix) {
            case ZONE1:
                if (!newVal.equals(this.mainSource)) {
                    this.mainSource = newVal;
                    handler.stateChanged(CHANNEL_MAIN_SOURCE, this.mainSource);
                }
                break;
            case ZONE2:
                if (!newVal.equals(this.zone2Source)) {
                    this.zone2Source = newVal;
                    handler.stateChanged(CHANNEL_ZONE2_SOURCE, this.zone2Source);
                }
                break;
            case ZONE3:
                if (!newVal.equals(this.zone3Source)) {
                    this.zone3Source = newVal;
                    handler.stateChanged(CHANNEL_ZONE3_SOURCE, this.zone3Source);
                }
                break;
            case ZONE4:
                if (!newVal.equals(this.zone4Source)) {
                    this.zone4Source = newVal;
                    handler.stateChanged(CHANNEL_ZONE4_SOURCE, this.zone4Source);
                }
                break;
            default:
                break;
        }
    }

    /**
     * @param prefix
     * @param mute
     */
    public void setMute(String prefix, boolean mute) {
        OnOffType newVal = mute ? OnOffType.ON : OnOffType.OFF;
        switch (prefix) {
            case ZONE1:
                if (newVal != this.mute) {
                    this.mute = newVal;
                    handler.stateChanged(CHANNEL_MAIN_MUTE, this.mute);
                }
                break;
            case ZONE2:
                if (newVal != this.zone2Mute) {
                    this.zone2Mute = newVal;
                    handler.stateChanged(CHANNEL_ZONE2_MUTE, this.zone2Mute);
                }
                break;
            case ZONE3:
                if (newVal != this.zone3Mute) {
                    this.zone3Mute = newVal;
                    handler.stateChanged(CHANNEL_ZONE3_MUTE, this.zone3Mute);
                }
                break;
            case ZONE4:
                if (newVal != this.zone4Mute) {
                    this.zone4Mute = newVal;
                    handler.stateChanged(CHANNEL_ZONE4_MUTE, this.zone4Mute);
                }
                break;
            default:
                break;
        }
    }

    /**
     * @param prefix
     * @param volume
     */
    public void setVolume(String prefix, BigDecimal volume) {
        DecimalType newVal = new DecimalType(volume);
        switch (prefix) {
            case ZONE1:
                if (!newVal.equals(this.mainVolumeDB)) {
                    this.mainVolumeDB = newVal;
                    handler.stateChanged(CHANNEL_MAIN_VOLUME_DB, this.mainVolumeDB);
                    PercentType newVolumePercent = new PercentType(calculateVolumePercent(volume));
                    if (!newVolumePercent.equals(this.mainVolume)) {
                        this.mainVolume = newVolumePercent;
                        handler.stateChanged(CHANNEL_MAIN_VOLUME, this.mainVolume);
                    }
                }
                break;
            case ZONE2:
                if (!newVal.equals(this.zone2VolumeDB)) {
                    this.zone2VolumeDB = newVal;
                    handler.stateChanged(CHANNEL_ZONE2_VOLUME_DB, this.zone2VolumeDB);
                    PercentType newVolumePercent = new PercentType(calculateVolumePercent(volume));
                    if (!newVolumePercent.equals(this.zone2Volume)) {
                        this.zone2Volume = newVolumePercent;
                        handler.stateChanged(CHANNEL_ZONE2_VOLUME, this.zone2Volume);
                    }
                }
                break;
            case ZONE3:
                if (!newVal.equals(this.zone3VolumeDB)) {
                    this.zone3VolumeDB = newVal;
                    handler.stateChanged(CHANNEL_ZONE3_VOLUME_DB, this.zone3VolumeDB);
                    PercentType newVolumePercent = new PercentType(calculateVolumePercent(volume));
                    if (!newVolumePercent.equals(this.zone3Volume)) {
                        this.zone3Volume = newVolumePercent;
                        handler.stateChanged(CHANNEL_ZONE3_VOLUME, this.zone3Volume);
                    }
                }
                break;
            case ZONE4:
                if (!newVal.equals(this.zone4VolumeDB)) {
                    this.zone4VolumeDB = newVal;
                    handler.stateChanged(CHANNEL_ZONE4_VOLUME_DB, this.zone4VolumeDB);
                    PercentType newVolumePercent = new PercentType(calculateVolumePercent(volume));
                    if (!newVolumePercent.equals(this.zone4Volume)) {
                        this.zone4Volume = newVolumePercent;
                        handler.stateChanged(CHANNEL_ZONE4_VOLUME, this.zone4Volume);
                    }
                }
                break;
            default:
                break;
        }
    }

    /**
     * Sets fixed volume for Zone 2
     *
     * @param volume (Range limits are -95 to +16)
     */
    public void setVolumeFixed(String prefix, BigDecimal volume) {
        DecimalType newVal = new DecimalType(volume);
        switch (prefix) {
            case ZONE2:
                if (!newVal.equals(this.zone2VolumeFixedDB)) {
                    this.zone2VolumeFixedDB = newVal;
                    handler.stateChanged(CHANNEL_ZONE2_VOLUME_FIXED_DB, this.zone2VolumeFixedDB);
                    PercentType newVolumePercent = new PercentType(calculateVolumePercent(volume));
                    if (!newVolumePercent.equals(this.zone2VolumeFixed)) {
                        this.zone2VolumeFixed = newVolumePercent;
                        handler.stateChanged(CHANNEL_ZONE2_VOLUME_FIXED, this.zone2VolumeFixed);
                    }
                }
                break;
            case ZONE3:
                if (!newVal.equals(this.zone3VolumeFixedDB)) {
                    this.zone3VolumeFixedDB = newVal;
                    handler.stateChanged(CHANNEL_ZONE3_VOLUME_FIXED_DB, this.zone3VolumeFixedDB);
                    PercentType newVolumePercent = new PercentType(calculateVolumePercent(volume));
                    if (!newVolumePercent.equals(this.zone3VolumeFixed)) {
                        this.zone3VolumeFixed = newVolumePercent;
                        handler.stateChanged(CHANNEL_ZONE3_VOLUME_FIXED, this.zone3VolumeFixed);
                    }
                }
                break;
            case ZONE4:
                if (!newVal.equals(this.zone4VolumeFixedDB)) {
                    this.zone4VolumeFixedDB = newVal;
                    handler.stateChanged(CHANNEL_ZONE4_VOLUME_FIXED_DB, this.zone4VolumeFixedDB);
                    PercentType newVolumePercent = new PercentType(calculateVolumePercent(volume));
                    if (!newVolumePercent.equals(this.zone4VolumeFixed)) {
                        this.zone4VolumeFixed = newVolumePercent;
                        handler.stateChanged(CHANNEL_ZONE4_VOLUME_FIXED, this.zone4VolumeFixed);
                    }
                }
                break;
            default:
                break;
        }
    }

    /**
     * @param prefix
     * @param volumeControl
     */
    public void setVolumeControl(String prefix, String volumeControl) {
        StringType newVal = StringType.valueOf(volumeControl);
        switch (prefix) {
            case ZONE2:
                if (!newVal.equals(this.zone2VolumeControl)) {
                    this.zone2VolumeControl = newVal;
                    handler.stateChanged(CHANNEL_ZONE2_VOLUME_CONTROL, this.zone2VolumeControl);
                }
                break;
            case ZONE3:
                if (!newVal.equals(this.zone3VolumeControl)) {
                    this.zone3VolumeControl = newVal;
                    handler.stateChanged(CHANNEL_ZONE3_VOLUME_CONTROL, this.zone3VolumeControl);
                }
                break;
            case ZONE4:
                if (!newVal.equals(this.zone4VolumeControl)) {
                    this.zone4VolumeControl = newVal;
                    handler.stateChanged(CHANNEL_ZONE4_VOLUME_CONTROL, this.zone4VolumeControl);
                }
                break;
            default:
                break;
        }
    }

    /**
     * Convert sound volume in decibels to a percentage
     *
     * @param volume
     * @return
     */
    private BigDecimal calculateVolumePercent(BigDecimal volume) {
        BigDecimal volumePct = volume.subtract(VOLUME_DB_MIN);
        BigDecimal volFactor = ONE_HUNDRED.divide(VOLUME_DB_RANGE, 8, RoundingMode.HALF_EVEN);
        BigDecimal volumePercent = volumePct.multiply(volFactor).divide(new BigDecimal(1), 0, RoundingMode.HALF_EVEN);
        return volumePercent;
    }
}
