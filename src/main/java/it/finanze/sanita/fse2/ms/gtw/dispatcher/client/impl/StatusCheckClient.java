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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.client.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.IStatusCheckClient;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.MicroservicesURLCFG;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.ErrorResponseDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.TransactionInspectResDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.ErrorInstanceEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.RestExecutionResultEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.NoRecordFoundException;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class StatusCheckClient implements IStatusCheckClient {

	@Autowired
	private RestTemplate restTemplate;
	
	@Autowired
	private MicroservicesURLCFG urlCFG;
	

	@Override
	public TransactionInspectResDTO callSearchEventByWorkflowInstanceId(final String workflowInstanceId) {
		String url =  urlCFG.getStatusCheckClientHost() + "/v1/" + workflowInstanceId;

		TransactionInspectResDTO out = null;
		try {
			out = restTemplate.getForEntity(url, TransactionInspectResDTO.class).getBody();
		} catch (HttpStatusCodeException e1) {
			errorHandler(e1);
		} catch (Exception e) {
			log.error("Errore durante l'invocazione dell' API call search event by wii.", e);
			throw new BusinessException("Errore durante l'invocazione dell' API call search event by wii.", e);
		}

		return out;
	}

	@Override
	public TransactionInspectResDTO callSearchEventByTraceId(final String traceId) {
		String url = urlCFG.getStatusCheckClientHost() + "/v1/search/" + traceId;
		TransactionInspectResDTO out = null;
		try {	
			out = restTemplate.getForEntity(url, TransactionInspectResDTO.class).getBody();
		} catch (HttpStatusCodeException e1) {
			errorHandler(e1);
		} catch (Exception e) {
			log.error("Errore durante l'invocazione dell' API call search event by traceid.", e);
			throw new BusinessException("Errore durante l'invocazione dell' API call search event by traceid.", e);
		}
		return out; 
	}

	@Override
	public TransactionInspectResDTO callSearchEventByIdDocumento(final String idDocumento) {
		String url = urlCFG.getStatusCheckClientHost() + "/v1/search/event/" + idDocumento;
		TransactionInspectResDTO out = null;
		try {	
			out = restTemplate.getForEntity(url, TransactionInspectResDTO.class).getBody();
		} catch (HttpStatusCodeException e1) {
			errorHandler(e1);
		} catch (Exception e) {
			log.error("Errore durante l'invocazione dell' API call search event by traceid.", e);
			throw new BusinessException("Errore durante l'invocazione dell' API call search event by traceid.", e);
		}
		return out; 
	}

	
	private void errorHandler(final HttpStatusCodeException e1) {
		String msg = null;
		
		// 404 Not found.
		if (HttpStatus.NOT_FOUND.equals(e1.getStatusCode())) {
			ErrorResponseDTO error = ErrorResponseDTO.builder().
					type(RestExecutionResultEnum.RECORD_NOT_FOUND.getType()).
					title(RestExecutionResultEnum.RECORD_NOT_FOUND.getTitle()).
					instance(ErrorInstanceEnum.RECORD_NOT_FOUND.getInstance()).
					detail("No Record Found").build();
			throw new NoRecordFoundException(error);
		}
		
		// 500 Internal Server Error.
		if (HttpStatus.INTERNAL_SERVER_ERROR.equals(e1.getStatusCode())) {
			throw new BusinessException(msg, e1);
		}
	}
}
