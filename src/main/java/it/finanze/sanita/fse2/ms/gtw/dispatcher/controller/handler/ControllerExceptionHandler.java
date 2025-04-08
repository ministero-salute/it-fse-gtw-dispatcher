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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.controller.handler;

import brave.Tracer;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.ErrorResponseDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.LogTraceInfoDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.ValidationErrorResponseDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.ErrorInstanceEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.RestExecutionResultEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.StringUtility;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 *	Exceptions Handler.
 */
@ControllerAdvice
@Slf4j
public class ControllerExceptionHandler extends ResponseEntityExceptionHandler {

	/**
	 * Tracker log.
	 */
	@Autowired
	private Tracer tracer;

	/**
	 * Management validation exception.
	 * 
	 * @param ex		exception
	 * @param request	request
	 * @return			
	 */
	@ExceptionHandler(value = {ValidationErrorException.class})
	protected ResponseEntity<ValidationErrorResponseDTO> handleValidationException(final ValidationErrorException ex, final WebRequest request) {
		log.error("" , ex);  
		Integer status = 400;
		if (RestExecutionResultEnum.SEMANTIC_ERROR.equals(ex.getResult())) {
			status = 422;
		}

		if (RestExecutionResultEnum.MANDATORY_ELEMENT_ERROR_TOKEN.equals(ex.getResult()) || 
				RestExecutionResultEnum.INVALID_TOKEN_FIELD.equals(ex.getResult()) 
				|| RestExecutionResultEnum.MISSING_TOKEN.equals(ex.getResult()) ) {
			status = 403;
		}


		if(RestExecutionResultEnum.DOCUMENT_TYPE_ERROR.equals(ex.getResult())) {
			status = 415;
		}

		String workflowInstanceId = StringUtility.isNullOrEmpty(ex.getWorkflowInstanceId()) ? null : ex.getWorkflowInstanceId(); 

		ValidationErrorResponseDTO out = new ValidationErrorResponseDTO(getLogTraceInfo(), ex.getResult().getType(), ex.getResult().getTitle(), ex.getMessage(), status, ex.getErrorInstance(), workflowInstanceId);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);

		return new ResponseEntity<>(out, headers, status);
	}

	@ExceptionHandler(value = {ValidationException.class})
	protected ResponseEntity<ErrorResponseDTO> handleGenericValidationException(final ValidationException ex, final WebRequest request) {
		log.error("" , ex);  
		Integer status = 400;
		if (RestExecutionResultEnum.SEMANTIC_ERROR.equals(RestExecutionResultEnum.get(ex.getError().getType()))) {
			status = 422;
		}

		if (RestExecutionResultEnum.MANDATORY_ELEMENT_ERROR_TOKEN.equals(RestExecutionResultEnum.get(ex.getError().getType()))) {
			status = 401;
		}

		final HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);

		ErrorResponseDTO errorResponseDTO = ex.getError();
		LogTraceInfoDTO traceInfoDTO = getLogTraceInfo();
		errorResponseDTO.setSpanID(traceInfoDTO.getSpanID());
		errorResponseDTO.setTraceID(traceInfoDTO.getTraceID());
		return new ResponseEntity<>(ex.getError(), headers, status);
	}

	/**
	 * 
	 * @param ex
	 * @param request
	 * @return
	 */
	@ExceptionHandler(value = {MockEnabledException.class})
	protected ResponseEntity<ErrorResponseDTO> handleMockException(final MockEnabledException ex, final WebRequest request) {

		final HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);

		StringBuilder detailedMessage = new StringBuilder("{");
		if (!StringUtility.isNullOrEmpty(ex.getIniErrorMessage())) {
			detailedMessage.append("INI Error Message: ").append(ex.getIniErrorMessage());
		}

		if (!StringUtility.isNullOrEmpty(ex.getEdsErrorMessage())) {
			detailedMessage.append(", ").append("EDS Error Message: ").append(ex.getEdsErrorMessage());
		}
		detailedMessage.append("}");

		ErrorResponseDTO out = new ErrorResponseDTO(getLogTraceInfo(), "/msg/mocked-service", "Mock enabled, this exception won't stop continuing to process the request.", detailedMessage.toString(), HttpStatus.INTERNAL_SERVER_ERROR.value(), "/msg/mocked-service");

		return new ResponseEntity<>(out, headers, HttpStatus.INTERNAL_SERVER_ERROR.value());
	}


	/**
	 * Management connection refused exception.
	 * 
	 * @param ex		exception
	 * @param request	request
	 * @return			
	 */
	@ExceptionHandler(value = {ConnectionRefusedException.class})
	protected ResponseEntity<ErrorResponseDTO> handleConnectionRefusedException(final ConnectionRefusedException ex, final WebRequest request) {
		log.error("" , ex);  
		Integer status = 500;

		String detailedMessage = ex.getMessage(); 
		ErrorResponseDTO out = new ErrorResponseDTO(getLogTraceInfo(), "/msg/connection-refused", ex.getMessage(), detailedMessage, status, "/msg/connection-refused");

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);

		return new ResponseEntity<>(out, headers, status);
	}

	/**
	 * Management generic server response exception.
	 * 
	 * @param ex		exception
	 * @return
	 */
	@ExceptionHandler(value = {ServerResponseException.class})
	protected ResponseEntity<ErrorResponseDTO> handleServerResponseException(ServerResponseException ex) {
		log.error("handleServerResponseException()" , ex);
		int status = ex.getStatusCode();

		ErrorResponseDTO out = new ErrorResponseDTO(
			getLogTraceInfo(),
			RestExecutionResultEnum.INI_EXCEPTION.getType(),
			RestExecutionResultEnum.INI_EXCEPTION.getTitle(),
			ex.getDetail(),
			ex.getStatusCode(),
			String.format("%s/%s", RestExecutionResultEnum.SERVICE_ERROR.getType(), ex.getMicroservice())
		);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);

		return new ResponseEntity<>(out, headers, status);
	}


	/**
	 * Management record not found exception received by clients.
	 * 
	 * @param ex		exception
	 * @param request	request
	 * @return			
	 */
	@ExceptionHandler(value = {NoRecordFoundException.class})
	protected ResponseEntity<ErrorResponseDTO> handleRecordNotFoundException(NoRecordFoundException ex, final WebRequest request) {
		log.error("" , ex);  
		Integer status = 404;
		LogTraceInfoDTO traceInfoDto = getLogTraceInfo();
		String detail = ExceptionUtils.getMessage(ex);
		ErrorResponseDTO out = new ErrorResponseDTO(traceInfoDto, RestExecutionResultEnum.RECORD_NOT_FOUND.getType(), RestExecutionResultEnum.RECORD_NOT_FOUND.getTitle(), detail , status, ErrorInstanceEnum.RECORD_NOT_FOUND.getInstance());
		if(ex.getError()!=null) {
			out = ex.getError();
			detail = ex.getError().getDetail(); 
		} 
		out.setSpanID(traceInfoDto.getSpanID());
		out.setTraceID(traceInfoDto.getTraceID());

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);

		return new ResponseEntity<>(out, headers, status);
	}



	/**
	 * Management generic exception.
	 * 
	 * @param ex		exception
	 * @param request	request
	 * @return			
	 */
	@ExceptionHandler(value = {Exception.class})
	protected ResponseEntity<ErrorResponseDTO> handleGenericException(final Exception ex, final WebRequest request) {
		log.error("Errore generico", ex);		
		Integer status = 500;

		String msg = StringUtility.isNullOrEmpty(ex.getMessage()) ? "Errore generico" : ex.getMessage();
		ErrorResponseDTO out = new ErrorResponseDTO(getLogTraceInfo(), RestExecutionResultEnum.GENERIC_ERROR.getType(), RestExecutionResultEnum.GENERIC_ERROR.getTitle(), msg , status, ErrorInstanceEnum.NO_INFO.getInstance());

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);

		return new ResponseEntity<>(out, headers, status);
	}

	@ExceptionHandler(value = {BusinessException.class})
	protected ResponseEntity<ErrorResponseDTO> handleBusinessException(final BusinessException ex, final WebRequest request) {
		log.error("Errore generico", ex);		
		int status = 500;

		LogTraceInfoDTO traceInfo = getLogTraceInfo();
		ErrorResponseDTO out = new ErrorResponseDTO(traceInfo, RestExecutionResultEnum.GENERIC_ERROR.getType(), RestExecutionResultEnum.GENERIC_ERROR.getTitle(), 
				ex.getMessage(), status, ErrorInstanceEnum.NO_INFO.getInstance());
		if(ex.getError()!=null) {
			out = ex.getError();
		}
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);

		return new ResponseEntity<>(out, headers, status);
	}

	/**
	 * Management validation exception.
	 * 
	 * @param ex		exception
	 * @param request	request
	 * @return			
	 */
	@ExceptionHandler(value = {ValidationPublicationErrorException.class})
	protected ResponseEntity<ErrorResponseDTO> handleValidationException(final ValidationPublicationErrorException ex, final WebRequest request) {
		log.error("" , ex);  
		int status = 400;

		if (RestExecutionResultEnum.MANDATORY_ELEMENT_ERROR_TOKEN.equals(ex.getResult()) || 
				RestExecutionResultEnum.INVALID_TOKEN_FIELD.equals(ex.getResult())) {
			status = 403;
		}
		
		if (RestExecutionResultEnum.GENERIC_TIMEOUT.equals(ex.getResult())) {
			status = 504;
		}

		ErrorResponseDTO out = new ErrorResponseDTO(getLogTraceInfo(), ex.getResult().getType(), ex.getResult().getTitle(), ex.getMessage(), status, ex.getErrorInstance());

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);

		return new ResponseEntity<>(out, headers, status);
	}

	private LogTraceInfoDTO getLogTraceInfo() {
		return new LogTraceInfoDTO(
				tracer.currentSpan().context().spanIdString(),
				tracer.currentSpan().context().traceIdString());
	}

	/**
	 * Management record not found exception received by clients.
	 * 
	 * @param ex		exception
	 * @param request	request
	 * @return			
	 */
	@ExceptionHandler(value = {IniException.class})
	protected ResponseEntity<ErrorResponseDTO> handleIniException(final IniException ex, final WebRequest request) {
		log.error("" , ex);  
		Integer status = 404;

		ErrorResponseDTO out = new ErrorResponseDTO(getLogTraceInfo(), RestExecutionResultEnum.RECORD_NOT_FOUND.getType(), RestExecutionResultEnum.RECORD_NOT_FOUND.getTitle(), ExceptionUtils.getMessage(ex), status, ErrorInstanceEnum.NO_INFO.getInstance());

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);

		return new ResponseEntity<>(out, headers, status);
	}
	
	/**
	 * Management record not found exception received by clients.
	 * 
	 * @param ex		exception
	 * @param request	request
	 * @return			
	 */
	@ExceptionHandler(value = {EdsException.class})
	protected ResponseEntity<ErrorResponseDTO> handleEdsException(final EdsException ex, final WebRequest request) {
		log.error("" , ex);  
		Integer status = 404;

		ErrorResponseDTO out = new ErrorResponseDTO(getLogTraceInfo(), RestExecutionResultEnum.EDS_EXCEPTION.getType(), RestExecutionResultEnum.EDS_EXCEPTION.getTitle(), ErrorInstanceEnum.EDS_DOCUMENT_MISSING.getDescription(), status, ErrorInstanceEnum.EDS_DOCUMENT_MISSING.getInstance());

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);

		return new ResponseEntity<>(out, headers, status);
	}

	@Override
	protected ResponseEntity<Object> handleMissingServletRequestPart(MissingServletRequestPartException ex, HttpHeaders h, HttpStatusCode s, WebRequest r) {
		log.error("" , ex);
		Integer status = 400;

		ErrorResponseDTO out = new ErrorResponseDTO(getLogTraceInfo(), RestExecutionResultEnum.GENERIC_ERROR.getType(), RestExecutionResultEnum.GENERIC_ERROR.getTitle(), ex.getMessage(), status, ErrorInstanceEnum.NO_INFO.getInstance());

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);

		return new ResponseEntity<>(out, headers, status);
	}
}