/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package it.finanze.sanita.fse2.ms.gtw.dispatcher;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.GetMergedMetadatiDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.IniException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.PublicationMetadataReqDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.EdsResponseDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.IniTraceResponseDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.ResponseDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.StringUtility;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ComponentScan(basePackages = {Constants.ComponentScan.BASE})
@AutoConfigureMockMvc
@ActiveProfiles(Constants.Profile.TEST)
class UpdateTest extends AbstractTest {

	@Autowired
	ServletWebServerApplicationContext webServerAppCtxt;

	@Autowired
	MockMvc mvc;

	@SpyBean
	RestTemplate restTemplate;

	private static final String TEST_REQUEST_BODY = "{\"tipologiaStruttura\":\"Ospedale\",\"attiCliniciRegoleAccesso\":[\"P99\"],\"tipoDocumentoLivAlto\":\"WOR\",\"assettoOrganizzativo\":\"AD_PSC001\",\"dataInizioPrestazione\":\"string\",\"dataFinePrestazione\":\"string\",\"conservazioneANorma\":\"string\",\"tipoAttivitaClinica\":\"PHR\",\"identificativoSottomissione\":\"string\"}";

	@Test
	@DisplayName("Update of a document")
	void givenAnInvalidValueShouldThrowValidationException() throws Exception {
		final String idDocument = StringUtility.generateUUID();
		mockEdsClient(HttpStatus.OK);
		mockIniClient(HttpStatus.OK, true);
		ResponseEntity<ResponseDTO> responseDTO = callUpdate(idDocument, true);
		assertEquals(HttpStatus.BAD_REQUEST, responseDTO.getStatusCode());
	}

	@Test
	@DisplayName("Update of a document")
	void givenAnIdentifier_shouldExecuteUpdate() throws Exception {

		final String idDocument = StringUtility.generateUUID();
		mockEdsClient(HttpStatus.OK);
		mockIniClient(HttpStatus.OK, true);

		final ResponseEntity<ResponseDTO> response = callUpdate(idDocument, false);
		final ResponseDTO body = response.getBody();

		assertNotNull(body, "A response body should have been returned");
		assertNotNull(body.getSpanID(), "A span ID should have been returned");
		assertNotNull(body.getTraceID(), "A trace ID should have been returned");
	}

	@Test
	@DisplayName("Not found INI Error returned")
	void whenIniFailsForNotFound_anErrorShouldBeReturned() throws Exception {
		final String idDocument = StringUtility.generateUUID();

		mockIniClient(HttpStatus.NOT_FOUND, false);
		mockEdsClient(HttpStatus.OK);
		ResponseEntity<ResponseDTO> response = callUpdate(idDocument, false);
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
	}

	@Test
	@DisplayName("Generic INI Error returned")
	void whenIniFailsForGenericHttp_anErrorShouldBeReturned() throws Exception {
		final String idDocument = StringUtility.generateUUID();

		mockIniClient(HttpStatus.BAD_REQUEST, false);
		mockEdsClient(HttpStatus.OK);
		ResponseEntity<ResponseDTO> response = callUpdate(idDocument, false);
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
	}

	@Test
	@DisplayName("EDS Error returned")
	void whenEdsFails_anErrorShouldBeReturned() throws Exception {

		final String idDocument = StringUtility.generateUUID();

		mockIniClient(HttpStatus.OK, true);
		mockEdsClient(HttpStatus.INTERNAL_SERVER_ERROR);
		ResponseEntity<ResponseDTO> response = callUpdate(idDocument, false);
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
	}

	void mockIniClient(final HttpStatus status, boolean esito) {
		log.info("Mocking INI client");
		GetMergedMetadatiDTO response = new GetMergedMetadatiDTO();
		if (status.is2xxSuccessful() && esito) {
			response.setMarshallResponse("response");
			response.setErrorMessage(null);
			Mockito.doReturn(new ResponseEntity<>(response, status)).when(restTemplate).exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), ArgumentMatchers.eq(GetMergedMetadatiDTO.class));
		} else if (status.equals(HttpStatus.NOT_FOUND) && !esito) {
			response.setMarshallResponse(null);
			response.setErrorMessage("Record not found");
			Mockito.doReturn(new ResponseEntity<>(response, HttpStatus.OK)).when(restTemplate).exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), ArgumentMatchers.eq(GetMergedMetadatiDTO.class));
		} else if (status.equals(HttpStatus.BAD_REQUEST) && !esito) {
			response.setErrorMessage("Generic error from INI");
			Mockito.doReturn(new ResponseEntity<>(response, HttpStatus.OK)).when(restTemplate).exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), ArgumentMatchers.eq(GetMergedMetadatiDTO.class));
		} else if (status.is4xxClientError()) {
			Mockito.doThrow(new RestClientException("")).when(restTemplate).exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), ArgumentMatchers.eq(GetMergedMetadatiDTO.class));
		} else {
			Mockito.doThrow(new BusinessException("")).when(restTemplate).exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), ArgumentMatchers.eq(IniTraceResponseDTO.class));
		}
	}


	void mockEdsClient(final HttpStatus status) {
		log.info("Mocking EDS client");

		if (status.is2xxSuccessful()) {
			EdsResponseDTO response = new EdsResponseDTO();
			response.setEsito(status.is2xxSuccessful());
			response.setMessageError(status.is2xxSuccessful() ? null : "Test error");
			Mockito.doReturn(new ResponseEntity<>(response, status)).when(restTemplate)
					.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), ArgumentMatchers.eq(EdsResponseDTO.class));
		} else if (status.is4xxClientError()) {
			Mockito.doThrow(new RestClientException("")).when(restTemplate)
					.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), ArgumentMatchers.eq(EdsResponseDTO.class));
		} else {
			Mockito.doThrow(new BusinessException("")).when(restTemplate)
					.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(EdsResponseDTO.class));
		}
	}

	ResponseEntity<ResponseDTO> callUpdate(final String documentId, boolean validationException) throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set(Constants.Headers.JWT_HEADER, generateJwt(null, false));

		ObjectMapper objectMapper = new ObjectMapper();
		PublicationMetadataReqDTO document = new Gson().fromJson(TEST_REQUEST_BODY, PublicationMetadataReqDTO.class);
		if (validationException) {
			document.setAttiCliniciRegoleAccesso(Collections.singletonList("invalid value"));
		}
		MockHttpServletRequestBuilder builder =
				MockMvcRequestBuilders.put("http://localhost:" +
					webServerAppCtxt.getWebServer().getPort() + webServerAppCtxt.getServletContext().getContextPath() +
					"/v1/documents/" + documentId + "/metadata")
					.param("requestBody", objectMapper.writeValueAsString(document))
					.headers(headers);

		MvcResult result = mvc.perform(builder
						.contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();
		MockHttpServletResponse response = result.getResponse();
		return new ResponseEntity<>(new Gson().fromJson(response.getContentAsString(), ResponseDTO.class), HttpStatus.valueOf(response.getStatus()));
	}

}
