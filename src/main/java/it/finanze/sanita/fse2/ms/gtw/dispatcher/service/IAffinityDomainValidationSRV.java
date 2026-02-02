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

import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.UpdateMetadataReqDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.dto.ValidationResultDTO;

import java.time.LocalDate;

/**
 * Service for Affinity Domain validation of metadata.
 */
public interface IAffinityDomainValidationSRV {

    /**
     * Validates metadata XML against the appropriate Affinity Domain strategy.
     * 
     * @param metadataXml The merged metadata XML to validate
     * @param referenceDate The reference date (typically document creationTime) to determine AD version
     * @return ValidationResultDTO with validation outcome
     */
    ValidationResultDTO validateMergedMetadataUpdate(String metadataXml, LocalDate referenceDate);

    /**
     * Extracts the creationTime from metadata XML and validates against the appropriate AD strategy.
     * 
     * @param metadataXml The merged metadata XML to validate
     * @return ValidationResultDTO with validation outcome
     */
    ValidationResultDTO validateMergedMetadataUpdate(String metadataXml);

    /**
     * Validates the update metadata request based on the specific affinity domain resolver.
     *
     * @param metadataReqDTO The metadata request DTO to validate.
     * @param referenceDate  The reference date (retrieved from metadata creation date).
     * @return ValidationResultDTO with validation outcome.
     */
    ValidationResultDTO validateUpdateMetadataRequest(UpdateMetadataReqDTO metadataReqDTO, LocalDate referenceDate);
}
