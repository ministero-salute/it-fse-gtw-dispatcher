package it.finanze.sanita.fse2.ms.gtw.dispatcher.isolation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.controller.impl.AbstractCTL;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.IValidatorClient;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.BenchmarkCFG;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.IJwtSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.facade.ICdaFacadeSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.StringUtility;

@Tag("ISOLATION")
class AbstractControllerIsolationTest extends AbstractCTL {
    private IValidatorClient validator;
    private ICdaFacadeSRV facade;
    private IJwtSRV jwt;
    private BenchmarkCFG benchmark;

    @BeforeEach
    void dependencies() {
        validator = mock(IValidatorClient.class);
        facade = mock(ICdaFacadeSRV.class);
        jwt = mock(IJwtSRV.class);
        benchmark = mock(BenchmarkCFG.class);
        ReflectionTestUtils.setField(this, "validatorClient", validator);
        ReflectionTestUtils.setField(this, "cdaFacadeSRV", facade);
        ReflectionTestUtils.setField(this, "jwtSRV", jwt);
        ReflectionTestUtils.setField(this, "benchmarkCfg", benchmark);
    }

    @Test
    void parsesValidationRequestsAndRejectsMissingActivity() {
        assertEquals(ActivityEnum.VALIDATION, getAndValidateValidationReq("{\"activity\":\"VALIDATION\"}").getActivity());
        assertEquals(ActivityEnum.VERIFICA, getAndValidateValidationFhirReq("{\"activity\":\"VERIFICA\"}").getActivity());
        assertTrue(assertThrows(ValidationException.class, () -> getAndValidateValidationReq("{}"))
                .getError().getDetail().contains("activity"));
        assertTrue(assertThrows(ValidationException.class, () -> getAndValidateValidationFhirReq("{}"))
                .getError().getDetail().contains("activity"));
    }

    static Stream<Consumer<PublicationCreationReqDTO>> missingPublicationFields() {
        return Stream.of(r -> r.setIdentificativoDoc(null), r -> r.setIdentificativoRep(null),
                r -> r.setTipoDocumentoLivAlto(null), r -> r.setAssettoOrganizzativo(null),
                r -> r.setTipoAttivitaClinica(null), r -> r.setIdentificativoSottomissione(null),
                r -> r.setTipologiaStruttura(null), r -> r.setAdministrativeRequest(null));
    }

    @ParameterizedTest
    @MethodSource("missingPublicationFields")
    void rejectsMissingPublicationMetadata(Consumer<PublicationCreationReqDTO> mutate) {
        var request = FhirResourcesIsolationTest.request();
        mutate.accept(request);
        String json = StringUtility.toJSONJackson(request);
        assertTrue(assertThrows(ValidationException.class, () -> getAndValidatePublicationReq(json, false))
                .getError().getDetail().contains("deve essere valorizzato"));
        assertThrows(ValidationException.class, () -> getAndValidateValdaPublicationReq(json));
    }

    @Test
    void acceptsCompletePublicationAndReplacementMetadata() {
        var request = FhirResourcesIsolationTest.request();
        request.setDescriptions(null);
        String json = StringUtility.toJSONJackson(request);
        assertEquals(request.getIdentificativoDoc(), getAndValidatePublicationReq(json, false).getIdentificativoDoc());
        assertEquals(request.getIdentificativoDoc(), getAndValidatePublicationReq(json, true).getIdentificativoDoc());
        assertEquals(request.getIdentificativoDoc(), getAndValidateValdaPublicationReq(json).getIdentificativoDoc());
        request.setDataInizioPrestazione("invalid");
        assertThrows(ValidationException.class, () -> getAndValidatePublicationReq(StringUtility.toJSONJackson(request), false));
        assertThrows(ValidationException.class, () -> getAndValidateValdaPublicationReq(StringUtility.toJSONJackson(request)));
    }

    @Test
    void rejectsBadDescriptionsAndClinicalAccessCodes() {
        var request = FhirResourcesIsolationTest.request();
        request.setDescriptions(List.of("malformed"));
        assertTrue(checkPublicationMandatoryElements(request, false).contains("descriptions"));
        request.setDescriptions(List.of("code^description^unknown"));
        assertTrue(checkPublicationMandatoryElements(request, false).contains("OID"));
        request.setDescriptions(List.of("code^description^" + DescriptionEnum.values()[0].getOid()));
        assertNull(checkPublicationMandatoryElements(request, false));
        request.setAttiCliniciRegoleAccesso(List.of("unknown"));
        assertTrue(checkPublicationMandatoryElements(request, false).contains("atti clinici"));
    }

    @Test
    void validatesUpdateMetadataDatesDescriptionsAndAccessCodes() {
        var request = new UpdateMetadataReqDTO();
        assertThrows(ValidationException.class, () -> validateUpdateMetadataReq(request, ""));
        request.setTipoDocumentoLivAlto("REF");
        assertDoesNotThrow(() -> validateUpdateMetadataReq(request, ""));
        request.setDataFinePrestazione("invalid");
        assertTrue(assertThrows(ValidationException.class, () -> validateUpdateMetadataReq(request, ""))
                .getError().getDetail().contains("data fine"));
        request.setDataFinePrestazione("20261030120000");
        request.setDescriptions(List.of("invalid"));
        assertThrows(ValidationException.class, () -> validateUpdateMetadataReq(request, ""));
        request.setDescriptions(null);
        request.setAttiCliniciRegoleAccesso(List.of("unknown"));
        assertThrows(ValidationException.class, () -> validateUpdateMetadataReq(request, ""));
        var hidden = new UpdateMetadataOscuramentoReqDTO();
        assertDoesNotThrow(() -> validateUpdateOscuramenteMetadataReq(hidden, ""));
        hidden.setAttiCliniciRegoleAccesso(List.of("unknown"));
        assertThrows(ValidationException.class, () -> validateUpdateOscuramenteMetadataReq(hidden, ""));
        assertNull(checkFormatDate(null, null));
        assertNull(checkFormatDate("20261030120000", "20261030130000"));
    }

    @Test
    void rejectsAbsentEmptyNonPdfAndIoFailure() throws Exception {
        assertThrows(ValidationException.class, () -> getAndValidateFile(null));
        assertThrows(ValidationException.class, () -> getAndValidateFile(new MockMultipartFile("file", new byte[0])));
        assertThrows(ValidationException.class, () -> getAndValidateFile(new MockMultipartFile("file", "not pdf".getBytes())));
        var file = mock(org.springframework.web.multipart.MultipartFile.class);
        var failure = new IOException("read failed");
        when(file.getBytes()).thenThrow(failure);
        assertSame(failure, assertThrows(BusinessException.class, () -> getAndValidateFile(file)).getCause());
        assertThrows(ValidationException.class, () -> getAndValidateFhirFile(null, null));
        assertThrows(BusinessException.class, () -> getAndValidateFhirFile(file, null));
    }

    @Test
    void validatesDocumentTypePatientAndHashWithoutMockingTheChecks() {
        var payload = AffinityStrategiesIsolationTest.jwt();
        assertDoesNotThrow(() -> validateJWT(payload, FhirResourcesIsolationTest.CDA));
        verify(jwt).checkFiscalCode(payload.getPerson_id(), "person_id");
        assertThrows(ValidationException.class, () -> validateJWT(payload, "<ClinicalDocument/>"));
        assertThrows(ValidationException.class, () -> validateJWT(payload, FhirResourcesIsolationTest.CDA.replace("11502-2", "other")));
        assertThrows(ValidationException.class, () -> validateJWT(payload, FhirResourcesIsolationTest.CDA.replace("RSSMRA22A01A399Z", "other")));
        assertDoesNotThrow(() -> validateDocumentHash("HaSh", payload));
        assertThrows(ValidationException.class, () -> validateDocumentHash("other", payload));
        assertDoesNotThrow(() -> validateJWTFhirDiretto(payload, FhirResourcesIsolationTest.BUNDLE));
        assertThrows(ValidationException.class, () -> validateJWTFhirDiretto(JwtServiceIsolationTest.payload().person_id("other").build(), FhirResourcesIsolationTest.BUNDLE));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void validatorPersistsOnlyForValidationActivity(boolean persist) {
        var info = new ValidationInfoDTO();
        info.setResult(RawValidationEnum.OK);
        info.setTransformID("map");
        info.setEngineID("engine");
        info.setMessage(List.of("WARNING: diagnostic", "information"));
        when(validator.validate(anyString(), eq("workflow"), any())).thenReturn(info);
        when(jwt.getSystemByIssuer("issuer")).thenReturn(SystemTypeEnum.NONE);
        assertEquals("WARNING: diagnostic", validate(FhirResourcesIsolationTest.CDA,
                persist ? ActivityEnum.VALIDATION : ActivityEnum.VERIFICA, "workflow", "issuer"));
        verify(facade, times(persist ? 1 : 0)).create(anyString(), eq("workflow"), eq("map"), eq("engine"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void validatorFailuresDoNotPersist(boolean dependencyFailure) {
        when(jwt.getSystemByIssuer("issuer")).thenReturn(SystemTypeEnum.NONE);
        if (dependencyFailure) {
            when(validator.validate(anyString(), anyString(), any())).thenThrow(new IllegalStateException("offline"));
            assertThrows(BusinessException.class, () -> validate("cda", ActivityEnum.VALIDATION, "workflow", "issuer"));
        } else {
            var info = new ValidationInfoDTO();
            info.setResult(RawValidationEnum.SYNTAX_ERROR);
            info.setMessage(List.of("blocking diagnostic"));
            when(validator.validate(anyString(), anyString(), any())).thenReturn(info);
            assertThrows(ValidationException.class, () -> validate("cda", ActivityEnum.VALIDATION, "workflow", "issuer"));
        }
        verifyNoInteractions(facade);
    }

    @Test
    void validatedHashLookupRejectsUnvalidatedAndAcceptsValidatedDocuments() {
        var data = new ValidationDataDTO();
        when(facade.retrieveValidationInfo(anyString(), eq("workflow"))).thenReturn(data);
        assertThrows(ValidationException.class, () -> getValidationInfo("cda", "workflow"));
        assertThrows(ValidationException.class, () -> getValidationInfoFhirBundle("bundle", "workflow"));
        data.setCdaValidated(true);
        assertSame(data, getValidationInfo("cda", "workflow"));
        assertSame(data, getValidationInfoFhirBundle("bundle", "workflow"));
        assertSame(data, getValidationInfo("<!--CDA_BENCHMARK_TEST-->cda", "workflow"));
    }

    @Test
    void anonymizesLegalAuthenticatorBeforeHashing() {
        String result = cdaWithoutLegalAuthenticator("<ClinicalDocument><LegalAuthenticator><Signature>secret signature</Signature><id root=\"secret\"/><name>Doctor</name></LegalAuthenticator><title>Keep</title></ClinicalDocument>");
        assertFalse(result.contains("secret"));
        assertFalse(result.contains("Doctor"));
        assertTrue(result.contains("PLACEHOLDER"));
        assertTrue(result.contains("Keep"));
    }
}
