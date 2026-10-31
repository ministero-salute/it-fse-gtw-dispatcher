package it.finanze.sanita.fse2.ms.gtw.dispatcher.isolation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import io.opentelemetry.api.OpenTelemetry;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.controller.impl.PublicationCTL;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.logging.LoggerHelper;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.facade.ICdaFacadeSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.impl.IniEdsInvocationSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.dto.ValidationResultDTO;

@Tag("ISOLATION")
class PublicationWorkflowIsolationTest {
    private PublicationCTL controller;
    private IIniClient ini;
    private IEdsClient eds;
    private IConfigSRV config;
    private IKafkaSRV kafka;
    private ICdaFacadeSRV cda;
    private IFhirSRV fhir;
    private IniEdsInvocationSRV invocation;
    private IIssuerSRV issuer;
    private FHIRCFG fhirConfig;
    private IErrorHandlerSRV errors;
    private IValidatorClient validator;
    private IAffinityDomainValidationSRV affinity;
    private AffinityDomainUtility affinityUtility;
    private ValidationCFG validationConfig;
    private BenchmarkCFG benchmark;
    private byte[] pdf;
    private MockHttpServletRequest request;
    private PublicationCreationReqDTO metadata;

    private <T> T dependency(String name, Class<T> type) {
        T mock = mock(type);
        // Inject duplicate fields in the concrete and base controller: both are used in real flows.
        for (Class<?> owner = PublicationCTL.class; owner != Object.class; owner = owner.getSuperclass()) {
            try {
                owner.getDeclaredField(name);
                ReflectionTestUtils.setField(controller, owner, name, mock, type);
            } catch (NoSuchFieldException ignored) { }
        }
        return mock;
    }

    @BeforeEach
    void setup() {
        controller = new PublicationCTL();
        ini = dependency("iniClient", IIniClient.class);
        eds = dependency("edsClient", IEdsClient.class);
        config = dependency("configSRV", IConfigSRV.class);
        kafka = dependency("kafkaSRV", IKafkaSRV.class);
        cda = dependency("cdaSRV", ICdaFacadeSRV.class);
        ReflectionTestUtils.setField(controller, "cdaFacadeSRV", cda);
        fhir = dependency("documentReferenceSRV", IFhirSRV.class);
        invocation = dependency("iniInvocationSRV", IniEdsInvocationSRV.class);
        issuer = dependency("issuerSRV", IIssuerSRV.class);
        fhirConfig = dependency("fhirCFG", FHIRCFG.class);
        errors = dependency("errorHandlerSRV", IErrorHandlerSRV.class);
        dependency("logger", LoggerHelper.class);
        dependency("jwtSRV", IJwtSRV.class);
        dependency("signSRV", ISignSRV.class);
        dependency("accreditationSimulationCFG", AccreditationSimulationCFG.class);
        validationConfig = dependency("validationCFG", ValidationCFG.class);
        when(validationConfig.getDaysAllowToPublishAfterValidation()).thenReturn(10);
        benchmark = dependency("benchmarkCFG", BenchmarkCFG.class);
        dependency("benchmarkCfg", BenchmarkCFG.class);
        validator = dependency("validatorClient", IValidatorClient.class);
        affinity = dependency("affinityDomainValidationSRV", IAffinityDomainValidationSRV.class);
        when(affinity.validateUpdateMetadataRequest(any(), any(), any())).thenReturn(ValidationResultDTO.builder().valid(true).adVersion("2.6.4").build());
        affinityUtility = dependency("affinityDomainUtility", AffinityDomainUtility.class);
        ReflectionTestUtils.setField(controller, "tracer", OpenTelemetry.noop().getTracer("isolation"));
        ReflectionTestUtils.setField(controller, "msCfg", new MicroservicesURLCFG());
        var cdaConfig = new CDACFG();
        ReflectionTestUtils.setField(cdaConfig, "cdaAttachmentName", "cda.xml");
        ReflectionTestUtils.setField(controller, "cdaCfg", cdaConfig);
        pdf = FileUtility.getFileFromInternalResources("Files/attachment/LAB_OK.pdf");
        request = new MockHttpServletRequest();
        String cdaText = PDFUtility.extractContentFromAttachments(pdf, "cda.xml");
        // Existing fixture names vary; extraction uses the configured attachment name.
        if (StringUtility.isNullOrEmpty(cdaText)) {
            cdaText = PDFUtility.extractContentFromAttachments(pdf, "cda");
            ReflectionTestUtils.setField(cdaConfig, "cdaAttachmentName", "cda");
        }
        metadata = FhirResourcesIsolationTest.request();
        metadata.setDescriptions(null);
        metadata.setMode(InjectionModeEnum.ATTACHMENT);
        metadata.setWorkflowInstanceId("workflow");
        request.addParameter("requestBody", StringUtility.toJSONJackson(metadata));
        var payload = JwtServiceIsolationTest.payload().attachment_hash(StringUtility.encodeSHA256(pdf)).build();
        request.addHeader(Constants.Headers.JWT_HEADER, "Bearer e30." + java.util.Base64.getEncoder()
                .encodeToString(StringUtility.toJSONJackson(payload).getBytes()) + ".signature");
        var data = new ValidationDataDTO();
        data.setHash("hash");
        data.setCdaValidated(true);
        data.setWorkflowInstanceId("workflow");
        data.setInsertionDate(new Date());
        when(cda.retrieveValidationInfo(anyString(), any())).thenReturn(data);
        var resource = new ResourceDTO();
        resource.setBundleJson("{\"resourceType\":\"Bundle\"}");
        when(fhir.createFhirResources(anyString(), any(), any(), anyInt(), anyString(), any(), any(), any(), any(), anyString())).thenReturn(resource);
        // Production handlers terminate invalid requests; preserve that contract in the dependency mock.
        doAnswer(i -> { throw i.getArgument(5, ValidationException.class); }).when(errors)
                .publicationValidationExceptionHandler(any(), any(), any(), any(), any(), any(), anyBoolean(), any());
        doAnswer(i -> { throw i.getArgument(4, ValidationException.class); }).when(errors)
                .validationExceptionHandler(any(), any(), any(), any(), any(), any(), any());
    }

    private MultipartFile file() { return new MockMultipartFile("file", "clinical.pdf", "application/pdf", pdf); }

    private GetDocumentMetadataDTO documentMetadata() {
        var data = new GetDocumentMetadataDTO();
        data.setUuid("uuid");
        data.setDocumentType("REF");
        data.setAuthorInstitution("institution");
        data.setAdministrativeRequest(List.of("SSN"));
        data.setEdsPublished("TRUE");
        return data;
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void publishesAndOptionallyReturnsBundle(boolean includeBundle) {
        when(fhirConfig.isEnableBundleInResponse()).thenReturn(includeBundle);
        when(issuer.isFhirBundleEnabledForIssuer("issuer")).thenReturn(includeBundle);
        var result = controller.create(metadata, file(), request);
        assertEquals(202, result.getStatusCode().value());
        assertEquals("workflow", result.getBody().getWorkflowInstanceId());
        verify(invocation).insert(eq("workflow"), any(), any());
        verify(cda).consumeHash("hash");
        verify(kafka).notifyChannel(eq(metadata.getIdentificativoDoc()), contains("PUBLISH"), eq(TipoDocAltoLivEnum.REF), eq(DestinationTypeEnum.INDEXER));
    }

    @Test
    void replacesUsingIniUuidAndIndexerOperation() {
        when(ini.documentMetadata(any(), any())).thenReturn(documentMetadata());
        var result = controller.replace("1.2.3^old", new PublicationUpdateReqDTO(), file(), request);
        assertEquals(202, result.getStatusCode().value());
        verify(invocation).replace(eq("workflow"), any(), any(), eq("uuid"));
        verify(kafka).notifyChannel(eq("1.2.3^old"), contains("REPLACE"), any(), eq(DestinationTypeEnum.INDEXER));
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "error", "uuid"})
    void replacementRejectsMissingIniReference(String failure) {
        var data = documentMetadata();
        if (failure.equals("error")) data.setErrorMessage("lookup failed");
        if (failure.equals("uuid")) data.setUuid("");
        when(ini.documentMetadata(any(), any())).thenReturn(failure.equals("null") ? null : data);
        assertThrows(IniException.class, () -> controller.replace("1.2.3^old", new PublicationUpdateReqDTO(), file(), request));
        verify(invocation, never()).replace(any(), any(), any(), any());
    }

    @Test
    void mappingDiagnosticBlocksPublicationAfterConsumingValidation() {
        var resource = new ResourceDTO();
        resource.setErrorMessage("mapping rejected");
        when(fhir.createFhirResources(anyString(), any(), any(), anyInt(), anyString(), any(), any(), any(), any(), anyString())).thenReturn(resource);
        var error = assertThrows(ValidationException.class, () -> controller.create(metadata, file(), request));
        assertEquals("mapping rejected", error.getError().getDetail());
        verifyNoInteractions(invocation);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void deleteSuccessAndAsyncRetry(boolean retry) {
        when(ini.documentMetadata(any(), any())).thenReturn(documentMetadata());
        when(eds.delete(anyString(), anyString(), anyString())).thenReturn(new EdsResponseDTO(true, "", ""));
        var response = new IniTraceResponseDTO();
        response.setEsito(!retry);
        when(ini.delete(any())).thenReturn(response);
        var result = controller.delete("1.2.3^document", request);
        assertEquals(retry ? 202 : 200, result.getStatusCode().value());
        verify(eds).delete(eq("1.2.3^document"), eq("RSSMRA22A01A399Z"), anyString());
        verify(ini).delete(argThat(r -> "1.2.3^document".equals(r.getIdDoc()) && List.of("uuid").equals(r.getUuid())));
        verify(kafka, times(retry ? 1 : 0)).sendDeleteRequest(anyString(), any());
    }

    @Test
    void deleteStopsOnEdsFailureOrMissingIniMetadata() {
        when(ini.documentMetadata(any(), any())).thenReturn(documentMetadata());
        when(eds.delete(anyString(), anyString(), anyString())).thenReturn(new EdsResponseDTO(false, "failed", ""));
        assertThrows(EdsException.class, () -> controller.delete("1.2.3^document", request));
        verify(ini, never()).delete(any());
        when(ini.documentMetadata(any(), any())).thenReturn(null);
        assertThrows(IniException.class, () -> controller.delete("1.2.3^document", request));
    }

    @Test
    void invalidIdentifiersFailBeforeExternalRequests() {
        assertThrows(ValidationException.class, () -> controller.delete("^", request));
        assertThrows(ValidationException.class, () -> controller.updateMetadata("^", new UpdateMetadataReqDTO(), request));
        verifyNoInteractions(ini, eds);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void metadataUpdateReturnsSyncOrRetryStatus(boolean retry) {
        var merged = new GetMergedMetadatiDTO();
        merged.setMarshallResponse("<metadata/>");
        merged.setMockEds(true);
        when(ini.metadata(any())).thenReturn(merged);
        var response = new IniTraceResponseDTO();
        response.setEsito(!retry);
        when(ini.update(any(), eq(false))).thenReturn(response);
        var update = new UpdateMetadataReqDTO();
        update.setTipoDocumentoLivAlto("REF");
        var result = controller.updateMetadata("1.2.3^document", update, request);
        assertEquals(retry ? 202 : 200, result.getStatusCode().value());
        verify(ini).update(any(), eq(false));
        verify(kafka, times(retry ? 1 : 0)).sendUpdateRequest(anyString(), any());
    }

    @Test
    void metadataLookupFailureDoesNotUpdateIni() {
        var merged = new GetMergedMetadatiDTO();
        merged.setErrorMessage("lookup failed");
        when(ini.metadata(any())).thenReturn(merged);
        var update = new UpdateMetadataReqDTO();
        update.setTipoDocumentoLivAlto("REF");
        assertThrows(IniException.class, () -> controller.updateMetadata("1.2.3^document", update, request));
        verify(ini, never()).update(any(), anyBoolean());
    }
}
