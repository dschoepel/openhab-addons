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
package org.openhab.binding.nadavr.internal.nadcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link NADProtocol} class to handle the NAD TCP/IP-RS232 protocol specifications.
 *
 * @author Dave J Schoepel - Initial contribution
 */
@NonNullByDefault
public class NADProtocol {

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

    /**
     * Builds command in a NAD Protocol format (prefix . variable operator value).
     *
     * @param msg
     *
     * @return String holding the full NAD Command
     */
    public static String createNADCommand(NADMessage msg) {
        String data = msg.getPrefix() + "." + msg.getVariable() + msg.getOperator() + msg.getValue();
        StringBuilder sb = new StringBuilder();
        // sb.append("\r"); // Precede with carriage return to ensure clear queue.
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
     * @throws NADcpException
     */
    public static NADMessage getNextMessage(BufferedReader stream)
            throws IOException, InterruptedException, NADcpException {
        // Initialize protocol place holders
        String prefix = "";
        String variable = "";
        String operator = "";
        String value = "";
        try {
            String line = stream.readLine();
            // verify that the line is valid - longer than 0, contains dot ".", contains operator
            Matcher match = NAD_FULL_MESSAGE_PATTERN.matcher(line);
            if (match.matches()) {
                prefix = match.group(1); // get prefix - Group 1: anything before first dot
                variable = match.group(2); // get variable - Group 2: between first dot and operator
                operator = match.group(3); // Get operator - Group 3: '=, ?, + or -'
                value = match.group(4).stripTrailing(); // Get value - Group 4: everything after the operator
                return new NADMessage.MessageBuilder().prefix(prefix).variable(variable).operator(operator).value(value)
                        .build();
            } else {
                Matcher matchPrefix = NAD_PREFIX_QUERY_PATTERN.matcher(line);
                if (matchPrefix.matches()) {
                    prefix = matchPrefix.group(1);
                    operator = matchPrefix.group(2);
                    return new NADMessage.MessageBuilder().prefix(prefix).variable(variable).operator(operator)
                            .value(value).build();
                } else {
                    throw new NADcpException(
                            "Skipping NAD response message, it is not in a valid message format (<prefix> . <variable> <operator> <value>): "
                                    + line);
                }
            }
        } catch (IOException e) {
            throw new NADcpException(
                    "Fatal error occurred when parsing NAD control protocol response message, cause=" + e.getCause());
        }
    }
}
