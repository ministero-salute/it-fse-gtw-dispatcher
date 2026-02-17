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
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.IStatusManagerClient;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.controller.ITransactionInspectCTL;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.CallbackTransactionDataRequestDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.CallbackTransactionDataResponseDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.ErrorResponseDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.GetIngestionStatusResponseDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.LogTraceInfoDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.TransactionInspectResDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.ErrorInstanceEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.RestExecutionResultEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.UnauthorizedException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ValidationException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.ITransactionInspectSRV;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class TransactionInspectCTL extends AbstractCTL implements ITransactionInspectCTL {

	@Autowired
	private ITransactionInspectSRV transactionInspectSRV;

	@Autowired
		private IStatusManagerClient statusManagerClient;
 
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

	        JsonNode subNode = payload.get("sub");
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
	public GetIngestionStatusResponseDTO getEdsStatus(String workflowInstanceId, HttpServletRequest request) {
		log.info("[START] {}() with arguments {}={}", "getEdsStatus", "workflowInstanceId", workflowInstanceId);

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

		GetIngestionStatusResponseDTO res = transactionInspectSRV.getEdsStatusByWorkflowInstanceId(workflowInstanceId);

		log.info("[EXIT] {}() with arguments {}={}", "getEdsStatus", "workflowInstanceId", workflowInstanceId);
		return res;
	}

	   @Override
	   public CallbackTransactionDataResponseDTO postTransactionDataEds(HttpServletRequest request, CallbackTransactionDataRequestDTO callbackTransactionDataRequestDTO) {
	       log.info("[START] {}() with arguments {}={}", "postTransactionDataEds", "CallbackTransactionDataRequestDTO", callbackTransactionDataRequestDTO);

		CallbackTransactionDataResponseDTO response = statusManagerClient
				.saveTransactionStatus(callbackTransactionDataRequestDTO);

		log.info("[EXIT] {}() with success={}", "postTransactionDataEds", response.getSuccess());
		return response;
	   }
}
