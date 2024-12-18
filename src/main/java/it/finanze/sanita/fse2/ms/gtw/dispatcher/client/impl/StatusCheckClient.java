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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.IStatusCheckClient;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.MicroservicesURLCFG;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.TransactionInspectResDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class StatusCheckClient implements IStatusCheckClient {

	@Autowired
	@Qualifier("restTemplateIni")
	private RestTemplate restTemplate;
	
	@Autowired
	private MicroservicesURLCFG urlCFG;
	

	@Override
	public TransactionInspectResDTO callSearchEventByWorkflowInstanceId(final String workflowInstanceId) {
		String url =  urlCFG.getStatusCheckClientHost() + "/v1/" + workflowInstanceId;

		TransactionInspectResDTO out = null;
		try {
			out = restTemplate.getForEntity(url, TransactionInspectResDTO.class).getBody();
		} catch (ResourceAccessException | HttpClientErrorException rax) {
			throw new BusinessException("Timeout error while call search event by worflow instance id");
		}
		return out;
	}

	@Override
	public TransactionInspectResDTO callSearchEventByTraceId(final String traceId) {
		String url = urlCFG.getStatusCheckClientHost() + "/v1/search/" + traceId;
		TransactionInspectResDTO out = null;
		try {	
			out = restTemplate.getForEntity(url, TransactionInspectResDTO.class).getBody();
		} catch (ResourceAccessException | HttpClientErrorException e1) {
			throw new BusinessException("Timeout error while call search event by trace id");
		} 

		return out; 
	}

	@Override
	public TransactionInspectResDTO callSearchEventByIdDocumento(final String idDocumento) {
		String url = urlCFG.getStatusCheckClientHost() + "/v1/search/event/" + idDocumento;
		TransactionInspectResDTO out = null;
		try {	
			out = restTemplate.getForEntity(url, TransactionInspectResDTO.class).getBody();
		} catch (ResourceAccessException | HttpClientErrorException e1) {
			throw new BusinessException("Timeout error while call search event by id documento");
		} 
		return out; 
	}
}
