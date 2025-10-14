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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.controller.impl;

import static it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants.App.MISSING_DOC_TYPE_PLACEHOLDER;
import static it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants.App.MISSING_WORKFLOW_PLACEHOLDER;
import static it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.EventStatusEnum.BLOCKING_ERROR;
import static it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.EventStatusEnum.SUCCESS;
import static it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.EventTypeEnum.EDS_DELETE;
import static it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.EventTypeEnum.INI_DELETE;
import static it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.EventTypeEnum.RIFERIMENTI_INI;
import static it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.RestExecutionResultEnum.FHIR_MAPPING_ERROR;
import static it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.RestExecutionResultEnum.GENERIC_ERROR;
import static it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.RestExecutionResultEnum.INI_EXCEPTION;
import static it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.RestExecutionResultEnum.get;
import static it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.CdaUtility.createMasterIdError;
import static it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.CdaUtility.createReqMasterIdError;
import static it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.CdaUtility.createWorkflowInstanceId;
import static it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.CdaUtility.getDocumentType;
import static it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.CdaUtility.isValidMasterId;
import static it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.StringUtility.encodeSHA256;
import static it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.StringUtility.isNullOrEmpty;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.google.gson.Gson;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.IEdsClient;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.IIniClient;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants.Misc;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.ValidationCFG;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.controller.IFhirPublicationCTL;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.IndexerValueDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.JWTPayloadDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.PersonDto;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.ResourceDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.ValidationCreationInputDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.ValidationDataDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.DeleteRequestDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.IniReferenceRequestDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.PublicationCreateReplaceMetadataDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.PublicationCreateReplaceWiiDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.PublicationCreationReqDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.PublicationMetadataReqDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.PublicationUpdateReqDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.EdsResponseDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.ErrorResponseDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.IniReferenceResponseDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.IniTraceResponseDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.LogTraceInfoDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.PublicationResDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.ResponseWifDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.DestinationTypeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.ErrorInstanceEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.EventStatusEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.EventTypeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.OperationLogEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.ProcessorOperationEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.RestExecutionResultEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.ResultLogEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ConnectionRefusedException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.EdsException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.IniException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.MockEnabledException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.NoRecordFoundException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ValidationException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.logging.LoggerHelper;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.IConfigSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.IErrorHandlerSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.IFhirSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.IKafkaSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.facade.ICdaFacadeSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.impl.IniEdsInvocationSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.StringUtility;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.ValidationUtility;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;

/**
 *  Publication controller.
 */
@Slf4j
@RestController
public class FhirPublicationCTL extends AbstractCTL implements IFhirPublicationCTL {

	@Autowired
	private IKafkaSRV kafkaSRV;

	@Autowired
	private IFhirSRV documentReferenceSRV;

	@Autowired
	private IniEdsInvocationSRV iniInvocationSRV;

	@Autowired
	private LoggerHelper logger;

	@Autowired
	private ICdaFacadeSRV cdaSRV;

	@Autowired
	private IErrorHandlerSRV errorHandlerSRV;

	@Autowired
	private IIniClient iniClient;

	@Autowired
	private ValidationCFG validationCFG;

	@Autowired
	private IEdsClient edsClient;
	
	@Autowired
	private IConfigSRV configSRV;


	@Override
	public ResponseEntity<PublicationResDTO> create(final PublicationCreationReqDTO requestBody, final MultipartFile file, final HttpServletRequest request) {
		final Date startDateOperation = new Date();
		final LogTraceInfoDTO traceInfoDTO = getLogTraceInfo();

		log.info("[START] {}() with arguments {}={}, {}={}, {}={}","create","traceId", traceInfoDTO.getTraceID(),"wif", requestBody.getWorkflowInstanceId(),"idDoc", requestBody.getIdentificativoDoc());

		ValidationCreationInputDTO validationInfo = new ValidationCreationInputDTO();
		validationInfo.setValidationData(new ValidationDataDTO(null, false, MISSING_WORKFLOW_PLACEHOLDER, null, null, new Date()));

		try {
			validationInfo = publicationAndReplace(file, request, false, null, traceInfoDTO);
			
			postExecutionCreate(startDateOperation, traceInfoDTO, validationInfo);
		} catch (ConnectionRefusedException ce) {
			errorHandlerSRV.connectionRefusedExceptionHandler(startDateOperation, validationInfo.getValidationData(), validationInfo.getJwtPayloadToken(), validationInfo.getJsonObj(), traceInfoDTO, ce, true, getDocumentType(validationInfo.getDocument()));
		} catch (final ValidationException e) {
			errorHandlerSRV.publicationValidationExceptionHandler(startDateOperation, validationInfo.getValidationData(), validationInfo.getJwtPayloadToken(), validationInfo.getJsonObj(), traceInfoDTO, e, true, getDocumentType(validationInfo.getDocument()));
		}

		String warning = "";

		if (validationInfo.getJsonObj().getMode() == null) {
			warning = Misc.WARN_EXTRACTION_SELECTION;
		}

		log.info("[EXIT] {}() with arguments {}={}, {}={}, {}={}","create","traceId", traceInfoDTO.getTraceID(),"wif", requestBody.getWorkflowInstanceId(),"idDoc", requestBody.getIdentificativoDoc());

		return new ResponseEntity<>(new PublicationResDTO(traceInfoDTO, warning, validationInfo.getValidationData().getWorkflowInstanceId()), HttpStatus.CREATED);
	}

	private void postExecutionCreate(final Date startDateOperation, final LogTraceInfoDTO traceInfoDTO,
			ValidationCreationInputDTO validationInfo) {
		iniInvocationSRV.insert(validationInfo.getValidationData().getWorkflowInstanceId(), validationInfo.getFhirResource(), validationInfo.getJwtPayloadToken());

		String idDoc = validationInfo.getJsonObj().getIdentificativoDoc();

		final IndexerValueDTO kafkaValue = new IndexerValueDTO();
		kafkaValue.setWorkflowInstanceId(validationInfo.getValidationData().getWorkflowInstanceId());
		kafkaValue.setIdDoc(idDoc);
		kafkaValue.setEdsDPOperation(ProcessorOperationEnum.PUBLISH);

		kafkaSRV.notifyChannel(idDoc, new Gson().toJson(kafkaValue), validationInfo.getJsonObj().getTipoDocumentoLivAlto(), DestinationTypeEnum.INDEXER);
		kafkaSRV.sendPublicationStatus(traceInfoDTO.getTraceID(), validationInfo.getValidationData().getWorkflowInstanceId(), SUCCESS, null, validationInfo.getJsonObj(), validationInfo.getJwtPayloadToken());

		logger.info(Constants.App.LOG_TYPE_CONTROL,validationInfo.getValidationData().getWorkflowInstanceId(),String.format("Publication CDA completed for workflow instance id %s", validationInfo.getValidationData().getWorkflowInstanceId()), OperationLogEnum.PUB_CDA2, ResultLogEnum.OK, startDateOperation, getDocumentType(validationInfo.getDocument()), validationInfo.getJwtPayloadToken(),null);
	}

	@Override
	public ResponseEntity<PublicationResDTO> replace(final String idDoc, final PublicationUpdateReqDTO requestBody, final MultipartFile file, final HttpServletRequest request) {

		final Date startDateOperation = new Date();
		final LogTraceInfoDTO traceInfoDTO = getLogTraceInfo();

		ValidationCreationInputDTO validationInfo = new ValidationCreationInputDTO();
		validationInfo.setValidationData(new ValidationDataDTO(null, false, MISSING_WORKFLOW_PLACEHOLDER, null, null, new Date()));

		try {
			if(!isValidMasterId(idDoc)) throw new ValidationException(createReqMasterIdError());
			validationInfo = publicationAndReplace(file, request, true,idDoc,traceInfoDTO);

			log.info("[START] {}() with arguments {}={}, {}={}, {}={}","replace","traceId", traceInfoDTO.getTraceID(),"wif", validationInfo.getValidationData().getWorkflowInstanceId(),"idDoc", idDoc);

			IniReferenceRequestDTO iniReq = new IniReferenceRequestDTO(idDoc, validationInfo.getJwtPayloadToken());
			IniReferenceResponseDTO response = iniClient.reference(iniReq, validationInfo.getValidationData().getWorkflowInstanceId());

			if(!isNullOrEmpty(response.getErrorMessage())) {
				log.error("Errore. Nessun riferimento trovato.");
				throw new IniException(response.getErrorMessage(),validationInfo.getValidationData().getWorkflowInstanceId());
			}


			log.debug("Executing replace of document: {}", idDoc);
			iniInvocationSRV.replace(validationInfo.getValidationData().getWorkflowInstanceId(), validationInfo.getFhirResource(), validationInfo.getJwtPayloadToken(), response.getUuid().get(0));

			final IndexerValueDTO kafkaValue = new IndexerValueDTO();
			kafkaValue.setWorkflowInstanceId(validationInfo.getValidationData().getWorkflowInstanceId());
			kafkaValue.setIdDoc(idDoc);
			kafkaValue.setEdsDPOperation(ProcessorOperationEnum.REPLACE);

			kafkaSRV.notifyChannel(idDoc, new Gson().toJson(kafkaValue), validationInfo.getJsonObj().getTipoDocumentoLivAlto(), DestinationTypeEnum.INDEXER);
			kafkaSRV.sendReplaceStatus(traceInfoDTO.getTraceID(), validationInfo.getValidationData().getWorkflowInstanceId(), SUCCESS, null, validationInfo.getJsonObj(), validationInfo.getJwtPayloadToken());

			logger.info(Constants.App.LOG_TYPE_CONTROL,validationInfo.getValidationData().getWorkflowInstanceId(),String.format("Replace CDA completed for workflow instance id %s", validationInfo.getValidationData().getWorkflowInstanceId()), OperationLogEnum.REPLACE_CDA2, ResultLogEnum.OK, startDateOperation,
					getDocumentType(validationInfo.getDocument()), validationInfo.getJwtPayloadToken(),null);
		} catch (ConnectionRefusedException ce) {
			errorHandlerSRV.connectionRefusedExceptionHandler(startDateOperation, validationInfo.getValidationData(), validationInfo.getJwtPayloadToken(), validationInfo.getJsonObj(), traceInfoDTO, ce, false, getDocumentType(validationInfo.getDocument()));
		} catch (final ValidationException e) {
			errorHandlerSRV.publicationValidationExceptionHandler(startDateOperation, validationInfo.getValidationData(), validationInfo.getJwtPayloadToken(), validationInfo.getJsonObj(), traceInfoDTO, e, false, getDocumentType(validationInfo.getDocument()));
		}

		String warning = "";

		if (validationInfo.getJsonObj().getMode() == null) {
			warning = Misc.WARN_EXTRACTION_SELECTION;
		}

		log.info("[EXIT] {}() with arguments {}={}, {}={}, {}={}","replace","traceId", traceInfoDTO.getTraceID(),"wif", validationInfo.getValidationData().getWorkflowInstanceId(),"idDoc", idDoc);
		return new ResponseEntity<>(new PublicationResDTO(traceInfoDTO, warning, validationInfo.getValidationData().getWorkflowInstanceId()), HttpStatus.OK);
	}
 

	private ValidationDataDTO executePublicationReplace(final ValidationCreationInputDTO validation,
			final JWTPayloadDTO jwtPayloadToken, PublicationCreateReplaceWiiDTO jsonObj, final byte[] bytePDF,
			final String bundle) {
		ValidationDataDTO validationInfo = null;
		
		validationInfo = getValidationInfoFhirBundle(bundle, jsonObj.getWorkflowInstanceId());
		
		validation.setValidationData(validationInfo);  

		cdaSRV.consumeHash(validationInfo.getHash());
		
		ValidationUtility.checkDayAfterValidation(validationInfo.getInsertionDate(), validationCFG.getDaysAllowToPublishAfterValidation());
		
		final String documentSha256 = encodeSHA256(bytePDF);
		validation.setDocumentSha(documentSha256);

		validateDocumentHash(documentSha256, validation.getJwtPayloadToken());

		ResourceDTO fhirMappingResult = buildMetadata(validationInfo.getTransformID(), validationInfo.getEngineID(), jwtPayloadToken, jsonObj, bytePDF, bundle,documentSha256);
		validation.setFhirResource(fhirMappingResult);
		return validationInfo;
	}

	private ResourceDTO buildMetadata(String transformId, String engineId,
			final JWTPayloadDTO jwtPayloadToken, PublicationCreateReplaceMetadataDTO jsonObj, final byte[] bytePDF,
			final String bundle, final String documentSha256) {
		String sha1 = StringUtility.encodeSHA1(bytePDF);
		
		ResourceDTO fhirResourcesDTO = null;
		try {
			fhirResourcesDTO = documentReferenceSRV.createFhirResourcesFromBundle(bundle, jwtPayloadToken.getSubject_role(), jsonObj, bytePDF.length, documentSha256, 
					jwtPayloadToken.getSubject_organization_id(), jwtPayloadToken.getLocality(), sha1);
			
			if(!StringUtility.isNullOrEmpty(fhirResourcesDTO.getErrorMessage())){
					final ErrorResponseDTO error = ErrorResponseDTO.builder()
							.type(FHIR_MAPPING_ERROR.getType())
							.title(FHIR_MAPPING_ERROR.getTitle())
							.instance(ErrorInstanceEnum.FHIR_RESOURCE_ERROR.getInstance())
							.detail(fhirResourcesDTO.getErrorMessage()).build();

					throw new ValidationException(error);
				
			}			
 		} catch(Exception ex) {
 			final ErrorResponseDTO error = ErrorResponseDTO.builder()
					.type(GENERIC_ERROR.getType())
					.title(GENERIC_ERROR.getTitle())
					.instance(ErrorInstanceEnum.INVALID_DATE_FORMAT.getInstance())
					.detail(ex.getMessage()).build();

			throw new ValidationException(error);

 		}

		return fhirResourcesDTO;
	}
	
	public static PersonDto parsePersonId(String personId) {
	    if (personId == null || personId.isEmpty()) {
	        return null;
	    }

	    String fiscalCode = null;
	    String oid = null;

	    if (personId.contains("^^^&")) {
	        String[] parts = personId.split("\\^\\^\\^&");
	        fiscalCode = parts[0];
	        if (parts.length > 1) {
	            String rest = parts[1];
	            int ampIndex = rest.indexOf('&');
	            if (ampIndex != -1) {
	                oid = rest.substring(0, ampIndex);
	            } else {
	                oid = rest; 
	            }
	        }
	    } else {
	        fiscalCode = personId; 
	    }

	    return new PersonDto(fiscalCode, oid);
	}


	@Override
	public ResponseWifDTO delete(String idDoc, HttpServletRequest request) {
		final Date startOperation = new Date();
		// Create request tracking
		LogTraceInfoDTO info = getLogTraceInfo();
		String workflowInstanceId = createWorkflowInstanceId(idDoc);

		log.info("[START] {}() with arguments {}={}, {}={}, {}={}","delete","traceId", info.getTraceID(),"wif", workflowInstanceId,"idDoc", idDoc);

		JWTPayloadDTO jwtPayloadToken = null;
		String warning = null;

		String subjApplicationId = null;
		String subjApplicationVendor = null;
		String subjApplicationVersion = null;

		if(!isValidMasterId(idDoc)) throw new ValidationException(createMasterIdError());

		try {
			// Extract token
			jwtPayloadToken = extractAndValidateJWT(request, EventTypeEnum.DELETE); 
			request.setAttribute("JWT_ISSUER", jwtPayloadToken.getIss());

			subjApplicationId = jwtPayloadToken.getSubject_application_id(); 
			subjApplicationVendor = jwtPayloadToken.getSubject_application_vendor();
			subjApplicationVersion = jwtPayloadToken.getSubject_application_version();

			// ==============================
			// [1] Retrieve reference from INI
			// ==============================
			IniReferenceResponseDTO iniReference = iniClient.reference(new IniReferenceRequestDTO(idDoc, jwtPayloadToken), workflowInstanceId);
			// Exit if necessary
			if(!isNullOrEmpty(iniReference.getErrorMessage())) {
				kafkaSRV.sendDeleteStatus(info.getTraceID(), workflowInstanceId, idDoc, iniReference.getErrorMessage(), BLOCKING_ERROR, jwtPayloadToken, RIFERIMENTI_INI);
				throw new IniException(iniReference.getErrorMessage(),workflowInstanceId);	
			} else {
				kafkaSRV.sendDeleteStatus(info.getTraceID(), workflowInstanceId, idDoc, "Riferimenti trovati: " +iniReference.getUuid(), SUCCESS, jwtPayloadToken, RIFERIMENTI_INI);
			}

			// ==============================
			// [2] Send delete request to EDS
			// ==============================
			EdsResponseDTO edsResponse = new EdsResponseDTO(true,"EDS_MOCK", "EDS_MOCK");
			if(!configSRV.isRemoveEds() && Boolean.FALSE.equals(iniReference.getMockEds())) {
				edsResponse = edsClient.delete(idDoc,jwtPayloadToken.getPerson_id());
				// Exit if necessary
				Objects.requireNonNull(edsResponse, "PublicationCTL returned an error - edsResponse is null!");

				if (!edsResponse.isEsito()) {
					// Update transaction status
					kafkaSRV.sendDeleteStatus(info.getTraceID(), workflowInstanceId, idDoc, edsResponse.getMessageError(), BLOCKING_ERROR, jwtPayloadToken, EDS_DELETE);
					throw new EdsException("Error encountered while sending delete information to EDS client");
				} else {
					// Update transaction status
					kafkaSRV.sendDeleteStatus(info.getTraceID(), workflowInstanceId, idDoc, "Delete effettuata su eds", SUCCESS, jwtPayloadToken, EDS_DELETE);
				}
			}
			

			// ==============================
			// [3] Send delete request to INI
			// ==============================
			DeleteRequestDTO deleteRequestDTO = buildRequestForIni(
					idDoc,
					iniReference.getUuid(),
					jwtPayloadToken,
					iniReference.getDocumentType(),
					subjApplicationId,
					subjApplicationVendor,
					subjApplicationVersion,
					workflowInstanceId,
					iniReference.getAuthorInstitution(),
					iniReference.getAdministrativeRequest() );
			IniTraceResponseDTO iniResponse = iniClient.delete(deleteRequestDTO);

			// Check mock errors
			boolean iniMockMessage = !isNullOrEmpty(iniResponse.getMessage()) && iniResponse.getMessage().contains("Invalid region ip");
			// Exit if necessary
			if (iniMockMessage) {
				throw new MockEnabledException(iniResponse.getMessage(), edsResponse.getMessageError());
			}

			// Check response errors
			if(Boolean.FALSE.equals(iniResponse.getEsito())) {
				// Send to indexer
				kafkaSRV.sendDeleteRequest(workflowInstanceId, deleteRequestDTO);
				// Update transaction status
				kafkaSRV.sendDeleteStatus(info.getTraceID(), workflowInstanceId, idDoc, "Transazione presa in carico", EventStatusEnum.ASYNC_RETRY, jwtPayloadToken,INI_DELETE);
				warning = Misc.WARN_ASYNC_TRANSACTION;
			} else {
				// Update transaction status
				kafkaSRV.sendDeleteStatus(info.getTraceID(), workflowInstanceId, idDoc, "Delete effettuata su ini", SUCCESS, jwtPayloadToken, INI_DELETE);
			}

			logger.info(Constants.App.LOG_TYPE_CONTROL,workflowInstanceId,String.format("Deletion of CDA completed for document with identifier %s", idDoc), OperationLogEnum.DELETE_CDA2, ResultLogEnum.OK, startOperation, MISSING_DOC_TYPE_PLACEHOLDER, jwtPayloadToken,null);
		} catch(MockEnabledException me) {
			throw me;
		} catch(IniException inEx) {
			logger.error(Constants.App.LOG_TYPE_CONTROL,workflowInstanceId,String.format("Error while delete record from ini %s", idDoc), OperationLogEnum.DELETE_CDA2, ResultLogEnum.KO, startOperation, INI_EXCEPTION.getErrorCategory(), MISSING_DOC_TYPE_PLACEHOLDER, jwtPayloadToken);
			throw inEx;

		} catch (Exception e) {
			RestExecutionResultEnum errorInstance = RestExecutionResultEnum.GENERIC_ERROR;
			if (e instanceof ValidationException) {
				errorInstance = get(((ValidationException) e).getError().getType());
			}

			logger.error(Constants.App.LOG_TYPE_CONTROL,workflowInstanceId,String.format("Error while deleting CDA of document with identifier %s", idDoc), OperationLogEnum.DELETE_CDA2, ResultLogEnum.KO, startOperation, errorInstance.getErrorCategory(), MISSING_DOC_TYPE_PLACEHOLDER, jwtPayloadToken);
			throw e;
		}

		log.info("[EXIT] {}() with arguments {}={}, {}={}, {}={}", "delete", "traceId", info.getTraceID(), "wif", workflowInstanceId, "idDoc", idDoc);
		return new ResponseWifDTO(workflowInstanceId, info, warning);
	}

	private DeleteRequestDTO buildRequestForIni(final String identificativoDocumento, final List<String> uuid, final JWTPayloadDTO jwtPayloadToken,
			final String documentType, String applicationId, String applicationVendor, String applicationVersion,
			final String workflowInstanceId, String authorInstitution, List<String> administrativeRequest) {
		DeleteRequestDTO out = null;
		try {
			out = DeleteRequestDTO.builder().
					action_id(jwtPayloadToken.getAction_id()).
					idDoc(identificativoDocumento).
					uuid(uuid).
					iss(jwtPayloadToken.getIss()).
					locality(jwtPayloadToken.getLocality()).
					patient_consent(jwtPayloadToken.getPatient_consent()).
					person_id(jwtPayloadToken.getPerson_id()).
					purpose_of_use(jwtPayloadToken.getPurpose_of_use()).
					resource_hl7_type(jwtPayloadToken.getResource_hl7_type()).
					sub(jwtPayloadToken.getSub()).
					subject_organization_id(jwtPayloadToken.getSubject_organization_id()).
					subject_organization(jwtPayloadToken.getSubject_organization()).
					subject_role(jwtPayloadToken.getSubject_role()).
					documentType(documentType).
					subject_application_id(applicationId).
					subject_application_vendor(applicationVendor).
					subject_application_version(applicationVersion).
					workflow_instance_id(workflowInstanceId).
					author_institution(authorInstitution).
					administrative_request(administrativeRequest).
					build();
		} catch(Exception ex) {
			log.error("Error while build request delete for ini : " , ex);
			throw new BusinessException("Error while build request delete for ini : " , ex);
		}
		return out;
	}
 
	 
	@Override
	public ResponseWifDTO updateMetadataIti_57(@Size(min = 1, max = 256) String idDoc, PublicationMetadataReqDTO requestBody, HttpServletRequest request) {
		return updateAbstract(idDoc, requestBody, true,request);
	}
	

	private ValidationCreationInputDTO publicationAndReplace(final MultipartFile file, final HttpServletRequest request, final boolean isReplace,final String idDoc, final LogTraceInfoDTO traceInfoDTO) {
		EventTypeEnum eventType = isReplace ? EventTypeEnum.REPLACE : EventTypeEnum.PUBLICATION; 
		ValidationCreationInputDTO validationResult = publicationAndReplaceValidationFhirDiretto(file, request, isReplace,idDoc, traceInfoDTO,eventType);

		String bundleJson = validationResult.getFhirResource()!=null ? validationResult.getFhirResource().getBundleJson() : "";
		validationResult.setValidationData(executePublicationReplace(validationResult,
				validationResult.getJwtPayloadToken(), validationResult.getJsonObj(), validationResult.getFile(), bundleJson));

		return validationResult;

	}
	
	private ValidationCreationInputDTO publicationAndReplaceValidationFhirDiretto(final MultipartFile file, final HttpServletRequest request, final boolean isReplace,final String idDocRep, final LogTraceInfoDTO traceInfoDTO,EventTypeEnum eventTypeEnum) {

		final ValidationCreationInputDTO validation = new ValidationCreationInputDTO();
		ValidationDataDTO validationInfo = new ValidationDataDTO();
		validationInfo.setCdaValidated(false);
		validationInfo.setWorkflowInstanceId(MISSING_WORKFLOW_PLACEHOLDER);
		validation.setValidationData(validationInfo);

		try {
			final JWTPayloadDTO jwtPayloadToken = extractAndValidateJWT(request, isReplace ? EventTypeEnum.FHIR_REPLACE : EventTypeEnum.FHIR_CREATE);
			validation.setJwtPayloadToken(jwtPayloadToken);
			request.setAttribute("JWT_ISSUER", jwtPayloadToken.getIss());
			PublicationCreateReplaceWiiDTO jsonObj = getAndValidatePublicationReq(request.getParameter("requestBody"), isReplace);
			validation.setJsonObj(jsonObj);

			String idDoc = jsonObj.getIdentificativoDoc();

			if(!isValidMasterId(idDoc)) throw new ValidationException(createMasterIdError());

			final byte[] bytePDF = getAndValidateFile(file);
			validation.setFile(bytePDF);

			final String fhirBundle = extractFHIR(bytePDF, jsonObj.getMode());
			if (validation.getFhirResource() == null) {
			    validation.setFhirResource(new ResourceDTO());
			}
			validation.getFhirResource().setBundleJson(fhirBundle);

			validateJWTFhirDiretto(validation.getJwtPayloadToken(), fhirBundle);

		} catch (final ValidationException | NoRecordFoundException ve) {
			cdaSRV.consumeHash(validationInfo.getHash());
			throw ve;
		}  

		return validation;
	}

}
