package it.finanze.sanita.fse2.ms.gtw.dispatcher.isolation;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.impl.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.EdsMetadataUpdateReqDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.SystemTypeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.*;

/** Exercises real HTTP request construction/response parsing with an in-memory transport. */
@Tag("ISOLATION")
class HttpClientsIsolationTest {
    private RestTemplate rest;
    private MockRestServiceServer server;
    private MicroservicesURLCFG urls;

    @BeforeEach void setup() {
        rest = new RestTemplate();
        server = MockRestServiceServer.bindTo(rest).build();
        urls = mock(MicroservicesURLCFG.class);
        when(urls.getEdsClientHost()).thenReturn("http://eds.test");
        when(urls.getValidatorHost()).thenReturn("http://validator.test");
        when(urls.getFhirValidatorHost()).thenReturn("http://fhir-validator.test");
    }

    private <T> T inject(T client) {
        ReflectionTestUtils.setField(client, "restTemplate", rest);
        ReflectionTestUtils.setField(client, "msUrlCFG", urls);
        return client;
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void edsDeleteAndLookupCarryJwtWhenPresent(boolean jwtPresent) {
        var client = inject(new EdsClient());
        var expected = server.expect(requestTo("http://eds.test/v1/documents/document/patient"))
                .andExpect(method(HttpMethod.DELETE)).andExpect(content().contentType(MediaType.APPLICATION_JSON));
        if (jwtPresent) expected.andExpect(header("Agid-JWT-Signature", "token"));
        else expected.andExpect(headerDoesNotExist("Agid-JWT-Signature"));
        expected.andRespond(withSuccess("{\"esito\":true}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://eds.test/v1/document/patient/document"))
                .andExpect(method(HttpMethod.GET)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        assertTrue(client.delete("document", "patient", jwtPresent ? "token" : null).isEsito());
        assertNotNull(client.getDocumentReferenceClient("patient", "document", ""));
        server.verify();
    }

    @Test void edsUpdateAndIngestionStatusDeserializeResponses() {
        var client = inject(new EdsClient());
        var request = new EdsMetadataUpdateReqDTO("document", "workflow", "{}", "patient");
        request.setIdDoc("document");
        server.expect(requestTo("http://eds.test" + Constants.Client.Eds.UPDATE_PATH.replace(Constants.Client.Eds.ID_DOC_PLACEHOLDER, "document")))
                .andExpect(method(HttpMethod.PUT)).andExpect(header("Agid-JWT-Signature", "token"))
                .andExpect(jsonPath("$.idDoc").value("document"))
                .andRespond(withSuccess("{\"esito\":true}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://eds.test/v1/status/workflow"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        assertTrue(client.update(request, "token").isEsito());
        assertNotNull(client.getEdsStatus("workflow"));
        server.verify();
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void edsErrorsAreTranslatedRatherThanReportedAsSuccess(boolean transportFailure) {
        var client = inject(new EdsClient());
        var request = new EdsMetadataUpdateReqDTO("document", "workflow", "{}", "patient");
        request.setIdDoc("document");
        var expected = server.expect(anything());
        if (transportFailure) expected.andRespond(withException(new java.io.IOException("offline")));
        else expected.andRespond(withStatus(HttpStatus.BAD_GATEWAY));
        Class<? extends RuntimeException> expectedType = transportFailure ? BusinessException.class : ServerResponseException.class;
        server.expect(anything()).andRespond(transportFailure ? withException(new java.io.IOException("offline")) : withStatus(HttpStatus.BAD_GATEWAY));
        assertThrows(expectedType, () -> client.update(request, null));
        assertThrows(expectedType, () -> client.getEdsStatus("workflow"));
        server.verify();
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void validatorCarriesRequestedValidationsAndSystemHeader(boolean extraValidations) {
        var client = inject(new ValidatorClient());
        var flows = new FlowsCFG();
        ReflectionTestUtils.setField(flows, "validationFlowValidations", extraValidations ? java.util.List.of("FHIR") : java.util.List.of());
        ReflectionTestUtils.setField(flows, "includeRemovalReport", extraValidations);
        ReflectionTestUtils.setField(client, "flowsCFG", flows);
        server.expect(requestTo("http://validator.test/v1/validate" + (extraValidations ? "?validations=FHIR&includeRemovalReport=true" : "")))
                .andExpect(method(HttpMethod.POST)).andExpect(jsonPath("$.cda").value("<ClinicalDocument/>"))
                .andExpect(jsonPath("$.workflowInstanceId").value("workflow"))
                .andRespond(withSuccess("{\"result\":{\"result\":\"OK\",\"transformID\":\"map\"}}", MediaType.APPLICATION_JSON));
        var result = client.validate("<ClinicalDocument/>", "workflow", extraValidations ? SystemTypeEnum.TS : SystemTypeEnum.NONE);
        assertEquals("map", result.getTransformID());
        server.verify();
        assertThrows(ConnectionRefusedException.class, () -> client.validateFallback("cda", "workflow", SystemTypeEnum.NONE, new IllegalStateException("offline")));
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void fhirValidatorSendsBundleAndTranslatesConnectionFailure(boolean failure) {
        var client = inject(new FhirValidatorClient());
        var expected = server.expect(requestTo("http://fhir-validator.test/v1/validate"))
                .andExpect(method(HttpMethod.POST)).andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.fhir").value("bundle"))
                .andExpect(jsonPath("$.workflowInstanceId").value("workflow"));
        if (failure) expected.andRespond(withException(new java.io.IOException("offline")));
        else expected.andRespond(withSuccess("{\"issues\":[]}", MediaType.APPLICATION_JSON));
        if (failure) assertThrows(ConnectionRefusedException.class, () -> client.validate("bundle", "workflow"));
        else assertTrue(client.validate("bundle", "workflow").getIssues().isEmpty());
        server.verify();
    }

    @ParameterizedTest @ValueSource(strings = {"workflow", "trace", "document"})
    void statusLookupChoosesEndpointAndTranslatesTransportFailure(String search) {
        var client = new StatusCheckClient();
        when(urls.getStatusCheckClientHost()).thenReturn("http://status.test");
        ReflectionTestUtils.setField(client, "restTemplate", rest);
        ReflectionTestUtils.setField(client, "urlCFG", urls);
        String path = search.equals("workflow") ? "/v1/id" : search.equals("trace") ? "/v1/search/id" : "/v1/search/event/id";
        server.expect(requestTo("http://status.test" + path)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://status.test" + path)).andRespond(withException(new java.io.IOException("offline")));
        java.util.function.Supplier<Object> call = () -> switch (search) {
            case "workflow" -> client.callSearchEventByWorkflowInstanceId("id");
            case "trace" -> client.callSearchEventByTraceId("id");
            default -> client.callSearchEventByIdDocumento("id");
        };
        assertNotNull(call.get());
        assertThrows(BusinessException.class, call::get);
        server.verify();
    }
}
