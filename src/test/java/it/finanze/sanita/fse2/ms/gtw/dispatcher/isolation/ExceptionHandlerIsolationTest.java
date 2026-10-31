package it.finanze.sanita.fse2.ms.gtw.dispatcher.isolation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import io.opentelemetry.api.OpenTelemetry;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.controller.handler.ControllerExceptionHandler;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.ErrorResponseDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.RestExecutionResultEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.*;

@Tag("ISOLATION")
class ExceptionHandlerIsolationTest extends ControllerExceptionHandler {
    @BeforeEach void setup() {
        ReflectionTestUtils.setField(this, "tracer", OpenTelemetry.noop().getTracer("test"));
    }

    private void problem(ResponseEntity<? extends ErrorResponseDTO> response, int status) {
        assertEquals(status, response.getStatusCode().value());
        assertEquals(MediaType.APPLICATION_PROBLEM_JSON, response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getTraceID());
    }

    @ParameterizedTest
    @CsvSource({"SEMANTIC_ERROR,422", "MANDATORY_ELEMENT_ERROR_TOKEN,401", "INVALID_TOKEN_FIELD,403", "GENERIC_ERROR,400"})
    void translatesGenericAndMetadataValidationErrors(RestExecutionResultEnum result, int status) {
        var error = ErrorResponseDTO.builder().type(result.getType()).title(result.getTitle()).detail("diagnostic").build();
        var response = handleGenericValidationException(new ValidationException(error), null);
        problem(response, status);
        assertSame(error, response.getBody());
        assertEquals(status, error.getStatus());
        problem(handleMetadataValidationException(new MetadataValidationException(error), null), status);
    }

    @ParameterizedTest
    @CsvSource({"SEMANTIC_ERROR,422", "MANDATORY_ELEMENT_ERROR_TOKEN,403", "INVALID_TOKEN_FIELD,403", "MISSING_TOKEN,403", "DOCUMENT_TYPE_ERROR,415", "GENERIC_ERROR,400"})
    void translatesValidationErrorsWithWorkflow(RestExecutionResultEnum result, int status) {
        var response = handleValidationException(new ValidationErrorException(result, "diagnostic", "workflow", "/validation"), null);
        problem(response, status);
        assertEquals("diagnostic", response.getBody().getDetail());
    }

    @ParameterizedTest
    @CsvSource({"MANDATORY_ELEMENT_ERROR_TOKEN,403", "INVALID_TOKEN_FIELD,403", "GENERIC_TIMEOUT,504", "GENERIC_ERROR,400"})
    void translatesPublicationErrors(RestExecutionResultEnum result, int status) {
        var response = handleValidationException(new ValidationPublicationErrorException(result, "diagnostic", "/publication"), null);
        problem(response, status);
        assertEquals("diagnostic", response.getBody().getDetail());
    }

    @Test void translatesGenericBusinessAuthorizationAndNotImplementedFailures() {
        var generic = handleGenericException(new IllegalStateException(), null);
        problem(generic, 500);
        assertEquals("Errore generico", generic.getBody().getDetail());
        assertEquals("offline", handleGenericException(new IllegalStateException("offline"), null).getBody().getDetail());
        problem(handleBusinessException(new BusinessException("offline"), null), 500);
        problem(handleUnauthorizedException(new UnauthorizedException("mismatch"), null), 401);
        problem(handleNotImplementedException(new org.apache.commons.lang3.NotImplementedException("unsupported"), null), 503);
        var error = ErrorResponseDTO.builder().detail("provided error").build();
        assertSame(error, handleBusinessException(new BusinessException(error), null).getBody());
    }

    @Test void translatesIniAndEdsFailuresAndMissingMultipartPart() {
        problem(handleIniException(new IniException("not found", "workflow"), null), 404);
        problem(handleEdsException(new EdsException("not found"), null), 404);
        var response = handleMissingServletRequestPart(new org.springframework.web.multipart.support.MissingServletRequestPartException("file"),
                new HttpHeaders(), HttpStatus.BAD_REQUEST, null);
        assertEquals(400, response.getStatusCode().value());
        assertTrue(((ErrorResponseDTO) response.getBody()).getDetail().contains("file"));
    }
}
