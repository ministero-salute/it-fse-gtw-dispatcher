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

import static it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.EventTypeEnum.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.*;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.google.gson.Gson;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.routes.IniClientRoutes;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.CDACFG;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.JWTHeaderDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.JWTPayloadDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.PublicationCreationReqDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.ValidationCDAReqDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.ErrorResponseDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.PublicationResDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.ValidationResDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.CfUtility;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.FileUtility;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.JsonUtility;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.PDFUtility;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.StringUtility;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractTest {

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	protected IniClientRoutes routes;

	@Autowired
    private ServletWebServerApplicationContext webServerAppCtxt;
	
	public Map<String, RestExecutionResultEnum> callValidationWithoutToken(ActivityEnum activity, HealthDataFormatEnum type, InjectionModeEnum mode, byte[] fileByte){
		return callValidation(activity, type, mode, fileByte, false, null, false, true);
	}

	public Map<String, RestExecutionResultEnum> callValidation(ActivityEnum activity, HealthDataFormatEnum type, InjectionModeEnum mode, byte[] fileByte,
			boolean tokenPresent, final boolean fromGovway, boolean isValidMultipart){
		return callValidation(activity, type, mode, fileByte, tokenPresent, null, fromGovway, isValidMultipart);
	}

	public Map<String, RestExecutionResultEnum> callValidation(ActivityEnum activity, HealthDataFormatEnum type, InjectionModeEnum mode, byte[] fileByte,
			boolean tokenPresent, ValidationCDAReqDTO reqDTO, final boolean fromGovway, final boolean isValidMultipart) {
		Map<String, RestExecutionResultEnum> output = new HashMap<>();
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
				map.add("requestBody", buildValidationReqDTO(activity, type, mode));
			} else {
				map.add("requestBody", reqDTO);
			}

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.MULTIPART_FORM_DATA);

			if(tokenPresent) {
				log.debug("Simulating a valid json payload");

				if (fromGovway) {
					headers.set(Constants.Headers.JWT_GOVWAY_HEADER, generateJwtGovwayPayload(fileByte));
				} else {
					headers.set(Constants.Headers.JWT_HEADER, generateJwt(fileByte, isValidMultipart, VALIDATION));
				}
			} 
			String urlValidation = getBaseUrl() + "/v1/documents/validation";

			HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(map, headers);

			ResponseEntity<ValidationResDTO> response = restTemplate.exchange(urlValidation, HttpMethod.POST, requestEntity, ValidationResDTO.class);
			if(ActivityEnum.VERIFICA.equals(activity)) {
				assertEquals(200, response.getStatusCode().value());
			} else if(ActivityEnum.VALIDATION.equals(activity)) {
				assertEquals(201, response.getStatusCode().value());
			}

			output.put(response.getBody().getWorkflowInstanceId(), RestExecutionResultEnum.OK);

		} catch (Exception ex) {
			String message = ex.getMessage();
			Integer firstIndex = message.indexOf("{");
			Integer lastIndex = message.indexOf("}");
			String subString = message.substring(firstIndex, lastIndex+1);

			ErrorResponseDTO errorClass = StringUtility.fromJSON(subString, ErrorResponseDTO.class);
			output.put("ERROR", RestExecutionResultEnum.get(errorClass.getType()));
			log.error("Error : " + ex.getMessage());
		}
		return output;
	}
	
	protected String getBaseUrl() {
		return "http://localhost:" + webServerAppCtxt.getWebServer().getPort() + webServerAppCtxt.getServletContext().getContextPath();
	}

	protected ValidationCDAReqDTO buildValidationReqDTO(ActivityEnum activity, HealthDataFormatEnum type, InjectionModeEnum mode) {
		ValidationCDAReqDTO validationReq = ValidationCDAReqDTO.builder().
				activity(activity).
				mode(mode).healthDataFormat(type).
				build(); 
		
		return validationReq;
	}
	
	protected ValidationCDAReqDTO buildValidationReqDTOCustom(ActivityEnum activity, HealthDataFormatEnum type, InjectionModeEnum mode,
			TipoDocAltoLivEnum tipoDocLivelloAlto,PracticeSettingCodeEnum assettoOrganizzativo, 
			String identificativoPaziente, AttivitaClinicaEnum tipoAttivitaClinica, 
			String identificativoSottomissione) {
		ValidationCDAReqDTO validationReq = ValidationCDAReqDTO.builder().
				activity(activity).
				mode(mode).healthDataFormat(type).
				build(); 
		
		return validationReq;
	}

	protected String randomFiscalCode() {
		// To generate a random fiscal code that passes CfUtility.isValid() requires too much effort.
		return "RSSMRA22A01A399Z";
	}

	protected String generateJwt(final byte[] file, final boolean isValidFile, EventTypeEnum eventType) {
		
		byte [] pdfFile = null;
		if (isValidFile) {
			pdfFile = file;
		} else {
			// To generate a jwt is necessary that the file is valid
			pdfFile = FileUtility.getFileFromInternalResources("Files" + File.separator + "attachment" + File.separator + "LAB_OK.pdf");
		}
		final String documentHash = StringUtility.encodeSHA256(pdfFile);
		final String cda = extractCDA(pdfFile);
		String docType = null;
		String personId = null;
		if (cda != null) {
			docType = getDocTypeFromCda(cda);
			personId = getPersonIdFromCda(cda);
		}

		StringBuilder encodedJwtToken = new StringBuilder(Constants.App.BEARER_PREFIX) // Bearer prefix
			.append("RkFLRV9IRUFERVI=") // Fake Header
			.append(".").append(generateJwtPayload(documentHash, personId, "SSSMNN75B01F257L^^^&amp;2.16.840.1.113883.2.9.4.3.2&amp;ISO", docType, eventType)) // Payload
			.append(".").append("RkFLRV9TSUdOQVRVUkU="); // Fake Signature
		return encodedJwtToken.toString();
	}

	private String getDocTypeFromCda(final String cda) {
		org.jsoup.nodes.Document docT = Jsoup.parse(cda);
		String code = docT.select("code").get(0).attr("code");
		String codeSystem = docT.select("code").get(0).attr("codeSystem");
		return "('" + code + "^^" + codeSystem + "')";
	}

	private String getPersonIdFromCda(final String cda) {
		org.jsoup.nodes.Document docT = Jsoup.parse(cda);
		return docT.select("patientRole > id").get(0).attr("extension");
	}

	protected String generateJwtPayload(final String documentHash, final String personId, final String subject, final String docType, EventTypeEnum eventType) {
		String action = ActionEnum.CREATE.name();
		String purposeOfUse = PurposeOfUseEnum.TREATMENT.name();
		if (UPDATE.equals(eventType)) {
			action = ActionEnum.UPDATE.name();
			purposeOfUse = PurposeOfUseEnum.UPDATE.name();
		} else if (DELETE.equals(eventType)) {
			action = ActionEnum.DELETE.name();
			purposeOfUse = PurposeOfUseEnum.UPDATE.name();
		}
		else if (REPLACE.equals(eventType)){
			action = ActionEnum.UPDATE.name();
			purposeOfUse = PurposeOfUseEnum.UPDATE.name();
		}
		else if(VALIDATION.equals(eventType)){

		}

		String applicationId = "ApplicationId";
		String applicationVendor = "ApplicationVendor";
		String applicationVersion = "ApplicationVersion";
		final JWTPayloadDTO jwtPayload = new JWTPayloadDTO("080", 1540890704, 1540918800, "1540918800", 
		"fse-gateway", subject, "080", "Regione Emilia-Romagna", "GTW_NAME^^^^^&2.16.840.1.113883.2.9.4.1.3&ISO^^^^GTW_STRUCTURE",
		"APR", personId, true, purposeOfUse, docType, action, documentHash,
		applicationId,applicationVendor,applicationVersion);
		return Base64.getEncoder().encodeToString(new Gson().toJson(jwtPayload).getBytes());
	}

	@Autowired
	CDACFG cdaCfg;

	protected String extractCDA(final byte[] bytesPDF) {
		String out = null;
		out = PDFUtility.unenvelopeA2(bytesPDF);
		if (StringUtility.isNullOrEmpty(out)) {
			out = PDFUtility.extractCDAFromAttachments(bytesPDF,cdaCfg.getCdaAttachmentName());
		}
		return out;
	}

	protected String generateJwtGovwayPayload(final byte[] file) {
		
		final String documentHash = StringUtility.encodeSHA256(file);
		final String cda = extractCDA(file);
		final String docType = getDocTypeFromCda(cda);
		final String personId = getPersonIdFromCda(cda);
		
		String applicationId = "ApplicationId";
		String applicationVendor = "ApplicationVendor";
		String applicationVersion = "ApplicationVersion";

		final JWTPayloadDTO jwtPayload = new JWTPayloadDTO("201123456", 1540890704, 1540918800, "1540918800", 
		"fse-gateway", "RSSMRA22A01A399Z", "120", "Regione Lazio", "GTW_NAME^^^^^&2.16.840.1.113883.2.9.4.1.3&ISO^^^^GTW_STRUCTURE",
		"AAS", personId, true, "TREATMENT", docType, "CREATE", documentHash,applicationId,applicationVendor,applicationVersion);
		return Base64.getEncoder().encodeToString(new Gson().toJson(jwtPayload).getBytes());
	}

	protected ResponseEntity<ValidationResDTO> callPlainValidation(final String jwtToken, final byte[] file, final ValidationCDAReqDTO requestBody) {
		String urlValidation = "http://localhost:" + webServerAppCtxt.getWebServer().getPort()
				+ webServerAppCtxt.getServletContext().getContextPath() + "/v1/documents/validation";

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		headers.set(Constants.Headers.JWT_HEADER, jwtToken);

		LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
		ByteArrayResource fileAsResource = new ByteArrayResource(file){
			@Override
			public String getFilename(){
				return "file";
			}
		};
		map.add("file",fileAsResource);
		map.add("requestBody", requestBody);
		
		return restTemplate.exchange(urlValidation, HttpMethod.POST, new HttpEntity<>(map, headers), ValidationResDTO.class);
	}

	protected ResponseEntity<PublicationResDTO> callPlainPublication(final String jwtToken, final byte[] fileByte, 
		final PublicationCreationReqDTO requestBody) {
		
		String urlPublication = getBaseUrl() + "/v1/documents";

		LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
		ByteArrayResource fileAsResource = new ByteArrayResource(fileByte){
			@Override
			public String getFilename(){
				return "file";
			}
		};

		map.add("file", fileAsResource);
		map.add("requestBody", requestBody);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		headers.set(Constants.Headers.JWT_HEADER, jwtToken);

		HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(map, headers);
		return restTemplate.exchange(urlPublication, HttpMethod.POST, requestEntity, PublicationResDTO.class);
	}

	protected String generateWrongJwt(final String documentHash, boolean nullHeader, boolean nullPayload, boolean customResourceHl7) {
		JWTHeaderDTO jwtHeader = null;
		JWTPayloadDTO jwtPayload = null;
		String applicationId = "ApplicationId";
		String applicationVendor = "ApplicationVendor";
		String applicationVersion = "ApplicationVersion";
		if (!nullPayload) {
			jwtPayload = new JWTPayloadDTO(
					"201123456",
					1540890704,
					1540918800,
					"1540918800",
					"fse-gateway",
					"RSSMRA22A01A399Z",
					"120",
					"Regione Lazio",
					"GTW_NAME^^^^^&2.16.840.1.113883.2.9.4.1.3&ISO^^^^GTW_STRUCTURE",
					"AAS",
					"RSSMRA22A01A399Z",
					true,
					"TREATMENT",
					customResourceHl7 ? "custom" : "('11502-2^^2.16.840.1.113883.6.1')",
					"CREATE",
					documentHash,
					applicationId,
					applicationVendor,
					applicationVersion
			);
		}

		if (!nullHeader) {
			jwtHeader = new JWTHeaderDTO(
					"RS256",
					Constants.App.JWT_TOKEN_TYPE,
					null,
					"X5C cert base 64"
			);
		}

		StringBuilder encodedJwtToken = new StringBuilder(Constants.App.BEARER_PREFIX) // Bearer prefix
				.append(Base64.getEncoder().encodeToString(new Gson().toJson(jwtHeader).getBytes())) // Header
				.append(".").append(Base64.getEncoder().encodeToString(new Gson().toJson(jwtPayload).getBytes())); // Payload
		return encodedJwtToken.toString();
	}

	protected ValidationCDAReqDTO validateDataPreparation() {
		ValidationCDAReqDTO validationRB = new ValidationCDAReqDTO();
		validationRB.setActivity(ActivityEnum.VALIDATION);
		validationRB.setHealthDataFormat(HealthDataFormatEnum.CDA);
		validationRB.setMode(InjectionModeEnum.ATTACHMENT);
		return validationRB;
	}

	protected PublicationCreationReqDTO publicationDataPreparation() {
		PublicationCreationReqDTO publicationRB = new PublicationCreationReqDTO();
		publicationRB.setWorkflowInstanceId(null); // Must be null if force publish
		publicationRB.setTipologiaStruttura(HealthcareFacilityEnum.Ospedale);
		publicationRB.setIdentificativoDoc(TestConstants.documentId);
		publicationRB.setIdentificativoRep("identificativoRep");
		publicationRB.setMode(InjectionModeEnum.ATTACHMENT);
		publicationRB.setTipoDocumentoLivAlto(TipoDocAltoLivEnum.REF);
		publicationRB.setAssettoOrganizzativo(PracticeSettingCodeEnum.AD_PSC055);
		publicationRB.setAssettoOrganizzativo(PracticeSettingCodeEnum.AD_PSC055);
		publicationRB.setTipoAttivitaClinica(AttivitaClinicaEnum.CON);
		publicationRB.setIdentificativoSottomissione("identificativoSottomissione");
		return publicationRB;
	}
	
	protected boolean validationCF(Boolean prop, String fiscalCode) {
		boolean result = false;
		
		if(fiscalCode!=null) {
	    	if(Boolean.TRUE.equals(prop)) {
	    		result = CfUtility.validaCF(fiscalCode) == CfUtility.CF_OK_16 || CfUtility.validaCF(fiscalCode) == CfUtility.CF_OK_11
		            || CfUtility.validaCF(fiscalCode) == CfUtility.CF_ENI_OK || CfUtility.validaCF(fiscalCode) == CfUtility.CF_STP_OK;
	    	} else {
	    		result = (fiscalCode.length() == 16 && CfUtility.validaCF(fiscalCode) == CfUtility.CF_OK_16);
	    	}
    	} else {
    		result = false;
    	}
		return result;
		
	}

	public PublicationCreationReqDTO buildReqDTO() {
		return JsonUtility.jsonToObject(TestConstants.mockRequest, PublicationCreationReqDTO.class);
	}
}
