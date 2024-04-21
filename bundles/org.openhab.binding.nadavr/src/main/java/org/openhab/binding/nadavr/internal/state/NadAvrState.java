/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.binding.nadavr.internal.state;

import static org.openhab.binding.nadavr.internal.NadAvrBindingConstants.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.nadavr.internal.xml.NadPreset;
import org.openhab.binding.nadavr.internal.xml.NadTunerPresets;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link NadAvrState.java} class represents/sets the state of channels (associated items) of an NAD AVR thing.
 *
 * @author Dave J Schoepel - Initial contribution
 */
@NonNullByDefault
public class NadAvrState {

    private final Logger logger = LoggerFactory.getLogger(NadAvrState.class);
    private NadTunerPresets tunerPresets = new NadTunerPresets();

    // ----- General ------
    private State tunerBand = StringType.EMPTY;
    private State tunerFMFrequency = DecimalType.ZERO;
    private State tunerAMFrequency = DecimalType.ZERO;
    private State tunerFMMute = UnDefType.UNDEF;
    private State tunerPreset = StringType.EMPTY;
    private State tunerPresetDetail = UnDefType.UNDEF;
    private State tunerFMRdsText = StringType.EMPTY;
    private State tunerXMChannel = StringType.EMPTY;
    private State tunerXMChannelName = new StringType(" ");
    private State tunerXMName = new StringType(" ");
    private State tunerXMSongTitle = new StringType(" ");
    private State tunerDABDlsText = StringType.EMPTY;
    private State tunerDABServiceName = StringType.EMPTY;

    // ----- Main ------
    private State mainPower = UnDefType.UNDEF;
    private State mainModel = StringType.EMPTY;
    private State listeningMode = StringType.EMPTY;
    private State mute = UnDefType.UNDEF;
    private State mainVolume = DecimalType.ZERO;
    private State mainVolumeDB = new DecimalType(VOLUME_TO_INITIALIZE_STATE);
    private State mainSource = StringType.EMPTY;

    // ----- Zone2 ------
    private State zone2Power = UnDefType.UNDEF;
    private State zone2Volume = DecimalType.ZERO;
    private State zone2VolumeDB = new DecimalType(VOLUME_TO_INITIALIZE_STATE);
    private State zone2Mute = UnDefType.UNDEF;
    private State zone2Source = StringType.EMPTY;
    private State zone2VolumeFixed = DecimalType.ZERO;
    private State zone2VolumeFixedDB = new DecimalType(VOLUME_TO_INITIALIZE_STATE);
    private State zone2VolumeControl = StringType.EMPTY;

    // ----- Zone3 ------
    private State zone3Power = UnDefType.UNDEF;
    private State zone3Volume = DecimalType.ZERO;
    private State zone3VolumeDB = new DecimalType(VOLUME_TO_INITIALIZE_STATE);
    private State zone3Mute = UnDefType.UNDEF;
    private State zone3Source = StringType.EMPTY;
    private State zone3VolumeFixed = DecimalType.ZERO;
    private State zone3VolumeFixedDB = new DecimalType(VOLUME_TO_INITIALIZE_STATE);
    private State zone3VolumeControl = StringType.EMPTY;

    // ----- Zone4 ------
    private State zone4Power = UnDefType.UNDEF;
    private State zone4Volume = DecimalType.ZERO;
    private State zone4VolumeDB = new DecimalType(VOLUME_TO_INITIALIZE_STATE);
    private State zone4Mute = UnDefType.UNDEF;
    private State zone4Source = StringType.EMPTY;
    private State zone4VolumeFixed = StringType.EMPTY;
    private State zone4VolumeFixedDB = new DecimalType(VOLUME_TO_INITIALIZE_STATE);
    private State zone4VolumeControl = StringType.EMPTY;

    private NadAvrStateChangedListener handler;

    /**
     * NadAvrState constructor associates the state changes to the handler for a thing
     *
     * @param handler - handler for this NAD AVR thing
     */
    public NadAvrState(NadAvrStateChangedListener handler) {
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
             * General
             */
            case CHANNEL_TUNER_BAND:
                return tunerBand;
            case CHANNEL_TUNER_FM_FREQUENCY:
                return tunerFMFrequency;
            case CHANNEL_TUNER_AM_FREQUENCY:
                return tunerAMFrequency;
            case CHANNEL_TUNER_FM_MUTE:
                return tunerFMMute;
            case CHANNEL_TUNER_PRESET:
                return tunerPreset;
            case CHANNEL_TUNER_XM_CHANNEL:
                return tunerXMChannel;
            case CHANNEL_TUNER_XM_CHANNEL_NAME:
                return tunerXMChannelName;
            case CHANNEL_TUNER_XM_NAME:
                return tunerXMName;
            case CHANNEL_TUNER_XM_SONG_TITLE:
                return tunerXMSongTitle;
            case CHANNEL_TUNER_DAB_DLS_TEXT:
                return tunerDABDlsText;
            case CHANNEL_TUNER_DAB_SERVICE_NAME:
                return tunerDABServiceName;
            /**
             * Main zone
             */
            case CHANNEL_MAIN_POWER:
                return mainPower;
            case CHANNEL_MAIN_MODEL:
                return mainModel;
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
     * Method to set power state for NAD AVR zone
     *
     * @param prefix - zone to set power state
     * @param powerSetting - true is ON, false is Off
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
     * Method to set model name state for NAD AVR zone
     *
     * @param prefix - zone to set model name
     * @param modelName - AVR ressponse to model name query
     */
    public void setModelName(String prefix, String modelName) {
        StringType newVal = StringType.valueOf(modelName);
        switch (prefix) {
            case ZONE1:
                if (!newVal.equals(mainModel)) {
                    this.mainModel = newVal;
                    handler.stateChanged(CHANNEL_MAIN_MODEL, this.mainModel);
                }
                break;
            // case ZONE2:
            // if (newVal != zone2Power) {
            // this.zone2Power = newVal;
            // handler.stateChanged(CHANNEL_ZONE2_POWER, this.zone2Power);
            // }
            // break;
            // case ZONE3:
            // if (newVal != zone3Power) {
            // this.zone3Power = newVal;
            // handler.stateChanged(CHANNEL_ZONE3_POWER, this.zone3Power);
            // }
            // break;
            // case ZONE4:
            // if (newVal != zone4Power) {
            // this.zone4Power = newVal;
            // handler.stateChanged(CHANNEL_ZONE4_POWER, this.zone4Power);
            // }
            // break;
            default:
                break;
        }
    }

    /**
     * Method to set Listening Mode state for the NAD AVR
     *
     * @param prefix - zone (Main)
     * @param listeningMode - Active listening mode to be set
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
     * Method to change the tuner broadcast band
     *
     * @param prefix - tuner
     * @param tunerBand - broadcast band to be set (AM, FM, XM, or DAB)
     */
    public void setTunerBand(String prefix, String tunerBand) {
        StringType newVal = StringType.valueOf(tunerBand);
        switch (prefix) {
            case TUNER:
                if (!newVal.equals(this.tunerBand)) {
                    this.tunerBand = newVal;
                    handler.stateChanged(CHANNEL_TUNER_BAND, this.tunerBand);
                }
                break;
            default:
                break;
        }
    }

    /**
     * Method to update the FM Radio Data Service (RDS) text stream
     *
     * @param prefix - tuner
     * @param tunerFMRdsText - text received from currently active FM station supporting RDSs
     */
    public void setTunerFMRdsText(String prefix, String tunerFMRdsText) {
        StringType newVal = StringType.valueOf(tunerFMRdsText);
        switch (prefix) {
            case TUNER:
                if (!newVal.equals(this.tunerFMRdsText)) {
                    this.tunerFMRdsText = newVal;
                    handler.stateChanged(CHANNEL_TUNER_FM_RDS_TEXT, this.tunerFMRdsText);
                } else if (this.tunerFMRdsText.equals(StringType.EMPTY)) {
                    handler.stateChanged(CHANNEL_TUNER_FM_RDS_TEXT, StringType.valueOf(""));
                }
                break;
            default:
                break;
        }
    }

    /**
     * Method to set the tuner FM Mute mode to filter out stations with weak signals
     *
     * @param prefix - tuner
     * @param tunerFMMute - true is On (filter weak signals), false is Off (no weak signal filter)
     */
    public void setTunerFMMute(String prefix, boolean tunerFMMute) {
        OnOffType newVal = tunerFMMute ? OnOffType.ON : OnOffType.OFF;
        switch (prefix) {
            case TUNER:
                if (!newVal.equals(this.tunerFMMute)) {
                    this.tunerFMMute = newVal;
                    handler.stateChanged(CHANNEL_TUNER_FM_MUTE, this.tunerFMMute);
                }
                break;
            default:
                break;
        }
    }

    /**
     * Method to set the tuners FM station frequency MHz
     *
     * @param prefix - tuner
     * @param tunerFMFrequency - range is 88.1-108.1 MHz
     */
    public void setTunerFMFrequency(String prefix, BigDecimal tunerFMFrequency) {
        DecimalType newVal = new DecimalType(tunerFMFrequency);
        switch (prefix) {
            case TUNER:
                if (!newVal.equals(this.tunerFMFrequency)) {
                    this.tunerFMFrequency = newVal;
                    handler.stateChanged(CHANNEL_TUNER_FM_FREQUENCY, this.tunerFMFrequency);
                }
                break;
            default:
                break;
        }
    }

    /**
     * Method to select pre-programmed tuner preset and optionally retrieve user supplied details
     *
     * @param prefix - tuner
     * @param tunerPreset - number from 1 - 40
     * @param fileName - optional file provided in thing config with preset details
     */
    public void setTunerPreset(String prefix, String tunerPreset, String fileName) {
        StringType newVal = new StringType(tunerPreset);
        StringType newPresetDetailVal = getPresetDetail(newVal, fileName);
        switch (prefix) {
            case TUNER:
                if (!newVal.equals(this.tunerPreset)) {
                    this.tunerPreset = newVal;
                    handler.stateChanged(CHANNEL_TUNER_PRESET, this.tunerPreset);
                }
                if (!fileName.isBlank()) {
                    if (!newPresetDetailVal.equals(this.tunerPresetDetail)) {
                        this.tunerPresetDetail = newPresetDetailVal;
                        handler.stateChanged(CHANNEL_TUNER_PRESET_DETAIL, newPresetDetailVal);
                    }
                } else {
                    newPresetDetailVal = StringType.valueOf(UnDefType.UNDEF.toString());
                    handler.stateChanged(CHANNEL_TUNER_PRESET_DETAIL, newPresetDetailVal);
                }
                break;
            default:
                break;
        }
    }

    /**
     * Method to set the tuners AM station frequency kHz
     *
     * @param prefix - tuner
     * @param tunerAMFrequency - number from 540-1700 kHz
     */
    public void setTunerAMFrequency(String prefix, BigDecimal tunerAMFrequency) {
        DecimalType newVal = new DecimalType(tunerAMFrequency);
        switch (prefix) {
            case TUNER:
                if (!newVal.equals(this.tunerAMFrequency)) {
                    this.tunerAMFrequency = newVal;
                    handler.stateChanged(CHANNEL_TUNER_AM_FREQUENCY, this.tunerAMFrequency);
                }
                break;
            default:
                break;
        }
    }

    /**
     * Method to change the tuners XM station channel
     *
     * @param prefix - tuner
     * @param tunerXMChannel - string None, numbers from 0-255
     */
    public void setTunerXMChannel(String prefix, String tunerXMChannel) {
        StringType newVal = new StringType(tunerXMChannel);
        switch (prefix) {
            case TUNER:
                if (!newVal.equals(this.tunerXMChannel)) {
                    this.tunerXMChannel = newVal;
                    handler.stateChanged(CHANNEL_TUNER_XM_CHANNEL, this.tunerXMChannel);
                }
        }
    }

    /**
     * method to update the tuners XM Channel name
     *
     * @param prefix - tuner
     * @param tunerXMChannelName - read only text received from the XM station
     */
    public void setTunerXMChannelName(String prefix, String tunerXMChannelName) {
        StringType newVal = new StringType(tunerXMChannelName);
        switch (prefix) {
            case TUNER:
                if (!newVal.equals(this.tunerXMChannelName)) {
                    this.tunerXMChannelName = newVal;
                    handler.stateChanged(CHANNEL_TUNER_XM_CHANNEL_NAME, this.tunerXMChannelName);
                }
        }
    }

    /**
     * Method to update the tuners XM channel performing artist name
     *
     * @param prefix - tuner
     * @param tunerXMName - XM performing artist name
     */
    public void setTunerXMName(String prefix, String tunerXMName) {
        StringType newVal = new StringType(tunerXMName);
        if (tunerXMName.isBlank()) {
            newVal = StringType.valueOf("  ");
        }
        switch (prefix) {
            case TUNER:
                if (!newVal.equals(this.tunerXMName)) {
                    this.tunerXMName = newVal;
                    handler.stateChanged(CHANNEL_TUNER_XM_NAME, this.tunerXMName);
                }
        }
    }

    /**
     * Method to update the tuners XM channel currently playing song title
     *
     * @param prefix - tuner
     * @param tunerXMSongTitle - XM song title
     */
    public void setTunerXMSongTitle(String prefix, String tunerXMSongTitle) {
        StringType newVal = new StringType(tunerXMSongTitle);
        switch (prefix) {
            case TUNER:
                if (!newVal.equals(this.tunerXMSongTitle)) {
                    this.tunerXMSongTitle = newVal;
                    handler.stateChanged(CHANNEL_TUNER_XM_SONG_TITLE, this.tunerXMSongTitle);
                }
        }
    }

    /**
     * Method to update the DAB broadcast bands Dynamic Label Segment (DLS) text feed for information on music titles,
     * program or station
     *
     * @param prefix - tuner
     * @param tunerDABDlsText - text feed for information on music titles, program or station
     */
    public void setTunerDABDlsText(String prefix, String tunerDABDlsText) {
        StringType newVal = StringType.valueOf(tunerDABDlsText);
        switch (prefix) {
            case TUNER:
                if (!newVal.equals(this.tunerDABDlsText)) {
                    this.tunerDABDlsText = newVal;
                    handler.stateChanged(CHANNEL_TUNER_DAB_DLS_TEXT, this.tunerDABDlsText);
                } else if (this.tunerDABDlsText.equals(StringType.EMPTY)) {
                    handler.stateChanged(CHANNEL_TUNER_DAB_DLS_TEXT, StringType.valueOf(""));
                }
                break;
            default:
                break;
        }
    }

    /**
     * Method to update the tuners DAB Broadcast station service name or id
     *
     * @param prefix - tuner
     * @param tunerDABServiceName - DAB Broadcast station service name or id
     */
    public void setTunerDABServiceName(String prefix, String tunerDABServiceName) {
        StringType newVal = new StringType(tunerDABServiceName);
        switch (prefix) {
            case TUNER:
                if (!newVal.equals(this.tunerDABServiceName)) {
                    this.tunerDABServiceName = newVal;
                    handler.stateChanged(CHANNEL_TUNER_DAB_SERVICE_NAME, this.tunerDABServiceName);
                }
        }
    }

    /**
     * Method to change the source input for a NAD AVR zone
     *
     * @param prefix - zone
     * @param source - value of 1-10, or 11 depending on zone
     */
    public void setSourceName(String prefix, String source) {
        StringType newVal = StringType.valueOf(source);
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
     * Method to mute volume on specific zone
     *
     * @param prefix - zone
     * @param mute - true is On, false is Off
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
     * Method to change the volume on a specific zone of the NAD AVR. Value is in dB and used to also change
     * volume percentage
     *
     * @param prefix - zone
     * @param volume - range limits are -95 to +16
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
     * Method to set fixed volume level in dB for Zones 2-4 of the NAD AVR and used to also change
     * volume percentage
     *
     * @param prefix - zone
     * @param volume - range limits are -95 to +16
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
     * Method to set the volume control mode for zones 2-4 on the NAD AVR
     *
     * @param prefix - zone 2-4
     * @param volumeControl - volume control mode is either Variable or Fixed
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
     * method to convert sound volume in decibels to a percentage for the NAD AVR
     *
     * @param volume - dB value to be converted to a percentage
     * @return percent representation of the dB volume value
     */
    private BigDecimal calculateVolumePercent(BigDecimal volume) {
        BigDecimal volumePct = volume.subtract(VOLUME_DB_MIN);
        BigDecimal volFactor = ONE_HUNDRED.divide(VOLUME_DB_RANGE, 8, RoundingMode.HALF_UP);
        BigDecimal volumePercent = volumePct.multiply(volFactor).divide(new BigDecimal(1), 0, RoundingMode.HALF_EVEN);
        return volumePercent;
    }

    /**
     * Method to retrieve user provided tuner preset details
     *
     * @param presetKey - represents preset number used to located detail in array...
     * @return presetDetail string containing tuner Band Frequency and User defined name to be associated with the
     *         preset.
     */
    public StringType getPresetDetail(StringType presetKey, String fileName) {
        StringType presetDetail = new StringType(UnDefType.UNDEF.toString());
        if (!fileName.isBlank()) {
            List<NadPreset> tunerPresetDetails = tunerPresets.parsePresets(fileName);
            Map<StringType, NadPreset> presetMap = new ConcurrentHashMap<StringType, NadPreset>();
            for (NadPreset pm : tunerPresetDetails) {
                StringType key = new StringType(pm.getID());
                presetMap.put(key, pm);
            }
            String temp = presetKey.toString();
            StringType key = new StringType(temp);
            if (temp.length() < 2) {
                temp = "0" + temp;
                key = new StringType(temp);
            }
            if (presetMap.containsKey(key)) {
                NadPreset pFromMap = presetMap.getOrDefault(key,
                        new NadPreset(key.toString(), UnDefType.UNDEF.toString(), "", ""));
                String fromMap = pFromMap.getBand() + " " + pFromMap.getFrequency() + " " + pFromMap.getName();
                presetDetail = StringType.valueOf(fromMap);
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("NadAvrState:getPresetDetail() presetKey \"{}\" not found in file \"{}\".", presetKey,
                            fileName);
                }
            }
        }
        return presetDetail;
    }
}
