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
package org.openhab.binding.nadavr.internal.nadcp;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link NadProtocol.java} class contains fields mapping thing configuration parameters.
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
    private static final Pattern NAD_FULL_MESSAGE_PATTERN = Pattern
            .compile("^(.[^.]+)\\.(.*[^=\\?\\-\\+])([=\\?\\+\\-])(.*)$", Pattern.CASE_INSENSITIVE);

    /**
     * REGEX to validate message that is a query
     * Group 1 = prefix + .variable(optional),
     * Group 2 = operator is ?
     */
    private static final Pattern NAD_PREFIX_QUERY_PATTERN = Pattern.compile("^(.[^.]+)(\\?)", Pattern.CASE_INSENSITIVE);

    private static final int LINE_FEED = 10;

    /**
     * Builds command in a NAD Protocol format (prefix . variable operator value).
     *
     * @param msg
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
     * Method to read NAD Message from input stream.
     *
     * @return message
     *
     * @throws IOException
     * @throws InterruptedException
     * @throws NadcpException
     */
    public static NadMessage getNextMessage(DataInputStream stream)
            throws IOException, InterruptedException, NadcpException {
        Logger logger = LoggerFactory.getLogger(NadProtocol.class);
        // Initialize protocol place holders
        String prefix = "";
        String variable = "";
        String operator = "";
        String value = "";
        try {
            String line = "";
            StringBuilder sb = new StringBuilder();
            byte character;
            boolean eol = false;
            while (!eol) {
                character = stream.readByte();
                int intCharacter = Integer.valueOf(character);
                if (Integer.valueOf(character) != LINE_FEED) {
                    sb.append(Character.toString((char) intCharacter));
                } else {
                    eol = true;
                }
            }

            line = sb.toString();
            logger.info("getNextMessage received line = {}", line);

            // verify that the line is valid - longer than 0, contains dot ".", contains operator
            Matcher match = NAD_FULL_MESSAGE_PATTERN.matcher(line);
            if (match.matches()) {
                prefix = match.group(1); // get prefix - Group 1: anything before first dot
                variable = match.group(2); // get variable - Group 2: between first dot and operator
                operator = match.group(3); // Get operator - Group 3: '=, ?, + or -'
                value = match.group(4).stripTrailing(); // Get value - Group 4: everything after the operator
                return new NadMessage.MessageBuilder().prefix(prefix).variable(variable).operator(operator).value(value)
                        .build();
            } else {
                Matcher matchPrefix = NAD_PREFIX_QUERY_PATTERN.matcher(line);
                if (matchPrefix.matches()) {
                    prefix = matchPrefix.group(1);
                    operator = matchPrefix.group(2);
                    return new NadMessage.MessageBuilder().prefix(prefix).variable(variable).operator(operator)
                            .value(value).build();
                } else {
                    throw new NadcpException(
                            "Skipping NAD response message, it is not in a valid message format (<prefix> . <variable> <operator> <value>): "
                                    + line);
                }
            }
        } catch (SocketTimeoutException ste) {
            throw ste;
        } catch (SocketException se) {
            throw se;
        } catch (IOException e) {
            throw new NadcpException(
                    "Fatal error occurred when parsing NAD control protocol response message, cause=" + e.getCause());
        }
    }
}
