package it.finanze.sanita.fse2.ms.gtw.dispatcher.isolation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import io.opentelemetry.api.OpenTelemetry;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.IStatusManagerClient;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.controller.impl.TransactionInspectCTL;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.StatusCheckDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.CallbackTransactionDataRequestDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.OperationLogEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.logging.LoggerHelper;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.ITransactionInspectSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.ProfileUtility;

@Tag("ISOLATION")
class TransactionControllerIsolationTest {
    private TransactionInspectCTL controller;
    private ITransactionInspectSRV service;
    private IStatusManagerClient status;
    private ProfileUtility profiles;
    private LoggerHelper logger;

    @BeforeEach void setup() {
        controller = new TransactionInspectCTL();
        service = mock(ITransactionInspectSRV.class);
        status = mock(IStatusManagerClient.class);
        profiles = mock(ProfileUtility.class);
        logger = mock(LoggerHelper.class);
        ReflectionTestUtils.setField(controller, "transactionInspectSRV", service);
        ReflectionTestUtils.setField(controller, "statusManagerClient", status);
        ReflectionTestUtils.setField(controller, "profileUtility", profiles);
        ReflectionTestUtils.setField(controller, "loggerHelper", logger);
        ReflectionTestUtils.setField(controller, "tracer", OpenTelemetry.noop().getTracer("test"));
    }

    private MockHttpServletRequest request(String payload) {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer e30." + Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes()) + ".signature");
        return request;
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void authorizesMatchingRegionUsingSubjectOrDevelopmentIssuer(boolean development) {
        when(profiles.isDevOrDockerProfile()).thenReturn(development);
        var item = new StatusCheckDTO();
        item.setIssuer("issuer#080-gateway#suffix");
        var result = new TransactionInspectResDTO(new LogTraceInfoDTO("span", "trace"), List.of(item));
        when(service.callSearchEventByWorkflowInstanceId("workflow")).thenReturn(result);
        assertSame(result, controller.getEvents("workflow", request("{\"" + (development ? "iss" : "sub") + "\":\"subject#080-app#suffix\"}")));
        when(service.callSearchEventByTraceId("trace")).thenReturn(result);
        assertSame(result, controller.getEventsByTraceId("trace", null));
        assertThrows(UnauthorizedException.class, () -> controller.getEvents("workflow", request("{\"sub\":\"x#090#y\",\"iss\":\"x#090#y\"}")));
    }

    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {"Basic abc", "Bearer malformed", "Bearer a.!.c", "Bearer a.e30.c"})
    void missingOrMalformedAuthorizationNeverQueriesStatus(String authorization) {
        var request = new MockHttpServletRequest();
        if (authorization != null) request.addHeader("Authorization", authorization);
        assertThrows(ValidationException.class, () -> controller.getEvents("workflow", request));
        verifyNoInteractions(service);
    }

    @Test void rejectsPlaceholderNullRequestsAndMissingTransactions() {
        assertThrows(ValidationException.class, () -> controller.getEvents(Constants.App.MISSING_WORKFLOW_PLACEHOLDER, null));
        assertThrows(ValidationException.class, () -> controller.getEvents("workflow", null));
        var response = new TransactionInspectResDTO();
        when(service.callSearchEventByWorkflowInstanceId("workflow")).thenReturn(response);
        assertThrows(ValidationException.class, () -> controller.getEvents("workflow", request("{\"sub\":\"x#080#y\"}")));
        response.setTransactionData(List.of());
        assertThrows(ValidationException.class, () -> controller.getEvents("workflow", request("{\"sub\":\"x#080#y\"}")));
    }

    @ParameterizedTest
    @CsvSource({"VALIDATION,VAL_CDA2", "VALIDATION_FOR_PUBLICATION,VAL_CDA2", "VALIDATION_FOR_REPLACE,VAL_CDA2", "PUBLICATION,PUB_CDA2", "FEEDING,PUB_CDA2", "REPLACE,REPLACE_CDA2", "FHIR_REPLACE,REPLACE_CDA2", "DELETE,DELETE_CDA2", "EDS_DELETE,DELETE_CDA2", "INI_DELETE,DELETE_CDA2", "RIFERIMENTI_INI,DELETE_CDA2", "UPDATE,UPDATE_METADATA_CDA2", "EDS_UPDATE,UPDATE_METADATA_CDA2", "INI_UPDATE,UPDATE_METADATA_CDA2", "UPDATE_OSCURAMENTO,UPDATE_METADATA_CDA2", "FHIR_VALIDATION,VAL_FHIR", "FHIR_CREATE,PUB_FHIR", "SEND_TO_UAR,SEND_TO_UAR", "UAR_FINAL_STATUS,UAR_FINAL_STATUS", "BROKER_COMMUNICATION_ERROR,EDS_CALLBACK"})
    void savesCallbackAndEmitsCorrectStructuredOperation(String type, OperationLogEnum operation) {
        var callback = CallbackTransactionDataRequestDTO.builder().build();
        callback.setType(" " + type.toLowerCase() + " ");
        callback.setEventType("INI_ERROR");
        callback.setStatus("BLOCKING_ERROR");
        callback.setWorkflowInstanceId("workflow");
        callback.setMessage("failure");
        callback.setErrorCode("E01");
        callback.setErrorDescription("details");
        var result = new CallbackTransactionDataResponseDTO(true);
        when(status.saveTransactionStatus(callback)).thenReturn(result);
        assertSame(result, controller.postTransactionDataEds(null, callback));
        verify(logger).callback(eq(Constants.App.LOG_TYPE_CONTROL), eq("workflow"),
                contains("failure [errorCode=E01] [errorDescription=details]"), eq(operation), eq("BLOCKING_ERROR"),
                any(), any(), any(), any(), any(), any());
    }

    @Test void unsupportedCallbackSkipsLoggingButStillPersists() {
        var callback = CallbackTransactionDataRequestDTO.builder().build();
        when(status.saveTransactionStatus(callback)).thenReturn(new CallbackTransactionDataResponseDTO(true));
        assertTrue(controller.postTransactionDataEds(null, callback).getSuccess());
        verifyNoInteractions(logger);
    }
}
