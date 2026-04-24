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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.RoutingTableCreateReqDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.RoutingTableUpdateReqDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.ErrorResponseDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.RoutingTableResDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

/**
 * Routing Table Controller
 * Manages CRUD operations for notification endpoint routing configuration
 */
@RequestMapping(path = "/v1")
@Tag(name = "Servizio gestione routing table")
public interface IRoutingTableCTL {

    @PostMapping(value = "/routing-table", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Crea nuova entry nella routing table", description = "Crea una nuova associazione tra ISSUER e endpoint di notifica")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Entry creata con successo", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RoutingTableResDTO.class))),
        @ApiResponse(responseCode = "400", description = "Richiesta non valida", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Non autorizzato", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "409", description = "Entry già esistente per questo ISSUER", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "500", description = "Errore interno del server", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    ResponseEntity<RoutingTableResDTO> createRoutingEntry(
        @Valid @RequestBody RoutingTableCreateReqDTO request,
        HttpServletRequest httpRequest
    );

    @GetMapping(value = "/routing-table/issuer/{issuer}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Recupera entry per ISSUER", description = "Recupera l'entry della routing table associata all'ISSUER specificato")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Entry recuperata con successo", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RoutingTableResDTO.class))),
        @ApiResponse(responseCode = "401", description = "Non autorizzato", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Entry non trovata", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "500", description = "Errore interno del server", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    ResponseEntity<RoutingTableResDTO> getRoutingEntryByIssuer(
        @PathVariable("issuer") @Size(max = 255) String issuer,
        HttpServletRequest httpRequest
    );

    @GetMapping(value = "/routing-table", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Recupera tutte le entry", description = "Recupera tutte le entry della routing table")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Entry recuperate con successo", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RoutingTableResDTO.class))),
        @ApiResponse(responseCode = "401", description = "Non autorizzato", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "500", description = "Errore interno del server", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    ResponseEntity<RoutingTableResDTO> getAllRoutingEntries(
        HttpServletRequest httpRequest
    );

    @PutMapping(value = "/routing-table/{issuer}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Aggiorna entry esistente", description = "Aggiorna l'endpoint di notifica e/o la descrizione per l'ISSUER specificato")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Entry aggiornata con successo", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RoutingTableResDTO.class))),
        @ApiResponse(responseCode = "400", description = "Richiesta non valida", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Non autorizzato", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Entry non trovata", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "500", description = "Errore interno del server", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    ResponseEntity<RoutingTableResDTO> updateRoutingEntry(
        @PathVariable("issuer") @Size(max = 255) String issuer,
        @Valid @RequestBody RoutingTableUpdateReqDTO request,
        HttpServletRequest httpRequest
    );

    @DeleteMapping(value = "/routing-table/{issuer}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Elimina entry", description = "Elimina l'entry della routing table per l'ISSUER specificato")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Entry eliminata con successo", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RoutingTableResDTO.class))),
        @ApiResponse(responseCode = "401", description = "Non autorizzato", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Entry non trovata", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "500", description = "Errore interno del server", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    ResponseEntity<RoutingTableResDTO> deleteRoutingEntry(
        @PathVariable("issuer") @Size(max = 255) String issuer,
        HttpServletRequest httpRequest
    );
}