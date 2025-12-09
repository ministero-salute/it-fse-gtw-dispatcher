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

import java.io.IOException;
import java.util.Date;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.controller.IValidationCTL;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.DirectFhirDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.JWTPayloadDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.ValidationCDAReqDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.ValidationFHIRReqDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.LogTraceInfoDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.ValidationResDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.ActivityEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.EventStatusEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.EventTypeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.OperationLogEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.ResultLogEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ValidationException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.logging.LoggerHelper;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.IErrorHandlerSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.IKafkaSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.CdaUtility;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.SignerUtility;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.StringUtility;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Validation controller.
 */
@Slf4j
@RestController
public class ValidationCTL extends AbstractCTL implements IValidationCTL {

	@Autowired
	private IKafkaSRV kafkaSRV;

	@Autowired
	private LoggerHelper logger;

	@Autowired
	private IErrorHandlerSRV errorHandlerSRV;

	@Value("${issuer.sonde:}")
	private String issuerSonde;

	@Override
	public ResponseEntity<ValidationResDTO> validate(final ValidationCDAReqDTO requestBody, final MultipartFile file, final HttpServletRequest request) {
		final Date startDateOperation = new Date();
		LogTraceInfoDTO traceInfoDTO = getLogTraceInfo();

		String workflowInstanceId = Constants.App.MISSING_WORKFLOW_PLACEHOLDER;
		JWTPayloadDTO jwtPayloadToken = null;
		ValidationCDAReqDTO jsonObj = null;
		String warning = null;
		Document docT = null;

		try {
			jwtPayloadToken = extractAndValidateJWT(request,EventTypeEnum.VALIDATION);
			jsonObj = getAndValidateValidationReq(request.getParameter("requestBody"));
			final byte[] bytes = getAndValidateFile(file);
			final String cda = extractCDA(bytes, jsonObj.getMode());
			warning = SignerUtility.isSigned(bytes) ? "[SIGN_WARN - Attenzione il documento risulta firmato in validazione]" : "";
			docT = Jsoup.parse(cda);
			workflowInstanceId = CdaUtility.getWorkflowInstanceId(docT);

			log.info("[START] {}() with arguments {}={}, {}={}","validate","traceId", traceInfoDTO.getTraceID(),"wif", workflowInstanceId);

			validateJWT(jwtPayloadToken, cda);

			String issuer = jwtPayloadToken.getIss();

			warning += validate(cda, jsonObj.getActivity(), workflowInstanceId, issuer);
			String message = null;
			if (jsonObj.getActivity().equals(ActivityEnum.VERIFICA)) {
				message = "Attenzione - è stato chiamato l'endpoint di validazione con VERIFICA";
			}

			if (!issuer.equals(issuerSonde)) {
				kafkaSRV.sendValidationStatus(traceInfoDTO.getTraceID(), workflowInstanceId, EventStatusEnum.SUCCESS, message, jwtPayloadToken);
				String typeIdExtension = docT.select("typeId").get(0).attr("extension");
				logger.info(Constants.App.LOG_TYPE_CONTROL, workflowInstanceId, "Validation CDA completed for workflow instance Id " + workflowInstanceId, OperationLogEnum.VAL_CDA2, ResultLogEnum.OK, startDateOperation, CdaUtility.getDocumentType(docT),
						jwtPayloadToken, typeIdExtension);
			}

			request.setAttribute("JWT_ISSUER", jwtPayloadToken.getIss());
		} catch (final ValidationException e) {
			errorHandlerSRV.validationExceptionHandler(startDateOperation, traceInfoDTO, workflowInstanceId, jwtPayloadToken, e, CdaUtility.getDocumentType(docT));
		}

		if (jsonObj != null && jsonObj.getMode() == null) {
			String schematronWarn = StringUtility.isNullOrEmpty(warning) ? "" : warning;
			warning = "[" + schematronWarn + "[WARNING_EXTRACT]" + Constants.Misc.WARN_EXTRACTION_SELECTION + "]";
		}
		warning = StringUtility.isNullOrEmpty(warning) ? null : warning;
		if(warning != null && warning.length() >= Constants.App.MAX_SIZE_WARNING) {
			warning = warning.substring(0, Constants.App.MAX_SIZE_WARNING-3) + "...";
		}
		if (jsonObj != null && ActivityEnum.VALIDATION.equals(jsonObj.getActivity())) {
			return new ResponseEntity<>(new ValidationResDTO(traceInfoDTO, workflowInstanceId, warning),
					HttpStatus.CREATED);
		}

		log.info("[EXIT] {}() with arguments {}={}, {}={}","validate","traceId", traceInfoDTO.getTraceID(),"wif", workflowInstanceId);

		return new ResponseEntity<>(new ValidationResDTO(traceInfoDTO, workflowInstanceId, warning), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<ValidationResDTO> fhirValidate(ValidationFHIRReqDTO requestBody, MultipartFile file,
			HttpServletRequest request) {
		final Date startDateOperation = new Date();
		LogTraceInfoDTO traceInfoDTO = getLogTraceInfo();

		String workflowInstanceId = Constants.App.MISSING_WORKFLOW_PLACEHOLDER;
		JWTPayloadDTO jwtPayloadToken = null;
		ValidationFHIRReqDTO jsonObj = null;
		String warning = null;

		try {

			jwtPayloadToken = extractAndValidateJWT(request, EventTypeEnum.FHIR_VALIDATION);
			jsonObj = getAndValidateValidationFhirReq(request.getParameter("requestBody"));

			DirectFhirDTO directFhirDTO = getAndValidateFhirFile(file, jsonObj.getMode());
			workflowInstanceId = directFhirDTO.getWii();

			log.info("[START] {}() with arguments {}={}, {}={}","validate","traceId", traceInfoDTO.getTraceID(),"wif", workflowInstanceId);

			String issuer = jwtPayloadToken.getIss();
			String result = fhirValidate(directFhirDTO.getFhir(), jsonObj.getActivity(), workflowInstanceId, issuer);
			if (!StringUtility.isNullOrEmpty(result)) {
				warning += result;
			}

			String message = null;
			if (jsonObj.getActivity().equals(ActivityEnum.VERIFICA)) {
				message = "Attenzione - è stato chiamato l'endpoint di validazione con VERIFICA";
			}

			if (!jsonObj.getActivity().equals(ActivityEnum.VERIFICA)) {
				kafkaSRV.sendValidationStatus(traceInfoDTO.getTraceID(), workflowInstanceId, EventStatusEnum.SUCCESS, message, jwtPayloadToken);
				logger.info(Constants.App.LOG_TYPE_CONTROL,workflowInstanceId, "Validation FHIR completed for workflow instance Id " + workflowInstanceId, OperationLogEnum.VAL_FHIR, ResultLogEnum.OK, startDateOperation, "TODO documentType", jwtPayloadToken,"TODO typeIdExtension");
			}

			request.setAttribute("JWT_ISSUER", jwtPayloadToken.getIss());
		} catch (final ValidationException e) {
			errorHandlerSRV.validationExceptionHandler(startDateOperation, traceInfoDTO, workflowInstanceId, jwtPayloadToken, e, null);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		warning = StringUtility.isNullOrEmpty(warning) ? null : warning;
		if(warning != null && warning.length() >= Constants.App.MAX_SIZE_WARNING) {
			warning = warning.substring(0, Constants.App.MAX_SIZE_WARNING-3) + "...";
		}

		log.info("[EXIT] {}() with arguments {}={}, {}={}","validate","traceId", traceInfoDTO.getTraceID(),"wif", workflowInstanceId);

		return new ResponseEntity<>(new ValidationResDTO(traceInfoDTO, workflowInstanceId, warning), HttpStatus.OK);
	}
}
