
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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.IValidatorClient;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.impl.base.AbstractClient;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.response.ValidatorErrorDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.MicroservicesURLCFG;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.ValidationInfoDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.client.ValidationRequestDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.ErrorResponseDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.client.ValidationResDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.ErrorInstanceEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.RestExecutionResultEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.SystemTypeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ConnectionRefusedException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ValidationException;
import lombok.extern.slf4j.Slf4j;

/**
 * Production implemention of Validator Client.
 */
@Slf4j
@Component
public class ValidatorClient extends AbstractClient implements IValidatorClient {

	@Autowired
    private RestTemplate restTemplate;
	
	@Autowired
	private MicroservicesURLCFG msUrlCFG;

    @Override
    @CircuitBreaker(name = "validationCDA")
    public ValidationInfoDTO validate(final String cda, final String workflowInstanceId, final SystemTypeEnum system) {
        log.debug("Validator Client - Calling Validator to execute validation of CDA");
        ValidationInfoDTO out = null;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        if(system != null && system != SystemTypeEnum.NONE) {
            headers.set(SYSTEM_TYPE_HEADER, system.value());
        }
        
        ValidationRequestDTO req = new ValidationRequestDTO();
        req.setCda(cda);
        req.setWorkflowInstanceId(workflowInstanceId);

        HttpEntity<?> entity = new HttpEntity<>(req, headers);
        
        ValidationResDTO response = null;
        try {
        	response = restTemplate.postForObject(msUrlCFG.getValidatorHost() + "/v1/validate", entity, ValidationResDTO.class);
        	if(response!=null) {
        		out = response.getResult();
        	}
        } catch(ResourceAccessException cex) {
        	log.error("Connect error while call validation ep :" + cex);
        	throw new ConnectionRefusedException(msUrlCFG.getValidatorHost(),"Connection refused");
		} catch (HttpClientErrorException ex) {
            throw new ValidationException(asValidationException(ex.getResponseBodyAsString()));
        }
        
        return out;
    }

    private static ErrorResponseDTO asValidationException(String message) {
        return ErrorResponseDTO.builder()
            .type(RestExecutionResultEnum.VALIDATOR_ERROR.getType())
            .title(RestExecutionResultEnum.VALIDATOR_ERROR.getTitle())
            .instance(ErrorInstanceEnum.CDA_NOT_VALIDATED.getInstance())
            .detail(fromErrorObject(message)).build();
    }

    private static String fromErrorObject(String message) {
        String out;
        try {
            out = new ObjectMapper().readValue(message, ValidatorErrorDTO.class).getError().getMessage();
        } catch (JsonProcessingException e) {
            out = "Impossibile deserializzare l'errore verificatosi sul gtw-validator";
        }
        return out;
    }

}
