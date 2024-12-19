
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

import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.IStatusCheckClient;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.TransactionInspectResDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.IConfigSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.ITransactionInspectSRV;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@AutoConfigureMockMvc
@ActiveProfiles(Constants.Profile.TEST)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TransactionInspectTest {

	@Autowired
	private ServletWebServerApplicationContext webServerAppCtxt;
	
	@SpyBean
	private IStatusCheckClient statusCheckClient; 
	
	@Autowired
	public MockMvc mvc; 

	@MockBean
    private ITransactionInspectSRV transactionInspectSRV;

	@MockBean
	private IConfigSRV config;


	@BeforeAll
	void init() {
		when(config.isAuditEnable()).thenReturn(true);
	}

	@Test
	@DisplayName("Call Search Event By wid Test")
	void callSearchEventByWorkflowIdTest() throws Exception {
		TransactionInspectResDTO res = new TransactionInspectResDTO(); 
		res.setSpanID("span"); 
		res.setTraceID("trace"); 
		
		Mockito.doReturn(res).when(statusCheckClient)
			.callSearchEventByWorkflowInstanceId(anyString()); 
		
		mvc.perform(get("http://localhost:" + webServerAppCtxt.getWebServer().getPort() + "/status/")
				.contentType(MediaType.APPLICATION_JSON_VALUE)).andExpectAll(status().is2xxSuccessful())
				.andExpect(status().isOk()); 
		
	}

	@Test
    public void getEventsTest() throws Exception {
        String workflowInstanceId = "wif_test";

        TransactionInspectResDTO mockResponse = new TransactionInspectResDTO();
        when(transactionInspectSRV.callSearchEventByWorkflowInstanceId(eq(workflowInstanceId)))
                .thenReturn(mockResponse);

        mvc.perform(get("/v1/status/{workflowInstanceId}", workflowInstanceId))
                .andExpect(status().isOk());
    }

    @Test
    public void getEventsWithMissingWifTest() throws Exception {
        String missingWorkflowInstanceId = Constants.App.MISSING_WORKFLOW_PLACEHOLDER;

        mvc.perform(get("/v1/status/{workflowInstanceId}", missingWorkflowInstanceId))
                .andExpect(status().isBadRequest());
    }

	@Test
    public void getEventsByTraceIdTest() throws Exception {
        String traceId = "trace_id_test";

        TransactionInspectResDTO mockResponse = new TransactionInspectResDTO();
        when(transactionInspectSRV.callSearchEventByTraceId(eq(traceId)))
                .thenReturn(mockResponse);

        mvc.perform(get("/v1/status/search/{traceId}", traceId))
                .andExpect(status().isOk());
    }

}
