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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.JWTPayloadDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.ErrorInstanceEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.RestExecutionResultEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ValidationException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.impl.JwtSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.impl.UtilitySRV;

/**
 * Verifica la coerenza di action_id con il servizio invocato
 * (Specifiche Interfacce REST Gateway, par. 16.3.12, Tabella 62 "action-id").
 */
@DisplayName("JwtSRV - coerenza action_id")
class JwtSRVActionIdTest {

	@Mock
	private UtilitySRV utilitySrv;

	@InjectMocks
	private JwtSRV jwtSRV;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		when(utilitySrv.isValidCf(anyString())).thenReturn(true);
	}

	private JWTPayloadDTO payload(final String actionId) {
		return JWTPayloadDTO.builder()
				.iss("080")
				.sub("RSSMRA22A01A399Z")
				.subject_organization_id("080")
				.subject_organization("Regione Emilia-Romagna")
				.locality("GTW_NAME^^^^^&2.16.840.1.113883.2.9.4.1.3&ISO^^^^GTW_STRUCTURE")
				.subject_role("APR")
				.person_id("RSSMRA22A01A399Z")
				.patient_consent(true)
				.purpose_of_use("TREATMENT")
				.resource_hl7_type("('11488-4^^2.16.840.1.113883.6.1')")
				.action_id(actionId)
				.subject_application_id("ApplicationId")
				.subject_application_vendor("ApplicationVendor")
				.subject_application_version("ApplicationVersion")
				.build();
	}

	private void assertActionIdRejected(final org.junit.jupiter.api.function.Executable call) {
		final ValidationException ex = assertThrows(ValidationException.class, call);
		assertEquals(RestExecutionResultEnum.INVALID_TOKEN_FIELD.getType(), ex.getError().getType());
		assertEquals(ErrorInstanceEnum.JWT_MALFORMED_FIELD.getInstance(), ex.getError().getInstance());
		assertEquals("Il campo action_id non è coerente con le operazioni richieste", ex.getError().getDetail());
	}

	@Test
	@DisplayName("Validazione: CREATE accettato")
	@Tag("Issue 3194")
	void validationAcceptsCreate() {
		assertDoesNotThrow(() -> jwtSRV.validatePayloadForValidation(payload("CREATE")));
	}

	@Test
	@DisplayName("Validazione: UPDATE rifiutato con /msg/jwt-validation")
	@Tag("Issue 3194")
	void validationRejectsUpdate() {
		assertActionIdRejected(() -> jwtSRV.validatePayloadForValidation(payload("UPDATE")));
	}

	@Test
	@DisplayName("Validazione: DELETE rifiutato con /msg/jwt-validation")
	@Tag("Issue 3194")
	void validationRejectsDelete() {
		assertActionIdRejected(() -> jwtSRV.validatePayloadForValidation(payload("DELETE")));
	}

	@Test
	@DisplayName("Validazione: READ rifiutato con /msg/jwt-validation")
	@Tag("Issue 3194")
	void validationRejectsRead() {
		assertActionIdRejected(() -> jwtSRV.validatePayloadForValidation(payload("READ")));
	}

	@Test
	@DisplayName("Create/Replace FHIR: CREATE e UPDATE accettati")
	@Tag("Issue 3194")
	void fhirPublicationAcceptsCreateAndUpdate() {
		assertDoesNotThrow(() -> jwtSRV.validatePayloadForFhirPublication(payload("CREATE")));
		assertDoesNotThrow(() -> jwtSRV.validatePayloadForFhirPublication(payload("UPDATE")));
	}

	@Test
	@DisplayName("Create/Replace FHIR: DELETE rifiutato")
	@Tag("Issue 3194")
	void fhirPublicationRejectsDelete() {
		assertActionIdRejected(() -> jwtSRV.validatePayloadForFhirPublication(payload("DELETE")));
	}

	@Test
	@DisplayName("Create/Replace FHIR: READ rifiutato")
	@Tag("Issue 3194")
	void fhirPublicationRejectsRead() {
		assertActionIdRejected(() -> jwtSRV.validatePayloadForFhirPublication(payload("READ")));
	}
}
