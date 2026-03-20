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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.service;

import java.util.List;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.RoutingTableCreateReqDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.RoutingTableUpdateReqDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.RoutingTableResDTO;

/**
 * Routing Table Service interface
 * Manages business logic for notification endpoint routing configuration
 */
public interface IRoutingTableSRV {
    
    /**
     * Create a new routing table entry
     * Validates that the issuer doesn't already exist
     *
     * @param request Create request DTO
     * @return Response DTO with created entry
     * @throws BusinessException if issuer already exists or validation fails
     */
    RoutingTableResDTO createRoutingEntry(RoutingTableCreateReqDTO request);
    
    /**
     * Get routing table entry by issuer
     *
     * @param issuer JWT Issuer identifier
     * @return Response DTO with routing entry
     * @throws NoRecordFoundException if issuer not found
     */
    RoutingTableResDTO getRoutingEntryByIssuer(String issuer);
    
    /**
     * Get all routing table entries
     *
     * @return Response DTO with list of all entries
     */
    RoutingTableResDTO getAllRoutingEntries();
    
    /**
     * Update an existing routing table entry
     * Only updates fields that are provided in the request
     *
     * @param issuer JWT Issuer identifier
     * @param request Update request DTO
     * @return Response DTO with updated entry
     * @throws NoRecordFoundException if issuer not found
     */
    RoutingTableResDTO updateRoutingEntry(String issuer, RoutingTableUpdateReqDTO request);
    
    /**
     * Delete routing table entry by issuer
     *
     * @param issuer JWT Issuer identifier
     * @return Response DTO with deletion confirmation
     * @throws NoRecordFoundException if issuer not found
     */
    RoutingTableResDTO deleteRoutingEntry(String issuer);
}