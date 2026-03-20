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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.mongo;

import java.util.List;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.entity.RoutingTableETY;

/**
 * Routing Table Repository interface
 * Manages CRUD operations for notification endpoint routing configuration
 */
public interface IRoutingTableRepo {
    
    /**
     * Insert a new routing table entry
     *
     * @param entity Routing table entity to insert
     * @return Inserted entity with generated ID
     */
    RoutingTableETY insert(RoutingTableETY entity);
    
    /**
     * Find routing table entry by issuer
     *
     * @param issuer JWT Issuer identifier
     * @return RoutingTableETY or null if not found
     */
    RoutingTableETY findByIssuer(String issuer);
    
    /**
     * Find all routing table entries
     *
     * @return List of all routing table entries
     */
    List<RoutingTableETY> findAll();
    
    /**
     * Update an existing routing table entry
     *
     * @param issuer JWT Issuer identifier
     * @param entity Updated routing table entity
     * @return Updated entity
     */
    RoutingTableETY update(String issuer, RoutingTableETY entity);
    
    /**
     * Delete routing table entry by issuer
     *
     * @param issuer JWT Issuer identifier
     * @return true if deleted, false if not found
     */
    boolean deleteByIssuer(String issuer);
    
    /**
     * Check if a routing table entry exists for the given issuer
     *
     * @param issuer JWT Issuer identifier
     * @return true if exists, false otherwise
     */
    boolean existsByIssuer(String issuer);
}