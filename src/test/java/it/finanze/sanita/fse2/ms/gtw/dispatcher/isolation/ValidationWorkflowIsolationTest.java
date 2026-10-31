package it.finanze.sanita.fse2.ms.gtw.dispatcher.isolation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.mock.web.*;
import org.springframework.test.util.ReflectionTestUtils;
import io.opentelemetry.api.OpenTelemetry;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.controller.impl.ValidationCTL;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.logging.LoggerHelper;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.facade.ICdaFacadeSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.*;

@Tag("ISOLATION")
class ValidationWorkflowIsolationTest {
    private ValidationCTL controller;
    private IKafkaSRV kafka;
    private ICdaFacadeSRV facade;
    private IValidatorClient validator;
    private MockHttpServletRequest request;

    private <T> T inject(String name, Class<T> type) {
        T value = mock(type);
        for (Class<?> owner = controller.getClass(); owner != Object.class; owner = owner.getSuperclass()) {
            try { owner.getDeclaredField(name); ReflectionTestUtils.setField(controller, owner, name, value, type); }
            catch (NoSuchFieldException ignored) { }
        }
        return value;
    }

    @BeforeEach void setup() {
        controller = new ValidationCTL();
        kafka = inject("kafkaSRV", IKafkaSRV.class);
        facade = inject("cdaFacadeSRV", ICdaFacadeSRV.class);
        validator = inject("validatorClient", IValidatorClient.class);
        var jwt = inject("jwtSRV", IJwtSRV.class);
        when(jwt.getSystemByIssuer("issuer")).thenReturn(SystemTypeEnum.NONE);
        inject("benchmarkCfg", BenchmarkCFG.class);
        inject("logger", LoggerHelper.class);
        var errors = inject("errorHandlerSRV", IErrorHandlerSRV.class);
        doAnswer(i -> { throw i.getArgument(4, ValidationException.class); }).when(errors)
                .validationExceptionHandler(any(), any(), any(), any(), any(), any(), any());
        ReflectionTestUtils.setField(controller, "tracer", OpenTelemetry.noop().getTracer("isolation"));
        ReflectionTestUtils.setField(controller, "msCfg", new MicroservicesURLCFG());
        var cdaConfig = new CDACFG();
        ReflectionTestUtils.setField(cdaConfig, "cdaAttachmentName", "cda.xml");
        ReflectionTestUtils.setField(controller, "cdaCfg", cdaConfig);
        request = new MockHttpServletRequest();
        request.addHeader(Constants.Headers.JWT_HEADER, "Bearer e30." + java.util.Base64.getEncoder().encodeToString(
                StringUtility.toJSONJackson(JwtServiceIsolationTest.payload().build()).getBytes()) + ".signature");
        var info = new ValidationInfoDTO();
        info.setResult(RawValidationEnum.OK);
        info.setTransformID("map");
        info.setEngineID("engine");
        info.setMessage(List.of("WARNING: diagnostic"));
        when(validator.validate(anyString(), anyString(), any())).thenReturn(info);
    }

    @ParameterizedTest @EnumSource(ActivityEnum.class)
    void cdaValidationReturnsActivityStatusAndPersistsOnlyValidation(ActivityEnum activity) {
        request.addParameter("requestBody", "{\"activity\":\"" + activity + "\",\"mode\":\"ATTACHMENT\"}");
        byte[] pdf = FileUtility.getFileFromInternalResources("Files/attachment/LAB_OK.pdf");
        var result = controller.validate(new ValidationCDAReqDTO(), new MockMultipartFile("file", "cda.pdf", "application/pdf", pdf), request);
        assertEquals(activity == ActivityEnum.VALIDATION ? 201 : 200, result.getStatusCode().value());
        assertEquals("issuer", request.getAttribute("JWT_ISSUER"));
        assertTrue(result.getBody().getWorkflowInstanceId().contains("workflowInstanceId"));
        verify(facade, times(activity == ActivityEnum.VALIDATION ? 1 : 0)).create(anyString(), anyString(), eq("map"), eq("engine"));
        verify(kafka).sendValidationStatus(anyString(), anyString(), eq(EventStatusEnum.SUCCESS), nullable(String.class), any());
    }

    @ParameterizedTest @EnumSource(ActivityEnum.class)
    void fhirValidationExtractsPdfBundleAndPersistsOnlyValidation(ActivityEnum activity) throws Exception {
        var client = inject("fhirValidatorClient", IFhirValidatorClient.class);
        var response = new ValidationFhirResponseDTO();
        response.setIssues(List.of());
        when(client.validate(anyString(), anyString())).thenReturn(response);
        request.addParameter("requestBody", "{\"activity\":\"" + activity + "\",\"mode\":\"RESOURCE\"}");
        var pdf = DocumentUtilitiesIsolationTest.pdf(FhirResourcesIsolationTest.BUNDLE.replace("\n", ""));
        var result = controller.fhirValidate(new ValidationFHIRReqDTO(), new MockMultipartFile("file", "bundle.pdf", "application/pdf", pdf), request);
        assertEquals(200, result.getStatusCode().value());
        assertEquals("issuer", request.getAttribute("JWT_ISSUER"));
        verify(facade, times(activity == ActivityEnum.VALIDATION ? 1 : 0)).create(anyString(), anyString(), isNull(), isNull());
        verify(kafka, times(activity == ActivityEnum.VERIFICA ? 0 : 1)).sendValidationStatus(anyString(), anyString(), eq(EventStatusEnum.SUCCESS), isNull(), any());
    }

    @Test void malformedRequestIsRoutedToErrorHandlerBeforeCallingValidator() {
        request.addParameter("requestBody", "{}");
        assertThrows(ValidationException.class, () -> controller.validate(new ValidationCDAReqDTO(), null, request));
        verifyNoInteractions(validator, facade, kafka);
    }
}
