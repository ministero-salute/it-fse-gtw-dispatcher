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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants.Misc;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.GetDocumentMetadataDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.EdsResponseDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.IniTraceResponseDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.EventTypeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.IConfigSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.StringUtility;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(Constants.Profile.TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DeleteTest extends AbstractTest {

	@Autowired
	ServletWebServerApplicationContext webServerAppCtxt;

	@MockitoSpyBean
	RestTemplate restTemplate;

	@MockitoSpyBean 
	@Qualifier ("restTemplateIni")
	RestTemplate restTemplateIni;

	@MockitoBean
	IConfigSRV config;


	@BeforeAll
	void init() {
		when(config.isAuditEnable()).thenReturn(true);
	}

	@Test
	@DisplayName("Delete of a document")
	void givenAnIdentifier_shouldExecuteDeletion() {

		String idDocument = "2.16.840.4^UAT_GTW_ID162";

		mockEdsClient(HttpStatus.OK);
		mockIniClient(HttpStatus.OK, true);
		final ResponseEntity<JsonNode> response = callDelete(idDocument);
		JsonNode body = response.getBody();

		assertEquals(response.getStatusCode(), HttpStatus.OK);
		assertNotNull(body, "A response body should have been returned");
		assertTrue(body.path("warning").isMissingNode());
	}

	@Test
	@DisplayName("INI Error returned")
	void whenIniFails_anErrorShouldBeReturned() {

		String idDocument = "2.16.840.4^UAT_GTW_ID162";

		mockIniClient(HttpStatus.INTERNAL_SERVER_ERROR, false);
		mockEdsClient(HttpStatus.OK);
		assertThrows(HttpServerErrorException.InternalServerError.class, () -> callDelete(idDocument));
	}

	@Test
	@DisplayName("Not found INI Error returned")
	void whenIniFailsForNotFound_anErrorShouldBeReturned() {

		String idDocument = "2.16.840.4^UAT_GTW_ID162";

		mockIniClient(HttpStatus.NOT_FOUND, false);
		mockEdsClient(HttpStatus.OK);
		assertThrows(HttpClientErrorException.NotFound.class, () -> callDelete(idDocument));
	}

	@Test
	@DisplayName("INI Warning returned")
	void whenIniFailsForGenericHttp_aWarningShouldBeReturned() {

		String idDocument = "2.16.840.4^UAT_GTW_ID162";

		mockIniClient(HttpStatus.BAD_REQUEST, false);
		mockEdsClient(HttpStatus.OK);
		final ResponseEntity<JsonNode> response = callDelete(idDocument);
		JsonNode body = response.getBody();

		assertEquals(response.getStatusCode(), HttpStatus.ACCEPTED);
		assertNotNull(body, "A response body should have been returned");
		assertFalse(body.path("warning").isMissingNode());
		assertEquals(body.path("warning").asText(), Misc.WARN_ASYNC_TRANSACTION);
	}

	@Test
	@DisplayName("EDS Error returned")
	void whenEdsFails_anErrorShouldBeReturned() {

		String idDocument = "2.16.840.4^UAT_GTW_ID162";

		mockIniClient(HttpStatus.OK, true);
		mockEdsClient(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThrows(HttpClientErrorException.NotFound.class, () -> callDelete(idDocument));
	}

	void mockIniClient(final HttpStatus status, boolean esito) {
		log.info("Mocking INI client");
		IniTraceResponseDTO response = new IniTraceResponseDTO();
		response.setSpanID(StringUtility.generateUUID());
		response.setTraceID(StringUtility.generateUUID());
		GetDocumentMetadataDTO metadata = new GetDocumentMetadataDTO();
		// "TRUE" is required to make the controller call EDS at all.
		metadata.setEdsPublished("TRUE");
		metadata.setDocumentType("('11502-2^^2.16.840.1.113883.6.1')");
		metadata.setAuthorInstitution("GTW_STRUCTURE");
		metadata.setAdministrativeRequest(Arrays.asList("P99"));

		if (status.is2xxSuccessful() && esito) {
			metadata.setErrorMessage(null);
			metadata.setUuid(StringUtility.generateUUID());
			response.setEsito(true);
			response.setMessage(null);
			Mockito.doReturn(new ResponseEntity<>(metadata, HttpStatus.OK)).when(restTemplateIni)
					.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(GetDocumentMetadataDTO.class));
			Mockito.doReturn(new ResponseEntity<>(response, HttpStatus.OK)).when(restTemplateIni)
					.exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(IniTraceResponseDTO.class));
		} else if (status.equals(HttpStatus.NOT_FOUND) && !esito) {
			// INI answers 200 with an error message in the body: it is the body, not the status,
			// that makes the controller raise an IniException (mapped to 404).
			metadata.setErrorMessage("Not found on INI");
			metadata.setUuid(null);
			Mockito.doReturn(new ResponseEntity<>(metadata, HttpStatus.OK)).when(restTemplateIni)
					.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(GetDocumentMetadataDTO.class));
		} else if (status.equals(HttpStatus.BAD_REQUEST) && !esito) {
			// Metadata retrieval succeeds, the delete on INI answers esito=false:
			// the transaction is taken over asynchronously and a warning is returned.
			metadata.setErrorMessage(null);
			metadata.setUuid(StringUtility.generateUUID());
			response.setEsito(false);
			response.setMessage("Generic error from INI");
			Mockito.doReturn(new ResponseEntity<>(metadata, HttpStatus.OK)).when(restTemplateIni)
					.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(GetDocumentMetadataDTO.class));
			Mockito.doReturn(new ResponseEntity<>(response, HttpStatus.OK)).when(restTemplateIni)
					.exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(IniTraceResponseDTO.class));
		} else if (status.is5xxServerError()) {
			// A RestClientResponseException is what AbstractClient turns into a ServerResponseException,
			// which the handler propagates with the very same status code.
			Mockito.doThrow(new HttpServerErrorException(status)).when(restTemplateIni)
					.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(GetDocumentMetadataDTO.class));
		}
	}


	void mockEdsClient(final HttpStatus status) {
		log.info("Mocking EDS client");
		EdsResponseDTO response = new EdsResponseDTO();
		response.setEsito(status.is2xxSuccessful());
		response.setMessageError(status.is2xxSuccessful() ? null : "Test error");
		// The failure is carried by esito/messageError in the body, so the client returns normally
		// and it is the controller that raises the EDS error.
		Mockito.doReturn(new ResponseEntity<>(response, HttpStatus.OK)).when(restTemplate)
				.exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(EdsResponseDTO.class));
	}

	ResponseEntity<JsonNode> callDelete(final String documentId) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set(Constants.Headers.JWT_HEADER, generateJwt(null, false, EventTypeEnum.DELETE));

		final String urlReplace = "http://localhost:" + webServerAppCtxt.getWebServer().getPort()
				+ webServerAppCtxt.getServletContext().getContextPath() + "/v1/documents/" + documentId;

		HttpEntity<?> requestEntity = new HttpEntity<>(null, headers);
		return restTemplate.exchange(urlReplace, HttpMethod.DELETE, requestEntity, JsonNode.class);
	}

}
