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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.IFhirValidatorClient;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.impl.base.AbstractClient;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.MicroservicesURLCFG;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.ValidationFhirResponseDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.client.ValidationFhirRequestDto;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ConnectionRefusedException;
import lombok.extern.slf4j.Slf4j;

/**
 * Production implemention of Validator Client.
 */
@Slf4j
@Component
public class FhirValidatorClient extends AbstractClient implements IFhirValidatorClient {

	@Autowired
	@Qualifier("restTemplateWithErrorHandler")
	private RestTemplate restTemplate;
	
	@Autowired
	private MicroservicesURLCFG msUrlCFG;

    @Override
    @CircuitBreaker(name = "validationFHIR")
    public ValidationFhirResponseDTO validate(final String fhir, final String workflowInstanceId) {
        log.debug("Fhir Validator Client - Calling Fhir Validator to execute validation of JSON Fhir");
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");


        ValidationFhirRequestDto req = new ValidationFhirRequestDto();
        req.setFhir(fhir);
        req.setWorkflowInstanceId(workflowInstanceId);
        HttpEntity<?> entity = new HttpEntity<>(req, headers);
        
        ValidationFhirResponseDTO response = null;
        try {
        	response = restTemplate.postForObject(msUrlCFG.getFhirValidatorHost() + "/v1/validate", entity, ValidationFhirResponseDTO.class);
        } catch(ResourceAccessException cex) {
        	log.error("Connect error while call fhir validation ep :" + cex);
        	throw new ConnectionRefusedException(msUrlCFG.getFhirValidatorHost(),"Connection refused");
		} 
        
        return response;
    }

}
