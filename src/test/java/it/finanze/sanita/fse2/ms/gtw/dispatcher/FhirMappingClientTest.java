package it.finanze.sanita.fse2.ms.gtw.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.impl.FhirMappingClient;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.FlowsCFG;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.MicroservicesURLCFG;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.FhirResourceDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.client.TransformResDTO;

@ExtendWith(MockitoExtension.class)
class FhirMappingClientTest {

    @Mock private MicroservicesURLCFG msUrlCFG;
    @Mock private FlowsCFG flowsCFG;
    @Spy private RestTemplate restTemplate = new RestTemplate();
    @InjectMocks private FhirMappingClient client;

    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        server = MockRestServiceServer.bindTo(restTemplate).build();
        when(msUrlCFG.getFhirMappingEngineHost()).thenReturn("http://mapping-engine");
    }

    @ParameterizedTest
    @CsvSource({"false,false", "false,true", "true,false", "true,true"})
    void transformUsesConfiguredOptionsAndReadsDiagnostics(boolean validations, boolean removalReport) {
        when(flowsCFG.getPublicationFlowValidations()).thenReturn(validations ? List.of("FHIR", "TERMINOLOGY") : List.of());
        when(flowsCFG.isIncludeRemovalReport()).thenReturn(removalReport);
        String url = "http://mapping-engine/v1/documents/transform";
        if (validations) {
            url += "?validations=FHIR&validations=TERMINOLOGY";
        }
        if (removalReport) {
            url += (validations ? "&" : "?") + "includeRemovalReport=true";
        }
        server.expect(requestTo(url))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"cda\":\"<ClinicalDocument/>\",\"engineId\":\"engine\",\"objectId\":\"map\"}"))
                .andRespond(withSuccess("""
                        {"json":{"resourceType":"Bundle"},"normalizationIssues":[
                          {"severity":"WARNING","code":"removed","message":"Resource removed"}],
                         "validationResponse":{"valid":true,"results":[
                          {"type":"FHIR","valid":true,"issues":[{"severity":"WARNING","message":"Warning"}]}]}}
                        """, MediaType.APPLICATION_JSON));
        FhirResourceDTO request = new FhirResourceDTO();
        request.setCda("<ClinicalDocument/>");
        request.setEngineId("engine");
        request.setObjectId("map");

        TransformResDTO response = client.callConvertCdaInBundle(request);

        assertNotNull(response);
        assertEquals("Bundle", response.getJson().getString("resourceType"));
        assertEquals("WARNING", response.getNormalizationIssues().getFirst().getSeverity());
        assertEquals("WARNING", response.getValidationResponse().getResults().getFirst().getIssues().getFirst().getSeverity());
        server.verify();
    }
}
