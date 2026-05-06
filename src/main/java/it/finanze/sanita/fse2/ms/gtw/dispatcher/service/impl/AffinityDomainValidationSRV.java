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

import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.IAffinityDomainValidationSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.AffinityDomainUtility;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.AffinityDomainStrategy;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.AffinityDomainStrategyResolver;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.PresentMetadataIndexer;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.dto.MetadataDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.dto.ValidationResultDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.JWTPayloadDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.UpdateMetadataReqDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ValidationException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.ErrorResponseDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.ErrorInstanceEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.RestExecutionResultEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Service implementation for Affinity Domain validation.
 * Orchestrates the validation flow: extract creationTime -> resolve strategy ->
 * parse XML -> validate.
 */
@Slf4j
@Service
public class AffinityDomainValidationSRV implements IAffinityDomainValidationSRV {

    @Autowired
    private AffinityDomainUtility affinityDomainUtility;

    @Autowired
    private AffinityDomainStrategyResolver strategyResolver;

    @Autowired
    private PresentMetadataIndexer presentMetadataIndexer;

    @Override
    public ValidationResultDTO validateMergedMetadataUpdate(String metadataXml, LocalDate referenceDate) {
        log.info("Starting Affinity Domain validation for reference date: {}", referenceDate);

        try {
            MetadataDTO metadata = presentMetadataIndexer.extractPresentFields(metadataXml);
            AffinityDomainStrategy strategy = strategyResolver.resolve(referenceDate);
            ValidationResultDTO result = strategy.validateMandatoryMetadataIti57Request(metadata);

            log.info("Affinity Domain validation completed: valid={}, adVersion={}",
                    result.isValid(), result.getAdVersion());

            return result;

        } catch (Exception e) {
            log.error("Error during Affinity Domain validation", e);
            return ValidationResultDTO.builder()
                    .valid(false)
                    .errorCode("VALIDATION_ERROR")
                    .errorMessage("Affinity Domain validation failed: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public ValidationResultDTO validateMergedMetadataUpdate(String metadataXml) {
        log.info("Starting Affinity Domain validation with date extraction");
        LocalDate creationDate = affinityDomainUtility.extractCreationTime(metadataXml);
        log.info("Extracted creationTime: {}", creationDate);
        return validateMergedMetadataUpdate(metadataXml, creationDate);
    }

    @Override
    public ValidationResultDTO validateUpdateMetadataRequest(UpdateMetadataReqDTO metadataReqDTO,
            LocalDate referenceDate,
            JWTPayloadDTO jwtPayloadToken) {
        log.info("Validating UpdateMetadataReqDTO against AD resolved for date: {}", referenceDate);
        try {
            AffinityDomainStrategy strategy = strategyResolver.resolve(referenceDate);
            ValidationResultDTO result = strategy.validateUpdateMetadataReqDTO(metadataReqDTO, jwtPayloadToken);

            if (!result.isValid()) {
                String detail = result.getErrorMessage() != null ? result.getErrorMessage() : "Invalid value in request";
                final ErrorResponseDTO error = ErrorResponseDTO.builder()
                        .type(RestExecutionResultEnum.VALIDATOR_ERROR.getType())
                        .title(RestExecutionResultEnum.VALIDATOR_ERROR.getTitle())
                        .instance(ErrorInstanceEnum.AD_MISSING_MANDATORY_FIELD.getInstance())
                        .detail(detail)
                        .build();
                throw new ValidationException(error);
            }

            return ValidationResultDTO.builder().valid(true).adVersion(strategy.versionId()).build();
            
        } catch (ValidationException ve) {
            throw ve;
        } catch (Exception e) {
            log.error("Error while validating UpdateMetadataReqDTO", e);
            return ValidationResultDTO.builder()
                    .valid(false)
                    .errorCode("VALIDATION_ERROR")
                    .errorMessage("Affinity Domain request validation failed: " + e.getMessage())
                    .build();
        }
    }
}