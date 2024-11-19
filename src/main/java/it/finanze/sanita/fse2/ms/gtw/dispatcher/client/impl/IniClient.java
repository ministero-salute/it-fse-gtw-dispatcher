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

import static it.finanze.sanita.fse2.ms.gtw.dispatcher.client.routes.base.ClientRoutes.Ini.DELETE_PATH;
import static it.finanze.sanita.fse2.ms.gtw.dispatcher.client.routes.base.ClientRoutes.Ini.REFERENCE_PATH;
import static it.finanze.sanita.fse2.ms.gtw.dispatcher.client.routes.base.ClientRoutes.Ini.UPDATE_PATH;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.IIniClient;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.impl.base.AbstractClient;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.routes.IniClientRoutes;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.MicroservicesURLCFG;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.IniAuditsDto;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.DeleteRequestDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.IniMetadataUpdateReqDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.IniReferenceRequestDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.MergedMetadatiRequestDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.GetMergedMetadatiDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.IniReferenceResponseDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.IniTraceResponseDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class IniClient extends AbstractClient implements IIniClient {

	@Autowired
	private IniClientRoutes routes;
	
	@Autowired
	@Qualifier("restTemplateIni")
	private RestTemplate restTemplateIni;
	
	@Autowired
	private MicroservicesURLCFG msUrlCfg;

	@Override
	public IniTraceResponseDTO delete(final DeleteRequestDTO request) {

		String endpoint = routes.delete();
		IniTraceResponseDTO output = null;

		log.debug("{} - Executing request: {}", routes.identifier(), endpoint);

		try {
			// Execute request
			ResponseEntity<IniTraceResponseDTO> response = restTemplateIni.exchange(endpoint,DELETE,new HttpEntity<>(request),IniTraceResponseDTO.class);
			// Retrieve body
			output = response.getBody();
		} catch (RestClientResponseException ex) {
			toServerResponseEx(routes.identifier(), routes.microservice(), ex, DELETE_PATH);
		}

		return output;
	}

	@Override
	public IniReferenceResponseDTO reference(IniReferenceRequestDTO request, String workflowInstanceId) {

		String endpoint = routes.references(request.getIdDoc());
		IniReferenceResponseDTO output = null;

		log.debug("{} - Executing request: {}", routes.identifier(), endpoint);

		try {
			Map<String, Object> requestBody = new HashMap<>();
			requestBody.put("token", request.getToken());
			requestBody.put("workflowInstanceId", workflowInstanceId);
			// Execute request
			ResponseEntity<IniReferenceResponseDTO> response = restTemplateIni.exchange(endpoint,POST,new HttpEntity<>(requestBody),IniReferenceResponseDTO.class);
			// Retrieve body
			output = response.getBody();
		} catch (RestClientResponseException ex) {
			toServerResponseEx(routes.identifier(), routes.microservice(), ex, REFERENCE_PATH);
		}

		return output;
	}

	@Override
	public IniTraceResponseDTO update(IniMetadataUpdateReqDTO request,boolean callUpdateV2) {

		String endpoint = "";;
		IniTraceResponseDTO output = null;

		log.debug("{} - Executing request: {}", routes.identifier(), endpoint);

		try {
			ResponseEntity<IniTraceResponseDTO> response = null;
			if(callUpdateV2) {
				endpoint = routes.update("v2");
				response = restTemplateIni.exchange(endpoint,PUT,new HttpEntity<>(request),IniTraceResponseDTO.class);
			} else {
				endpoint = routes.update("v1");
				response = restTemplateIni.exchange(endpoint,PUT,new HttpEntity<>(request),IniTraceResponseDTO.class);	
			}
			
			// Retrieve body
			output = response.getBody();
		} catch (RestClientResponseException ex) {
			toServerResponseEx(routes.identifier(), routes.microservice(), ex, UPDATE_PATH);
		}

		return output;
	}

	
	@Override
	public GetMergedMetadatiDTO metadata(final MergedMetadatiRequestDTO request) {

		String endpoint = routes.metadata();
		GetMergedMetadatiDTO output = null;

		log.debug("{} - Executing request: {}", routes.identifier(), endpoint);

		try {
			ResponseEntity<GetMergedMetadatiDTO> response = restTemplateIni.exchange(endpoint,PUT,new HttpEntity<>(request),GetMergedMetadatiDTO.class);
			output = response.getBody();
		} catch (ResourceAccessException ex) {
			throw new BusinessException("Timeout error while call merge metadati"); 
		}

		return output;
	}
	
	@Override
	public IniAuditsDto callSearchEventByWorkflowInstanceId(final String workflowInstanceId) {
		String url =  msUrlCfg.getIniClientHost() + "/v1/" + workflowInstanceId;

		IniAuditsDto out = null;
		try {
			out = restTemplateIni.getForObject(url, IniAuditsDto.class);
		} catch (ResourceAccessException rax) {
			throw new BusinessException("Timeout error while call search event by worflow instance id");
		}
		return out;
	}
	
 
}
