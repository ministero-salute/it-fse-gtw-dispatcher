
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

import com.fasterxml.jackson.core.JsonProcessingException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.IValidatorClient;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.ValidationInfoDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.ValidationCDAReqDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.ValidationResDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ValidationException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.entity.ValidatedDocumentsETY;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.ICdaSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.IConfigSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.FileUtility;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.StringUtility;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.ValidationUtility;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(Constants.Profile.TEST)
@TestInstance(Lifecycle.PER_CLASS)

class ValidationTest extends AbstractTest {


	@Autowired
	private ICdaSRV cdaSRV;

	/**
	 * Calcolo performance: numero di prove per singolo utente.
	 */
	private static final int N_PROVE = 50;

	/**
	 * Calcolo performance: numero utenti.
	 */
	private static final int N_UTENTI = 20;


	/**
	 * Numero di verifiche di raggiungimento del numero di test necessari.
	 */
	private static final int SLEEP_TRY = 500;

	/**
	 * Costante utilizzata per i calcoli del throughput.
	 */
	private static final int MILLISECONDS_IN_S = 1000;

	/**
	 * Quanto di tempo di attesa tra due check adiacenti per comprendere se si è raggiunto il numero di test necessari.
	 */
	private static final int SLEEP_TIME = 100;

	@MockBean
	IValidatorClient validatorClient;


	@Autowired
    public MongoTemplate mongo;

	@MockBean
	private IConfigSRV config;

	@BeforeAll
	void init() {
		when(config.isAuditEnable()).thenReturn(true);
	}

	@BeforeEach
	void createCollection(){
		mongo.dropCollection(ValidatedDocumentsETY.class);
		ValidatedDocumentsETY ety = new ValidatedDocumentsETY();
		ety.setHashCda("hdkdkd");
		ety.setInsertionDate(new Date());

        mongo.save(ety);
	}

	@BeforeEach
	void mockValidatorClient() {
		when(validatorClient.validate(anyString(),anyString(), any())).thenReturn(ValidationInfoDTO.builder().result(RawValidationEnum.OK).build());
	}

    @Test
    @DisplayName("Wrong File Test")
    void wrongFileTest() {
    	byte[] docxByte = FileUtility.getFileFromInternalResources("Files" + File.separator + "Test.docx");

    	//invio un non pdf -
    	Map<String, RestExecutionResultEnum> result = callValidation(ActivityEnum.VERIFICA, HealthDataFormatEnum.CDA, InjectionModeEnum.ATTACHMENT, docxByte, true, false, false);
    	assertEquals(RestExecutionResultEnum.DOCUMENT_TYPE_ERROR, result.values().iterator().next());

    	byte[] pdfByte = FileUtility.getFileFromInternalResources("Files" + File.separator + "Test.pdf");
    	result = callValidation(ActivityEnum.VERIFICA, HealthDataFormatEnum.CDA, InjectionModeEnum.ATTACHMENT, pdfByte, true, false, false);
    	assertEquals(RestExecutionResultEnum.MINING_CDA_ERROR, result.values().iterator().next());
    }
 
    @Test
    @DisplayName("Pre Publish Test")
    void prePublishTest() {

    	byte[] pdfAttachment = FileUtility.getFileFromInternalResources("Files" + File.separator + 
    			"attachment" + File.separator + "pdf_msg_SATLED_LED_Lettera_di_Dimissione.pdf");
    	byte[] pdfResource = FileUtility.getFileFromInternalResources("Files/resource/cert1.pdf");

    	Map<String, RestExecutionResultEnum> result = callValidation(ActivityEnum.VALIDATION, HealthDataFormatEnum.CDA, InjectionModeEnum.ATTACHMENT, pdfResource, true, false, true);
    	assertEquals(RestExecutionResultEnum.MINING_CDA_ERROR, result.values().iterator().next());

    	result = callValidation(ActivityEnum.VALIDATION, HealthDataFormatEnum.CDA, InjectionModeEnum.RESOURCE, pdfAttachment, true, false, true);
    	assertEquals(RestExecutionResultEnum.MINING_CDA_ERROR, result.values().iterator().next());
    	
		String cda = extractCDA(pdfAttachment);
		String hashedCda = StringUtility.encodeSHA256B64(cdaWithoutLegalAuthenticator(cda));

    	result = callValidation(ActivityEnum.VALIDATION, HealthDataFormatEnum.CDA, InjectionModeEnum.RESOURCE, pdfAttachment, true, false, true);
    	assertEquals(RestExecutionResultEnum.MINING_CDA_ERROR, result.values().iterator().next());

    	result = callValidation(ActivityEnum.VALIDATION, HealthDataFormatEnum.CDA, InjectionModeEnum.ATTACHMENT, pdfAttachment, true, false, true);
    	assertEquals(RestExecutionResultEnum.OK, result.values().iterator().next());
    	assertNotNull(cdaSRV.get(hashedCda), "La transazione non deve essere presente.");

    	
    }
    
    @Test
    @DisplayName("Pre Publish Test Second")
    void prePublishTestSecond() {
    	
    	byte[] pdfAttachment = FileUtility.getFileFromInternalResources("Files" + File.separator +
    			"attachment" + File.separator + "pdf_msg_SATLED_LED_Lettera_di_Dimissione.pdf");
    	byte[] pdfResource = FileUtility.getFileFromInternalResources("Files/resource/cert1.pdf");
		String cda = extractCDA(pdfResource);
		String hashedCda = StringUtility.encodeSHA256B64(cdaWithoutLegalAuthenticator(cda));
		Map<String, RestExecutionResultEnum> result = callValidation(ActivityEnum.VALIDATION, HealthDataFormatEnum.CDA, InjectionModeEnum.RESOURCE, pdfResource, true, false, true);
    	assertEquals(RestExecutionResultEnum.OK, result.values().iterator().next());
    	assertNotNull(cdaSRV.get(hashedCda), "La transazione non deve essere presente.");
    	result = callValidation(ActivityEnum.VALIDATION, HealthDataFormatEnum.CDA, InjectionModeEnum.ATTACHMENT, pdfResource, true, false, true);
    	assertEquals(RestExecutionResultEnum.MINING_CDA_ERROR, result.values().iterator().next());
		cda = extractCDA(pdfAttachment);
		hashedCda = StringUtility.encodeSHA256B64(cdaWithoutLegalAuthenticator(cda));
		
    	result = callValidation(ActivityEnum.VALIDATION, HealthDataFormatEnum.CDA, null, pdfAttachment, true, false, true);
    	assertEquals(RestExecutionResultEnum.OK, result.values().iterator().next());
    	assertNotNull(cdaSRV.get(hashedCda), "La transazione non deve essere presente.");
    }
    
    protected String cdaWithoutLegalAuthenticator(final String cda) {
		Document doc = Jsoup.parse(cda, "", Parser.xmlParser());
		Element authenticator = doc.selectFirst("LegalAuthenticator");
		if(authenticator != null) {
			authenticator.forEach(e -> {
				// Reset attributes
				e.attributes().forEach(a -> a.setValue("PLACEHOLDER"));
				// Reset values on node without children and with text
				if(e.children().isEmpty() && !e.text().isEmpty()) e.text("PLACEHOLDER");
			});
		} else {
			log.warn("Unable to calculate cda-hash correctly because LegalAuthenticator doesn't exists");
		}
		return doc.toString();
	}
    
    @Test
    @DisplayName("Pre Publish Test - Null Cases")
    void prePublishTestNullCases() {
    	
    	byte[] pdfAttachment = FileUtility.getFileFromInternalResources("Files" + File.separator + 
    			"attachment" + File.separator + "pdf_msg_SATLED_LED_Lettera_di_Dimissione.pdf");
    	byte[] pdfResource = FileUtility.getFileFromInternalResources("Files/resource/cert1.pdf");
    	
		String cda = extractCDA(pdfResource);
		String hashedCda = StringUtility.encodeSHA256B64(cdaWithoutLegalAuthenticator(cda));

		Map<String, RestExecutionResultEnum> result = callValidation(ActivityEnum.VALIDATION, HealthDataFormatEnum.CDA, null, pdfResource, true, false, true);
    	assertEquals(RestExecutionResultEnum.OK, result.values().iterator().next());
    	assertNotNull(cdaSRV.get(hashedCda), "La transazione non deve essere presente.");
    	
		result = callValidation(ActivityEnum.VALIDATION, HealthDataFormatEnum.CDA, null, pdfAttachment, true, false, true);
    	assertEquals(RestExecutionResultEnum.OK, result.values().iterator().next());
    	assertNotNull(cdaSRV.get(hashedCda), "La transazione non deve essere presente.");

    }
    
    @DisplayName("Warning string available if InjectionModeEnum is null")
    @Test
	void validationWarningTestOK() {
    	byte[] pdfAttachment = FileUtility.getFileFromInternalResources("Files/attachment/pdf_msg_SATLED_LED_Lettera_di_Dimissione.pdf");
    	byte[] pdfResource = FileUtility.getFileFromInternalResources("Files/resource/cert1.pdf");
		final byte[] file = FileUtility.getFileFromInternalResources("Files" + File.separator + "attachment" + File.separator + "LAB_OK.pdf");
		final String jwtToken = generateJwt(file, true, EventTypeEnum.VALIDATION);

		ValidationCDAReqDTO requestBody = new ValidationCDAReqDTO();
		
		requestBody.setActivity(ActivityEnum.VALIDATION);
		requestBody.setActivity(ActivityEnum.VERIFICA);
		requestBody.setMode(null);

		final ResponseEntity<ValidationResDTO> validationResponse = callPlainValidation(jwtToken, file, requestBody);
		assertTrue(validationResponse.getBody().getWarning().contains("[WARNING_EXTRACT]"));

		Map<String, RestExecutionResultEnum> resultValidation = callValidation(ActivityEnum.VERIFICA, HealthDataFormatEnum.CDA, null, pdfResource, true, false, true);    	
		assertEquals(RestExecutionResultEnum.OK, resultValidation.values().iterator().next());
    	
		Map<String, RestExecutionResultEnum> resultPrePublishing = callValidation(ActivityEnum.VALIDATION, HealthDataFormatEnum.CDA, null, pdfAttachment, true, false, true);    	
    	assertEquals(RestExecutionResultEnum.OK, resultPrePublishing.values().iterator().next());

	}
    
    @DisplayName("No Warning string available if InjectionModeEnum is not null")
    @Test
	void validationWarningTestKO() {
    	byte[] pdfAttachment = FileUtility.getFileFromInternalResources("Files/attachment/pdf_msg_SATLED_LED_Lettera_di_Dimissione.pdf");
    	byte[] pdfResource = FileUtility.getFileFromInternalResources("Files/resource/cert1.pdf");
		final byte[] file = FileUtility.getFileFromInternalResources("Files" + File.separator + "attachment" + File.separator + "LAB_OK.pdf");
		final String jwtToken = generateJwt(file, true, EventTypeEnum.VALIDATION);

		ValidationCDAReqDTO requestBody = new ValidationCDAReqDTO();
		 
		requestBody.setActivity(ActivityEnum.VERIFICA);
		requestBody.setMode(InjectionModeEnum.ATTACHMENT);

		final ResponseEntity<ValidationResDTO> validationResponse = callPlainValidation(jwtToken, file, requestBody);
		assertTrue(StringUtility.isNullOrEmpty(validationResponse.getBody().getWarning()));

		final ResponseEntity<ValidationResDTO> prePublishingResponse = callPlainValidation(jwtToken, file, requestBody);
		assertTrue(StringUtility.isNullOrEmpty(prePublishingResponse.getBody().getWarning()));

		Map<String, RestExecutionResultEnum> resultValidation = callValidation(ActivityEnum.VERIFICA, HealthDataFormatEnum.CDA, null, pdfResource, true, false, true);    	
		assertEquals(RestExecutionResultEnum.OK, resultValidation.values().iterator().next());
    	
		Map<String, RestExecutionResultEnum> resultPrePublishing = callValidation(ActivityEnum.VALIDATION, HealthDataFormatEnum.CDA, null, pdfAttachment, true, false, true);    	
    	assertEquals(RestExecutionResultEnum.OK, resultPrePublishing.values().iterator().next());

	}


	@Test
	@DisplayName("Mandatory elements")
	void checkMandatoryElement() {
		byte[] pdfAttachment = FileUtility.getFileFromInternalResources("Files/attachment/pdf_msg_SATLED_LED_Lettera_di_Dimissione.pdf");

		Map<String,RestExecutionResultEnum> validationResult = callValidation(null, HealthDataFormatEnum.CDA, InjectionModeEnum.ATTACHMENT, pdfAttachment, true, false, true);
		assertEquals(RestExecutionResultEnum.MANDATORY_ELEMENT_ERROR, validationResult.get("ERROR"));
 
		validationResult = callValidation(null, HealthDataFormatEnum.CDA, InjectionModeEnum.ATTACHMENT, pdfAttachment, true,
				buildValidationReqDTOCustom(null, HealthDataFormatEnum.CDA, InjectionModeEnum.ATTACHMENT,
						TipoDocAltoLivEnum.WOR, null, null, null, null), false, true);
		assertEquals(RestExecutionResultEnum.MANDATORY_ELEMENT_ERROR, validationResult.get("ERROR"));

	}

	/**
	 * Valutazione performance.
	 */
	@Test
	@Disabled("Used to evalate performance, does not asser anything")
	void performance() {
		final Collection<Long> syncOK = Collections.synchronizedCollection(new ArrayList<>());
		final Collection<Long> syncKO = Collections.synchronizedCollection(new ArrayList<>());
		final Long start = new Date().getTime();
		int u = 0;

		byte[] pdf = FileUtility.getFileFromInternalResources("LAB_OK.pdf");
		for (; u < N_UTENTI; u++) {
			log.info("START USER: " + u);
			startUser(syncOK, syncKO,pdf);
		}

		wait(syncOK, syncKO);
		final Long stop = new Date().getTime();
		final BigDecimal bdSumTime = new BigDecimal(stop - start).divide(new BigDecimal(MILLISECONDS_IN_S), 2, RoundingMode.HALF_UP);

		final BigDecimal nOkReq = BigDecimal.valueOf(syncOK.size());
		if (nOkReq.compareTo(BigDecimal.ZERO) > 0) {
			log.error("REQ/S: " + BigDecimal.valueOf(syncOK.size()).divide(bdSumTime, 2, RoundingMode.HALF_UP).toString(), new Exception());
		}
		assertEquals(N_UTENTI, u);
	}

	private void wait(final Collection<Long> syncOK, final Collection<Long> syncKO) {
		Integer nSleepTime = 0;
		for (;;) {
			try {
				final Integer nSamples = syncOK.size() + syncKO.size();
				final Integer nNeededSamples = N_UTENTI * N_PROVE;
				Thread.sleep(SLEEP_TIME);
				if ((nSamples.equals(nNeededSamples)) || (nSleepTime > SLEEP_TRY)) {
					break;
				}
				nSleepTime++;
			} catch (final InterruptedException e) {
				log.error(""+e);
				throw new RuntimeException(e);
			}
		}
	}

	private void startUser(final Collection<Long> syncOK, final Collection<Long> syncKO, byte[] pdf) {
		new Thread() {
			@Override
			public void run() {
				for (int x = 0; x < N_PROVE; x++) {
					boolean fail = true;
					try {
						final Map<String, RestExecutionResultEnum> resp = callValidation(ActivityEnum.VERIFICA, HealthDataFormatEnum.CDA, InjectionModeEnum.ATTACHMENT, pdf, true, false, true);
						if (RestExecutionResultEnum.OK.equals(resp.values().iterator().next())) {
							fail = false;
						}
					} catch (final Exception e) {
						log.error(""+e);
					}
					if (fail) {
						log.info("SAMPLE KO");
						syncKO.add(1L);
					} else {
						log.info("SAMPLE OK");
						syncOK.add(1L);
					}
				}
			}

		}.start();

	}

	@Test
	void validationErrorTest() throws JsonProcessingException {

		final byte[] file = FileUtility.getFileFromInternalResources("Files" + File.separator + "attachment" + File.separator + "LAB_OK.pdf");

		final String jwtToken = generateJwt(file, true, EventTypeEnum.VALIDATION);
		ValidationCDAReqDTO requestBody = new ValidationCDAReqDTO();
		Exception thrownException = assertThrows(HttpClientErrorException.BadRequest.class, () -> callPlainValidation(jwtToken, file, requestBody));
		assertTrue(thrownException.getMessage().contains(RestExecutionResultEnum.MANDATORY_ELEMENT_ERROR.getType()));

		requestBody.setActivity(ActivityEnum.VERIFICA);

		final ResponseEntity<ValidationResDTO> validationResponse200 = callPlainValidation(jwtToken, file, requestBody);
		assertNotNull(validationResponse200.getBody());
		assertNotNull(validationResponse200.getBody().getWorkflowInstanceId());
		assertNotNull(validationResponse200.getBody().getWarning());
		
		requestBody.setMode(InjectionModeEnum.ATTACHMENT);
		final ResponseEntity<ValidationResDTO> validationRes = callPlainValidation(jwtToken, file, requestBody);
		assertTrue(StringUtility.isNullOrEmpty(validationRes.getBody().getWarning()));
		
		thrownException = assertThrows(HttpClientErrorException.Forbidden.class, () -> callPlainValidation(null, file, requestBody));
		thrownException = assertThrows(HttpClientErrorException.BadRequest.class, () -> callPlainValidation(jwtToken, new byte[0], requestBody));
		assertTrue(thrownException.getMessage().contains(RestExecutionResultEnum.EMPTY_FILE_ERROR.getType()));
	}

	@Test
	@DisplayName("extractJWT error tests")
	void extractJwtErrorTests() {
		final byte[] file = FileUtility.getFileFromInternalResources(
				"Files" + File.separator + "attachment" + File.separator + "LAB_OK.pdf");

		// null payload
		final String jwtTokenNullPayload = generateWrongJwt(StringUtility.encodeSHA256(file), false, true, false);
		ValidationCDAReqDTO requestBody2 = new ValidationCDAReqDTO();
		assertThrows(HttpClientErrorException.Forbidden.class, () -> callPlainValidation(jwtTokenNullPayload, file, requestBody2));

	}

	@Test
	@DisplayName("validate JWT error tests")
	void validateJwtErrorTests() {
		final byte[] file = FileUtility.getFileFromInternalResources(
				"Files" + File.separator + "attachment" + File.separator + "LAB_OK.pdf");

		// null payload
		final String jwtTokenWrongPayload = generateWrongJwt(StringUtility.encodeSHA256(file), false, false, true);
		ValidationCDAReqDTO requestBody = new ValidationCDAReqDTO();
		requestBody.setActivity(ActivityEnum.VERIFICA);
		assertThrows(HttpClientErrorException.Forbidden.class, () -> callPlainValidation(jwtTokenWrongPayload, file, requestBody));

	}

	@Test
	@DisplayName("validator client error test")
	void validatorClientErrorTest() {
		byte[] pdfAttachment = FileUtility.getFileFromInternalResources("Files/attachment/pdf_msg_SATLED_LED_Lettera_di_Dimissione.pdf");
		byte[] pdfResource = FileUtility.getFileFromInternalResources("Files/resource/cert1.pdf");

		String cda = extractCDA(pdfAttachment);
		final String hashedCdaAttachment = StringUtility.encodeSHA256B64(cda);

		cdaSRV.consumeHash(hashedCdaAttachment);

		cda = extractCDA(pdfResource);
		final String hashedCdaResource = StringUtility.encodeSHA256B64(cda);

		cdaSRV.consumeHash(hashedCdaResource);
		when(validatorClient.validate(anyString(),anyString(), any())).thenReturn(ValidationInfoDTO.builder().result(RawValidationEnum.SYNTAX_ERROR).build());
		Map<String, RestExecutionResultEnum> result = callValidation(
				ActivityEnum.VERIFICA, HealthDataFormatEnum.CDA,
				null,
				pdfResource,
				true, false, true);
		assertEquals(RestExecutionResultEnum.SYNTAX_ERROR, result.values().iterator().next());
	}

	@Test
	@DisplayName("Validator String repositoryUniqueId Test")
	void validatorRepositoryUniqueIdFromStringTest() {
		String rightId = "2.16.840.1.113883.2.9.2.080.4.5.1234";
		String wrongRegionId = "2.16.840.1.113883.2.9.2.123.4.5.1234";

		assertDoesNotThrow(() -> ValidationUtility.repositoryUniqueIdValidation(rightId));
		assertThrows(ValidationException.class, () -> ValidationUtility.repositoryUniqueIdValidation(wrongRegionId));
	}

}
