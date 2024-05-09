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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.service.impl;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.JWTPayloadDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.ValidationDataDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.PublicationCreateReplaceMetadataDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.LogTraceInfoDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ConnectionRefusedException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ValidationErrorException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ValidationException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ValidationPublicationErrorException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.logging.LoggerHelper;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.ICdaSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.IErrorHandlerSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.IKafkaSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.StringUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class ErrorHandlerSRV implements IErrorHandlerSRV {

    @Autowired
    private IKafkaSRV kafkaSRV;

    @Autowired
    private ICdaSRV cdaSRV;

    @Autowired
    private LoggerHelper logger;

    @Override
    public void connectionRefusedExceptionHandler(
        Date startDateOperation, ValidationDataDTO validationInfo, JWTPayloadDTO jwtPayloadToken,
        PublicationCreateReplaceMetadataDTO jsonObj, LogTraceInfoDTO traceInfoDTO, ConnectionRefusedException ex,
        boolean isPublication, final String documentType
    ) {
        if (jsonObj == null) {
            cdaSRV.consumeHash(validationInfo.getHash());
        }

        String errorMessage = ex.getMessage();
        String capturedErrorType = RestExecutionResultEnum.GENERIC_TIMEOUT.getType();
        String errorInstance = ErrorInstanceEnum.NO_INFO.getInstance();
        
        EventStatusEnum errorEventStatus = RestExecutionResultEnum.GENERIC_TIMEOUT.getEventStatusEnum();

        kafkaSRV.sendPublicationStatus(traceInfoDTO.getTraceID(), validationInfo.getWorkflowInstanceId(), errorEventStatus,
                errorMessage, jsonObj, jwtPayloadToken);

        final RestExecutionResultEnum errorType = RestExecutionResultEnum.get(capturedErrorType);

        if(isPublication) {
        	logger.error(Constants.App.LOG_TYPE_CONTROL,validationInfo.getWorkflowInstanceId(),errorMessage + " " + validationInfo.getWorkflowInstanceId(), OperationLogEnum.PUB_CDA2, ResultLogEnum.KO, startDateOperation, errorType.getErrorCategory(),  documentType, 
        			jwtPayloadToken);
        } else {
        	logger.error(Constants.App.LOG_TYPE_CONTROL,validationInfo.getWorkflowInstanceId(),errorMessage + " " + validationInfo.getWorkflowInstanceId(), OperationLogEnum.REPLACE_CDA2, ResultLogEnum.KO, startDateOperation, errorType.getErrorCategory(),documentType,
        			jwtPayloadToken);
        }
        throw new ValidationPublicationErrorException(errorType, StringUtility.sanitizeMessage(errorType.getTitle()), errorInstance);
    }

    @Override
    public void publicationValidationExceptionHandler(
        Date startDateOperation, ValidationDataDTO validationInfo, JWTPayloadDTO jwtPayloadToken,
        PublicationCreateReplaceMetadataDTO jsonObj, LogTraceInfoDTO traceInfoDTO, ValidationException e, boolean isPublication, final String documentType
    ) {

        if (jsonObj == null) {
            cdaSRV.consumeHash(validationInfo.getHash());
        }

        String errorMessage = e.getMessage();
        String capturedErrorType = RestExecutionResultEnum.GENERIC_ERROR.getType();
        String errorInstance = ErrorInstanceEnum.NO_INFO.getInstance();
        EventStatusEnum errorEventStatus =  RestExecutionResultEnum.GENERIC_ERROR.getEventStatusEnum();
        
        if (e.getError() != null) {
            errorMessage = e.getError().getDetail();
            capturedErrorType = e.getError().getType();
            errorInstance = e.getError().getInstance();
            errorEventStatus = RestExecutionResultEnum.get(capturedErrorType).getEventStatusEnum();
        }

        if(isPublication) {
        	kafkaSRV.sendPublicationStatus(traceInfoDTO.getTraceID(), validationInfo.getWorkflowInstanceId(), errorEventStatus,
        			errorMessage, jsonObj, jwtPayloadToken);
        } else {
        	kafkaSRV.sendReplaceStatus(traceInfoDTO.getTraceID(), validationInfo.getWorkflowInstanceId(), errorEventStatus,
        			errorMessage, jsonObj, jwtPayloadToken);
        }
        

        final RestExecutionResultEnum errorType = RestExecutionResultEnum.get(capturedErrorType);

      
        if(isPublication) {
        	logger.error(Constants.App.LOG_TYPE_CONTROL,validationInfo.getWorkflowInstanceId(),errorMessage + " " + validationInfo.getWorkflowInstanceId(), OperationLogEnum.PUB_CDA2, ResultLogEnum.KO, startDateOperation, errorType.getErrorCategory(), documentType,
        			jwtPayloadToken);
        } else {
        	logger.error(Constants.App.LOG_TYPE_CONTROL,validationInfo.getWorkflowInstanceId(),errorMessage + " " + validationInfo.getWorkflowInstanceId(), OperationLogEnum.REPLACE_CDA2, ResultLogEnum.KO, startDateOperation, errorType.getErrorCategory(), documentType, 
        			jwtPayloadToken);
        }
        throw new ValidationPublicationErrorException(errorType,StringUtility.sanitizeMessage(errorMessage), errorInstance);
    }

    @Override
    public void validationExceptionHandler(Date startDateOperation, LogTraceInfoDTO traceInfoDTO, String workflowInstanceId, JWTPayloadDTO jwtPayloadToken, 
        ValidationException e, final String documentType) {
        
        String errorMessage = e.getMessage();
        String capturedErrorType = RestExecutionResultEnum.GENERIC_ERROR.getType();
        String errorInstance = ErrorInstanceEnum.NO_INFO.getInstance();
        EventStatusEnum errorEventStatus =  RestExecutionResultEnum.GENERIC_ERROR.getEventStatusEnum();
        if (e.getError() != null) {
            errorMessage = e.getError().getDetail();
            capturedErrorType = e.getError().getType();
            errorInstance = e.getError().getInstance();
            errorEventStatus = RestExecutionResultEnum.get(capturedErrorType).getEventStatusEnum();
        }

        final RestExecutionResultEnum validationResult = RestExecutionResultEnum.get(capturedErrorType);
        kafkaSRV.sendValidationStatus(traceInfoDTO.getTraceID(), workflowInstanceId, errorEventStatus, errorMessage, jwtPayloadToken);

        if (RestExecutionResultEnum.VOCABULARY_ERROR != RestExecutionResultEnum.get(capturedErrorType)) {
        	logger.error(Constants.App.LOG_TYPE_CONTROL,workflowInstanceId, errorMessage + " " + workflowInstanceId, OperationLogEnum.VAL_CDA2, ResultLogEnum.KO, startDateOperation, validationResult.getErrorCategory(),  documentType, 
        			jwtPayloadToken);
        }
        throw new ValidationErrorException(validationResult, StringUtility.sanitizeMessage(errorMessage), workflowInstanceId, errorInstance);
    }
    
    @Override
    public void updateValidationExceptionHandler(Date startDateOperation, LogTraceInfoDTO traceInfoDTO, String workflowInstanceId, JWTPayloadDTO jwtPayloadToken, 
    		ValidationException e, final String documentType,final String idDoc) {

    	String errorMessage = e.getMessage();
    	String capturedErrorType = RestExecutionResultEnum.GENERIC_ERROR.getType();
    	String errorInstance = ErrorInstanceEnum.NO_INFO.getInstance();
    	if (e.getError() != null) {
    		errorMessage = e.getError().getDetail();
    		capturedErrorType = e.getError().getType();
    		errorInstance = e.getError().getInstance();
    	}

    	final RestExecutionResultEnum validationResult = RestExecutionResultEnum.get(capturedErrorType);
    	kafkaSRV.sendUpdateStatus(traceInfoDTO.getTraceID(), workflowInstanceId, idDoc, EventStatusEnum.BLOCKING_ERROR, jwtPayloadToken, errorMessage, EventTypeEnum.UPDATE);
    	logger.error(Constants.App.LOG_TYPE_CONTROL,workflowInstanceId,e.getError().getDetail() + " " + workflowInstanceId, OperationLogEnum.UPDATE_METADATA_CDA2, ResultLogEnum.KO, startDateOperation, validationResult.getErrorCategory(),  documentType, 
    			jwtPayloadToken);
    	throw new ValidationErrorException(validationResult, StringUtility.sanitizeMessage(e.getError().getDetail()), workflowInstanceId, errorInstance);
    }

}
