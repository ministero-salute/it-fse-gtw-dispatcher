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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.controller.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.controller.IRoutingTableCTL;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.RoutingTableCreateReqDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.RoutingTableUpdateReqDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.RoutingTableResDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.IRoutingTableSRV;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Routing Table Controller implementation
 */
@RestController
@Slf4j
public class RoutingTableCTL implements IRoutingTableCTL {
    
    @Autowired
    private IRoutingTableSRV routingTableService;
    
    @Override
    public ResponseEntity<RoutingTableResDTO> createRoutingEntry(
            RoutingTableCreateReqDTO request,
            HttpServletRequest httpRequest) {
        
        log.info("[START] Creating routing table entry - issuer: {}", request.getIssuer());
        
        RoutingTableResDTO response = routingTableService.createRoutingEntry(request);
        
        log.info("[END] Routing table entry created successfully - issuer: {}", request.getIssuer());
        
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @Override
    public ResponseEntity<RoutingTableResDTO> getRoutingEntryByIssuer(
            String issuer,
            HttpServletRequest httpRequest) {
        
        log.info("[START] Getting routing table entry - issuer: {}", issuer);
        
        RoutingTableResDTO response = routingTableService.getRoutingEntryByIssuer(issuer);
        
        log.info("[END] Routing table entry retrieved successfully - issuer: {}", issuer);
        
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    @Override
    public ResponseEntity<RoutingTableResDTO> getAllRoutingEntries(
            HttpServletRequest httpRequest) {
        
        log.info("[START] Getting all routing table entries");
        
        RoutingTableResDTO response = routingTableService.getAllRoutingEntries();
        
        log.info("[END] All routing table entries retrieved successfully");
        
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    @Override
    public ResponseEntity<RoutingTableResDTO> updateRoutingEntry(
            String issuer,
            RoutingTableUpdateReqDTO request,
            HttpServletRequest httpRequest) {
        
        log.info("[START] Updating routing table entry - issuer: {}", issuer);
        
        RoutingTableResDTO response = routingTableService.updateRoutingEntry(issuer, request);
        
        log.info("[END] Routing table entry updated successfully - issuer: {}", issuer);
        
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    @Override
    public ResponseEntity<RoutingTableResDTO> deleteRoutingEntry(
            String issuer,
            HttpServletRequest httpRequest) {
        
        log.info("[START] Deleting routing table entry - issuer: {}", issuer);
        
        RoutingTableResDTO response = routingTableService.deleteRoutingEntry(issuer);
        
        log.info("[END] Routing table entry deleted successfully - issuer: {}", issuer);
        
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}