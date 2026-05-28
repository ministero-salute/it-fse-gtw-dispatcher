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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.entity.IssuerETY;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.mongo.IIssuerRepo;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.impl.IssuerSRV;

/**
 * Unit tests for IssuerSRV service.
 * Tests the business logic for FHIR bundle configuration per issuer.
 * Uses Mockito without Spring context for faster, isolated unit tests.
 */
@ExtendWith(MockitoExtension.class)
class IssuerSRVTest {

    @Mock
    private IIssuerRepo issuerRepo;

    @InjectMocks
    private IssuerSRV issuerSRV;

    @BeforeEach
    void setUp() {
        // Reset mocks before each test if needed
    }

    @Test
    @DisplayName("Test FHIR bundle enabled when issuer has fhirBundleInResponse = true")
    void testIsFhirBundleEnabledForIssuer_WhenTrue() {
        // Arrange
        String issuerName = "test-issuer";
        IssuerETY issuer = new IssuerETY();
        issuer.setIssuer(issuerName);
        issuer.setFhirBundleInResponse(true);
        
        when(issuerRepo.getByName(issuerName)).thenReturn(issuer);

        // Act
        boolean result = issuerSRV.isFhirBundleEnabledForIssuer(issuerName);

        // Assert
        assertTrue(result, "FHIR bundle should be enabled when field is true");
    }

    @Test
    @DisplayName("Test FHIR bundle disabled when issuer has fhirBundleInResponse = false")
    void testIsFhirBundleEnabledForIssuer_WhenFalse() {
        // Arrange
        String issuerName = "test-issuer";
        IssuerETY issuer = new IssuerETY();
        issuer.setIssuer(issuerName);
        issuer.setFhirBundleInResponse(false);
        
        when(issuerRepo.getByName(issuerName)).thenReturn(issuer);

        // Act
        boolean result = issuerSRV.isFhirBundleEnabledForIssuer(issuerName);

        // Assert
        assertFalse(result, "FHIR bundle should be disabled when field is false");
    }

    @Test
    @DisplayName("Test FHIR bundle disabled when issuer has fhirBundleInResponse = null")
    void testIsFhirBundleEnabledForIssuer_WhenNull() {
        // Arrange
        String issuerName = "test-issuer";
        IssuerETY issuer = new IssuerETY();
        issuer.setIssuer(issuerName);
        issuer.setFhirBundleInResponse(null);
        
        when(issuerRepo.getByName(issuerName)).thenReturn(issuer);

        // Act
        boolean result = issuerSRV.isFhirBundleEnabledForIssuer(issuerName);

        // Assert
        assertFalse(result, "FHIR bundle should be disabled when field is null");
    }

    @Test
    @DisplayName("Test FHIR bundle disabled when issuer not found in database")
    void testIsFhirBundleEnabledForIssuer_WhenIssuerNotFound() {
        // Arrange
        String issuerName = "non-existent-issuer";
        when(issuerRepo.getByName(issuerName)).thenReturn(null);

        // Act
        boolean result = issuerSRV.isFhirBundleEnabledForIssuer(issuerName);

        // Assert
        assertFalse(result, "FHIR bundle should be disabled when issuer not found");
    }

    @Test
    @DisplayName("Test FHIR bundle disabled when repository throws exception (fail-safe)")
    void testIsFhirBundleEnabledForIssuer_WhenException() {
        // Arrange
        String issuerName = "test-issuer";
        when(issuerRepo.getByName(anyString())).thenThrow(new RuntimeException("Database error"));

        // Act
        boolean result = issuerSRV.isFhirBundleEnabledForIssuer(issuerName);

        // Assert
        assertFalse(result, "FHIR bundle should be disabled on exception (fail-safe)");
    }

    @Test
    @DisplayName("Test FHIR bundle enabled with Boolean.TRUE object")
    void testIsFhirBundleEnabledForIssuer_WithBooleanTrueObject() {
        // Arrange
        String issuerName = "test-issuer";
        IssuerETY issuer = new IssuerETY();
        issuer.setIssuer(issuerName);
        issuer.setFhirBundleInResponse(Boolean.TRUE);
        
        when(issuerRepo.getByName(issuerName)).thenReturn(issuer);

        // Act
        boolean result = issuerSRV.isFhirBundleEnabledForIssuer(issuerName);

        // Assert
        assertTrue(result, "FHIR bundle should be enabled with Boolean.TRUE");
    }

    @Test
    @DisplayName("Test FHIR bundle disabled with Boolean.FALSE object")
    void testIsFhirBundleEnabledForIssuer_WithBooleanFalseObject() {
        // Arrange
        String issuerName = "test-issuer";
        IssuerETY issuer = new IssuerETY();
        issuer.setIssuer(issuerName);
        issuer.setFhirBundleInResponse(Boolean.FALSE);
        
        when(issuerRepo.getByName(issuerName)).thenReturn(issuer);

        // Act
        boolean result = issuerSRV.isFhirBundleEnabledForIssuer(issuerName);

        // Assert
        assertFalse(result, "FHIR bundle should be disabled with Boolean.FALSE");
    }

    @Test
    @DisplayName("Test multiple issuers with different configurations")
    void testIsFhirBundleEnabledForIssuer_MultipleIssuers() {
        // Arrange - Issuer 1 (enabled)
        String issuer1Name = "issuer-enabled";
        IssuerETY issuer1 = new IssuerETY();
        issuer1.setIssuer(issuer1Name);
        issuer1.setFhirBundleInResponse(true);
        
        // Arrange - Issuer 2 (disabled)
        String issuer2Name = "issuer-disabled";
        IssuerETY issuer2 = new IssuerETY();
        issuer2.setIssuer(issuer2Name);
        issuer2.setFhirBundleInResponse(false);
        
        when(issuerRepo.getByName(issuer1Name)).thenReturn(issuer1);
        when(issuerRepo.getByName(issuer2Name)).thenReturn(issuer2);

        // Act
        boolean result1 = issuerSRV.isFhirBundleEnabledForIssuer(issuer1Name);
        boolean result2 = issuerSRV.isFhirBundleEnabledForIssuer(issuer2Name);

        // Assert
        assertTrue(result1, "First issuer should have FHIR bundle enabled");
        assertFalse(result2, "Second issuer should have FHIR bundle disabled");
    }

    @Test
    @DisplayName("Test with null issuer name")
    void testIsFhirBundleEnabledForIssuer_WithNullIssuerName() {
        // Arrange
        when(issuerRepo.getByName(null)).thenReturn(null);

        // Act
        boolean result = issuerSRV.isFhirBundleEnabledForIssuer(null);

        // Assert
        assertFalse(result, "FHIR bundle should be disabled with null issuer name");
    }

    @Test
    @DisplayName("Test with empty issuer name")
    void testIsFhirBundleEnabledForIssuer_WithEmptyIssuerName() {
        // Arrange
        String issuerName = "";
        when(issuerRepo.getByName(issuerName)).thenReturn(null);

        // Act
        boolean result = issuerSRV.isFhirBundleEnabledForIssuer(issuerName);

        // Assert
        assertFalse(result, "FHIR bundle should be disabled with empty issuer name");
    }
}