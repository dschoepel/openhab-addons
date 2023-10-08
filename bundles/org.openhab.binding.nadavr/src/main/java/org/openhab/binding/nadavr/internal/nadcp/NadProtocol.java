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
package org.openhab.binding.nadavr.internal.nadcp;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.nadavr.internal.NadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link NadProtocol} class validates State status messages received from the NAD device
 * and creates commands to be sent to the NAD device to change Channel States. Commands are in the
 * NAD Protocol format (prefix . variable operator value). For example:
 *
 * <ul>
 * <li>Main.Power=On</li>
 * <li>Zone2.VolumeControl=Variable</li>
 * <li>Tuner.FM.Frequency=105.7</li>
 * </ul>
 *
 * @author Dave J Schoepel - Initial contribution
 */

@NonNullByDefault
public class NadProtocol {

    /**
     * REGEX to validate message and group components of the message
     * Group 1 = prefix,
     * Group 2 = variable,
     * Group 3 = operator,
     * Group 4 = value
     */
    // private static final Pattern NAD_FULL_MESSAGE_PATTERN = Pattern
    // .compile("^(.[^.]+)\\.(.*[^=\\?\\-\\+])([=\\?\\+\\-])(.*)$", Pattern.CASE_INSENSITIVE);

    private static final Pattern NAD_FULL_MESSAGE_PATTERN = Pattern
            .compile("^(.[^.]+)\\.(.[^=\\?\\+\\-]+)([=\\?\\+\\-])(.*)$", Pattern.CASE_INSENSITIVE);
    /**
     * REGEX to validate message that is a query
     * Group 1 = prefix + .variable(optional),
     * Group 2 = operator is ?
     */
    private static final Pattern NAD_PREFIX_QUERY_PATTERN = Pattern.compile("^(.[^.]+)(\\?)", Pattern.CASE_INSENSITIVE);

    /**
     * Builds command in a NAD Protocol format (prefix . variable operator value).
     *
     * @param msg - command to be wrapped in carriage returns
     *
     * @return String holding the fully formated Command to send to the NAD Device
     */
    public static String createNADCommand(NadMessage msg) {
        String data = msg.getPrefix() + "." + msg.getVariable() + msg.getOperator() + msg.getValue();
        StringBuilder sb = new StringBuilder();
        // Precede with carriage return to ensure clear queue.
        sb.append("\r");
        sb.append(data);
        sb.append("\r");
        return sb.toString();
    }

    /**
     * Validate the content of a received message from the NAD Device
     *
     * @param receivedMessage - the byte stream containing the State status message
     *
     * @throws NadException If the message has unexpected content
     */
    public static NadMessage validateResponse(byte[] receivedMessage) throws NadException {
        Logger logger = LoggerFactory.getLogger(NadProtocol.class);
        String prefix = "";
        String variable = "";
        String operator = "";
        String value = "";
        String message = "";
        if (receivedMessage.length >= 1) {
            message = new String(receivedMessage, 0, receivedMessage.length - 1, StandardCharsets.US_ASCII);
        } else {
            throw new NadException("Error: The received message lenght {" + receivedMessage.length + "} is too short!");
        }

        if (logger.isDebugEnabled()) {
            logger.debug("NadProtocol: validateResponse - Message Recieved: chars *{}*", message);
        }
        message = message.trim();
        if (message.isEmpty()) {
            return new NadMessage.MessageBuilder().prefix(NadCommand.EMPTY_COMMAND.getPrefix())
                    .variable(NadCommand.EMPTY_COMMAND.getVariable()).operator(NadCommand.EMPTY_COMMAND.getOperator())
                    .value(NadCommand.EMPTY_COMMAND.getValue()).build();
        }
        // verify that the message is valid - longer than 0, contains dot ".", contains operator
        Matcher match = NAD_FULL_MESSAGE_PATTERN.matcher(message);
        if (match.matches()) {
            prefix = match.group(1); // get prefix - Group 1: anything before first dot
            variable = match.group(2); // get variable - Group 2: between first dot and operator
            operator = match.group(3); // Get operator - Group 3: '=, ?, + or -'
            value = match.group(4).stripTrailing(); // Get value - Group 4: everything after the operator
            return new NadMessage.MessageBuilder().prefix(prefix).variable(variable).operator(operator).value(value)
                    .build();
        } else {
            Matcher matchPrefix = NAD_PREFIX_QUERY_PATTERN.matcher(message);
            if (matchPrefix.matches()) {
                prefix = matchPrefix.group(1);
                operator = matchPrefix.group(2);
                return new NadMessage.MessageBuilder().prefix(prefix).variable(variable).operator(operator).value(value)
                        .build();
            } else {
                throw new NadException(
                        "Skipping NAD response message, it is not in a valid message format (<prefix> . <variable> <operator> <value>): "
                                + message);
            }
        }
    }
}
