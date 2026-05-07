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
package it.finanze.sanita.fse2.ms.gtw.dispatcher;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.JWTPayloadDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.impl.AffinityDomainValidationSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.AffinityDomainUtility;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.AffinityDomainStrategy;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.AffinityDomainStrategyResolver;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.PresentMetadataIndexer;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.dto.MetadataDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.dto.ValidationResultDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Affinity Domain Validation Service Test")
class AffinityDomainValidationSRVTest {

    @Mock
    private AffinityDomainStrategyResolver resolver;

    @Mock
    private PresentMetadataIndexer indexer;

    @Mock
    private AffinityDomainUtility affinityDomainUtility;

    @Mock
    private AffinityDomainStrategy mockStrategy;

    @InjectMocks
    private AffinityDomainValidationSRV service;

    private MetadataDTO metadata;
    private JWTPayloadDTO jwtPayloadDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        metadata = new MetadataDTO();
        metadata.setDocumentEntryFields(new HashSet<>(Arrays.asList(
            "slot:creationTime",
            "classification:urn:uuid:author",
            "externalId:urn:uuid:patientId"
        )));
        metadata.setSubmissionSetFields(new HashSet<>(Arrays.asList(
            "slot:submissionTime",
            "externalId:urn:uuid:sourceId"
        )));

        // Initialize JWT payload for tests
        jwtPayloadDTO = new JWTPayloadDTO(
                "test-issuer", // iss
                System.currentTimeMillis() / 1000, // iat
                System.currentTimeMillis() / 1000 + 3600, // exp
                "test-jti", // jti
                "test-audience", // aud
                "test-subject", // sub
                "test-org-id", // subject_organization_id
                "test-org", // subject_organization
                "test-locality", // locality
                "test-role", // subject_role
                "RSSMRA80A01H501U", // person_id
                true, // patient_consent
                "TREATMENT", // purpose_of_use
                "test-resource-type", // resource_hl7_type
                "CREATE", // action_id
                "test-hash", // attachment_hash
                "test-app-id", // subject_application_id
                "test-vendor", // subject_application_vendor
                "1.0.0", // subject_application_version
                false // use_subject_as_author
        );
    }

    @Test
    @DisplayName("Should validate successfully with extracted creationTime")
    void shouldValidateSuccessfullyWithExtractedDate() throws IOException, URISyntaxException {
        String xml = buildValidXml();
        LocalDate creationDate = LocalDate.of(2026, 1, 15);
        
        // Mock behavior
        when(affinityDomainUtility.extractCreationTime(xml)).thenReturn(creationDate);
        when(resolver.resolve(creationDate)).thenReturn(mockStrategy);
        when(indexer.extractPresentFields(xml)).thenReturn(metadata);
        
        ValidationResultDTO validResult = ValidationResultDTO.builder()
                .valid(true)
                .adVersion("TEST")
                .build();
        when(mockStrategy.validateMandatoryMetadataIti57Request(metadata)).thenReturn(validResult);
        
        // Execute
        ValidationResultDTO result = service.validateMergedMetadataUpdate(xml);
        
        // Verify
        assertNotNull(result);
        assertTrue(result.isValid());
        assertEquals("TEST", result.getAdVersion());
        
        verify(affinityDomainUtility).extractCreationTime(xml);
        verify(resolver).resolve(creationDate);
        verify(indexer).extractPresentFields(xml);
    }

    @Test
    @DisplayName("Should validate successfully with explicit reference date")
    void shouldValidateSuccessfullyWithExplicitDate() throws IOException, URISyntaxException {
        String xml = buildValidXml();
        LocalDate referenceDate = LocalDate.of(2026, 1, 15);
        
        // Mock behavior
        when(resolver.resolve(referenceDate)).thenReturn(mockStrategy);
        when(indexer.extractPresentFields(xml)).thenReturn(metadata);
        
        ValidationResultDTO validResult = ValidationResultDTO.builder()
                .valid(true)
                .adVersion("TEST")
                .build();
        when(mockStrategy.validateMandatoryMetadataIti57Request(metadata)).thenReturn(validResult);
        
        // Execute
        ValidationResultDTO result = service.validateMergedMetadataUpdate(xml, referenceDate);
        
        // Verify
        assertNotNull(result);
        assertTrue(result.isValid());
        assertEquals("TEST", result.getAdVersion());
        
        verify(resolver).resolve(referenceDate);
        verify(indexer).extractPresentFields(xml);
        verifyNoInteractions(affinityDomainUtility); // Should not extract date when explicitly provided
    }

    @Test
    @DisplayName("Should fail validation when creationTime extraction fails")
    void shouldFailWhenCreationTimeExtractionFails() throws IOException, URISyntaxException {
        String xml = buildValidXml();
        
        // Mock behavior - extraction fails
        when(affinityDomainUtility.extractCreationTime(xml)).thenThrow(new IllegalArgumentException("Cannot extract creationTime"));
        
        // Execute and verify exception is thrown
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            service.validateMergedMetadataUpdate(xml);
        });
        
        // Verify exception message
        assertTrue(exception.getMessage().contains("creationTime"));
        
        verify(affinityDomainUtility).extractCreationTime(xml);
        verifyNoInteractions(resolver, indexer);
    }

    @Test
    @DisplayName("Should fail validation when strategy resolution fails")
    void shouldFailWhenStrategyResolutionFails() throws IOException, URISyntaxException {
        String xml = buildValidXml();
        LocalDate creationDate = LocalDate.of(2020, 1, 1);
        
        // Mock behavior - strategy resolution fails
        when(affinityDomainUtility.extractCreationTime(xml)).thenReturn(creationDate);
        when(resolver.resolve(creationDate)).thenThrow(new IllegalArgumentException("No strategy for date"));
        when(indexer.extractPresentFields(xml)).thenReturn(metadata);
        
        // Execute
        ValidationResultDTO result = service.validateMergedMetadataUpdate(xml);
        
        // Verify
        assertNotNull(result);
        assertFalse(result.isValid());
        assertNotNull(result.getErrorCode());
        assertNotNull(result.getErrorMessage());
        
        verify(affinityDomainUtility).extractCreationTime(xml);
        verify(resolver).resolve(creationDate);
        verify(indexer).extractPresentFields(xml);
    }

    @Test
    @DisplayName("Should fail validation when mandatory fields are missing")
    void shouldFailWhenMandatoryFieldsMissing() throws IOException, URISyntaxException {
        String xml = buildValidXml();
        LocalDate creationDate = LocalDate.of(2026, 1, 15);
        
        // Mock behavior
        when(affinityDomainUtility.extractCreationTime(xml)).thenReturn(creationDate);
        when(resolver.resolve(creationDate)).thenReturn(mockStrategy);
        when(indexer.extractPresentFields(xml)).thenReturn(metadata);
        
        ValidationResultDTO invalidResult = ValidationResultDTO.builder()
                .valid(false)
                .adVersion("TEST")
                .errorCode("MISSING_MANDATORY")
                .errorMessage("Missing mandatory fields")
                .missingFields(Arrays.asList("DocumentEntry.slot:languageCode"))
                .build();
        when(mockStrategy.validateMandatoryMetadataIti57Request(metadata)).thenReturn(invalidResult);
        
        // Execute
        ValidationResultDTO result = service.validateMergedMetadataUpdate(xml);
        
        // Verify
        assertNotNull(result);
        assertFalse(result.isValid());
        assertEquals("MISSING_MANDATORY", result.getErrorCode());
        assertFalse(result.getMissingFields().isEmpty());
    }

    @Test
    @DisplayName("Should handle null XML gracefully")
    void shouldHandleNullXml() {
        // Execute
        ValidationResultDTO result = service.validateMergedMetadataUpdate(null);
        
        // Verify
        assertNotNull(result);
        assertFalse(result.isValid());
        assertNotNull(result.getErrorMessage());
    }

    @Test
    @DisplayName("Should handle empty XML gracefully")
    void shouldHandleEmptyXml() {
        // Execute
        ValidationResultDTO result = service.validateMergedMetadataUpdate("");
        
        // Verify
        assertNotNull(result);
        assertFalse(result.isValid());
        assertNotNull(result.getErrorMessage());
    }

    @Test
    @DisplayName("Should handle XML parsing errors gracefully")
    void shouldHandleXmlParsingErrors() {
        String malformedXml = "<?xml version=\"1.0\"?><invalid>not closed";
        LocalDate creationDate = LocalDate.of(2026, 1, 15);
        
        // Mock behavior
        when(affinityDomainUtility.extractCreationTime(malformedXml)).thenReturn(creationDate);
        when(resolver.resolve(creationDate)).thenReturn(mockStrategy);
        when(indexer.extractPresentFields(malformedXml)).thenThrow(new RuntimeException("XML parsing error"));
        
        // Execute
        ValidationResultDTO result = service.validateMergedMetadataUpdate(malformedXml);
        
        // Verify
        assertNotNull(result);
        assertFalse(result.isValid());
        assertNotNull(result.getErrorMessage());
    }

    @Test
    @DisplayName("Should validate UpdateMetadataReqDTO successfully")
    void shouldValidateUpdateMetadataReqDTOSuccessfully() {
        // Prepare request
        it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.UpdateMetadataReqDTO request =
            new it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.UpdateMetadataReqDTO();
        request.setTipologiaStruttura("Hospital");
        request.setTipoDocumentoLivAlto("REF");
        request.setAssettoOrganizzativo("SER");
        request.setTipoAttivitaClinica("CON");
        request.setIdentificativoSottomissione("12345");
        
        LocalDate referenceDate = LocalDate.of(2026, 1, 15);
        
        // Mock behavior
        when(resolver.resolve(referenceDate)).thenReturn(mockStrategy);
        
        ValidationResultDTO validResult = ValidationResultDTO.builder()
                .valid(true)
                .adVersion("TEST")
                .build();
        when(mockStrategy.validateUpdateMetadataReqDTO(request, jwtPayloadDTO)).thenReturn(validResult);
        when(mockStrategy.versionId()).thenReturn("TEST");
        
        // Execute
        ValidationResultDTO result = service.validateUpdateMetadataRequest(request, referenceDate, jwtPayloadDTO);
        
        // Verify
        assertNotNull(result);
        assertTrue(result.isValid());
        assertEquals("TEST", result.getAdVersion());
        
        verify(resolver).resolve(referenceDate);
        verify(mockStrategy).validateUpdateMetadataReqDTO(request, jwtPayloadDTO);
    }

    @Test
    @DisplayName("Should throw ValidationException when UpdateMetadataReqDTO validation fails")
    void shouldThrowValidationExceptionWhenUpdateMetadataReqDTOValidationFails() {
        // Prepare request
        it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.UpdateMetadataReqDTO request =
            new it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.UpdateMetadataReqDTO();
        request.setTipologiaStruttura("InvalidType");
        
        LocalDate referenceDate = LocalDate.of(2026, 1, 15);
        
        // Mock behavior - validation fails
        when(resolver.resolve(referenceDate)).thenReturn(mockStrategy);
        
        ValidationResultDTO invalidResult = ValidationResultDTO.builder()
                .valid(false)
                .adVersion("TEST")
                .errorCode("INVALID_VALUE")
                .errorMessage("Invalid tipologiaStruttura value")
                .build();
        when(mockStrategy.validateUpdateMetadataReqDTO(request, jwtPayloadDTO)).thenReturn(invalidResult);
        
        // Execute and verify exception is thrown
        it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ValidationException exception =
            assertThrows(it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ValidationException.class, () -> {
                    service.validateUpdateMetadataRequest(request, referenceDate, jwtPayloadDTO);
            });
        
        // Verify exception details
        assertNotNull(exception.getError());
        assertTrue(exception.getError().getDetail().contains("Invalid tipologiaStruttura"));
        
        verify(resolver).resolve(referenceDate);
        verify(mockStrategy).validateUpdateMetadataReqDTO(request, jwtPayloadDTO);
    }

    @Test
    @DisplayName("Should handle strategy resolution failure for UpdateMetadataReqDTO")
    void shouldHandleStrategyResolutionFailureForUpdateMetadataReqDTO() {
        // Prepare request
        it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.UpdateMetadataReqDTO request =
            new it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.UpdateMetadataReqDTO();
        
        LocalDate referenceDate = LocalDate.of(2020, 1, 1);
        
        // Mock behavior - strategy resolution fails
        when(resolver.resolve(referenceDate)).thenThrow(new IllegalArgumentException("No strategy for date"));
        
        // Execute
        ValidationResultDTO result = service.validateUpdateMetadataRequest(request, referenceDate, jwtPayloadDTO);
        
        // Verify
        assertNotNull(result);
        assertFalse(result.isValid());
        assertEquals("VALIDATION_ERROR", result.getErrorCode());
        assertTrue(result.getErrorMessage().contains("No strategy for date"));
        
        verify(resolver).resolve(referenceDate);
        verifyNoInteractions(mockStrategy);
    }

    @Test
    @DisplayName("Should handle null UpdateMetadataReqDTO")
    void shouldHandleNullUpdateMetadataReqDTO() {
        LocalDate referenceDate = LocalDate.of(2026, 1, 15);
        
        // Mock behavior
        when(resolver.resolve(referenceDate)).thenReturn(mockStrategy);
        when(mockStrategy.validateUpdateMetadataReqDTO(null, jwtPayloadDTO))
                .thenThrow(new NullPointerException("Request cannot be null"));
        
        // Execute
        ValidationResultDTO result = service.validateUpdateMetadataRequest(null, referenceDate, jwtPayloadDTO);
        
        // Verify
        assertNotNull(result);
        assertFalse(result.isValid());
        assertEquals("VALIDATION_ERROR", result.getErrorCode());
        assertNotNull(result.getErrorMessage());
    }

    // Helper methods
    private String buildValidXml() throws IOException, URISyntaxException{
        return new String(java.nio.file.Files.readAllBytes(
                java.nio.file.Paths.get(getClass().getClassLoader()
                        .getResource("merged_metadata.xml").toURI())));
    }

    /**
     * Test strategy for validation testing
     */
    private static class TestStrategy implements AffinityDomainStrategy {
        @Override
        public String versionId() {
            return "TEST";
        }

        @Override
        public LocalDate effectiveFrom() {
            return LocalDate.of(2025, 1, 1);
        }

        @Override
        public ValidationResultDTO validateMandatoryMetadataIti57Request(MetadataDTO metadata) {
            return ValidationResultDTO.builder()
                    .valid(true)
                    .adVersion("TEST")
                    .build();
        }

        @Override
        public ValidationResultDTO validateUpdateMetadataReqDTO(
                it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.UpdateMetadataReqDTO request,
                JWTPayloadDTO jwtPayloadToken) {
            return ValidationResultDTO.builder()
                    .valid(true)
                    .adVersion("TEST")
                    .build();
        }
    }
}
