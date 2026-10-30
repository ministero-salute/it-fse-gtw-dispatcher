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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;

import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.impl.FhirMappingClient;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.JWTPayloadDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.ResourceDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.PublicationCreationReqDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.client.TransformResDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.AttivitaClinicaEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.EventCodeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.EventTypeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.HealthDataFormatEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.HealthcareFacilityEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.InjectionModeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.PracticeSettingCodeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.TipoDocAltoLivEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ValidationException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.IConfigSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.IFhirSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.impl.IniEdsInvocationSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.FileUtility;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.StringUtility;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(Constants.Profile.TEST)
class DocumentReferenceTest extends AbstractTest {

	@Autowired
	private IFhirSRV documentReferenceSRV;

	@MockitoSpyBean
	private RestTemplate restTemplate;

	@Autowired
	private FhirMappingClient fhirMappingClient;

	@MockitoBean
	private IConfigSRV configSRV;

	@Autowired
	private IniEdsInvocationSRV iniEdsInvocationSRV;

	@Test
	@DisplayName("createDocumentReference OK")
	void createDocumentReferenceOkTest() {
		String workflowInstanceId = StringUtility.generateUUID();
		byte[] cdaFile = FileUtility.getFileFromInternalResources(
				"Files" + File.separator + "Esempio CDA2_Referto Medicina di Laboratorio v6_OK.xml");
		String cda = new String(cdaFile);
		PublicationCreationReqDTO reqDTO = buildPublicationReqDTO(workflowInstanceId, false);
		String documentSha = StringUtility.encodeSHA256(cdaFile);

		TransformResDTO res = new TransformResDTO();
		res.setErrorMessage("");
		res.setJson(Document.parse("{\"json\" : \"json\"}"));
		Mockito.doReturn(res).when(restTemplate).postForObject(
				anyString(), any(HttpEntity.class), eq(TransformResDTO.class));

		ResourceDTO resourceDTO = documentReferenceSRV.createFhirResources(cda, "", reqDTO, documentSha.length(),
				documentSha, "", "", "", "", "");
		assertNotNull(resourceDTO.getDocumentEntryJson());
		assertNotNull(resourceDTO.getSubmissionSetEntryJson());
		assertNull(resourceDTO.getErrorMessage());

		final String jwt = generateJwt(cdaFile, true, EventTypeEnum.PUBLICATION);
		String[] chunks = jwt.substring(Constants.App.BEARER_PREFIX.length()).split("\\.");

		final String payload = new String(Base64.getDecoder().decode(chunks[1]));

		Boolean insert = iniEdsInvocationSRV.insert(workflowInstanceId, resourceDTO,
				JWTPayloadDTO.extractPayload(payload));
		assertTrue(insert);
	}

	private PublicationCreationReqDTO buildPublicationReqDTO(String workflowInstanceId, boolean validationException) {
		PublicationCreationReqDTO output = new PublicationCreationReqDTO();
		output.setAssettoOrganizzativo(PracticeSettingCodeEnum.AD_PSC001);
		output.setConservazioneANorma("Conservazione");
		output.setDataFinePrestazione(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
		output.setHealthDataFormat(HealthDataFormatEnum.CDA);
		output.setIdentificativoDoc("Identificativo doc");
		output.setIdentificativoRep(validationException ? "error" : "2.16.840.1.113883.2.9.2.080.4.5.1234" );
		output.setIdentificativoSottomissione("identificativo sottomissione");
		output.setMode(InjectionModeEnum.ATTACHMENT);
		output.setAttiCliniciRegoleAccesso(Arrays.asList(EventCodeEnum._94503_0.getCode()));
		output.setTipoAttivitaClinica(AttivitaClinicaEnum.CON);
		output.setTipoDocumentoLivAlto(TipoDocAltoLivEnum.PRE);
		output.setTipologiaStruttura(HealthcareFacilityEnum.Ospedale);
		output.setWorkflowInstanceId(workflowInstanceId);
		return output;
	}

	@Test
	@DisplayName("createDocumentReference error")
	void createDocumentReferenceErrorTest() {
		String workflowInstanceId = StringUtility.generateUUID();

		TransformResDTO res = new TransformResDTO();
		res.setErrorMessage("errorMessage");
		Mockito.doReturn(res).when(restTemplate).postForObject(
				anyString(), any(HttpEntity.class), eq(TransformResDTO.class));

		byte[] cdaFile = FileUtility.getFileFromInternalResources(
				"Files" + File.separator + "Esempio CDA2_Referto Medicina di Laboratorio v6_OK.xml");
		String cda = new String(cdaFile);
		PublicationCreationReqDTO reqDTO = buildPublicationReqDTO(workflowInstanceId, false);
		String documentSha = StringUtility.encodeSHA256(cdaFile);
		ResourceDTO resourceDTO = documentReferenceSRV.createFhirResources(cda, "", reqDTO, documentSha.length(),
				documentSha, "PersonId", "", "", "", "");

		assertEquals("errorMessage", resourceDTO.getErrorMessage());
	}

	@Test
	@DisplayName("createDocumentReferenceErrorConnectionRefused Test")
	void createDocumentReferenceConnectionRefusedTest() {
		Mockito.doThrow(new ResourceAccessException("Connection refused")).when(restTemplate).postForObject(
				anyString(), any(HttpEntity.class), eq(TransformResDTO.class));

		byte[] cdaFile = FileUtility.getFileFromInternalResources(
				"Files" + File.separator + "Esempio CDA2_Referto Medicina di Laboratorio v6_OK.xml");
		String cda = new String(cdaFile);
		String workflowInstanceId = StringUtility.generateUUID();
		PublicationCreationReqDTO reqDTO = buildPublicationReqDTO(workflowInstanceId, false);
		String documentSha = StringUtility.encodeSHA256(cdaFile);
		assertThrows(BusinessException.class,
				() -> documentReferenceSRV.createFhirResources(cda, "", reqDTO, documentSha.length(), documentSha,
						"PersonId", "", "", "", ""));
	}

	@Test
	@DisplayName("createDocumentReferenceErrorBusinessException Test")
	void createDocumentReferenceErrorBusinessException() {
		Mockito.doThrow(new BusinessException("Error")).when(restTemplate).postForObject(
				anyString(), any(HttpEntity.class), eq(TransformResDTO.class));

		byte[] cdaFile = FileUtility.getFileFromInternalResources(
				"Files" + File.separator + "Esempio CDA2_Referto Medicina di Laboratorio v6_OK.xml");
		String cda = new String(cdaFile);
		String workflowInstanceId = StringUtility.generateUUID();
		PublicationCreationReqDTO reqDTO = buildPublicationReqDTO(workflowInstanceId, false);
		String documentSha = StringUtility.encodeSHA256(cdaFile);
		assertThrows(BusinessException.class,
				() -> documentReferenceSRV.createFhirResources(cda, "", reqDTO, documentSha.length(), documentSha,
						"PersonId", "", "", "", ""));
	}

	@Test 
	@DisplayName("createDocumentReferenceValidationException Test")
	void createDocumentReferenceValidationException(){

		byte[] cdaFile = FileUtility.getFileFromInternalResources(
				"Files" + File.separator + "Esempio CDA2_Referto Medicina di Laboratorio v6_OK.xml");
		String cda = new String(cdaFile);
		String workflowInstanceId = StringUtility.generateUUID();
		String documentSha = StringUtility.encodeSHA256(cdaFile);
		PublicationCreationReqDTO reqDTO = buildPublicationReqDTO(workflowInstanceId, true);
		assertThrows(ValidationException.class,
				() -> documentReferenceSRV.createFhirResources(cda, "", reqDTO, documentSha.length(), documentSha,
						"PersonId", "", "", "", ""));

	}

}
