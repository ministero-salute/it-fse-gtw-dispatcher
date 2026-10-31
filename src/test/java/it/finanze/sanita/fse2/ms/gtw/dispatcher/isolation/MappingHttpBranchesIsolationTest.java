package it.finanze.sanita.fse2.ms.gtw.dispatcher.isolation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.impl.FhirMappingClient;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.UpdateDocumentReferenceRequestDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.*;

@Tag("ISOLATION")
class MappingHttpBranchesIsolationTest {
    private FhirMappingClient client;
    private MockRestServiceServer server;
    private FlowsCFG flows;

    @BeforeEach void setup() {
        var rest = new RestTemplate();
        server = MockRestServiceServer.bindTo(rest).build();
        var urls = mock(MicroservicesURLCFG.class);
        when(urls.getFhirMappingEngineHost()).thenReturn("http://mapping.test");
        flows = mock(FlowsCFG.class);
        client = new FhirMappingClient();
        ReflectionTestUtils.setField(client, "restTemplate", rest);
        ReflectionTestUtils.setField(client, "msUrlCFG", urls);
        ReflectionTestUtils.setField(client, "flowsCFG", flows);
    }

    @ParameterizedTest @ValueSource(strings = {"none", "empty", "configured"})
    void cdaMappingUsesConfiguredValidationsAndRemovalReport(String mode) {
        boolean configured = mode.equals("configured");
        when(flows.getPublicationFlowValidations()).thenReturn(configured ? List.of("FHIR") : mode.equals("empty") ? List.of() : null);
        when(flows.isIncludeRemovalReport()).thenReturn(configured);
        String url = "http://mapping.test/v1/documents/transform" + (configured ? "?validations=FHIR&includeRemovalReport=true" : "");
        server.expect(requestTo(url)).andExpect(method(HttpMethod.POST))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        assertNotNull(client.callConvertCdaInBundle(mock(FhirResourceDTO.class)));
        server.verify();
    }

    @ParameterizedTest @ValueSource(strings = {"add", "transaction", "update"})
    void documentMappingUsesCorrectEndpoint(String operation) {
        String path = switch (operation) {
            case "add" -> "add-document-reference";
            case "transaction" -> "document-to-transaction";
            default -> "update";
        };
        server.expect(requestTo("http://mapping.test/v1/documents/transform/" + path))
            .andExpect(method(HttpMethod.POST)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        assertNotNull(switch (operation) {
            case "add" -> client.addDocumentReferenceToBundle(mock(FhirDocumentDTO.class));
            case "transaction" -> client.callConvertDocumentInTransaction(mock(FhirDocumentDTO.class));
            default -> client.updateDocumentReferenceClient(mock(UpdateDocumentReferenceRequestDTO.class));
        });
        server.verify();
    }

    @ParameterizedTest @ValueSource(strings = {"cda", "add", "transaction"})
    void connectionFailuresAreTranslated(String operation) {
        server.expect(anything()).andRespond(withException(new IOException("offline")));
        assertThrows(ConnectionRefusedException.class, () -> invoke(operation));
        server.verify();
    }

    @ParameterizedTest @ValueSource(strings = {"add", "transaction"})
    void serverFailuresAreNotReportedAsSuccessfulTransforms(String operation) {
        server.expect(anything()).andRespond(withServerError());
        assertThrows(BusinessException.class, () -> invoke(operation));
        server.verify();
    }

    private Object invoke(String operation) {
        return switch (operation) {
            case "cda" -> client.callConvertCdaInBundle(mock(FhirResourceDTO.class));
            case "add" -> client.addDocumentReferenceToBundle(mock(FhirDocumentDTO.class));
            default -> client.callConvertDocumentInTransaction(mock(FhirDocumentDTO.class));
        };
    }
}
