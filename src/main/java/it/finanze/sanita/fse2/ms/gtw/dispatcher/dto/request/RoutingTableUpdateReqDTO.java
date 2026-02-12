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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating an existing routing table entry
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoutingTableUpdateReqDTO {
    
    /**
     * Notification endpoint URL (must be HTTPS)
     * Optional - only updated if provided
     */
    @Pattern(regexp = "^https://.*", message = "Notification endpoint must be a valid HTTPS URL")
    @Size(max = 500, message = "Notification endpoint must not exceed 500 characters")
    private String notificationEndpoint;
    
    /**
     * Human-readable description
     * Optional - only updated if provided
     */
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
}