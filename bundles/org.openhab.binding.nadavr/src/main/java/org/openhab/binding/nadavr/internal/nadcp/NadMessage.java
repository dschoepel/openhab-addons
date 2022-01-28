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

    private NadMessage(MessageBuilder messageBuilder) {
        this.prefix = messageBuilder.prefix;
        this.variable = messageBuilder.variable;
        this.operator = messageBuilder.operator;
        this.value = messageBuilder.value;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getVariable() {
        return variable;
    }

    public void setVariable(String variable) {
        this.variable = variable;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
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

        public MessageBuilder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        public MessageBuilder variable(String variable) {
            this.variable = variable;
            return this;
        }

        public MessageBuilder operator(String operator) {
            this.operator = operator;
            return this;
        }

        public MessageBuilder value(String value) {
            this.value = value;
            return this;
        }

        public NadMessage build() {
            return new NadMessage(this);
        }
    }
}
