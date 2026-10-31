package it.finanze.sanita.fse2.ms.gtw.dispatcher.isolation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import java.util.Date;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.*;
import org.springframework.test.util.ReflectionTestUtils;
import io.opentelemetry.api.OpenTelemetry;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.controller.impl.FhirPublicationCTL;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.logging.LoggerHelper;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.facade.ICdaFacadeSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.impl.IniEdsInvocationSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.StringUtility;

@Tag("ISOLATION")
class FhirPublicationWorkflowIsolationTest {
    private FhirPublicationCTL controller;
    private IFhirSRV mapping;
    private ICdaFacadeSRV cda;
    private IniEdsInvocationSRV invocation;
    private IIniClient ini;
    private IKafkaSRV kafka;
    private IConfigSRV config;
    private IIssuerSRV issuer;
    private byte[] pdf;
    private MockHttpServletRequest request;
    private PublicationCreationReqDTO metadata;
    private ResourceDTO mapped;

    private <T> T inject(String name, Class<T> type) {
        T value = mock(type);
        for (Class<?> owner = controller.getClass(); owner != Object.class; owner = owner.getSuperclass()) {
            try {
                owner.getDeclaredField(name);
                ReflectionTestUtils.setField(controller, owner, name, value, type);
            } catch (NoSuchFieldException ignored) { }
        }
        return value;
    }

    @BeforeEach void setup() throws Exception {
        controller = new FhirPublicationCTL();
        mapping = inject("documentReferenceSRV", IFhirSRV.class);
        cda = inject("cdaSRV", ICdaFacadeSRV.class);
        ReflectionTestUtils.setField(controller, "cdaFacadeSRV", cda);
        invocation = inject("iniInvocationSRV", IniEdsInvocationSRV.class);
        ini = inject("iniClient", IIniClient.class);
        kafka = inject("kafkaSRV", IKafkaSRV.class);
        config = inject("configSRV", IConfigSRV.class);
        issuer = inject("issuerSRV", IIssuerSRV.class);
        inject("jwtSRV", IJwtSRV.class);
        inject("logger", LoggerHelper.class);
        var errors = inject("errorHandlerSRV", IErrorHandlerSRV.class);
        doAnswer(i -> { throw i.getArgument(5, ValidationException.class); }).when(errors)
                .publicationValidationExceptionHandler(any(), any(), any(), any(), any(), any(), anyBoolean(), any());
        var validation = inject("validationCFG", ValidationCFG.class);
        when(validation.getDaysAllowToPublishAfterValidation()).thenReturn(10);
        ReflectionTestUtils.setField(controller, "msCfg", new MicroservicesURLCFG());
        ReflectionTestUtils.setField(controller, "tracer", OpenTelemetry.noop().getTracer("isolation"));
        pdf = DocumentUtilitiesIsolationTest.pdf(FhirResourcesIsolationTest.BUNDLE.replace("\n", ""));
        metadata = FhirResourcesIsolationTest.request();
        metadata.setDescriptions(null);
        metadata.setMode(InjectionModeEnum.RESOURCE);
        metadata.setWorkflowInstanceId("workflow");
        request = new MockHttpServletRequest();
        request.addParameter("requestBody", StringUtility.toJSONJackson(metadata));
        request.addHeader("X-Callback-Url", "https://callback.test");
        var jwt = JwtServiceIsolationTest.payload().attachment_hash(StringUtility.encodeSHA256(pdf)).build();
        request.addHeader(Constants.Headers.JWT_HEADER, "Bearer e30." + java.util.Base64.getEncoder()
                .encodeToString(StringUtility.toJSONJackson(jwt).getBytes()) + ".signature");
        var data = new ValidationDataDTO();
        data.setHash("hash");
        data.setWorkflowInstanceId("workflow");
        data.setCdaValidated(true);
        data.setInsertionDate(new Date());
        when(cda.retrieveValidationInfo(anyString(), eq("workflow"))).thenReturn(data);
        mapped = new ResourceDTO();
        mapped.setBundleJson("{\"resourceType\":\"Bundle\"}");
        mapped.setDocumentEntryJson("document-entry");
        mapped.setSubmissionSetEntryJson("submission-entry");
        when(mapping.createFhirResourcesFromBundle(anyString(), any(), any(), anyInt(), anyString(), any(), any(), anyString())).thenReturn(mapped);
        var transaction = new ResourceDTO();
        transaction.setBundleJson("{\"resourceType\":\"Bundle\",\"type\":\"transaction\"}");
        when(mapping.convertDocumentToTransaction(anyString())).thenReturn(transaction);
    }

    private MockMultipartFile file() { return new MockMultipartFile("file", "bundle.pdf", "application/pdf", pdf); }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void publicationPersistsTransactionAndPropagatesMappedMetadata(boolean edsEnabled) {
        when(issuer.isEdsEnabledForIssuer("issuer")).thenReturn(edsEnabled);
        var response = controller.create(metadata, file(), request);
        assertEquals(201, response.getStatusCode().value());
        assertEquals("workflow", response.getBody().getWorkflowInstanceId());
        assertEquals("issuer", request.getAttribute("JWT_ISSUER"));
        verify(cda).consumeHash("hash");
        verify(invocation).insert(eq("workflow"), argThat(resource -> "document-entry".equals(resource.getDocumentEntryJson())
                && "submission-entry".equals(resource.getSubmissionSetEntryJson()) && resource.getBundleJson().contains("transaction")), any());
        verify(kafka).notifyChannel(eq(metadata.getIdentificativoDoc()), contains("\"edsPublished\":" + edsEnabled),
                eq(TipoDocAltoLivEnum.REF), eq(DestinationTypeEnum.INDEXER));
        verify(kafka).sendPublicationStatus(anyString(), eq("workflow"), eq(EventStatusEnum.SUCCESS), isNull(), any(), any(), eq("https://callback.test"));
    }

    @Test void replacementUsesIniUuidAndRetainsEdsPublicationState() {
        var prior = new GetDocumentMetadataDTO();
        prior.setUuid("prior-uuid");
        prior.setEdsPublished("TRUE");
        when(ini.documentMetadata(any(), eq("workflow"))).thenReturn(prior);
        var response = controller.replace("1.2.3^prior", new PublicationUpdateReqDTO(), file(), request);
        assertEquals(200, response.getStatusCode().value());
        verify(invocation).replace(eq("workflow"), any(), any(), eq("prior-uuid"));
        verify(kafka).notifyChannel(eq("1.2.3^prior"), contains("REPLACE"), any(), eq(DestinationTypeEnum.INDEXER));
        verify(kafka).sendReplaceStatus(anyString(), eq("workflow"), eq(EventStatusEnum.SUCCESS), isNull(), any(), any(), eq("https://callback.test"));
    }

    @ParameterizedTest @ValueSource(strings = {"missing", "error", "uuid"})
    void replacementRejectsUnavailablePriorDocument(String failure) {
        var prior = new GetDocumentMetadataDTO();
        prior.setUuid(failure.equals("uuid") ? "" : "uuid");
        if (failure.equals("error")) prior.setErrorMessage("lookup failed");
        when(ini.documentMetadata(any(), any())).thenReturn(failure.equals("missing") ? null : prior);
        assertThrows(IniException.class, () -> controller.replace("1.2.3^prior", new PublicationUpdateReqDTO(), file(), request));
        verify(invocation, never()).replace(any(), any(), any(), any());
    }

    @Test void mappingDiagnosticIsTranslatedAndDoesNotPersistInvocation() {
        mapped.setErrorMessage("mapping diagnostic");
        var exception = assertThrows(ValidationException.class, () -> controller.create(metadata, file(), request));
        assertEquals("mapping diagnostic", exception.getError().getDetail());
        verifyNoInteractions(invocation);
        verify(cda).consumeHash("hash");
    }

    @Test void invalidReplacementIdentifierFailsBeforeMapping() {
        assertThrows(ValidationException.class, () -> controller.replace("^", new PublicationUpdateReqDTO(), file(), request));
        verifyNoInteractions(mapping, invocation, ini);
    }

    @Test void parsesPatientCompositeAndSimpleIdentifiers() {
        assertNull(FhirPublicationCTL.parsePersonId(null));
        assertNull(FhirPublicationCTL.parsePersonId(""));
        assertEquals("patient", FhirPublicationCTL.parsePersonId("patient").getFiscalCode());
        assertNull(FhirPublicationCTL.parsePersonId("patient").getOid());
        assertEquals("1.2.3", FhirPublicationCTL.parsePersonId("patient^^^&1.2.3&ISO").getOid());
        assertEquals("1.2.3", FhirPublicationCTL.parsePersonId("patient^^^&1.2.3").getOid());
    }
}
