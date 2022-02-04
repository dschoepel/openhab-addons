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

import static org.openhab.binding.nadavr.internal.NadAvrBindingConstants.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.nadavr.internal.NadAvrConfiguration;
import org.openhab.binding.nadavr.internal.state.NadAvrState;
import org.openhab.core.library.types.OnOffType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link NadTunerMonitor} class Monitors for active tuner and turns on RDS stream if FM band is set.
 *
 * @author Dave J Schoepel - Initial contribution
 */
@NonNullByDefault
public class NadTunerMonitor {

    Logger logger = LoggerFactory.getLogger(NadTunerMonitor.class);
    private ScheduledExecutorService tmExecutor = Executors.newSingleThreadScheduledExecutor();
    private String threadNamePrefix = "";
    private volatile boolean isTmPaused;
    private volatile boolean isTmStarted;
    private volatile boolean tunerIsActive;
    private volatile boolean tunerBandIsFM;
    private volatile boolean tunerBandIsXM;
    private NadAvrConfiguration config;
    private NadFMRdsTextStream rdsText;
    private NadXmRefreshChannelInfo xmInfo;
    private NadAvrState nadavrState;

    /**
     * The constructor for {@link NadTunerMonitor}
     *
     * @param connector to the NAD AVR Thing
     * @param config details for the NAD AVR thing
     */

    public NadTunerMonitor(NadIpConnector connection, NadAvrConfiguration config, NadAvrState nadavrState,
            String threadNamePrefix) {
        // this.connection = connection;
        this.config = config;
        this.nadavrState = nadavrState;
        this.rdsText = new NadFMRdsTextStream(connection);
        this.xmInfo = new NadXmRefreshChannelInfo(connection);
        this.threadNamePrefix = threadNamePrefix;
    }

    /**
     * The tunerMonitorThread determines if an FM RDS stream is active on the Tuner. The tuner source,
     * must have its band set to FM and be assigned to an input which is powered On for an RDS Stream
     * to be displayed.
     */
    Runnable tunerMonitorThread = new Runnable() {
        @Override
        public void run() {
            Thread.currentThread().setName(threadNamePrefix + "-TunerMonitor");
            tunerBandIsFM = FM.equals(nadavrState.getStateForChannelID(CHANNEL_TUNER_BAND));
            tunerBandIsXM = XM.equals(nadavrState.getStateForChannelID(CHANNEL_TUNER_BAND));
            if (logger.isDebugEnabled()) {
                logger.debug(
                        "Start setTunerStatus: tunerIsActive = {}, tunerBandIsFM = {}, tunerBandIsXM = {}, isTmStarted = {}, isTmPaused = {}, isRdsStarted = {}, isRdsPaused = {}, isXMStarted = {}, isXmPaused = {}",
                        tunerIsActive, tunerBandIsFM, tunerBandIsXM, isTmStarted, isTmPaused, rdsText.isRdsStarted(),
                        rdsText.isRdsPaused(), xmInfo.isXmStarted(), xmInfo.isXmPaused());
            }
            if (tunerBandIsFM || tunerBandIsXM) {
                if (!isTmPaused) {
                    setTunerStatus();
                } else {
                    resumeTm();
                    setTunerStatus();
                } // end if !isTmPaused
            } else { // tunerBand is not FM or XM pause Monitor and RDS threads
                if (!isTmPaused) {
                    if (rdsText.isRdsStarted() & !rdsText.isRdsPaused()) {
                        rdsText.pauseRds();
                    }
                    if (xmInfo.isXmStarted() & !xmInfo.isXmPaused()) {
                        xmInfo.pauseXmRefreshChannleInfo();
                    }
                    pauseTm();
                } // end if !isTmPaused
            } // end if tunerBandIsFM or tunerBandIsXM

            if (logger.isDebugEnabled()) {
                logger.debug(
                        "End setTunerStatus: tunerIsActive = {}, tunerBandIsFM = {}, isTmStarted = {}, isTmPaused = {}, isRdsStarted = {}, isRdsPaused = {}",
                        tunerIsActive, tunerBandIsFM, isTmStarted, isTmPaused, rdsText.isRdsStarted(),
                        rdsText.isRdsPaused());
            }
        }
    };

    /**
     * The setTunerStatus() method looks for inputs that are powered On and set to the Tuner source.
     * When found, the rdsRFMTextStream is started, resumed if the tuner band is set to RM.
     */
    public void setTunerStatus() {
        tunerIsActive = false;
        /*
         * Check to see which zone sources are set to the tuner
         */
        Map<String, Integer> zonesWithTuner = new ConcurrentHashMap<String, Integer>();
        for (int i = 1; i <= config.getZoneCount(); i++) {
            switch (i) {
                case 1:
                    if (TUNER.equals(nadavrState.getStateForChannelID(CHANNEL_MAIN_SOURCE).toString())) {
                        zonesWithTuner.put(CHANNEL_MAIN_SOURCE, i);
                    }
                    break;
                case 2:
                    if (TUNER.equals(nadavrState.getStateForChannelID(CHANNEL_ZONE2_SOURCE).toString())) {
                        zonesWithTuner.put(CHANNEL_ZONE2_SOURCE, i);
                    }
                    break;
                case 3:
                    if (TUNER.equals(nadavrState.getStateForChannelID(CHANNEL_ZONE3_SOURCE).toString())) {
                        zonesWithTuner.put(CHANNEL_ZONE3_SOURCE, i);
                    }
                    break;
                case 4:
                    if (TUNER.equals(nadavrState.getStateForChannelID(CHANNEL_ZONE4_SOURCE).toString())) {
                        zonesWithTuner.put(CHANNEL_ZONE4_SOURCE, i);
                    }
                    break;
                default:
                    tunerIsActive = false;
            }
        }
        /*
         * Check to see if a zone that is powered on is using the tuner and the band is set to FM
         */
        // If a zone is powered on and using the tuner, then the tuner is active = true
        for (Map.Entry<String, Integer> source : zonesWithTuner.entrySet()) {
            String sourceWithTuner = source.getKey();
            if (!tunerIsActive) {
                switch (sourceWithTuner) {
                    case CHANNEL_MAIN_SOURCE:
                        tunerIsActive = OnOffType.ON.equals(nadavrState.getStateForChannelID(CHANNEL_MAIN_POWER));
                        break;
                    case CHANNEL_ZONE2_SOURCE:
                        tunerIsActive = OnOffType.ON.equals(nadavrState.getStateForChannelID(CHANNEL_ZONE2_POWER));
                        break;
                    case CHANNEL_ZONE3_SOURCE:
                        tunerIsActive = OnOffType.ON.equals(nadavrState.getStateForChannelID(CHANNEL_ZONE3_POWER));
                        break;
                    case CHANNEL_ZONE4_SOURCE:
                        tunerIsActive = OnOffType.ON.equals(nadavrState.getStateForChannelID(CHANNEL_ZONE3_POWER));
                        break;
                }
            }
        }
        // If the tuner is active and the band is set to FM, then start/resume the rdsFMTextStream thread.
        if (tunerIsActive && tunerBandIsFM) {
            logger.debug("isRdsStarted = {}", rdsText.isRdsStarted());
            if (!rdsText.isRdsStarted()) {
                rdsText.start(threadNamePrefix);
            } else {
                if (rdsText.isRdsPaused()) {
                    rdsText.resumeRds();
                }
            }
        } else { // If the tuner is not active make sure the rdsFMTextStream thread is paused.
            if (rdsText.isRdsStarted() && !rdsText.isRdsPaused()) {
                rdsText.pauseRds();
            }
        }

        // If the tuner is active and the band is set to XM, then start/resume the NadXmRefreshChannelInfo thread.
        if (tunerIsActive && tunerBandIsXM) {
            logger.debug("isXmStarted = {}", xmInfo.isXmStarted());
            if (!xmInfo.isXmStarted()) {
                xmInfo.start(threadNamePrefix);
            } else {
                if (xmInfo.isXmPaused()) {
                    xmInfo.resumeXmRefreshChannelInfo();
                }
            }
        } else { // If the tuner is not active make sure the XM Channel Refresh Info thread is paused.
            if (xmInfo.isXmStarted() && !xmInfo.isXmPaused()) {
                xmInfo.pauseXmRefreshChannleInfo();
            }
        }
    }

    /**
     * The startTm() method starts the tuner monitor thread if it is not running or resumes it
     * if it is already running.
     */
    public void startTm() {
        if (!isTmStarted) { // If the monitor is not running, schedule it and mark it as started.
            tmExecutor.scheduleWithFixedDelay(tunerMonitorThread, getTmInitialDelay(), getTmPeriodDelay(),
                    getTimeUnits());
            isTmStarted = true;
        } else if (isTmPaused) { // If the monitor is started, but paused, then resume
            resumeTm();
        }
    }

    /**
     * The pauseTm() method is used to pause the tuner monitor thread when there are no
     * powered On inputs using the Tuner.
     */
    public void pauseTm() {
        isTmPaused = true;
    }

    /**
     * The resumTm() method is used to resume the tuner monitor thread when there are
     * powered On inputs using the Tuner.
     */
    public void resumeTm() {
        isTmPaused = false;
    }

    /**
     * The stopTm() method is used to shutdown the tuner monitor thread.
     */
    public void stopTm() {
        isTmStarted = false;
        tmExecutor.shutdownNow();
        logger.debug("TunerMonitor is stopped!");
        rdsText.stopRds();
        logger.debug("RdsTextStream is stopping");
        xmInfo.stopXmRefreshChannelInfo();
        logger.debug("XmRefreshChannelInfo is stopping");
    }

    /**
     * @return The tuner monitor thread's running status.
     */
    public boolean isTmStarted() {
        return isTmStarted;
    }

    /**
     * @return The tuner monitor thread's paused status.
     */
    public boolean isTmPaused() {
        return isTmPaused;
    }

    /**
     * @return The tunerMonitor thread scheduler startup delay in seconds.
     */
    public int getTmInitialDelay() {
        return 15;
    }

    /**
     * @return The tunerMonitor thread scheduler delay between executions in seconds.
     */
    public int getTmPeriodDelay() {
        return 10;
    }

    /**
     * @return The time units to be used for scheduling the tunerMonitor thread.
     */
    public TimeUnit getTimeUnits() {
        return TimeUnit.SECONDS;
    }
}
