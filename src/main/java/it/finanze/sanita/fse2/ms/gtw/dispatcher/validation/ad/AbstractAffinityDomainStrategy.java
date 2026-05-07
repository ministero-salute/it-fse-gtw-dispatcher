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

import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.UpdateMetadataReqDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.dto.MetadataDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.dto.ValidationResultDTO;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Abstract base class for Affinity Domain strategy implementations.
 * Provides common utility methods for validation logic shared across all AD
 * versions.
 * 
 * <p>
 * This class extracts common patterns found in concrete strategy
 * implementations:
 * <ul>
 * <li>Field validation and collection logic</li>
 * <li>Result building with consistent error handling</li>
 * <li>Template methods for common validation flows</li>
 * </ul>
 * 
 * <p>
 * Concrete strategy classes should:
 * <ul>
 * <li>Extend this class</li>
 * <li>Define version-specific mandatory field sets</li>
 * <li>Implement {@link #versionId()} and {@link #effectiveFrom()}</li>
 * <li>Use protected utility methods for validation logic</li>
 * </ul>
 * 
 * @see AffinityDomainStrategy
 * @since 2.1
 */
@Slf4j
public abstract class AbstractAffinityDomainStrategy implements AffinityDomainStrategy {

    // Standard error codes used across all AD versions
    protected static final String ERROR_CODE_MISSING_MANDATORY = "MISSING_MANDATORY_FIELD";
    protected static final String ERROR_CODE_INVALID_VALUE_SET = "INVALID_VALUE_SET";
    protected static final String ERROR_CODE_UNSUPPORTED_FEATURE = "UNSUPPORTED_FEATURE";

    /**
     * Collects missing mandatory fields by comparing present fields against required fields.
     * 
     * <p>This method handles null presentFields gracefully by treating all mandatory fields
     * as missing when presentFields is null.
     *
     * @param presentFields Set of fields present in the metadata (can be null)
     * @param mandatoryFields Set of mandatory fields to check (must not be null)
     * @param prefix Prefix for error messages (e.g., "DocumentEntry", "SubmissionSet")
     * @param missingFields List to collect missing field names (will be modified)
     */
    protected void collectMissingFields(Set<String> presentFields, Set<String> mandatoryFields,
                                       String prefix, List<String> missingFields) {
        if (presentFields == null) {
            // If presentFields is null, all mandatory fields are missing
            mandatoryFields.forEach(field -> missingFields.add(prefix + "." + field));
            return;
        }

        mandatoryFields.stream()
                .filter(field -> !presentFields.contains(field))
                .forEach(field -> missingFields.add(prefix + "." + field));
    }

    /**
     * Validates a single field against a validation predicate.
     * 
     * <p>Only validates non-null field values. Null values are considered valid
     * (use mandatory field validation for required fields).
     *
     * @param fieldValue The field value to validate (can be null)
     * @param validator Predicate to validate the field (must not be null)
     * @param fieldName Name of the field for error messages
     * @param codeType Type of code for error messages (e.g., "HealthcareFacility", "PracticeSettingCode")
     * @param validationErrors List to collect validation errors (will be modified)
     */
    protected void validateField(String fieldValue, Predicate<String> validator,
                                String fieldName, String codeType, List<String> validationErrors) {
        if (fieldValue != null && !validator.test(fieldValue)) {
            validationErrors.add(String.format(
                    "Campo invalido %s: '%s' non è un %s valido per AD %s",
                    fieldName, fieldValue, codeType, versionId()));
        }
    }

    /**
     * Validates a list of field values against a validation predicate.
     *
     * <p>Validates each non-null value in the list. Null values and empty lists are considered valid.
     * Each invalid value generates a separate validation error.
     *
     * @param fieldValues List of field values to validate (can be null or empty)
     * @param validator Predicate to validate each field value (must not be null)
     * @param fieldName Name of the field for error messages
     * @param codeType Type of code for error messages (e.g., "EventCode", "TypeCode")
     * @param validationErrors List to collect validation errors (will be modified)
     */
    protected void validateFieldList(List<String> fieldValues, Predicate<String> validator,
                                    String fieldName, String codeType, List<String> validationErrors) {
        if (fieldValues != null) {
            for (String fieldValue : fieldValues) {
                if (fieldValue != null && !validator.test(fieldValue)) {
                    validationErrors.add(String.format(
                            "Campo invalido %s: '%s' non è un %s valido per AD %s",
                            fieldName, fieldValue, codeType, versionId()));
                }
            }
        }
    }

    /**
     * Validates that a single-value field is not present (unsupported feature).
     *
     * <p>Checks if a field value is not null and adds an error if it's present,
     * indicating the field is not supported in this AD version.
     *
     * @param fieldValue The field value to check (can be null)
     * @param fieldName Name of the field for error messages
     * @param validationErrors List to collect validation errors (will be modified)
     */
    protected void validateUnsupportedField(String fieldValue, String fieldName, List<String> validationErrors) {
        if (fieldValue != null) {
            validationErrors.add(String.format(
                    "%s metadato non supportato da AD %s", fieldName, versionId()));
        }
    }

    /**
     * Validates that a list-value field is not present (unsupported feature).
     *
     * <p>Checks if a field list is not null and not empty, and adds an error if it contains values,
     * indicating the field is not supported in this AD version.
     *
     * @param fieldValues List of field values to check (can be null)
     * @param fieldName Name of the field for error messages
     * @param validationErrors List to collect validation errors (will be modified)
     */
    protected void validateUnsupportedField(List<String> fieldValues, String fieldName, List<String> validationErrors) {
        if (fieldValues != null && !fieldValues.isEmpty()) {
            validationErrors.add(String.format(
                    "%s metadato non supportato da AD %s", fieldName, versionId()));
        }
    }

    /**
     * Builds a successful validation result.
     * 
     * <p>Creates a ValidationResultDTO with:
     * <ul>
     *   <li>valid = true</li>
     *   <li>adVersion = current version ID</li>
     *   <li>missingFields = empty list</li>
     * </ul>
     *
     * @return ValidationResultDTO indicating success
     */
    protected ValidationResultDTO buildSuccessResult() {
        return ValidationResultDTO.builder()
                .valid(true)
                .adVersion(versionId())
                .missingFields(Collections.emptyList())
                .build();
    }

    /**
     * Builds an error validation result.
     * 
     * <p>Creates a ValidationResultDTO with:
     * <ul>
     *   <li>valid = false</li>
     *   <li>adVersion = current version ID</li>
     *   <li>errorCode = provided error code</li>
     *   <li>errorMessage = provided error message</li>
     *   <li>missingFields = provided errors list</li>
     * </ul>
     *
     * @param errorCode The error code (e.g., ERROR_CODE_MISSING_MANDATORY)
     * @param errorMessage The error message describing the validation failure
     * @param errors List of specific errors or missing fields (can be null)
     * @return ValidationResultDTO indicating failure
     */
    protected ValidationResultDTO buildErrorResult(String errorCode, String errorMessage, List<String> errors) {
        return ValidationResultDTO.builder()
                .valid(false)
                .adVersion(versionId())
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .missingFields(errors != null ? errors : Collections.emptyList())
                .build();
    }

    /**
     * Template method for validating mandatory metadata fields.
     * 
     * <p>Implements the common validation flow used by all AD versions:
     * <ol>
     *   <li>Null check on metadata object</li>
     *   <li>Collect missing DocumentEntry fields</li>
     *   <li>Collect missing SubmissionSet fields</li>
     *   <li>Build success or error result</li>
     * </ol>
     * 
     * <p>Concrete strategies should call this method from their
     * {@link #validateMandatoryMetadataIti57Request(MetadataDTO)} implementation,
     * passing their version-specific mandatory field sets.
     *
     * @param metadata The metadata to validate
     * @param mandatoryDocumentEntryFields Set of mandatory DocumentEntry fields for this AD version
     * @param mandatorySubmissionSetFields Set of mandatory SubmissionSet fields for this AD version
     * @return ValidationResultDTO with validation results
     */
    protected ValidationResultDTO validateMandatoryFieldsTemplate(
            MetadataDTO metadata,
            Set<String> mandatoryDocumentEntryFields,
            Set<String> mandatorySubmissionSetFields) {
        
        if (metadata == null) {
            log.error("Metadato non puo essere null per AD version {}", versionId());
            return buildErrorResult(
                    ERROR_CODE_MISSING_MANDATORY,
                    "Metadata object is null",
                    Collections.singletonList("metadata")
            );
        }

        log.debug("Validating metadata against AD version {}", versionId());

        // Collect missing fields
        List<String> missingFields = new ArrayList<>();
        collectMissingFields(metadata.getDocumentEntryFields(), mandatoryDocumentEntryFields, 
                "DocumentEntry", missingFields);
        collectMissingFields(metadata.getSubmissionSetFields(), mandatorySubmissionSetFields, 
                "SubmissionSet", missingFields);

        // Build and return result
        if (missingFields.isEmpty()) {
            log.info("Validazione OK per AD {}", versionId());
            return buildSuccessResult();
        } else {
            log.warn("Validazione fallita per AD {}: missing {} field(s)", versionId(), missingFields.size());
            return buildErrorResult(
                    ERROR_CODE_MISSING_MANDATORY,
                    String.format("Missing mandatory fields for AD %s: %s", 
                            versionId(), String.join(", ", missingFields)),
                    missingFields
            );
        }
    }

    /**
     * Template method for validating UpdateMetadataReqDTO with common error handling.
     * 
     * <p>Implements the common validation flow:
     * <ol>
     *   <li>Null check on request object</li>
     *   <li>Delegate to subclass for value set validation</li>
     *   <li>Build success or error result</li>
     * </ol>
     * 
     * <p>Concrete strategies should override {@link #validateValueSetsInternal(UpdateMetadataReqDTO, List)}
     * to provide version-specific value set validation logic.
     *
     * @param request The update metadata request to validate
     * @return ValidationResultDTO with validation results
     */
    protected ValidationResultDTO validateUpdateMetadataReqDTOTemplate(UpdateMetadataReqDTO request) {
        if (request == null) {
            log.error("UpdateMetadataReqDTO non puo' essere null per AD version {}", versionId());
            return buildErrorResult(
                    ERROR_CODE_MISSING_MANDATORY,
                    "Request object is null",
                    Collections.singletonList("request")
            );
        }

        log.debug("Validating UpdateMetadataReqDTO against AD version {}", versionId());

        List<String> validationErrors = new ArrayList<>();

        // Delegate to subclass for value set validation
        validateValueSetsInternal(request, validationErrors);

        // Build and return result
        if (validationErrors.isEmpty()) {
            log.debug("UpdateMetadataReqDTO validazione OK per AD {}", versionId());
            return buildSuccessResult();
        } else {
            log.warn("UpdateMetadataReqDTO validazione fallita per AD {}: {} error(s)",
                            versionId(), validationErrors.size());
            return buildErrorResult(
                    ERROR_CODE_INVALID_VALUE_SET,
                    String.format("Invalid value set for AD %s: %s", 
                            versionId(), String.join(", ", validationErrors)),
                    validationErrors
            );
        }
    }

    /**
     * Hook method for subclasses to implement version-specific value set validation.
     * 
     * subclasses should override this method to validate value sets against their version-specific enums.
     * 
     * @param request The update metadata request to validate
     * @param validationErrors List to collect validation errors (will be modified)
     */
    protected void validateValueSetsInternal(UpdateMetadataReqDTO request, List<String> validationErrors) {
        // Default implementation: no value set validation
        // Subclasses should override to provide version-specific validation
    }
}
