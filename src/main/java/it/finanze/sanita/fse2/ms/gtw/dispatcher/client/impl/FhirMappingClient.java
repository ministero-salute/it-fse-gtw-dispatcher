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
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.IFhirMappingClient;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.MicroservicesURLCFG;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.FhirResourceDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.client.TransformResDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ConnectionRefusedException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class FhirMappingClient implements IFhirMappingClient {

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private MicroservicesURLCFG msUrlCFG;

	@Override
	public TransformResDTO callConvertCdaInBundle(final FhirResourceDTO resourceToConvert) {
		TransformResDTO out = null;
		log.debug("Fhir Mapping Client - Calling Fhir Mapping to execute conversion");
		String url = msUrlCFG.getFhirMappingEngineHost() + "/v1/documents/transform";
		HttpHeaders headers = new HttpHeaders();
		headers.set("Content-Type", "application/json");
		HttpEntity<?> entity = new HttpEntity<>(resourceToConvert, headers);
		try {
			out = restTemplate.postForObject(url, entity, TransformResDTO.class);
		} catch(ResourceAccessException cex) {
			log.error("Connect error while call document transform :",cex);
			throw new ConnectionRefusedException(msUrlCFG.getFhirMappingEngineHost(),"Connection refused");
		} catch(Exception ex){
			log.error("Error while convert cda in bundle :",ex);
			throw new BusinessException("Error while convert cda in bundle :",ex);
		}
		return out;
	}
}
