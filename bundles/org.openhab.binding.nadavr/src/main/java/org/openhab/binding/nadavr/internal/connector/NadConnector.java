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

import static org.openhab.binding.nadavr.internal.NadAvrBindingConstants.NAD_QUERY;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.nadavr.internal.NadException;
import org.openhab.binding.nadavr.internal.nadcp.NadCommand;
import org.openhab.binding.nadavr.internal.nadcp.NadCommand.Prefix;
import org.openhab.binding.nadavr.internal.nadcp.NadMessage;
import org.openhab.binding.nadavr.internal.nadcp.NadProtocol;
import org.openhab.binding.nadavr.internal.nadcp.NadUnsupportedCommandTypeException;
import org.openhab.binding.nadavr.internal.state.NadAvrState;
import org.openhab.binding.nadavr.internal.state.NadAvrStateChangedListener;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.IncreaseDecreaseType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.openhab.core.util.HexUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link NadConnection} abstract class for communicating with an NAD AV Receiver device.
 *
 * @author Dave J Schoepel - Initial contribution
 */
@NonNullByDefault
public abstract class NadConnector {

    private final Logger logger = LoggerFactory.getLogger(NadConnector.class);

    private NadAvrStateChangedListener stateChangedListener = new NadAvrStateChangedListener() {
        @Override
        public void stateChanged(String channelID, State state) {
        }

        @Override
        public void connectionError(String errorMessage) {
        }
    };

    private static final BigDecimal VOLUME_RANGE = new BigDecimal("118");
    private static final BigDecimal VOLUME_DB_MIN = new BigDecimal("-99");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    protected NadAvrState state = new NadAvrState(stateChangedListener);

    public static final byte[] READ_ERROR = "read_error".getBytes(StandardCharsets.US_ASCII);

    protected static final byte START = (byte) 0xFE;

    /** The output stream */
    protected @Nullable OutputStream dataOut;

    /** The input stream */
    protected @Nullable InputStream dataIn;

    /** True if the connection is established, false if not */
    private boolean connected;

    protected String msgReaderThreadName;
    private @Nullable Thread msgReaderThread;

    private List<NadEventListener> listeners = new ArrayList<>();

    /**
     * Constructor for NAD device IP connection
     *
     * @param msgReaderThreadName - name to be assigned to connection thread
     */
    public NadConnector(String msgReaderThreadName) {
        this.msgReaderThreadName = msgReaderThreadName;
    }

    /**
     * Mehtod to get whether the connection is established or not
     *
     * @return true if the connection is established
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * Method to set whether the connection is established or not
     *
     * @param connected true if the connection is established
     */
    protected void setConnected(boolean connected) {
        this.connected = connected;
    }

    /**
     * Method to set the thread that handles the feedback messages
     *
     * @param readerThread - the thread
     */
    protected void setMsgReaderThread(Thread readerThread) {
        this.msgReaderThread = readerThread;
    }

    /**
     * Method to open the connection with the NAD device
     *
     * @throws NadException - In case of any problem, provide a description for troubleshooting
     */
    public abstract void open() throws NadException;

    /**
     * Method to close the connection with the NAD device
     */
    public abstract void close();

    /**
     * Method to ensure the connection is properly closed, stop the thread that handles the feedback messages and close
     * the opened input and output streams.
     */
    protected void cleanup() {
        Thread readerThread = this.msgReaderThread;
        if (readerThread != null) {
            readerThread.interrupt();
            try {
                readerThread.join();
            } catch (InterruptedException e) {
            }
            this.msgReaderThread = null;
        }
        OutputStream dataOut = this.dataOut;
        if (dataOut != null) {
            try {
                dataOut.close();
            } catch (IOException e) {
            }
            this.dataOut = null;
        }
        InputStream dataIn = this.dataIn;
        if (dataIn != null) {
            try {
                dataIn.close();
            } catch (IOException e) {
            }
            this.dataIn = null;
        }
    }

    /**
     * Reads some number of bytes from the input stream and stores them into the buffer array b. The number of bytes
     * actually read is returned as an integer.
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
    protected int readInput(byte[] dataBuffer) throws NadException, InterruptedIOException {
        InputStream dataIn = this.dataIn;
        if (dataIn == null) {
            throw new NadException("readInput failed: input stream is null");
        }
        try {
            return dataIn.read(dataBuffer);
        } catch (IOException e) {
            logger.debug("readInput failed: {}", e.getMessage());
            throw new NadException("readInput failed", e);
        }
    }

    /**
     * Method to send a command in the NAD command protocol format to the NAD Device
     *
     * @param msg - NAD formated message to be sent
     * @throws NadException - details of any errors resulting from sending the command message to the NAD device
     */
    public void sendCommand(NadMessage msg) throws NadException {
        String data = NadProtocol.createNADCommand(msg);
        byte[] message = new byte[0];
        message = data.getBytes(StandardCharsets.US_ASCII);
        OutputStream dataOut = this.dataOut;
        if (dataOut == null) {
            throw new NadException("Send command \"" + msg.toString() + "\" failed: output stream is null");
        }
        try {
            dataOut.write(message);
            dataOut.flush();
        } catch (IOException e) {
            throw new NadException("Send command \"" + msg.toString() + "\" failed", e);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Send command \"{}\" succeeded.", msg.toString());
        }
    }

    /**
     * Method to add a listener to the list of listeners to be notified with events
     *
     * @param listener - the event listener to be added
     */
    public void addEventListener(NadEventListener listener) {
        listeners.add(listener);
    }

    /**
     * Method to remove a listener from the list of listeners to be notified with events
     *
     * @param listener - the event listener to be removed
     */
    public void removeEventListener(NadEventListener listener) {
        listeners.remove(listener);
    }

    /**
     * Method to analyze an incoming message and dispatch corresponding (key, value) to the event listeners
     *
     * @param incomingMessage - the message received from the NAD device
     */
    public void handleIncomingMessage(byte[] incomingMessage) {
        if (logger.isDebugEnabled()) {
            logger.debug("handleIncomingMessage: bytes {}", HexUtils.bytesToHex(incomingMessage));
        }
        // Check for errors
        if (READ_ERROR == incomingMessage) {
            sendConnectionErrorEvent("Read error on incoming message \"" + HexUtils.bytesToHex(incomingMessage) + "\"");
            return;
        }
        try {
            // Validate the message received from the NAD device
            NadMessage message = NadProtocol.validateResponse(incomingMessage);
            // When a message has a prefix, send it to the event listeners so that the thing handler can process it
            if (!message.getPrefix().equals("")) {
                sendMessageEvent(message);
            }
        } catch (NadException e) {
            return;
        }
    }

    /**
     * Method to send Power (ON/OFF/Status) commands for a specific zone
     *
     * @param command - POWER_SET
     * @param zone - see Prefix eNum for command prefixes
     * @throws NadUnsupportedCommandTypeException - invalid value was used on the command
     * @throws NadException - send failures captured for troubleshooting
     */
    public void sendPowerCommand(Command command, Prefix zone) throws NadUnsupportedCommandTypeException, NadException {
        String cmdValue = "";
        if (command == OnOffType.ON) {
            cmdValue = "On";
        } else if (command == OnOffType.OFF) {
            cmdValue = "Off";
        } else if (command instanceof RefreshType) {
            cmdValue = NAD_QUERY; // "?"
        } else {
            String message = "Send Power command to \"" + zone + "\", encountered an unsupported command value of \""
                    + cmdValue + "\"!";
            throw new NadUnsupportedCommandTypeException(message);
        }
        NadMessage msg = new NadMessage.MessageBuilder().prefix(zone.toString())
                .variable(NadCommand.POWER_SET.getVariable().toString())
                .operator(NadCommand.POWER_SET.getOperator().toString()).value(cmdValue).build();
        try {
            sendCommand(msg);
        } catch (NadException e) {
            throw new NadException("Send command \"" + msg.toString() + "\" failed", e);
        }
    }

    /**
     * Method to send Listening Mode setting command for a specific zone
     *
     * @param command - LISTENING_MODE_SET
     * @param zone - see Prefix eNum for command prefixes
     * @throws NadUnsupportedCommandTypeException - invalid value was used on the command
     * @throws NadException - send failures captured for troubleshooting
     */
    public void sendListeningModeCommand(Command command, Prefix zone)
            throws NadUnsupportedCommandTypeException, NadException {
        String cmdValue = "";
        if (command instanceof StringType) {
            cmdValue = command.toString();
        } else if (command instanceof RefreshType) {
            cmdValue = NAD_QUERY; // "?"else {
        } else {
            String message = "Send Listening Mode command to \"" + zone
                    + "\", encountered an unsupported command value of \"" + cmdValue + "\"!";
            throw new NadUnsupportedCommandTypeException(message);
        }

        NadMessage msg = new NadMessage.MessageBuilder().prefix(zone.toString())
                .variable(NadCommand.LISTENING_MODE_SET.getVariable().toString())
                .operator(NadCommand.LISTENING_MODE_SET.getOperator().toString()).value(cmdValue).build();
        try {
            sendCommand(msg);
        } catch (NadException e) {
            throw new NadException("Send command \"" + msg.toString() + "\" failed", e);
        }
    }

    /**
     * Method to send the Tuner Band setting command
     *
     * @param command - TUNER_BAND_SET
     * @param tuner - see Prefix eNum for command prefixes
     * @throws NadUnsupportedCommandTypeException - invalid value was used on the command
     * @throws NadException - send failures captured for troubleshooting
     */
    public void sendTunerBandCommand(Command command, Prefix tuner)
            throws NadUnsupportedCommandTypeException, NadException {
        String cmdValue = "";
        if (command instanceof StringType) {
            cmdValue = command.toString();
        } else if (command instanceof RefreshType) {
            cmdValue = NAD_QUERY; // "?"else {
        } else {
            String message = "Send Tuner Band command to \"" + tuner
                    + "\", encountered an unsupported command value of \"" + cmdValue + "\"!";
            throw new NadUnsupportedCommandTypeException(message);
        }

        NadMessage msg = new NadMessage.MessageBuilder().prefix(tuner.toString())
                .variable(NadCommand.TUNER_BAND_SET.getVariable().toString())
                .operator(NadCommand.TUNER_BAND_SET.getOperator().toString()).value(cmdValue).build();
        try {
            sendCommand(msg);
        } catch (NadException e) {
            throw new NadException("Send command \"" + msg.toString() + "\" failed", e);
        }
    }

    /**
     * Method to send the source setting command for a specific zone
     *
     * @param command - SOURCE_SET
     * @param zone - see Prefix eNum for command prefixes
     * @throws NadUnsupportedCommandTypeException - invalid value was used on the command
     * @throws NadException - send failures captured for troubleshooting
     */
    public void sendSourceCommand(Command command, Prefix zone)
            throws NadUnsupportedCommandTypeException, NadException {
        String cmdValue = "";
        if (command instanceof StringType) {
            cmdValue = command.toString();
        } else if (command instanceof RefreshType) {
            cmdValue = NAD_QUERY; // "?"else {
        } else {
            String message = "Send Source command to \"" + zone + "\", encountered an unsupported command value of \""
                    + cmdValue + "\"!";
            throw new NadUnsupportedCommandTypeException(message);
        }
        NadMessage msg = new NadMessage.MessageBuilder().prefix(zone.toString())
                .variable(NadCommand.SOURCE_SET.getVariable().toString())
                .operator(NadCommand.SOURCE_SET.getOperator().toString()).value(cmdValue).build();
        try {
            sendCommand(msg);
        } catch (NadException e) {
            throw new NadException("Send command \"" + msg.toString() + "\" failed", e);
        }
    }

    /**
     * Method to send the mute setting command for a specific zone
     *
     * @param command - MUTE_SET
     * @param zone - see Prefix eNum for command prefixes
     * @throws NadUnsupportedCommandTypeException - invalid value was used on the command
     * @throws NadException - send failures captured for troubleshooting
     */
    public void sendMuteCommand(Command command, Prefix zone) throws NadUnsupportedCommandTypeException, NadException {
        String cmdValue = "";
        if (command == OnOffType.ON) {
            cmdValue = "On";
        } else if (command == OnOffType.OFF) {
            cmdValue = "Off";
        } else if (command instanceof RefreshType) {
            cmdValue = NAD_QUERY; // "?"
        } else {
            String message = "Send Mute command to \"" + zone + "\", encountered an unsupported command value of \""
                    + cmdValue + "\"!";
            throw new NadUnsupportedCommandTypeException(message);
        }
        NadMessage msg = new NadMessage.MessageBuilder().prefix(zone.toString())
                .variable(NadCommand.MUTE_SET.getVariable().toString())
                .operator(NadCommand.MUTE_SET.getOperator().toString()).value(cmdValue).build();
        try {
            sendCommand(msg);
        } catch (NadException e) {
            throw new NadException("Send command \"" + msg.toString() + "\" failed", e);
        }
    }

    /**
     * Method to send the volume setting command for a specific zone. This value represents a
     * percentage for the dimmer type control, it needs to be converted to dB before sending
     *
     * @param command - VOLUME_SET
     * @param zone - see Prefix eNum for command prefixes
     * @throws NadUnsupportedCommandTypeException - invalid value was used on the command
     * @throws NadException - send failures captured for troubleshooting
     */
    public void sendVolumeCommand(Command command, Prefix zone)
            throws NadUnsupportedCommandTypeException, NadException {
        String cmdValue = "";
        if (command instanceof RefreshType) {
            cmdValue = NAD_QUERY;
        } else if (command == IncreaseDecreaseType.INCREASE) {
            cmdValue += "+";
        } else if (command == IncreaseDecreaseType.DECREASE) {
            cmdValue += "-";
        } else if (command instanceof PercentType) {
            cmdValue = percentToNadDbValue(((PercentType) command).toBigDecimal());
        } else if (command instanceof DecimalType) {
            cmdValue = toNadValue(((DecimalType) command));
        } else {
            String message = "Send Volume command to \"" + zone + "\", encountered an unsupported command value of \""
                    + cmdValue + "\"!";
            throw new NadUnsupportedCommandTypeException(message);
        }
        NadMessage msg = new NadMessage.MessageBuilder().prefix(zone.toString())
                .variable(NadCommand.VOLUME_SET.getVariable().toString())
                .operator(NadCommand.VOLUME_SET.getOperator().toString()).value(cmdValue).build();
        try {
            sendCommand(msg);
        } catch (NadException e) {
            throw new NadException("Send command \"" + msg.toString() + "\" failed", e);
        }
    }

    /**
     * Method to send the fixed volume setting command for a specific zone. Does not apply to the Main (Zone1).
     *
     * @param command - VOLUME_FIXED_SET
     * @param zone - see Prefix eNum for command prefixes
     * @throws NadUnsupportedCommandTypeException - invalid value was used on the command
     * @throws NadException - send failures captured for troubleshooting
     */
    public void sendVolumeFixedCommand(Command command, Prefix zone)
            throws NadUnsupportedCommandTypeException, NadException {
        String cmdValue = "";
        if (command instanceof RefreshType) {
            cmdValue = NAD_QUERY;
        } else if (command == IncreaseDecreaseType.INCREASE) {
            cmdValue += "+";
        } else if (command == IncreaseDecreaseType.DECREASE) {
            cmdValue += "-";
        } else if (command instanceof PercentType) {
            cmdValue = percentToNadDbValue(((PercentType) command).toBigDecimal());
        } else if (command instanceof DecimalType) {
            cmdValue = toNadValue(((DecimalType) command));
        } else {
            String message = "Send Volume Fixed command to \"" + zone
                    + "\", encountered an unsupported command value of \"" + cmdValue + "\"!";
            throw new NadUnsupportedCommandTypeException(message);
        }
        NadMessage msg = new NadMessage.MessageBuilder().prefix(zone.toString())
                .variable(NadCommand.VOLUME_FIXED_SET.getVariable().toString())
                .operator(NadCommand.VOLUME_FIXED_SET.getOperator().toString()).value(cmdValue).build();
        try {
            sendCommand(msg);
        } catch (NadException e) {
            throw new NadException("Send command \"" + msg.toString() + "\" failed", e);
        }
    }

    /**
     * Method to send the FM frequency setting command to the tuner.
     *
     * @param command - TUNER_FM_FREQUENCY_SET
     * @param tuner - see Prefix eNum for command prefixes
     * @throws NadUnsupportedCommandTypeException - invalid value was used on the command
     * @throws NadException - send failures captured for troubleshooting
     */
    public void sendTunerFmFrequencyCommand(Command command, Prefix tuner)
            throws NadUnsupportedCommandTypeException, NadException {
        String cmdValue = "";
        if (command instanceof RefreshType) {
            cmdValue = NAD_QUERY;
        } else if (command == IncreaseDecreaseType.INCREASE) {
            cmdValue += "+";
        } else if (command == IncreaseDecreaseType.DECREASE) {
            cmdValue += "-";
        } else if (command instanceof DecimalType) {
            cmdValue = toDenonFloatValue(((DecimalType) command));
        } else {
            String message = "Send FM Frequency command to \"" + tuner
                    + "\", encountered an unsupported command value of \"" + cmdValue + "\"!";
            throw new NadUnsupportedCommandTypeException(message);
        }
        NadMessage msg = new NadMessage.MessageBuilder().prefix(tuner.toString())
                .variable(NadCommand.TUNER_FM_FREQUENCY_SET.getVariable().toString())
                .operator(NadCommand.TUNER_FM_FREQUENCY_SET.getOperator().toString()).value(cmdValue).build();
        try {
            sendCommand(msg);
        } catch (NadException e) {
            throw new NadException("Send command \"" + msg.toString() + "\" failed", e);
        }
    }

    /**
     * Method to send the AM frequency setting command to the tuner.
     *
     * @param command - TUNER_AM_FREQUENCY_SET
     * @param tuner - see Prefix eNum for command prefixes
     * @throws NadUnsupportedCommandTypeException - invalid value was used on the command
     * @throws NadException - send failures captured for troubleshooting
     */
    public void sendTunerAmFrequencyCommand(Command command, Prefix tuner)
            throws NadUnsupportedCommandTypeException, NadException {
        String cmdValue = "";
        if (command instanceof RefreshType) {
            cmdValue = NAD_QUERY;
        } else if (command == IncreaseDecreaseType.INCREASE) {
            cmdValue += "+";
        } else if (command == IncreaseDecreaseType.DECREASE) {
            cmdValue += "-";
        } else if (command instanceof DecimalType) {
            cmdValue = toNadValue(((DecimalType) command));
        } else {
            String message = "Send AM Frequency command to \"" + tuner
                    + "\", encountered an unsupported command value of \"" + cmdValue + "\"!";
            throw new NadUnsupportedCommandTypeException(message);
        }
        NadMessage msg = new NadMessage.MessageBuilder().prefix(tuner.toString())
                .variable(NadCommand.TUNER_AM_FREQUENCY_SET.getVariable().toString())
                .operator(NadCommand.TUNER_AM_FREQUENCY_SET.getOperator().toString()).value(cmdValue).build();
        try {
            sendCommand(msg);
        } catch (NadException e) {
            throw new NadException("Send command \"" + msg.toString() + "\" failed", e);
        }
    }

    /**
     * Method to send the preset selection setting command to the tuner.
     *
     * @param command - TUNER_PRESET_SET
     * @param tuner - see Prefix eNum for command prefixes
     * @throws NadUnsupportedCommandTypeException - invalid value was used on the command
     * @throws NadException - send failures captured for troubleshooting
     */
    public void sendTunerPresetCommand(Command command, Prefix tuner)
            throws NadUnsupportedCommandTypeException, NadException {
        String cmdValue = "";
        if (command instanceof RefreshType) {
            cmdValue = NAD_QUERY;
        } else if (command == IncreaseDecreaseType.INCREASE) {
            cmdValue += "+";
        } else if (command == IncreaseDecreaseType.DECREASE) {
            cmdValue += "-";
        } else if (command instanceof StringType) {
            cmdValue = command.toString();
        } else {
            String message = "Send Preset command to \"" + tuner + "\", encountered an unsupported command value of \""
                    + cmdValue + "\"!";
            throw new NadUnsupportedCommandTypeException(message);
        }
        NadMessage msg = new NadMessage.MessageBuilder().prefix(tuner.toString())
                .variable(NadCommand.TUNER_PRESET_SET.getVariable().toString())
                .operator(NadCommand.TUNER_PRESET_SET.getOperator().toString()).value(cmdValue).build();
        try {
            sendCommand(msg);
        } catch (NadException e) {
            throw new NadException("Send command \"" + msg.toString() + "\" failed", e);
        }
    }

    /**
     * Method to send the XM Channel number setting command to the tuner.
     *
     * @param command - TUNER_XM_CHANNEL_SET
     * @param tuner - see Prefix eNum for command prefixes
     * @throws NadUnsupportedCommandTypeException - invalid value was used on the command
     * @throws NadException - send failures captured for troubleshooting
     */
    public void sendTunerXMChannelCommand(Command command, Prefix tuner)
            throws NadUnsupportedCommandTypeException, NadException {
        String cmdValue = "";
        if (command instanceof RefreshType) {
            cmdValue = NAD_QUERY;
        } else if (command == IncreaseDecreaseType.INCREASE) {
            cmdValue += "+";
        } else if (command == IncreaseDecreaseType.DECREASE) {
            cmdValue += "-";
        } else if (command instanceof DecimalType) {
            cmdValue = toNadValue(((DecimalType) command));
        } else {
            String message = "Send XM Channel command to \"" + tuner
                    + "\", encountered an unsupported command value of \"" + cmdValue + "\"!";
            throw new NadUnsupportedCommandTypeException(message);
        }
        NadMessage msg = new NadMessage.MessageBuilder().prefix(tuner.toString())
                .variable(NadCommand.TUNER_XM_CHANNEL_SET.getVariable().toString())
                .operator(NadCommand.TUNER_XM_CHANNEL_SET.getOperator().toString()).value(cmdValue).build();
        try {
            sendCommand(msg);
        } catch (NadException e) {
            throw new NadException("Send command \"" + msg.toString() + "\" failed", e);
        }
    }

    /**
     * Method to send the FM mute setting command to the tuner.
     *
     * @param command - TUNER_FM_MUTE_SET
     * @param tuner - see Prefix eNum for command prefixes
     * @throws NadUnsupportedCommandTypeException - invalid value was used on the command
     * @throws NadException - send failures captured for troubleshooting
     */
    public void sendTunerFmMuteCommand(Command command, Prefix tuner)
            throws NadUnsupportedCommandTypeException, NadException {
        String cmdValue = "";
        if (command == OnOffType.ON) {
            cmdValue = "On";
        } else if (command == OnOffType.OFF) {
            cmdValue = "Off";
        } else if (command instanceof RefreshType) {
            cmdValue = NAD_QUERY; // "?"
        } else {
            String message = "Send FM Mute command to \"" + tuner + "\", encountered an unsupported command value of \""
                    + cmdValue + "\"!";
            throw new NadUnsupportedCommandTypeException(message);
        }
        NadMessage msg = new NadMessage.MessageBuilder().prefix(tuner.toString())
                .variable(NadCommand.TUNER_FM_MUTE_SET.getVariable().toString())
                .operator(NadCommand.TUNER_FM_MUTE_SET.getOperator().toString()).value(cmdValue).build();
        try {
            sendCommand(msg);
        } catch (NadException e) {
            throw new NadException("Send command \"" + msg.toString() + "\" failed", e);
        }
    }

    /**
     * Method to send the FM RDS Text Query command to the tuner.
     *
     * @param command - TUNER_FM_RDS_TEXT_SET
     * @param zone - see Prefix eNum for command prefixes
     * @throws NadUnsupportedCommandTypeException - invalid value was used on the command
     * @throws NadException - send failures captured for troubleshooting
     */
    public void sendTunerFmRdsTextCommand(Command command, Prefix tuner)
            throws NadUnsupportedCommandTypeException, NadException {
        String cmdValue = "";
        if (command instanceof RefreshType) {
            cmdValue = "?"; // Only option is to query this setting
        } else {
            String message = "Send FM RDS Text command to \"" + tuner
                    + "\", encountered an unsupported command value of \"" + cmdValue + "\"!";
            throw new NadUnsupportedCommandTypeException(message);
        }
        NadMessage msg = new NadMessage.MessageBuilder().prefix(tuner.toString())
                .variable(NadCommand.TUNER_FM_RDS_TEXT_SET.getVariable().toString())
                .operator(NadCommand.TUNER_FM_RDS_TEXT_SET.getOperator().toString()).value(cmdValue).build();
        try {
            sendCommand(msg);
        } catch (NadException e) {
            throw new NadException("Send command \"" + msg.toString() + "\" failed", e);
        }
    }

    /**
     * Method to send the Model Query command to the AVR.
     *
     * @param command - MODEL_QUERY
     * @param zone - see Prefix eNum for command prefixes
     * @throws NadUnsupportedCommandTypeException - invalid value was used on the command
     * @throws NadException - send failures captured for troubleshooting
     */
    public void sendModelQueryCommand(Command command, Prefix zone)
            throws NadUnsupportedCommandTypeException, NadException {
        String cmdValue = "";
        if (command instanceof RefreshType) {
            cmdValue = "?"; // Only option is to query this setting
        } else {
            String message = "Send Model Query command to \"" + zone
                    + "\", encountered an unsupported command value of \"" + cmdValue + "\"!";
            throw new NadUnsupportedCommandTypeException(message);
        }
        NadMessage msg = new NadMessage.MessageBuilder().prefix(zone.toString())
                .variable(NadCommand.MODEL_QUERY.getVariable().toString())
                .operator(NadCommand.MODEL_QUERY.getOperator().toString()).value(cmdValue).build();
        try {
            sendCommand(msg);
        } catch (NadException e) {
            throw new NadException("Send command \"" + msg.toString() + "\" failed", e);
        }
    }

    /**
     * Method to send the fixed volume dB setting command for a specific zone. Does not apply to the Main (Zone1).
     *
     * @param command - VOLUME_FIXED_SET
     * @param zone - see Prefix eNum for command prefixes
     * @throws NadUnsupportedCommandTypeException - invalid value was used on the command
     * @throws NadException - send failures captured for troubleshooting
     */
    public void sendVolumeFixedDBCommand(Command command, Prefix zone)
            throws NadUnsupportedCommandTypeException, NadException {
        Command dbCommand = command;
        if (dbCommand instanceof PercentType) {
            String message = "Send Volume Fixed dB command to \"" + zone
                    + "\", encountered an unsupported command of \"" + command.toString() + "\"!";
            throw new NadUnsupportedCommandTypeException(message);
        }
        sendVolumeFixedCommand(dbCommand, zone);
    }

    /**
     * Method to send the volume control setting command (Variable or Fixed) for a specific zone. Does not apply to the
     * Main (Zone1).
     *
     * @param command - VOLUME_CONTROL_SET
     * @param zone - see Prefix eNum for command prefixes
     * @throws NadUnsupportedCommandTypeException - invalid value was used on the command
     * @throws NadException - send failures captured for troubleshooting
     */
    public void sendVolumeControlCommand(Command command, Prefix zone)
            throws NadUnsupportedCommandTypeException, NadException {
        String cmdValue = "";
        if (command instanceof StringType) {
            cmdValue = command.toString();
        } else if (command instanceof RefreshType) {
            cmdValue = NAD_QUERY; // "?"else {
        } else {
            String message = "Send Volume Control command to \"" + zone
                    + "\", encountered an unsupported command value of \"" + cmdValue + "\"!";
            throw new NadUnsupportedCommandTypeException(message);
        }
        NadMessage msg = new NadMessage.MessageBuilder().prefix(zone.toString())
                .variable(NadCommand.VOLUME_CONTROL_SET.getVariable().toString())
                .operator(NadCommand.VOLUME_CONTROL_SET.getOperator().toString()).value(cmdValue).build();
        try {
            sendCommand(msg);
        } catch (NadException e) {
            throw new NadException("Send command \"" + msg.toString() + "\" failed", e);
        }
    }

    // public void sendVolumeDbCommand(Command command, Prefix zone)
    // throws NadUnsupportedCommandTypeException, NadException {
    // Command dbCommand = command;
    // if (dbCommand instanceof PercentType) {
    // throw new NadUnsupportedCommandTypeException();
    // }
    // sendVolumeCommand(dbCommand, zone);
    // }

    protected String toDenonFloatValue(DecimalType number) {
        String dbString = String.valueOf(number.floatValue());
        return dbString;
    }

    protected String toNadValue(DecimalType number) {
        String dbString = String.valueOf(number.intValue());
        return dbString;
    }

    protected String percentToNadDbValue(BigDecimal pct) {
        BigDecimal percent = pct.multiply(VOLUME_RANGE).divide(ONE_HUNDRED).add(VOLUME_DB_MIN);
        return toNadValue(new DecimalType(percent));
    }

    /**
     * Method to send a received message to the thing Handler
     *
     * @param message - received from the NAD device
     */
    private void sendMessageEvent(NadMessage message) {
        // send message to all event listeners
        try {
            for (NadEventListener listener : listeners) {
                listener.receivedMessage(message);
            }
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Event listener invoking error: {}", e.getMessage());
            }
        }
    }

    /**
     * Method to send a connection diagnostic messages to event listeners
     *
     * @param errorMsg - error details to be sent to listener
     */
    private void sendConnectionErrorEvent(@Nullable String errorMsg) {
        // send error message to all event listeners
        try {
            for (NadEventListener listener : listeners) {
                listener.connectionError(errorMsg);
            }
        } catch (Exception ex) {
            if (logger.isDebugEnabled()) {
                logger.debug("Event listener invoking error: {}", ex.getMessage());
            }
        }
    }
}
