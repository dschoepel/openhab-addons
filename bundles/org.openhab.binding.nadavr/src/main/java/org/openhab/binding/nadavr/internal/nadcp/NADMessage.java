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

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * This {@link NADMessage} class handles NAD Protocol Messages.
 *
 * @author Dave J Schoepel - Initial contribution
 */
@NonNullByDefault
public class NADMessage {
    private String prefix = "";
    private String variable = "";
    private String operator = "";
    private String value = "";

    private NADMessage(MessageBuilder messageBuilder) {
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
        return "NADMessage [prefix=" + prefix + ", variable=" + variable + ", operator=" + operator + ", value=" + value
                + "]";
    }

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

        public NADMessage build() {
            return new NADMessage(this);
        }
    }
}
