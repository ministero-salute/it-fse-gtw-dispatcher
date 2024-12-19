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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.IStatusCheckClient;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.StatusCheckDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.TransactionInspectResDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.NoRecordFoundException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(Constants.Profile.TEST)
public class StatusCheckClientTest {
    
    @SpyBean
    @Qualifier("restTemplateIni")
    private RestTemplate restTemplate;

    @Autowired
    private IStatusCheckClient client;

    @Test
    void callSearchEventByWidTest() {
        // Mock
        TransactionInspectResDTO responseMock = buildResponseMock("id_test", "wiid_test", "trace_id_test");
        Mockito.doReturn(new ResponseEntity<>(responseMock, HttpStatus.OK)).when(restTemplate).getForEntity(
            anyString(),
            eq(TransactionInspectResDTO.class)
        );
        // Perform callSearchEventByWorkflowInstanceId
        TransactionInspectResDTO response = client.callSearchEventByWorkflowInstanceId("wiid_test");
        // Assertions
        StatusCheckDTO statusMock = responseMock.getTransactionData().get(0);
        StatusCheckDTO statusResponse = response.getTransactionData().get(0);
        assertEquals(statusMock.getIdentificativoDocumento(), statusResponse.getIdentificativoDocumento());
        assertEquals(statusMock.getWorkflowInstanceId(), statusResponse.getWorkflowInstanceId());
        assertEquals(statusMock.getTraceId(), statusResponse.getTraceId());
    }

    @Test
    void callSearchEventByWidExceptionTest() {
        // Mock INTERNAL_SERVER_ERROR
        Mockito.doThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR)).when(restTemplate).getForEntity(
            anyString(),
            eq(TransactionInspectResDTO.class)
        );
        // Assertion
        assertThrows(BusinessException.class, () -> client.callSearchEventByWorkflowInstanceId("wiid_test"));
        // Mock NOT_FOUND
        Mockito.doThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND)).when(restTemplate).getForEntity(
            anyString(),
            eq(TransactionInspectResDTO.class)
        );
        // Assertion
        assertThrows(BusinessException.class, () -> client.callSearchEventByWorkflowInstanceId("wiid_test"));
    }

    @Test
    void callSearchEventByTraceIdTest() {
        // Mock
        TransactionInspectResDTO responseMock = buildResponseMock("id_test", "wiid_test", "trace_id_test");
        Mockito.doReturn(new ResponseEntity<>(responseMock, HttpStatus.OK)).when(restTemplate).getForEntity(
            anyString(),
            eq(TransactionInspectResDTO.class)
        );
        // Perform callSearchEventByTraceId
        TransactionInspectResDTO response = client.callSearchEventByTraceId("trace_id_test");
        // Assertions
        StatusCheckDTO statusMock = responseMock.getTransactionData().get(0);
        StatusCheckDTO statusResponse = response.getTransactionData().get(0);
        assertEquals(statusMock.getIdentificativoDocumento(), statusResponse.getIdentificativoDocumento());
        assertEquals(statusMock.getWorkflowInstanceId(), statusResponse.getWorkflowInstanceId());
        assertEquals(statusMock.getTraceId(), statusResponse.getTraceId());
    }

    @Test
    void callSearchEventByTraceIdExceptionTest() {
         // Mock INTERNAL_SERVER_ERROR
        Mockito.doThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR)).when(restTemplate).getForEntity(
            anyString(),
            eq(TransactionInspectResDTO.class)
        );
        // Assertion
        assertThrows(BusinessException.class, () -> client.callSearchEventByTraceId("trace_id_test"));
        // Mock NOT_FOUND
        Mockito.doThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND)).when(restTemplate).getForEntity(
            anyString(),
            eq(TransactionInspectResDTO.class)
        );
        // Assertion
        assertThrows(BusinessException.class, () -> client.callSearchEventByTraceId("trace_id_test"));
    }

    @Test
    void callSearchEventByIdDocTest() {
        // Mock
        TransactionInspectResDTO responseMock = buildResponseMock("id_test", "wiid_test", "trace_id_test");
        Mockito.doReturn(new ResponseEntity<>(responseMock, HttpStatus.OK)).when(restTemplate).getForEntity(
            anyString(),
            eq(TransactionInspectResDTO.class)
        );
        // Perform callSearchEventByIdDocumento
        TransactionInspectResDTO response = client.callSearchEventByIdDocumento("id_test");
        // Assertions
        StatusCheckDTO statusMock = responseMock.getTransactionData().get(0);
        StatusCheckDTO statusResponse = response.getTransactionData().get(0);
        assertEquals(statusMock.getIdentificativoDocumento(), statusResponse.getIdentificativoDocumento());
        assertEquals(statusMock.getWorkflowInstanceId(), statusResponse.getWorkflowInstanceId());
        assertEquals(statusMock.getTraceId(), statusResponse.getTraceId());
    }

    @Test
    void callSearchEventByIdDocExceptionTest() {
        // Mock INTERNAL_SERVER_ERROR
        Mockito.doThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR)).when(restTemplate).getForEntity(
            anyString(),
            eq(TransactionInspectResDTO.class)
        );
        // Assertion
        assertThrows(BusinessException.class, () -> client.callSearchEventByIdDocumento("id_test"));
        // Mock NOT_FOUND
        Mockito.doThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND)).when(restTemplate).getForEntity(
            anyString(),
            eq(TransactionInspectResDTO.class)
        );
        // Assertion
        assertThrows(BusinessException.class, () -> client.callSearchEventByIdDocumento("id_test"));
    }

    private TransactionInspectResDTO buildResponseMock(String idDoc, String wiid, String traceId) {
        StatusCheckDTO statusCheck = new StatusCheckDTO();
        statusCheck.setIdentificativoDocumento(idDoc);
        statusCheck.setWorkflowInstanceId(wiid);
        statusCheck.setTraceId(traceId);
        List<StatusCheckDTO> listStatus = new ArrayList<>();
        listStatus.add(statusCheck);
        TransactionInspectResDTO responseMock = new TransactionInspectResDTO();
        responseMock.setTransactionData(listStatus);
        return responseMock;
    }

}
