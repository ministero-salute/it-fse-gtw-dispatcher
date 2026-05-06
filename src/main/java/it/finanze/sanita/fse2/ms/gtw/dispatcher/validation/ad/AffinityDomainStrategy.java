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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad;

import java.time.LocalDate;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.JWTPayloadDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.UpdateMetadataReqDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.dto.MetadataDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.dto.ValidationResultDTO;

/**
 * Strategy interface for Affinity Domain version-specific validation rules.
 * Each implementation represents a specific AD version with its mandatory fields.
 */
public interface AffinityDomainStrategy {
    
    /**
     * Returns the version identifier (e.g., "2.6.2", "2.6.3")
     */
    String versionId();
    
    /**
     * Returns the effective date from which this strategy applies
     */
    LocalDate effectiveFrom();
    
    /**
     * Validates mandatory metadata fields for ITI-57 request.
     *
     * @param metadata The extracted metadata containing present fields
     * @return ValidationResultDTO with validation results
     */
    ValidationResultDTO validateMandatoryMetadataIti57Request(MetadataDTO metadata);
    
    /**
     * Validates the UpdateMetadataReqDTO against AD version-specific value sets.
     * This validation ensures that the input request contains valid codes for input metadata such as:
     * - tipologiaStruttura (HealthcareFacility)
     * - assettoOrganizzativo (PracticeSetting)
     * - tipoAttivitaClinica (AttivitaClinica)
     * - tipoDocumentoLivAlto (TipoDocAltoLiv)
     * - administrativeRequest
     *
     * @param request The update metadata request DTO to validate
     * @return ValidationResultDTO with validation results
     */
    ValidationResultDTO validateUpdateMetadataReqDTO(UpdateMetadataReqDTO request, JWTPayloadDTO jwtPayloadToken);

}
