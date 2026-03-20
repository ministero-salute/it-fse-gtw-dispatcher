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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.IStatusManagerClient;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.impl.base.AbstractClient;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.MicroservicesURLCFG;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.CallbackTransactionDataRequestDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.CallbackTransactionDataResponseDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;
import lombok.extern.slf4j.Slf4j;

/**
 * Client implementation for status-manager microservice.
 */
@Slf4j
@Component
public class StatusManagerClient extends AbstractClient implements IStatusManagerClient {

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private MicroservicesURLCFG msUrlCFG;

	@Override
	public CallbackTransactionDataResponseDTO saveTransactionStatus(CallbackTransactionDataRequestDTO request) {
		CallbackTransactionDataResponseDTO output = null;

		log.debug("Status Manager Client - Calling status-manager to save transaction status");
		String endpoint = msUrlCFG.getStatusManagerHost() + "/v1/status";
		
		try {
			HttpEntity<CallbackTransactionDataRequestDTO> entity = new HttpEntity<>(request, null);
			ResponseEntity<CallbackTransactionDataResponseDTO> restExchange = 
				restTemplate.exchange(endpoint, HttpMethod.POST, entity, CallbackTransactionDataResponseDTO.class);
			output = restExchange.getBody();
			log.debug("Status Manager Client - Transaction status saved successfully");
		} catch (HttpStatusCodeException e1) {
			errorHandler("status-manager", e1, "/v1/status");
		} catch (Exception e) {
			log.error("Error while calling status-manager API saveTransactionStatus(). ", e);
			throw new BusinessException("Error while calling status-manager API saveTransactionStatus(). ", e);
		}

		return output;
	}

}
