package it.finanze.sanita.fse2.ms.gtw.dispatcher.isolation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.impl.IniClient;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.routes.IniClientRoutes;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.MicroservicesURLCFG;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.*;

@Tag("ISOLATION")
class IniHttpContractIsolationTest {
    private IniClient client;
    private MockRestServiceServer server;

    @BeforeEach void setup() {
        client = new IniClient();
        var rest = new RestTemplate();
        server = MockRestServiceServer.bindTo(rest).build();
        var routes = mock(IniClientRoutes.class);
        when(routes.delete()).thenReturn("http://ini.test/delete");
        when(routes.references("document")).thenReturn("http://ini.test/reference");
        when(routes.documentMetadata("document")).thenReturn("http://ini.test/document");
        when(routes.metadata()).thenReturn("http://ini.test/metadata");
        when(routes.update("v1")).thenReturn("http://ini.test/update/v1");
        when(routes.update("v2")).thenReturn("http://ini.test/update/v2");
        when(routes.updateOscuramentoCatena("v1")).thenReturn("http://ini.test/hidden");
        var urls = mock(MicroservicesURLCFG.class);
        when(urls.getIniClientHost()).thenReturn("http://ini.test");
        ReflectionTestUtils.setField(client, "routes", routes);
        ReflectionTestUtils.setField(client, "restTemplateIni", rest);
        ReflectionTestUtils.setField(client, "msUrlCfg", urls);
    }

    @Test void lookupRequestsCarryWorkflowAndTokenAndDeserializeBody() {
        for (String endpoint : java.util.List.of("reference", "document")) {
            server.expect(requestTo("http://ini.test/" + endpoint)).andExpect(method(HttpMethod.POST))
                    .andExpect(jsonPath("$.workflowInstanceId").value("workflow"))
                    .andExpect(jsonPath("$.token.iss").value("issuer"))
                    .andRespond(withSuccess(endpoint.equals("reference") ? "{\"uuid\":[\"uuid\"]}" : "{\"uuid\":\"uuid\"}", MediaType.APPLICATION_JSON));
        }
        var request = new IniReferenceRequestDTO("document", JwtServiceIsolationTest.payload().build());
        assertEquals(java.util.List.of("uuid"), client.reference(request, "workflow").getUuid());
        assertEquals("uuid", client.documentMetadata(request, "workflow").getUuid());
        server.verify();
    }

    @Test void mutationsChooseCorrectMethodVersionAndParseOutcome() {
        for (String path : java.util.List.of("delete", "update/v1", "update/v2", "hidden", "metadata")) {
            server.expect(requestTo("http://ini.test/" + path)).andExpect(method(path.equals("delete") ? HttpMethod.DELETE : HttpMethod.PUT))
                    .andRespond(withSuccess(path.equals("metadata") ? "{\"marshallResponse\":\"<metadata/>\"}" : "{\"esito\":true}", MediaType.APPLICATION_JSON));
        }
        assertTrue(client.delete(new DeleteRequestDTO()).getEsito());
        assertTrue(client.update(update(), false).getEsito());
        assertTrue(client.update(update(), true).getEsito());
        assertTrue(client.updateOscuramentoCatena(hidden()).getEsito());
        assertEquals("<metadata/>", client.metadata(new MergedMetadatiRequestDTO()).getMarshallResponse());
        server.verify();
    }

    @ParameterizedTest @ValueSource(strings = {"delete", "reference", "document", "update", "hidden"})
    void httpFailuresAreTranslated(String operation) {
        server.expect(anything()).andRespond(withStatus(HttpStatus.BAD_GATEWAY));
        var request = new IniReferenceRequestDTO("document", JwtServiceIsolationTest.payload().build());
        assertThrows(ServerResponseException.class, () -> {
            switch (operation) {
                case "delete" -> client.delete(new DeleteRequestDTO());
                case "reference" -> client.reference(request, "workflow");
                case "document" -> client.documentMetadata(request, "workflow");
                case "hidden" -> client.updateOscuramentoCatena(hidden());
                default -> client.update(update(), false);
            }
        });
        server.verify();
    }

    private IniMetadataUpdateReqDTO update() {
        return new IniMetadataUpdateReqDTO("document", JwtServiceIsolationTest.payload().build(), "workflow", "<metadata/>", java.util.List.of("SSN"), "REF");
    }

    private IniMetadataUpdateOscuramentoReqDTO hidden() {
        return new IniMetadataUpdateOscuramentoReqDTO(JwtServiceIsolationTest.payload().build(), java.util.List.of(), "document", "workflow", "REF", "institution", "patient");
    }

    @Test void auditLookupUsesWorkflowPath() {
        server.expect(requestTo("http://ini.test/v1/workflow")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        assertNotNull(client.callSearchEventByWorkflowInstanceId("workflow"));
        server.verify();
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void timeoutsAreReportedAsBusinessFailures(boolean audit) {
        server.expect(anything()).andRespond(withException(new java.io.IOException("offline")));
        assertThrows(BusinessException.class, () -> {
            if (audit) client.callSearchEventByWorkflowInstanceId("workflow");
            else client.metadata(new MergedMetadatiRequestDTO());
        });
        server.verify();
    }
}
