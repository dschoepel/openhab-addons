/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.binding.amazonechocontrol.internal.dto.response;

import org.eclipse.jdt.annotation.NonNull;

/**
 * The {@link ListItemTO} encapsulates a single list item
 *
 * @author Jan N. Klug - Initial contribution
 */
public class ListItemTO {
    public String listId;
    public boolean shoppingListItem;
    public String customerId;
    public long createdDateTime;
    public boolean completed;
    public String id;
    public String value;
    public int version;
    public long updatedDateTime;

    @Override
    public @NonNull String toString() {
        return "ListItemTO{listId='" + listId + "', shoppingListItem=" + shoppingListItem + ", customerId='"
                + customerId + "', createdDateTime=" + createdDateTime + ", completed=" + completed + ", id='" + id
                + "', value='" + value + "', version=" + version + ", updatedDateTime=" + updatedDateTime + "}";
    }
}
