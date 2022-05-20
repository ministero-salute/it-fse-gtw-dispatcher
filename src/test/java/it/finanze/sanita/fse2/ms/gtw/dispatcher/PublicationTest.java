package it.finanze.sanita.fse2.ms.gtw.dispatcher;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.IValidatorClient;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.impl.FhirMappingClient;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.DocumentReferenceDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.ValidationInfoDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.PublicationCreationReqDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.ErrorResponseDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.PublicationCreationResDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.client.DocumentReferenceResDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.ActivityEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.AttivitaClinicaEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.EventCodeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.HealthDataFormatEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.HealthcareFacilityEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.InjectionModeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.PracticeSettingCodeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.RawValidationEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.TipoDocAltoLivEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.ValidationResultEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.facade.ICdaFacadeSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.FileUtility;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.StringUtility;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ComponentScan(basePackages = {Constants.ComponentScan.BASE})
@ActiveProfiles(Constants.Profile.TEST)
class PublicationTest extends AbstractTest {

	@Autowired
	private ICdaFacadeSRV cdaFacadeSRV;

	@Autowired
	private ServletWebServerApplicationContext webServerAppCtxt;

	@Autowired
	private RestTemplate restTemplate;

	@MockBean
	private IValidatorClient validatorClient;

	@MockBean
	private FhirMappingClient client;
	
	@Test
	void testHashPublication() {

		log.info("Testing hash check in publication phase");
		final String txID = UUID.randomUUID().toString();
		final String hash = StringUtility.encodeSHA256B64("hash");
		final String expectedHash = StringUtility.encodeSHA256B64("expected_hash");

		assertThrows(BusinessException.class, () -> cdaFacadeSRV.validateHash(hash, null), "If the txId is null, a Business exception should be thrown.");
		assertFalse(cdaFacadeSRV.validateHash(hash, txID), "If the hash is not present on Redis, the result should be false.");

		log.info("Inserting a key on Redis");
		cdaFacadeSRV.create(txID, hash);
		assertTrue(cdaFacadeSRV.validateHash(hash, txID), "If the hash is present on Redis, the result should be true");

		final String unmatchinTxID = UUID.randomUUID().toString();
		cdaFacadeSRV.create(unmatchinTxID, expectedHash);

		assertFalse(cdaFacadeSRV.validateHash(hash, unmatchinTxID), "If the hash present on Redis is different from expected one, the result should be false");
	}
	
	@Test
	void testPublication() {
		DocumentReferenceResDTO ref = new DocumentReferenceResDTO();
		ref.setErrorMessage("");
		ref.setJson("{\"json\" : \"json\"}");
		given(client.callCreateDocumentReference(any(DocumentReferenceDTO.class))).willReturn(ref);
		
		byte[] pdfAttachment = FileUtility.getFileFromInternalResources("CDA_OK_SIGNED.pdf");

		ValidationInfoDTO info = new ValidationInfoDTO(RawValidationEnum.OK, new ArrayList<>());
		given(validatorClient.validate(anyString())).willReturn(info);

		Map<String,ValidationResultEnum> res = callValidation(ActivityEnum.PRE_PUBLISHING, HealthDataFormatEnum.CDA, InjectionModeEnum.ATTACHMENT, pdfAttachment, true);
		Optional<String> firstKey = res.keySet().stream().findFirst();

		String transactionId = "";
		if (firstKey.isPresent()) {
			transactionId = firstKey.get();
		}
		PublicationCreationResDTO resPublication = callPublication(pdfAttachment,null, transactionId);
		assertNotNull(resPublication.getTraceID()); 
	}

	public PublicationCreationResDTO callPublication(byte[] fileByte,PublicationCreationReqDTO reqDTO, String transactionId) {
		PublicationCreationResDTO output = null;
		LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();

		try {
			ByteArrayResource fileAsResource = new ByteArrayResource(fileByte){
				@Override
				public String getFilename(){
					return "file";
				}
			};

			map.add("file",fileAsResource);

			if(reqDTO==null) {
				map.add("requestBody", buildCreationDTO(transactionId));
			} else {
				map.add("requestBody", reqDTO);
			}

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.MULTIPART_FORM_DATA);
			headers.set("Authorization", "test");

			String urlPublication = "http://localhost:" + webServerAppCtxt.getWebServer().getPort() + webServerAppCtxt.getServletContext().getContextPath() + "/v1/publish-creation";

			HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(map, headers);

			ResponseEntity<PublicationCreationResDTO> response = restTemplate.exchange(urlPublication, HttpMethod.POST, requestEntity, PublicationCreationResDTO.class);
			output = response.getBody();
		} catch (Exception ex) {
			String message = ex.getMessage();
			Integer firstIndex = message.indexOf("{");
			Integer lastIndex = message.indexOf("}");
			String subString = message.substring(firstIndex, lastIndex+1);

			ErrorResponseDTO errorClass = StringUtility.fromJSON(subString, ErrorResponseDTO.class);
			log.info("Status {}", errorClass.getStatus());
			log.error("Error : " + ex.getMessage());
		}
		return output;
	}

	private PublicationCreationReqDTO buildCreationDTO(String transactionId) {
		PublicationCreationReqDTO output = PublicationCreationReqDTO.builder().
				assettoOrganizzativo(PracticeSettingCodeEnum.AD_PSC001).
				conservazioneSostitutiva("Conservazione sostitutiva").
				dataFinePrestazione(""+new Date().getTime()).
				dataInizioPrestazione(""+new Date().getTime()).
				forcePublish(false).
				healthDataFormat(HealthDataFormatEnum.CDA).
				identificativoDoc("Identificativo doc").
				identificativoPaziente("Identificativo paziente").
				identificativoRep("Identificativo rep").
				identificativoSottomissione("Identificativo Sottomissione").
				mode(InjectionModeEnum.ATTACHMENT).
				regoleAccesso(java.util.Arrays.asList(EventCodeEnum._94503_0)).
				tipoAttivitaClinica(AttivitaClinicaEnum.CONSULTO).
				tipoDocumentoLivAlto(TipoDocAltoLivEnum.DOCUMENTO_WORKFLOW).
				tipologiaStruttura(HealthcareFacilityEnum.OSPEDALE).
				transactionID(transactionId).
				build();
		return output;
	}

}
