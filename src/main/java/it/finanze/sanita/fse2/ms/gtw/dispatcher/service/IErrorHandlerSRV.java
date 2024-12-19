
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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.service;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.JWTPayloadDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.ValidationDataDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.PublicationCreateReplaceMetadataDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.LogTraceInfoDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ConnectionRefusedException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ValidationException;

import java.util.Date;

public interface IErrorHandlerSRV {

    /**
     * Handle validation exception for validation ep
     * @param startDateOperation
     * @param traceInfoDTO
     * @param workflowInstanceId
     * @param jwtToken
     * @param e
     */
    void validationExceptionHandler(Date startDateOperation, LogTraceInfoDTO traceInfoDTO, String workflowInstanceId, JWTPayloadDTO jwtPayloadToken,  ValidationException e, String documentType);
    /**
     * Handle connection refused exception for publication ep
     * @param requestBody
     * @param startDateOperation
     * @param validationInfo
     * @param jwtToken
     * @param jsonObj
     * @param traceInfoDTO
     * @param ex
     * @param isPublication
     */
    void connectionRefusedExceptionHandler(Date startDateOperation, ValidationDataDTO validationInfo, JWTPayloadDTO jwtPayloadToken,
                                           PublicationCreateReplaceMetadataDTO jsonObj, LogTraceInfoDTO traceInfoDTO, ConnectionRefusedException ex,
                                           boolean isPublication, String documentType);
    /**
     * Handle validation exception for publication ep.
     * 
     * @param startDateOperation
     * @param validationInfo
     * @param jwtToken
     * @param jsonObj
     * @param traceInfoDTO
     * @param e
     * @param isPublication
     */
    void publicationValidationExceptionHandler(Date startDateOperation, ValidationDataDTO validationInfo, JWTPayloadDTO jwtPayloadToken,
                                               PublicationCreateReplaceMetadataDTO jsonObj, LogTraceInfoDTO traceInfoDTO, ValidationException e, boolean isPublication, final String documentType);
    
    void updateValidationExceptionHandler(Date startDateOperation, LogTraceInfoDTO traceInfoDTO, String workflowInstanceId, JWTPayloadDTO jwtPayloadToken, 
    		ValidationException e, String documentType,String idDoc);
    
}
