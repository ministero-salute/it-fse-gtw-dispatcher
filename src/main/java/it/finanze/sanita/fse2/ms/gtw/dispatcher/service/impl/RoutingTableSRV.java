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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.RoutingTableCreateReqDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.RoutingTableUpdateReqDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.ErrorResponseDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.RoutingTableResDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.NoRecordFoundException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.entity.RoutingTableETY;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.mongo.IRoutingTableRepo;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.IRoutingTableSRV;
import lombok.extern.slf4j.Slf4j;

/**
 * Routing Table Service implementation
 * Manages business logic for notification endpoint routing configuration
 */
@Service
@Slf4j
public class RoutingTableSRV implements IRoutingTableSRV {
    
    @Autowired
    private IRoutingTableRepo routingTableRepo;
    
    @Override
    public RoutingTableResDTO createRoutingEntry(RoutingTableCreateReqDTO request) {
        log.info("Creating routing table entry for issuer: {}", request.getIssuer());
        
        // Check if issuer already exists
        if (routingTableRepo.existsByIssuer(request.getIssuer())) {
            log.warn("Routing table entry already exists for issuer: {}", request.getIssuer());
            throw new BusinessException(
                ErrorResponseDTO.builder()
                    .type("Duplicate Entry")
                    .title("Routing entry already exists")
                    .detail("A routing table entry already exists for issuer: " + request.getIssuer())
                    .status(409)
                    .instance("POST /v1/routing-table")
                    .build()
            );
        }
        
        // Create entity
        RoutingTableETY entity = new RoutingTableETY();
        entity.setIssuer(request.getIssuer());
        entity.setNotificationEndpoint(request.getNotificationEndpoint());
        entity.setDescription(request.getDescription());
        
        // Insert into database
        RoutingTableETY created = routingTableRepo.insert(entity);
        
        log.info("Successfully created routing table entry for issuer: {}", request.getIssuer());
        
        return mapToResponseDTO(created, "Routing table entry created successfully");
    }
    
    @Override
    public RoutingTableResDTO getRoutingEntryByIssuer(String issuer) {
        log.info("Getting routing table entry for issuer: {}", issuer);
        
        RoutingTableETY entity = routingTableRepo.findByIssuer(issuer);
        
        if (entity == null) {
            log.warn("No routing table entry found for issuer: {}", issuer);
            throw new NoRecordFoundException(
                ErrorResponseDTO.builder()
                    .type("Not Found")
                    .title("Routing entry not found")
                    .detail("No routing table entry found for issuer: " + issuer)
                    .status(404)
                    .instance("GET /v1/routing-table/issuer/" + issuer)
                    .build()
            );
        }
        
        log.info("Successfully retrieved routing table entry for issuer: {}", issuer);
        
        return mapToResponseDTO(entity, "Routing table entry retrieved successfully");
    }
    
    @Override
    public RoutingTableResDTO getAllRoutingEntries() {
        log.info("Getting all routing table entries");
        
        List<RoutingTableETY> entities = routingTableRepo.findAll();
        
        List<RoutingTableResDTO> entries = entities.stream()
            .map(entity -> mapToResponseDTO(entity, null))
            .collect(Collectors.toList());
        
        log.info("Successfully retrieved {} routing table entries", entries.size());
        
        return RoutingTableResDTO.builder()
            .message("Retrieved " + entries.size() + " routing table entries")
            .entries(entries)
            .build();
    }
    
    @Override
    public RoutingTableResDTO updateRoutingEntry(String issuer, RoutingTableUpdateReqDTO request) {
        log.info("Updating routing table entry for issuer: {}", issuer);
        
        // Check if entry exists
        if (!routingTableRepo.existsByIssuer(issuer)) {
            log.warn("No routing table entry found to update for issuer: {}", issuer);
            throw new NoRecordFoundException(
                ErrorResponseDTO.builder()
                    .type("Not Found")
                    .title("Routing entry not found")
                    .detail("No routing table entry found for issuer: " + issuer)
                    .status(404)
                    .instance("PUT /v1/routing-table/" + issuer)
                    .build()
            );
        }
        
        // Validate that at least one field is provided for update
        if (request.getNotificationEndpoint() == null && request.getDescription() == null) {
            log.warn("No fields provided for update for issuer: {}", issuer);
            throw new BusinessException(
                ErrorResponseDTO.builder()
                    .type("Invalid Request")
                    .title("No fields to update")
                    .detail("At least one field (notificationEndpoint or description) must be provided for update")
                    .status(400)
                    .instance("PUT /v1/routing-table/" + issuer)
                    .build()
            );
        }
        
        // Create entity with update fields
        RoutingTableETY updateEntity = new RoutingTableETY();
        updateEntity.setNotificationEndpoint(request.getNotificationEndpoint());
        updateEntity.setDescription(request.getDescription());
        
        // Update in database
        RoutingTableETY updated = routingTableRepo.update(issuer, updateEntity);
        
        log.info("Successfully updated routing table entry for issuer: {}", issuer);
        
        return mapToResponseDTO(updated, "Routing table entry updated successfully");
    }
    
    @Override
    public RoutingTableResDTO deleteRoutingEntry(String issuer) {
        log.info("Deleting routing table entry for issuer: {}", issuer);
        
        boolean deleted = routingTableRepo.deleteByIssuer(issuer);
        
        if (!deleted) {
            log.warn("No routing table entry found to delete for issuer: {}", issuer);
            throw new NoRecordFoundException(
                ErrorResponseDTO.builder()
                    .type("Not Found")
                    .title("Routing entry not found")
                    .detail("No routing table entry found for issuer: " + issuer)
                    .status(404)
                    .instance("DELETE /v1/routing-table/" + issuer)
                    .build()
            );
        }
        
        log.info("Successfully deleted routing table entry for issuer: {}", issuer);
        
        return RoutingTableResDTO.builder()
            .issuer(issuer)
            .message("Routing table entry deleted successfully")
            .build();
    }
    
    /**
     * Map entity to response DTO
     *
     * @param entity Routing table entity
     * @param message Response message
     * @return Response DTO
     */
    private RoutingTableResDTO mapToResponseDTO(RoutingTableETY entity, String message) {
        return RoutingTableResDTO.builder()
            .id(entity.getId())
            .issuer(entity.getIssuer())
            .notificationEndpoint(entity.getNotificationEndpoint())
            .description(entity.getDescription())
            .insertionDate(entity.getInsertionDate())
            .message(message)
            .build();
    }
}