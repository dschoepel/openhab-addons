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

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * This {@link NadMessage} class defines an NAD Protocol Message. The
 * NAD Protocol format has four components (prefix . variable operator value). For example:
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
public class NadMessage {
    private String prefix = "";
    private String variable = "";
    private String operator = "";
    private String value = "";

    /**
     * Constructor for NAD Protocol message
     *
     * @param messageBuilder
     */
    private NadMessage(MessageBuilder messageBuilder) {
        this.prefix = messageBuilder.prefix;
        this.variable = messageBuilder.variable;
        this.operator = messageBuilder.operator;
        this.value = messageBuilder.value;
    }

    /**
     * Method to get the prefix of a command protocol formated response from an NAD Device
     *
     * @return prefix - from an NAD device response (usually Zone or Tuner)
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Method to get the variable portion of a command protocol formated response from an NAD Device
     *
     * @return variable - from an NAD device response (details related to a specific Prefix, e.g. power, volume, etc)
     */
    public String getVariable() {
        return variable;
    }

    /**
     * Method to get the operator portion of a command protocol formated response from an NAD Device
     *
     * @return operator - from an NAD device response (can be one of ?, or =)
     */
    public String getOperator() {
        return operator;
    }

    /**
     * Method to get the value portion of a command protocol formated response from an NAD Device
     *
     * @return value - from an NAD device response (followed by an = sign) is current state of the device setting
     */
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "NadMessage [prefix=" + prefix + ", variable=" + variable + ", operator=" + operator + ", value=" + value
                + "]";
    }

    /**
     * This {@link MessageBuilder} sub-class builds an NAD Protocol Message. The
     * NAD Protocol format has four components (prefix . variable operator value). For example:
     *
     * <ul>
     * <li>Main.Power=On</li>
     * <li>Zone2.VolumeControl=Variable</li>
     * <li>Tuner.FM.Frequency=105.7</li>
     * </ul>
     *
     * @author Dave J Schoepel - Initial contribution
     */
    public static class MessageBuilder {
        private String prefix = "";
        private String variable = "";
        private String operator = "";
        private String value = "";

        /**
         * Method to assign a prefix to be used in the NAD protocol command
         *
         * @param prefix - to be used for the command (usually zone or tuner)
         * @return prefix
         */
        public MessageBuilder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * Method to assign a variable to be used in the NAD protocol command
         *
         * @param variable - to be used in the NAD protocol command (.e.g. details related to a specific
         *            Prefix, e.g. power, volume, etc)
         * @return variable
         */
        public MessageBuilder variable(String variable) {
            this.variable = variable;
            return this;
        }

        /**
         * Method to assign an operator to be used in the NAD protocol command
         * 
         * @param operator - can be one of ?, or =
         * @return operator
         */
        public MessageBuilder operator(String operator) {
            this.operator = operator;
            return this;
        }

        /**
         * Method to assign an value to be used in the NAD protocol command
         *
         * @param value - setting to be changed, can be text or number or +, - depending on the variable
         * @return
         */
        public MessageBuilder value(String value) {
            this.value = value;
            return this;
        }

        /**
         * Method to assemble the four components of a NAD control protocol command
         *
         * @return NadMessage - fully assembled message in the NAD control protocol format
         */
        public NadMessage build() {
            return new NadMessage(this);
        }
    }
}
