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

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.IStatusManagerClient;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.controller.ITransactionInspectCTL;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.JWTPayloadDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.CallbackTransactionDataRequestDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.CallbackTransactionDataResponseDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.ErrorResponseDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.LogTraceInfoDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.TransactionInspectResDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.ErrorInstanceEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.ErrorLogEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.OperationLogEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.RestExecutionResultEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.UnauthorizedException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ValidationException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.logging.LoggerHelper;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.ITransactionInspectSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.ProfileUtility;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class TransactionInspectCTL extends AbstractCTL implements ITransactionInspectCTL {

	@Autowired
	private ITransactionInspectSRV transactionInspectSRV;

	@Autowired
	private IStatusManagerClient statusManagerClient;
	
	@Autowired
	private ProfileUtility profileUtility;

	@Autowired
	private LoggerHelper loggerHelper;

	@Override
	public TransactionInspectResDTO getEvents(String workflowInstanceId, HttpServletRequest request) {
		log.info("[START] {}() with arguments {}={}", "getEvents", "wif", workflowInstanceId);

		LogTraceInfoDTO traceInfoDto = getLogTraceInfo();

		if (Constants.App.MISSING_WORKFLOW_PLACEHOLDER.equalsIgnoreCase(workflowInstanceId)) {
			ErrorResponseDTO error = new ErrorResponseDTO(traceInfoDto);
			error.setType(RestExecutionResultEnum.INVALID_WII.getType());
			error.setDetail(ErrorInstanceEnum.INVALID_ID_WII.getDescription());
			error.setStatus(HttpStatus.BAD_REQUEST.value());
			error.setTitle(RestExecutionResultEnum.INVALID_WII.getTitle());
			error.setInstance(ErrorInstanceEnum.INVALID_ID_WII.getInstance());
			throw new ValidationException(error);
		}

		String subValue = extractSubjectValueFromRequest(request);
		if (subValue == null) {
			ErrorResponseDTO error = new ErrorResponseDTO(traceInfoDto);
			error.setType(RestExecutionResultEnum.MANDATORY_ELEMENT_ERROR_TOKEN.getType());
			error.setDetail(RestExecutionResultEnum.MANDATORY_ELEMENT_ERROR_TOKEN.getTitle());
			error.setStatus(HttpStatus.BAD_REQUEST.value());
			error.setTitle(RestExecutionResultEnum.MANDATORY_ELEMENT_ERROR_TOKEN.getTitle());
			error.setInstance(ErrorInstanceEnum.MISSING_JWT.getInstance());
			throw new ValidationException(error);
		}

		TransactionInspectResDTO res = transactionInspectSRV.callSearchEventByWorkflowInstanceId(workflowInstanceId);

		if (res.getTransactionData() == null || res.getTransactionData().isEmpty()) {
			ErrorResponseDTO error = new ErrorResponseDTO(traceInfoDto, RestExecutionResultEnum.RECORD_NOT_FOUND.getType(), RestExecutionResultEnum.RECORD_NOT_FOUND.getTitle(), RestExecutionResultEnum.RECORD_NOT_FOUND.getType() , 404
					, ErrorInstanceEnum.RECORD_NOT_FOUND.getInstance());
			throw new ValidationException(error);
		}

		boolean matchFound = res.getTransactionData().stream()
				.map(t -> extractValueBetweenHashes(t.getIssuer()))
				.anyMatch(issuerValue -> Objects.equals(subValue, issuerValue));

		if (!matchFound) {
			throw new UnauthorizedException("Mismatch sub/issuer");
		}

		log.info("[EXIT] {}() with arguments {}={}, {}={}", "getEvents", "reqTraceId", res.getTraceID(), "wif", workflowInstanceId);
		return res;
	}

	private String extractSubjectValueFromRequest(HttpServletRequest request) {
		log.info("Sono in extractSubjectValueFromRequest");

		if (request == null) {
			log.warn("HttpServletRequest is null");
			return null;
		}

		String authorization = request.getHeader("Authorization");

		if (authorization == null || !authorization.startsWith("Bearer ")) {
			log.info("Authorization header non presente o non Bearer");
			return null;
		}

		String jwt = authorization.substring("Bearer ".length());

		String sub = extractSubFromJwtWithoutValidation(jwt);
		if (sub == null) {
			log.warn("Claim sub non trovato nel JWT");
			return null;
		}

		log.debug("Claim sub = {}", sub);

		return extractValueBetweenHashes(sub);
	}

	private String extractValueBetweenHashes(String value) {
		if (value == null) {
			return null;
		}

		String[] parts = value.split("#");
		if (parts.length < 2) {
			return null;
		}

		String middle = parts[1];

		if (middle.length() > 3) {
			return middle.substring(0, 3);
		}

		return middle;
	}


	private String extractSubFromJwtWithoutValidation(String jwt) {
		if (jwt == null) {
			return null;
		}

		String[] parts = jwt.split("\\.");
		if (parts.length < 2) {
			return null;
		}

		try {
			String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);

			ObjectMapper mapper = new ObjectMapper();
			JsonNode payload = mapper.readTree(payloadJson);

			JsonNode subNode = null;
			if(profileUtility.isDevOrDockerProfile()) {
				subNode = payload.get("iss");
			} else {
				subNode = payload.get("sub"); 
			}
			 
			return subNode != null ? subNode.asText() : null;

		} catch (Exception e) {
			log.error("Errore parsing payload JWT", e);
			return null;
		}
	}

	@Override
	public TransactionInspectResDTO getEventsByTraceId(String traceId, HttpServletRequest request) {
		log.info("[START] {}() with arguments {}={}", "getEventsByTraceId", "traceId", traceId);
		TransactionInspectResDTO res = transactionInspectSRV.callSearchEventByTraceId(traceId);
		log.info("[EXIT] {}() with arguments {}={}", "getEventsByTraceId", "traceId", traceId);
		return res;
	}

	@Override
	public CallbackTransactionDataResponseDTO postTransactionDataEds(HttpServletRequest request, CallbackTransactionDataRequestDTO callbackTransactionDataRequestDTO) {
		log.info("[START] {}() with arguments {}={}", "postTransactionDataEds", "CallbackTransactionDataRequestDTO", callbackTransactionDataRequestDTO);

		CallbackTransactionDataResponseDTO response = statusManagerClient
				.saveTransactionStatus(callbackTransactionDataRequestDTO);

		try {
			emitStructuredLog(callbackTransactionDataRequestDTO);
		} catch (RuntimeException ex) {
			log.warn("Unable to emit EDS callback structured log for workflow instance id {}",
					callbackTransactionDataRequestDTO.getWorkflowInstanceId(), ex);
		}

		log.info("[EXIT] {}() with success={}", "postTransactionDataEds", response.getSuccess());
		return response;
	}

	private void emitStructuredLog(final CallbackTransactionDataRequestDTO callback) {
		final String type = normalize(callback.getType());
		final String eventType = normalize(callback.getEventType());
		final OperationLogEnum operation = resolveOperation(type);
		if (operation == null) {
			log.debug("Structured log skipped for unsupported EDS callback type {}", callback.getType());
			return;
		}

		final Date startDate = callback.getInsertionDate() != null ? callback.getInsertionDate() : new Date();
		final JWTPayloadDTO jwtPayload = JWTPayloadDTO.builder()
				.iss(callback.getIssuer())
				.sub(callback.getSubject())
				.subject_role(callback.getSubjectRole())
				.locality(callback.getLocality())
				.subject_application_id(callback.getSubjectApplicationId())
				.subject_application_vendor(callback.getSubjectApplicationVendor())
				.subject_application_version(callback.getSubjectApplicationVersion())
				.build();
		final String message = buildStructuredLogMessage(callback);

		final boolean success = "SUCCESS".equals(normalize(callback.getStatus()));
		loggerHelper.callback(Constants.App.LOG_TYPE_CONTROL, callback.getWorkflowInstanceId(), message,
				operation, callback.getStatus(), startDate, success ? null : resolveError(eventType),
				callback.getDocumentType(), jwtPayload, callback.getFiscalCode(), callback.getIdDocumento());
	}

	private OperationLogEnum resolveOperation(final String type) {
		if ("VALIDATION".equals(type) || "VALIDATION_FOR_PUBLICATION".equals(type)
				|| "VALIDATION_FOR_REPLACE".equals(type)) {
			return OperationLogEnum.VAL_CDA2;
		}
		if ("PUBLICATION".equals(type) || "FEEDING".equals(type)) {
			return OperationLogEnum.PUB_CDA2;
		}
		if ("REPLACE".equals(type) || "FHIR_REPLACE".equals(type)) {
			return OperationLogEnum.REPLACE_CDA2;
		}
		if ("DELETE".equals(type) || "EDS_DELETE".equals(type) || "INI_DELETE".equals(type)
				|| "RIFERIMENTI_INI".equals(type)) {
			return OperationLogEnum.DELETE_CDA2;
		}
		if ("UPDATE".equals(type) || "EDS_UPDATE".equals(type) || "INI_UPDATE".equals(type)
				|| "UPDATE_OSCURAMENTO".equals(type)) {
			return OperationLogEnum.UPDATE_METADATA_CDA2;
		}
		if ("FHIR_VALIDATION".equals(type)) {
			return OperationLogEnum.VAL_FHIR;
		}
		if ("FHIR_CREATE".equals(type)) {
			return OperationLogEnum.PUB_FHIR;
		}
		if ("SEND_TO_UAR".equals(type)) {
			return OperationLogEnum.SEND_TO_UAR;
		}
		if ("UAR_FINAL_STATUS".equals(type)) {
			return OperationLogEnum.UAR_FINAL_STATUS;
		}
		if ("BROKER_COMMUNICATION_ERROR".equals(type)) {
			return OperationLogEnum.EDS_CALLBACK;
		}
		return null;
	}

	private ErrorLogEnum resolveError(final String eventType) {
		if ("INI_ERROR".equals(eventType)) {
			return ErrorLogEnum.KO_INI_CALLBACK;
		}
		if ("ANA_ERROR".equals(eventType)) {
			return ErrorLogEnum.KO_ANA_CALLBACK;
		}
		return ErrorLogEnum.KO_EDS_CALLBACK;
	}

	private String buildStructuredLogMessage(final CallbackTransactionDataRequestDTO callback) {
		final StringBuilder message = new StringBuilder("EDS callback ")
				.append(callback.getEventType())
				.append(" received");
		if (callback.getMessage() != null && !callback.getMessage().isBlank()) {
			message.append(": ").append(callback.getMessage());
		}
		if (callback.getErrorCode() != null && !callback.getErrorCode().isBlank()) {
			message.append(" [errorCode=").append(callback.getErrorCode()).append(']');
		}
		if (callback.getErrorDescription() != null && !callback.getErrorDescription().isBlank()
				&& !callback.getErrorDescription().equals(callback.getMessage())) {
			message.append(" [errorDescription=").append(callback.getErrorDescription()).append(']');
		}
		return message.toString();
	}

	private String normalize(final String value) {
		return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
	}
}
