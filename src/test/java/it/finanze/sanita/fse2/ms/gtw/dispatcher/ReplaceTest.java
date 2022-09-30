package it.finanze.sanita.fse2.ms.gtw.dispatcher;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.ValidationInfoDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.PublicationCreationReqDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.PublicationResDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.client.TransformResDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.client.ValidationResDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.entity.IniEdsInvocationETY;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.FileUtility;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.StringUtility;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ComponentScan(basePackages = {Constants.ComponentScan.BASE})
@ActiveProfiles(Constants.Profile.TEST)
class ReplaceTest extends AbstractTest {

	@Autowired
	ServletWebServerApplicationContext webServerAppCtxt;

	@SpyBean
	RestTemplate restTemplate;

	@Autowired
	MongoTemplate mongoTemplate;

	@ParameterizedTest
	@DisplayName("Replace of a document")
	@ValueSource(strings = {"CDA_OK_SIGNED.pdf", "LDO_OK.pdf", "RAD_OK.pdf", "RSA_OK.pdf", "VPS_OK.pdf"})
	void givenACorrectCDA_shouldInsertInInvocations(final String filename) {

		mockDocumentRef();
		mockFhirMapping();
		
		final byte[] pdfAttachment = FileUtility.getFileFromInternalResources("Files/attachment/" + filename);
		final String idDocument = StringUtility.generateUUID();
		
		mockValidation(pdfAttachment);
		
		final ResponseEntity<PublicationResDTO> response = callReplace(idDocument, pdfAttachment);
		final PublicationResDTO body = response.getBody();

		assertTrue(StringUtility.isNullOrEmpty(body.getWarning()), "With a correct CDA, no warning should have been returned");
		assertNotNull(body.getWorkflowInstanceId(), "A workflow instance id should have been returned");
	
		final List<IniEdsInvocationETY> invocations = getIniInvocationEntities(body.getWorkflowInstanceId());
		assertEquals(1, invocations.size(), "Only one insertion should have been made");
		
		final IniEdsInvocationETY invocation = invocations.get(0);
		assertEquals(idDocument, invocation.getIdDoc(), "The same document Id should be present on the entity invocations");
		assertEquals(3, invocation.getMetadata().size(), "The three metadata should have been present on invocations collection");
		assertNotNull(invocation.getData(), "The data of invocation contains CDA info and cannot be null");
	}

	private void mockValidation(byte[] pdfAttachment) {
		ValidationInfoDTO info = new ValidationInfoDTO(RawValidationEnum.OK, new ArrayList<>());
		Mockito.doReturn(new ResponseEntity<>(info, HttpStatus.OK)).when(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(ValidationResDTO.class));
		callValidation(ActivityEnum.VALIDATION, HealthDataFormatEnum.CDA, InjectionModeEnum.ATTACHMENT, pdfAttachment, true, false, true);
	}

	@ParameterizedTest
	@DisplayName("Calling replace with invalid request body")
	@ValueSource(strings = {"CDA_OK_SIGNED.pdf", "LDO_OK.pdf", "RAD_OK.pdf", "RSA_OK.pdf", "VPS_OK.pdf"})
	void givenInvalidRequest_shouldReturnError(final String filename) {

		mockDocumentRef();
		mockFhirMapping();

		final String idDocument = StringUtility.generateUUID();
		final byte[] notPdfFile = FileUtility.getFileFromInternalResources("Files/Test.docx");
		
		assertThrows(HttpClientErrorException.BadRequest.class, 
			() ->  callReplace(idDocument, notPdfFile, null, false), "Not providing a valid file should throw a bad request exception");

		final byte[] pdfAttachment = FileUtility.getFileFromInternalResources("Files/attachment/" + filename);
		PublicationCreationReqDTO rBody = PublicationCreationReqDTO.builder().build();
		assertThrows(HttpClientErrorException.BadRequest.class, 
			() ->  callReplace(idDocument, pdfAttachment, rBody, true), "Not providing a valid request body should throw a bad request exception");

		rBody.setAssettoOrganizzativo(PracticeSettingCodeEnum.AD_PSC001);
		assertThrows(HttpClientErrorException.BadRequest.class, 
			() ->  callReplace(idDocument, pdfAttachment, rBody, true), "Not providing a valid request body should throw a bad request exception");

		rBody.setConservazioneANorma("Conservazione sostitutiva");
		assertThrows(HttpClientErrorException.BadRequest.class, 
			() ->  callReplace(idDocument, pdfAttachment, rBody, true), "Not providing a valid request body should throw a bad request exception");
	
		
		rBody.setDataFinePrestazione(String.valueOf(new Date().getTime()));
		rBody.setDataInizioPrestazione(String.valueOf(new Date().getTime()));
		rBody.setHealthDataFormat(HealthDataFormatEnum.CDA);
		assertThrows(HttpClientErrorException.BadRequest.class, 
			() ->  callReplace(idDocument, pdfAttachment, rBody, true), "Not providing a valid request body should throw a bad request exception");
	
		rBody.setIdentificativoRep("Identificativo rep");
		assertThrows(HttpClientErrorException.BadRequest.class, 
			() ->  callReplace(idDocument, pdfAttachment, rBody, true), "Not providing a valid request body should throw a bad request exception");
	
		rBody.setIdentificativoSottomissione("Identificativo Sottomissione");
		assertThrows(HttpClientErrorException.BadRequest.class, 
			() ->  callReplace(idDocument, pdfAttachment, rBody, true), "Not providing a valid request body should throw a bad request exception");
	
		rBody.setAttiCliniciRegoleAccesso(java.util.Arrays.asList(EventCodeEnum._94503_0.getCode()));
		assertThrows(HttpClientErrorException.BadRequest.class, 
			() ->  callReplace(idDocument, pdfAttachment, rBody, true), "Not providing a valid request body should throw a bad request exception");
	
		rBody.setTipoAttivitaClinica(AttivitaClinicaEnum.CON);
		assertThrows(HttpClientErrorException.BadRequest.class, 
			() ->  callReplace(idDocument, pdfAttachment, rBody, true), "Not providing a valid request body should throw a bad request exception");
	
		rBody.setTipoDocumentoLivAlto(TipoDocAltoLivEnum.WOR);
		assertThrows(HttpClientErrorException.BadRequest.class, 
			() ->  callReplace(idDocument, pdfAttachment, rBody, true), "Not providing a valid request body should throw a bad request exception");
	
		rBody.setTipologiaStruttura(HealthcareFacilityEnum.Ospedale);
	
		mockValidation(pdfAttachment);
		ResponseEntity<PublicationResDTO> response = callReplace(idDocument, pdfAttachment, rBody, true);
		assertEquals(Constants.Misc.WARN_EXTRACTION_SELECTION, response.getBody().getWarning(), 
			"Not providing injection mode should not stop the call but should return a warning");
		
		assertNotNull(response.getBody().getWorkflowInstanceId(), "Workflow instance id should not be null");	

		rBody.setMode(InjectionModeEnum.ATTACHMENT);
		
		mockValidation(pdfAttachment);
		response = callReplace(idDocument, pdfAttachment, rBody, true);
		assertTrue(StringUtility.isNullOrEmpty(response.getBody().getWarning()), "No warning should have be returned with a correct CDA");
		assertNotNull(response.getBody().getWorkflowInstanceId(), "Workflow instance id should not be null");
	}

	private void mockFhirMapping() {
		log.info("Mocking fhir-mapping client");
		final ValidationInfoDTO info = new ValidationInfoDTO(RawValidationEnum.OK, new ArrayList<>());
		Mockito.doReturn(new ResponseEntity<>(info, HttpStatus.OK)).when(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(ValidationResDTO.class));
	}

	private List<IniEdsInvocationETY> getIniInvocationEntities(final String workflowInstanceId) {
		Query query = new Query();
		query.addCriteria(Criteria.where("workflow_instance_id").is(workflowInstanceId));
		return mongoTemplate.find(query, IniEdsInvocationETY.class);
	}

	ResponseEntity<PublicationResDTO> callReplace(final String idDocumentReplace, final byte[] fileByte) {
		return callReplace(idDocumentReplace, fileByte, null, true);
	}

	ResponseEntity<PublicationResDTO> callReplace(final String idDocumentReplace, final byte[] fileByte, final PublicationCreationReqDTO requestBody, final boolean isValidFile) {
		LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();

		ByteArrayResource fileAsResource = new ByteArrayResource(fileByte){
			@Override
			public String getFilename(){
				return "file";
			}
		};

		map.add("file", fileAsResource);
		if (requestBody == null) {
			log.info("Creating a valid request body");
			map.add("requestBody", buildCreationDTO());
		} else {
			map.add("requestBody", requestBody);
		}

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		log.info("Simulating a valid json payload");
		headers.set(Constants.Headers.JWT_HEADER, generateJwt(fileByte, isValidFile));

		final String urlReplace = "http://localhost:" + webServerAppCtxt.getWebServer().getPort() + webServerAppCtxt.getServletContext().getContextPath() + "/v1/documents/" + idDocumentReplace;
		HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(map, headers);
		return restTemplate.exchange(urlReplace, HttpMethod.PUT, requestEntity, PublicationResDTO.class);
	}
	

	PublicationCreationReqDTO buildCreationDTO() {
		PublicationCreationReqDTO output = PublicationCreationReqDTO.builder().
				assettoOrganizzativo(PracticeSettingCodeEnum.AD_PSC001).
				conservazioneANorma("Conservazione sostitutiva").
				dataFinePrestazione(""+new Date().getTime()).
				dataInizioPrestazione(""+new Date().getTime()).
				healthDataFormat(HealthDataFormatEnum.CDA).
				identificativoRep("Identificativo rep").
				identificativoSottomissione("Identificativo Sottomissione").
				mode(InjectionModeEnum.ATTACHMENT).
				attiCliniciRegoleAccesso(java.util.Arrays.asList(EventCodeEnum._94503_0.getCode())).
				tipoAttivitaClinica(AttivitaClinicaEnum.CON).
				tipoDocumentoLivAlto(TipoDocAltoLivEnum.WOR).
				tipologiaStruttura(HealthcareFacilityEnum.Ospedale).
				build();
		return output;
	}

	void mockDocumentRef() {

		log.info("Mocking document reference");

		TransformResDTO ref = new TransformResDTO();
		ref.setErrorMessage("");
		ref.setJson(Document.parse("{\"json\" : \"json\"}"));

		Mockito.doReturn(new ResponseEntity<>(ref, HttpStatus.OK)).when(restTemplate)
				.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TransformResDTO.class));
	}
}