/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * 
 * Copyright (C) 2023 Ministero della Salute
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.entity;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing the routing table for notification endpoints.
 * Maps JWT ISSUER identifiers to notification endpoints.
 */
@Document(collection = "#{@routingTableBean}")
@Data
@NoArgsConstructor
public class RoutingTableETY {

    public static final String FIELD_ID = "_id";
    public static final String FIELD_ISSUER = "issuer";
    public static final String FIELD_NOTIFICATION_ENDPOINT = "notification_endpoint";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_INSERTION_DATE = "insertion_date";

    /**
     * MongoDB document ID
     */
    @Id
    private String id;

    /**
     * JWT Issuer identifier (unique)
     */
    @Field(name = FIELD_ISSUER)
    private String issuer;

    /**
     * Notification endpoint URL
     */
    @Field(name = FIELD_NOTIFICATION_ENDPOINT)
    private String notificationEndpoint;

    /**
     * Human-readable description of the routing entry
     */
    @Field(name = FIELD_DESCRIPTION)
    private String description;

    /**
     * Date when the entry was created
     */
    @Field(name = FIELD_INSERTION_DATE)
    private Date insertionDate;
}