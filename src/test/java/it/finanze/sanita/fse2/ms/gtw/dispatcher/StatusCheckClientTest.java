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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.IStatusCheckClient;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.StatusCheckDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.ErrorResponseDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.TransactionInspectResDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.NoRecordFoundException;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import com.google.gson.Gson;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.MicroservicesURLCFG;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.MetadataValidationException;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(Constants.Profile.TEST)
public class StatusCheckClientTest {
    
    @MockitoSpyBean
    @Qualifier("restTemplateIni")
    private RestTemplate restTemplate;

    @Autowired
    private IStatusCheckClient client;

    @Autowired
    private MicroservicesURLCFG urlCFG;

    /**
     * Request factory originale del bean restTemplateIni: viene sostituita da
     * MockRestServiceServer e va ripristinata, perché il bean è un singleton
     * condiviso con gli altri test del contesto.
     */
    private ClientHttpRequestFactory originalRequestFactory;

    @BeforeEach
    void setUp() {
        originalRequestFactory = restTemplate.getRequestFactory();
    }

    @AfterEach
    void tearDown() {
        restTemplate.setRequestFactory(originalRequestFactory);
    }

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
       // Url to which the mocked server is binded
        String url =  urlCFG.getStatusCheckClientHost() + "/v1/wiid_test";

        // INTERNAL_SERVER_ERROR test
        ErrorResponseDTO errorMock = buildErrorMock(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
        MockRestServiceServer mockServer = mockErrorResponse(url, HttpStatus.INTERNAL_SERVER_ERROR, errorMock);
        BusinessException businessException = assertThrows(BusinessException.class, () -> client.callSearchEventByWorkflowInstanceId("wiid_test"));
        mockServer.verify();
        assertEquals(errorMock, businessException.getError());

        // NOT_FOUND test
        errorMock = buildErrorMock(HttpStatus.NOT_FOUND, "Not found");
        mockServer = mockErrorResponse(url, HttpStatus.NOT_FOUND, errorMock);
        NoRecordFoundException noRecordFoundException = assertThrows(NoRecordFoundException.class, () -> client.callSearchEventByWorkflowInstanceId("wiid_test"));
        mockServer.verify();
        assertEquals(errorMock, noRecordFoundException.getError());

        // BAD_REQUEST test
        errorMock = buildErrorMock(HttpStatus.BAD_REQUEST, "Bad request");
        mockServer = mockErrorResponse(url, HttpStatus.BAD_REQUEST, errorMock);
        MetadataValidationException metadataValidationException  = assertThrows(MetadataValidationException.class, () -> client.callSearchEventByWorkflowInstanceId("wiid_test"));
        mockServer.verify();
        assertEquals(errorMock, metadataValidationException.getError());

        // Generic error test
        errorMock = buildErrorMock(HttpStatus.SERVICE_UNAVAILABLE, "Service unavailable");
        mockServer = mockErrorResponse(url, HttpStatus.SERVICE_UNAVAILABLE, errorMock);
        businessException = assertThrows(BusinessException.class, () ->  client.callSearchEventByWorkflowInstanceId("wiid_test"));
        mockServer.verify();
        assertEquals(businessException.getMessage(), "Generic error");

        // ResouceAccessException test
        Mockito.doThrow(new ResourceAccessException("error")).when(restTemplate).getForEntity(
            anyString(),
            eq(TransactionInspectResDTO.class)
        );

        assertThrows(BusinessException.class, () ->  client.callSearchEventByWorkflowInstanceId("wiid_test"));

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

        // Url to which the mocked server is binded
        String url =  urlCFG.getStatusCheckClientHost() + "/v1/search/trace_id_test";

        // INTERNAL_SERVER_ERROR test
        ErrorResponseDTO errorMock = buildErrorMock(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
        MockRestServiceServer mockServer = mockErrorResponse(url, HttpStatus.INTERNAL_SERVER_ERROR, errorMock);
        BusinessException businessException = assertThrows(BusinessException.class, () -> client.callSearchEventByTraceId("trace_id_test"));
        mockServer.verify();
        assertEquals(errorMock, businessException.getError());

        // NOT_FOUND test
        errorMock = buildErrorMock(HttpStatus.NOT_FOUND, "Not found");
        mockServer = mockErrorResponse(url, HttpStatus.NOT_FOUND, errorMock);
        NoRecordFoundException noRecordFoundException = assertThrows(NoRecordFoundException.class, () -> client.callSearchEventByTraceId("trace_id_test"));
        mockServer.verify();
        assertEquals(errorMock, noRecordFoundException.getError());

        // BAD_REQUEST test
        errorMock = buildErrorMock(HttpStatus.BAD_REQUEST, "Bad request");
        mockServer = mockErrorResponse(url, HttpStatus.BAD_REQUEST, errorMock);
        MetadataValidationException metadataValidationException  = assertThrows(MetadataValidationException.class, () -> client.callSearchEventByTraceId("trace_id_test"));
        mockServer.verify();
        assertEquals(errorMock, metadataValidationException.getError());

        // Generic error test
        errorMock = buildErrorMock(HttpStatus.SERVICE_UNAVAILABLE, "Service unavailable");
        mockServer = mockErrorResponse(url, HttpStatus.SERVICE_UNAVAILABLE, errorMock);
        businessException = assertThrows(BusinessException.class, () ->  client.callSearchEventByTraceId("trace_id_test"));
        mockServer.verify();
        assertEquals(businessException.getMessage(), "Generic error");

        // ResouceAccessException test
        Mockito.doThrow(new ResourceAccessException("error")).when(restTemplate).getForEntity(
            anyString(),
            eq(TransactionInspectResDTO.class)
        );

        assertThrows(BusinessException.class, () ->  client.callSearchEventByTraceId("trace_id_test"));

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

        // Url to which the mocked server is binded
        String url =  urlCFG.getStatusCheckClientHost() + "/v1/search/event/id_test";

        // INTERNAL_SERVER_ERROR test
        ErrorResponseDTO errorMock = buildErrorMock(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
        MockRestServiceServer mockServer = mockErrorResponse(url, HttpStatus.INTERNAL_SERVER_ERROR, errorMock);
        BusinessException businessException = assertThrows(BusinessException.class, () -> client.callSearchEventByIdDocumento("id_test"));
        mockServer.verify();
        assertEquals(errorMock, businessException.getError());

        // NOT_FOUND test
        errorMock = buildErrorMock(HttpStatus.NOT_FOUND, "Not found");
        mockServer = mockErrorResponse(url, HttpStatus.NOT_FOUND, errorMock);
        NoRecordFoundException noRecordFoundException = assertThrows(NoRecordFoundException.class, () -> client.callSearchEventByIdDocumento("id_test"));
        mockServer.verify();
        assertEquals(errorMock, noRecordFoundException.getError());

        // BAD_REQUEST test
        errorMock = buildErrorMock(HttpStatus.BAD_REQUEST, "Bad request");
        mockServer = mockErrorResponse(url, HttpStatus.BAD_REQUEST, errorMock);
        MetadataValidationException metadataValidationException  = assertThrows(MetadataValidationException.class, () -> client.callSearchEventByIdDocumento("id_test"));
        mockServer.verify();
        assertEquals(errorMock, metadataValidationException.getError());

        // Generic error test
        errorMock = buildErrorMock(HttpStatus.SERVICE_UNAVAILABLE, "Service unavailable");
        mockServer = mockErrorResponse(url, HttpStatus.SERVICE_UNAVAILABLE, errorMock);
        businessException = assertThrows(BusinessException.class, () -> client.callSearchEventByIdDocumento("id_test"));
        mockServer.verify();
        assertEquals(businessException.getMessage(), "Generic error");

        // ResouceAccessException test
        Mockito.doThrow(new ResourceAccessException("error")).when(restTemplate).getForEntity(
            anyString(),
            eq(TransactionInspectResDTO.class)
        );

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

    private MockRestServiceServer mockErrorResponse(final String expectedUrl, final HttpStatus status, final ErrorResponseDTO error) {
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(restTemplate).build();
        mockServer.expect(once(), requestTo(expectedUrl))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new Gson().toJson(error)));
        return mockServer;
    }

    private ErrorResponseDTO buildErrorMock(final HttpStatus status, final String detail) {
        return ErrorResponseDTO.builder()
            .traceID("trace_id_test")
            .spanID("span_id_test")
            .type("/msg/" + status.name())
            .title(status.getReasonPhrase())
            .detail(detail)
            .status(status.value())
            .instance("/v1/status-check")
            .build();
    }

}
