package it.finanze.sanita.fse2.ms.gtw.dispatcher.isolation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import java.util.Date;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.logging.LoggerHelper;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.impl.ErrorHandlerSRV;

@Tag("ISOLATION")
class ErrorTranslationIsolationTest {
    private ErrorHandlerSRV handler;
    private IKafkaSRV kafka;
    private ICdaSRV cda;
    private LoggerHelper logger;
    private final Date date = new Date();
    private final LogTraceInfoDTO trace = new LogTraceInfoDTO("span", "trace");
    private final JWTPayloadDTO jwt = JwtServiceIsolationTest.payload().build();

    @BeforeEach void setup() {
        handler = new ErrorHandlerSRV();
        kafka = mock(IKafkaSRV.class);
        cda = mock(ICdaSRV.class);
        logger = mock(LoggerHelper.class);
        ReflectionTestUtils.setField(handler, "kafkaSRV", kafka);
        ReflectionTestUtils.setField(handler, "cdaSRV", cda);
        ReflectionTestUtils.setField(handler, "logger", logger);
    }

    private ValidationDataDTO data() {
        var data = new ValidationDataDTO();
        data.setHash("hash");
        data.setWorkflowInstanceId("workflow");
        return data;
    }

    private ErrorResponseDTO error(RestExecutionResultEnum result) {
        return ErrorResponseDTO.builder().type(result.getType()).title(result.getTitle())
                .detail("diagnostic").instance("instance").build();
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void timeoutConsumesUnassignedHashAndTranslatesToPublicationError(boolean publication) {
        assertThrows(ValidationPublicationErrorException.class, () -> handler.connectionRefusedExceptionHandler(
                date, data(), jwt, null, trace, new ConnectionRefusedException("http://dependency.test", "offline"), publication, "REF"));
        verify(cda).consumeHash("hash");
        verify(kafka).sendPublicationStatus(eq("trace"), eq("workflow"), eq(RestExecutionResultEnum.GENERIC_TIMEOUT.getEventStatusEnum()),
                eq("offline"), isNull(), same(jwt), isNull());
        verify(logger).error(anyString(), eq("workflow"), contains("offline"),
                eq(publication ? OperationLogEnum.PUB_CDA2 : OperationLogEnum.REPLACE_CDA2), eq(ResultLogEnum.KO), same(date),
                eq(RestExecutionResultEnum.GENERIC_TIMEOUT.getErrorCategory()), eq("REF"), same(jwt), eq(""));
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void publicationAndReplacementKeepAssignedHashAndReportDiagnostic(boolean publication) {
        var metadata = FhirResourcesIsolationTest.request();
        var result = RestExecutionResultEnum.FHIR_MAPPING_ERROR;
        assertThrows(ValidationPublicationErrorException.class, () -> handler.publicationValidationExceptionHandler(
                date, data(), jwt, metadata, trace, new ValidationException(error(result)), publication, "REF"));
        verifyNoInteractions(cda);
        if (publication) verify(kafka).sendPublicationStatus("trace", "workflow", result.getEventStatusEnum(), "diagnostic", metadata, jwt, null);
        else verify(kafka).sendReplaceStatus("trace", "workflow", result.getEventStatusEnum(), "diagnostic", metadata, jwt, null);
        verify(logger).error(anyString(), eq("workflow"), contains("diagnostic"), any(), eq(ResultLogEnum.KO), same(date),
                eq(result.getErrorCategory()), eq("REF"), same(jwt), eq(metadata.getIdentificativoDoc()));
    }

    @ParameterizedTest @EnumSource(value = RestExecutionResultEnum.class, names = {"VOCABULARY_ERROR", "GENERIC_ERROR", "FHIR_MAPPING_ERROR"})
    void validationPreservesStatusAndSuppressesVocabularyErrorLogging(RestExecutionResultEnum result) {
        assertThrows(ValidationErrorException.class, () -> handler.validationExceptionHandler(
                date, trace, "workflow", jwt, new ValidationException(error(result)), "REF", "document"));
        verify(kafka).sendValidationStatus("trace", "workflow", result.getEventStatusEnum(), "diagnostic", jwt);
        if (result == RestExecutionResultEnum.VOCABULARY_ERROR) verifyNoInteractions(logger);
        else verify(logger).error(anyString(), eq("workflow"), contains("diagnostic"), eq(OperationLogEnum.VAL_CDA2),
                eq(ResultLogEnum.KO), same(date), eq(result.getErrorCategory()), eq("REF"), same(jwt), eq("document"));
    }

    @Test void unassignedPublicationFailureConsumesHash() {
        assertThrows(ValidationPublicationErrorException.class, () -> handler.publicationValidationExceptionHandler(
                date, data(), jwt, null, trace, new ValidationException(error(RestExecutionResultEnum.GENERIC_ERROR)), true, "REF"));
        verify(cda).consumeHash("hash");
    }

    @Test void metadataFailuresKeepTheirExceptionContract() {
        var detail = error(RestExecutionResultEnum.GENERIC_ERROR);
        assertThrows(ValidationErrorException.class, () -> handler.updateValidationExceptionHandler(
                date, trace, "workflow", jwt, new ValidationException(detail), "REF", "document"));
        var original = new MetadataValidationException(detail);
        assertSame(original, assertThrows(MetadataValidationException.class, () -> handler.updateValidationExceptionHandler(
                date, trace, "workflow", jwt, original, "REF", "document")));
        verify(kafka, times(2)).sendUpdateStatus("trace", "workflow", "document", EventStatusEnum.BLOCKING_ERROR, jwt, "diagnostic", EventTypeEnum.UPDATE);
    }
}
