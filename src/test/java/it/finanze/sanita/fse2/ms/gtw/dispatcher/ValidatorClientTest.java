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
package it.finanze.sanita.fse2.ms.gtw.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.IValidatorClient;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.ValidationInfoDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.LogTraceInfoDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.client.ValidationResDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.SystemTypeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ConnectionRefusedException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ValidationException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(Constants.Profile.TEST)
public class ValidatorClientTest {
    
    @SpyBean
    private RestTemplate restTemplate;

    @Autowired
    private IValidatorClient client;

    @Test
    void validateTest() {
        // Mock
        ValidationInfoDTO infoMock = new ValidationInfoDTO();
        infoMock.setEngineID("engine_test");
        infoMock.setTransformID("transform_id_test");
        ValidationResDTO responseMock = new ValidationResDTO(new LogTraceInfoDTO("spanId", "traceId"), infoMock);
        Mockito.doReturn(responseMock).when(restTemplate).postForObject(
            anyString(),
            any(HttpEntity.class),
            eq(ValidationResDTO.class)
        );
        // Perform validation
        ValidationInfoDTO outcome = client.validate("cda_test", "wif_test", SystemTypeEnum.TS);
        // Assertion
        assertEquals(infoMock.getEngineID(), outcome.getEngineID());
        assertEquals(infoMock.getTransformID(), outcome.getTransformID());
    }

    @Test
    void validateConnectionRefusedExceptionTest() {
        // Mock
        Mockito.doThrow(ResourceAccessException.class).when(restTemplate).postForObject(
            anyString(),
            any(HttpEntity.class),
            eq(ValidationResDTO.class)
        );
        // Assertion
        assertThrows(ConnectionRefusedException.class, () -> client.validate("cda_test", "wif_test", SystemTypeEnum.TS));
    }

    @Test
    void validateValidationExceptionTest() {
        // Mock
        Mockito.doThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR)).when(restTemplate).postForObject(
            anyString(),
            any(HttpEntity.class),
            eq(ValidationResDTO.class)
        );
        // Assertion
        assertThrows(ValidationException.class, () -> client.validate("cda_test", "wif_test", SystemTypeEnum.TS));
    }

}
