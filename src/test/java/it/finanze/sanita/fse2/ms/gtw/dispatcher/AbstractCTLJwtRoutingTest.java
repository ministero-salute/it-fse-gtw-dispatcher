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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.MicroservicesURLCFG;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.controller.impl.AbstractCTL;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.JWTPayloadDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.EventTypeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.IJwtSRV;

/**
 * Verifica che {@code AbstractCTL#extractAndValidateJWT} instradi ogni tipo di evento
 * sul metodo di validazione JWT corretto di {@link IJwtSRV}.
 * <ul>
 * <li>VALIDATION e FHIR_VALIDATION: solo action_id CREATE (Tabella 62, par. 16.3.12);</li>
 * <li>FHIR_CREATE e FHIR_REPLACE: CREATE o UPDATE (publication FHIR).</li>
 * </ul>
 */
@DisplayName("AbstractCTL - instradamento validazione JWT per evento")
class AbstractCTLJwtRoutingTest {

	private static class TestCTL extends AbstractCTL {
		JWTPayloadDTO run(final String jwt, final EventTypeEnum eventType) {
			return extractAndValidateJWT(jwt, eventType);
		}
	}

	private IJwtSRV jwtSRV;
	private TestCTL ctl;
	private String jwt;

	@BeforeEach
	void setUp() {
		jwtSRV = mock(IJwtSRV.class);
		final MicroservicesURLCFG msCfg = mock(MicroservicesURLCFG.class);
		when(msCfg.getFromGovway()).thenReturn(false);

		ctl = new TestCTL();
		ReflectionTestUtils.setField(ctl, "jwtSRV", jwtSRV);
		ReflectionTestUtils.setField(ctl, "msCfg", msCfg);

		final String payload = "{\"iss\":\"080\",\"action_id\":\"UPDATE\"}";
		jwt = "header." + Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + ".signature";
	}

	@Test
	@DisplayName("VALIDATION usa validatePayloadForValidation")
	@Tag("Issue 3194")
	void validation() {
		ctl.run(jwt, EventTypeEnum.VALIDATION);
		verify(jwtSRV).validatePayloadForValidation(any(JWTPayloadDTO.class));
		verify(jwtSRV, never()).validatePayloadForFhirPublication(any());
		verifyNoMoreInteractions(jwtSRV);
	}

	@Test
	@DisplayName("FHIR_VALIDATION usa validatePayloadForValidation")
	@Tag("Issue 3194")
	void fhirValidation() {
		ctl.run(jwt, EventTypeEnum.FHIR_VALIDATION);
		verify(jwtSRV).validatePayloadForValidation(any(JWTPayloadDTO.class));
		verify(jwtSRV, never()).validatePayloadForFhirPublication(any());
		verifyNoMoreInteractions(jwtSRV);
	}

	@Test
	@DisplayName("FHIR_CREATE usa validatePayloadForFhirPublication")
	@Tag("Issue 3194")
	void fhirCreate() {
		ctl.run(jwt, EventTypeEnum.FHIR_CREATE);
		verify(jwtSRV).validatePayloadForFhirPublication(any(JWTPayloadDTO.class));
		verify(jwtSRV, never()).validatePayloadForValidation(any());
		verifyNoMoreInteractions(jwtSRV);
	}

	@Test
	@DisplayName("FHIR_REPLACE usa validatePayloadForFhirPublication")
	@Tag("Issue 3194")
	void fhirReplace() {
		ctl.run(jwt, EventTypeEnum.FHIR_REPLACE);
		verify(jwtSRV).validatePayloadForFhirPublication(any(JWTPayloadDTO.class));
		verify(jwtSRV, never()).validatePayloadForValidation(any());
		verifyNoMoreInteractions(jwtSRV);
	}
}
