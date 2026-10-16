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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Date;
import org.bson.Document;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.JWTPayloadDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.PublicationMetadataReqDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.UpdateMetadataReqDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.EdsResponseDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.GetDocumentReferenceResDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.GetMergedMetadatiDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.IniTraceResponseDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.ResponseDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.client.TransformResDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.EventTypeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.UpdateFlowTypeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.impl.AffinityDomainValidationSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.impl.ConfigSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.AffinityDomainUtility;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.dto.ValidationResultDTO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles(Constants.Profile.TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UpdateTest extends AbstractTest {

	@Autowired
	ServletWebServerApplicationContext webServerAppCtxt;

	@Autowired
	MockMvc mvc;

	@MockitoBean
	@Qualifier("restTemplateIni")
	RestTemplate restTemplateIni;

	@MockitoBean
	RestTemplate restTemplate;

	@MockitoBean 
	AffinityDomainValidationSRV affinityDomainValidationSRV;

	@MockitoBean
	AffinityDomainUtility affinityDomainUtility;

	private static final String TEST_REQUEST_BODY = "{\"tipologiaStruttura\":\"Ospedale\",\"attiCliniciRegoleAccesso\":[\"P99\"],\"tipoDocumentoLivAlto\":\"WOR\",\"assettoOrganizzativo\":\"AD_PSC001\",\"dataInizioPrestazione\":\"string\",\"dataFinePrestazione\":\"string\",\"conservazioneANorma\":\"string\",\"tipoAttivitaClinica\":\"PHR\",\"identificativoSottomissione\":\"string\"}";

	@MockitoBean
	private ConfigSRV config;


	@BeforeAll
	void init() {
		when(config.isAuditEnable()).thenReturn(true);
	}

	@Test 
	@DisplayName("Update of a document with wrong document id")
	void givenInvalidDocumentIdentifierThrowValidationException() throws Exception{
		String idDocument = "Error-Test";

		ResponseEntity<ResponseDTO> responseDTO = callUpdate(idDocument, false, UpdateFlowTypeEnum.UPDATE_ITI42);
		assertEquals(HttpStatus.BAD_REQUEST, responseDTO.getStatusCode());
	}

	@Test
	@DisplayName("Update of a document with wrong request body iti-42/iti-57")
	void givenAnInvalidValueShouldThrowValidationException() throws Exception {
		String idDocument = "2.16.840.4^UAT_GTW_ID162";
	
		ResponseEntity<ResponseDTO> responseDTO = callUpdate(idDocument, true, UpdateFlowTypeEnum.UPDATE_ITI42);
		assertEquals(HttpStatus.BAD_REQUEST, responseDTO.getStatusCode());
	}

	@Test 
	@DisplayName("Update of a document with wrong request body oscuramento")
	void givenAnInvalidValueOscuramentoShouldThrowValidationException() throws Exception {
		String idDocument = "2.16.840.4^UAT_GTW_ID162";
	
		ResponseEntity<ResponseDTO> responseDTO = callUpdate(idDocument, true, UpdateFlowTypeEnum.UPDATE_OSCURAMENTO);
		assertEquals(HttpStatus.BAD_REQUEST, responseDTO.getStatusCode());
	}

	 @Test
	 @DisplayName("Update of a document with correct request body")
	 void givenAnIdentifier_shouldExecuteUpdate() throws Exception {

	 	String idDocument = "2.16.840.4^UAT_GTW_ID162";
	 	mockEdsClient(HttpStatus.OK);
	 	mockIniClient(HttpStatus.OK, true);
		mockDomainValidation(true);
		mockFhirClient();

	 	final ResponseEntity<ResponseDTO> response = callUpdate(idDocument, false, UpdateFlowTypeEnum.UPDATE_ITI42);
	 	final ResponseDTO body = response.getBody();

		assertEquals(HttpStatus.OK, response.getStatusCode());
	 	assertNotNull(body, "A response body should have been returned");
	 	assertNotNull(body.getSpanID(), "A span ID should have been returned");
	 	assertNotNull(body.getTraceID(), "A trace ID should have been returned");
	 }

	 @Test
	 @DisplayName("Update of a document with correct request body oscuramento")
	 void givenAnIdentifier_shouldExecuteUpdateOscuramento() throws Exception {

	 	String idDocument = "2.16.840.4^UAT_GTW_ID162";
	 	mockEdsClient(HttpStatus.OK);
	 	mockIniClient(HttpStatus.OK, true);
		mockDomainValidation(true);
		mockFhirClient();

	 	final ResponseEntity<ResponseDTO> response = callUpdate(idDocument, false, UpdateFlowTypeEnum.UPDATE_OSCURAMENTO);
	 	final ResponseDTO body = response.getBody();

		assertEquals(HttpStatus.OK, response.getStatusCode());
	 	assertNotNull(body, "A response body should have been returned");
	 	assertNotNull(body.getSpanID(), "A span ID should have been returned");
	 	assertNotNull(body.getTraceID(), "A trace ID should have been returned");
	 }

	 @Test
	 @DisplayName("Not found INI Error returned")
	 void whenIniFailsForNotFound_anErrorShouldBeReturned() throws Exception {
	 	String idDocument = "2.16.840.4^UAT_GTW_ID162";

	 	mockIniClient(HttpStatus.NOT_FOUND, false);
	 	ResponseEntity<ResponseDTO> response = callUpdate(idDocument, false, UpdateFlowTypeEnum.UPDATE_ITI42);
	 	assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
	 }

	 @Test
	 @DisplayName("EDS Error returned")
	 void whenEdsFails_anErrorShouldBeReturned() throws Exception {

	 	String idDocument = "2.16.840.4^UAT_GTW_ID162";

	 	mockIniClient(HttpStatus.OK, true);
		mockDomainValidation(true);
		mockFhirClient();
	 	mockEdsClient(HttpStatus.INTERNAL_SERVER_ERROR);
	 	ResponseEntity<ResponseDTO> response = callUpdate(idDocument, false, UpdateFlowTypeEnum.UPDATE_ITI42);
	 	assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
	 }

	 @Test 
	 @DisplayName("Affinity Domain validation failed")
	 void whenADFails_anErrorShouldBeReturned() throws Exception{

		String idDocument = "2.16.840.4^UAT_GTW_ID162";

		mockIniClient(HttpStatus.OK, true);
		mockDomainValidation(false);

		ResponseEntity<ResponseDTO> response = callUpdate(idDocument, false, UpdateFlowTypeEnum.UPDATE_ITI42);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
	 }

	// This method is used to mock the rest calls performed by the ini-client during the update flow.
	// IniClient.metadata()  -> restTemplateIni.exchange(String, PUT, HttpEntity<MergedMetadatiRequestDTO>, GetMergedMetadatiDTO.class)
	// IniClient.update()    -> restTemplateIni.exchange(String, PUT, HttpEntity<IniMetadataUpdateReqDTO>, IniTraceResponseDTO.class)
	// Both calls use PUT on the same overload, so they are told apart by the expected response type.
	void mockIniClient(final HttpStatus status, boolean esito) {
		log.info("Mocking INI client");
		GetMergedMetadatiDTO response = new GetMergedMetadatiDTO();
		response.setEdsPublished("TRUE");
		IniTraceResponseDTO res = new IniTraceResponseDTO();
		if (status.is2xxSuccessful() && esito) {
			response.setMarshallResponse("response");
			response.setErrorMessage(null);
			response.setMockEds(false);
			res.setEsito(true);
			Mockito.doReturn(new ResponseEntity<>(response, status)).when(restTemplateIni)
					.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(GetMergedMetadatiDTO.class));
			Mockito.doReturn(new ResponseEntity<>(res, status)).when(restTemplateIni)
					.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(IniTraceResponseDTO.class));
		} else if (status.equals(HttpStatus.NOT_FOUND) && !esito) {
			response.setMarshallResponse(null);
			response.setErrorMessage("Record not found");
			// INI answers 200 with an error message in the body: it is the body, not the status, that drives the flow.
			Mockito.doReturn(new ResponseEntity<>(response, HttpStatus.OK)).when(restTemplateIni)
					.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(GetMergedMetadatiDTO.class));
		}
	}
	

	// This method is used to mock the rest calls performed by the eds-client during the update flow.
	// EdsClient.getDocumentReferenceClient() -> restTemplate.exchange(URI, GET, HttpEntity<Void>, GetDocumentReferenceResDTO.class)
	// EdsClient.update()                     -> restTemplate.exchange(String, PUT, HttpEntity<Object>, EdsResponseDTO.class)
	// Note the first one builds a URI, so it needs the URI overload and not anyString().
	void mockEdsClient(final HttpStatus status) {
		log.info("Mocking EDS client");
		EdsResponseDTO response = new EdsResponseDTO();
		response.setEsito(status.is2xxSuccessful());
		response.setMessageError(status.is2xxSuccessful() ? null : "Test error");
		GetDocumentReferenceResDTO documentReference = new GetDocumentReferenceResDTO();
		documentReference.setDocumentReference("prova");
		Mockito.doReturn(new ResponseEntity<>(documentReference, HttpStatus.OK)).when(restTemplate)
				.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(GetDocumentReferenceResDTO.class));
		// The failure is carried by esito/messageError in the body, so the client returns normally
		// and it is the controller that raises the EDS error.
		Mockito.doReturn(new ResponseEntity<>(response, HttpStatus.OK)).when(restTemplate)
				.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(EdsResponseDTO.class));
		}
	

	// This method is used to mock the affinityDomainValidationSRV called during the update flow.
	void mockDomainValidation(boolean isValid){

		ValidationResultDTO result = new ValidationResultDTO();
		result.setValid(isValid);
		if(!isValid){
			result.setErrorMessage("Affinity Domain validation failed for document");
		}else {
			result.setAdVersion("2.6.x");
		}

		when(affinityDomainUtility.extractCreationTime(anyString())).thenReturn(LocalDate.of(2026, 1, 1));
		when(affinityDomainValidationSRV.validateUpdateMetadataRequest(any(UpdateMetadataReqDTO.class), any(LocalDate.class), any(JWTPayloadDTO.class))).thenReturn(result);

	}

	// This method is used to mock the rest call performed by the fhir-mapping-client during the update flow.
	// FhirMappingClient.updateDocumentReferenceClient() -> restTemplate.exchange(URI, POST, HttpEntity<UpdateDocumentReferenceRequestDTO>, TransformResDTO.class)
	void mockFhirClient(){
		TransformResDTO updateDocRef = new TransformResDTO();
		updateDocRef.setErrorMessage("");
	 	updateDocRef.setJson(Document.parse("{\"json\" : \"json\"}"));
		Mockito.doReturn(new ResponseEntity<>(updateDocRef, HttpStatus.OK)).when(restTemplate)
				.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(TransformResDTO.class));
	}

	ResponseEntity<ResponseDTO> callUpdate(final String documentId, boolean validationException, UpdateFlowTypeEnum type) throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set(Constants.Headers.JWT_HEADER, generateJwt(null, false, type.equals(UpdateFlowTypeEnum.UPDATE_OSCURAMENTO) ? EventTypeEnum.UPDATE_OSCURAMENTO : EventTypeEnum.UPDATE));

		ObjectMapper objectMapper = new ObjectMapper();
		PublicationMetadataReqDTO document = new Gson().fromJson(TEST_REQUEST_BODY, PublicationMetadataReqDTO.class);
		document.setDataInizioPrestazione(""+new Date().getTime());
		document.setDataFinePrestazione(""+new Date().getTime());
		if (validationException) {
			document.setAttiCliniciRegoleAccesso(Collections.singletonList("invalid value"));
		}

		String updateType;
		if (type.equals(UpdateFlowTypeEnum.UPDATE_ITI42)){
			updateType = "/metadata";
		} else if (type.equals(UpdateFlowTypeEnum.UPDATE_ITI57)){
			updateType = "/metadata-iti-57";
		}else {
			updateType = "/metadata-oscuramento-catena";
		}

		MockHttpServletRequestBuilder builder =
				MockMvcRequestBuilders.put("http://localhost:" +
					webServerAppCtxt.getWebServer().getPort() + webServerAppCtxt.getServletContext().getContextPath() +
					"/v1/documents/" + documentId + updateType)
					.content(objectMapper.writeValueAsString(document))
					.param("requestBody", objectMapper.writeValueAsString(document))
					.headers(headers);

		MvcResult result = mvc.perform(builder
						.contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();
		MockHttpServletResponse response = result.getResponse();
		return new ResponseEntity<>(new Gson().fromJson(response.getContentAsString(), ResponseDTO.class), HttpStatus.valueOf(response.getStatus()));
	}

}
