package it.finanze.sanita.fse2.ms.gtw.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.impl.FhirMappingClient;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.FlowsCFG;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.ResourceDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.PublicationCreationReqDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.client.FhirValidationDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.client.FhirValidationDTO.ValidationIssueDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.client.FhirValidationDTO.ValidationResultDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.client.TransformResDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.AttivitaClinicaEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.HealthcareFacilityEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.PracticeSettingCodeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.TipoDocAltoLivEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.IConfigSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.impl.FhirSRV;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class FhirMappingPublicationTest {

    @Mock private FhirMappingClient client;
    @Mock private IConfigSRV configSrv;
    @Mock private FlowsCFG flowsCFG;
    @InjectMocks private FhirSRV service;

    private PublicationCreationReqDTO request;

    @BeforeEach
    void setUp() {
        request = new PublicationCreationReqDTO();
        request.setTipologiaStruttura(HealthcareFacilityEnum.Ospedale);
        request.setAssettoOrganizzativo(PracticeSettingCodeEnum.AD_PSC001);
        request.setTipoDocumentoLivAlto(TipoDocAltoLivEnum.PRE);
        request.setTipoAttivitaClinica(AttivitaClinicaEnum.CON);
        request.setIdentificativoRep("2.16.840.1.113883.2.9.2.080.4.5.1234");
    }

    @Test
    void normalizationWarningsDoNotBlockPublicationWithoutValidations(CapturedOutput output) throws Exception {
        TransformResDTO response = new ObjectMapper().readValue("""
                {"json":{"resourceType":"Bundle"},"normalizationIssues":[
                  {"severity":"WARNING","code":"removed","paths":["Bundle.entry[0]"],
                   "message":"sensitive clinical diagnostic"}],"futureField":true}
                """, TransformResDTO.class);
        when(client.callConvertCdaInBundle(any())).thenReturn(response);

        ResourceDTO result = publish();

        assertNull(result.getErrorMessage());
        assertTrue(result.getWarning().contains("NORMALIZATION - WARNING: sensitive clinical diagnostic"));
        assertNotNull(result.getBundleJson());
        assertEquals(List.of("Bundle.entry[0]"), response.getNormalizationIssues().getFirst().getPaths());
        assertFalse(output.getAll().contains("sensitive clinical diagnostic"));
    }

    @Test
    void validationWarningsDoNotBlockPublication(CapturedOutput output) {
        TransformResDTO response = responseWithValidation("WARNING");
        when(flowsCFG.getPublicationFlowValidations()).thenReturn(List.of("FHIR"));
        when(client.callConvertCdaInBundle(any())).thenReturn(response);

        ResourceDTO result = publish();

        assertNull(result.getErrorMessage());
        assertTrue(result.getWarning().contains("FHIR - WARNING: sensitive clinical diagnostic"));
        assertNotNull(result.getBundleJson());
        assertFalse(output.getAll().contains("sensitive clinical diagnostic"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ERROR", "FATAL", "error", "fatal"})
    void validationErrorsBlockPublicationEvenWithNormalizationWarnings(String severity) {
        TransformResDTO response = responseWithValidation(severity);
        response.setNormalizationIssues(List.of(issue("WARNING")));
        when(client.callConvertCdaInBundle(any())).thenReturn(response);

        ResourceDTO result = publish();

        assertTrue(result.getErrorMessage().contains(severity));
        assertNull(result.getBundleJson());
    }

    @Test
    void transformationFailureTakesPrecedence() {
        TransformResDTO response = responseWithValidation("WARNING");
        response.setErrorMessage("Transformation failed");
        when(client.callConvertCdaInBundle(any())).thenReturn(response);

        ResourceDTO result = publish();

        assertEquals("Transformation failed", result.getErrorMessage());
        assertNull(result.getBundleJson());
    }

    @Test
    void emptyResponseFailsExplicitly() {
        BusinessException error = assertThrows(BusinessException.class, this::publish);
        assertTrue(error.getCause().getMessage().contains("empty response"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void missingRequestedValidationResultsFailExplicitly(boolean hasValidationResponse) {
        TransformResDTO response = new TransformResDTO("", new Document("resourceType", "Bundle"));
        if (hasValidationResponse) {
            response.setValidationResponse(new FhirValidationDTO());
        }
        when(flowsCFG.getPublicationFlowValidations()).thenReturn(List.of("FHIR"));
        when(client.callConvertCdaInBundle(any())).thenReturn(response);

        BusinessException error = assertThrows(BusinessException.class, this::publish);

        assertTrue(error.getCause().getMessage().contains("missing the requested validation results"));
    }

    @Test
    void legacyResponseWithoutDiagnosticsStillPublishes() {
        when(client.callConvertCdaInBundle(any())).thenReturn(
                new TransformResDTO("", new Document("resourceType", "Bundle")));

        ResourceDTO result = publish();

        assertNull(result.getErrorMessage());
        assertNull(result.getWarning());
        assertNotNull(result.getBundleJson());
    }

    @Test
    void mixedSeveritiesAreSeparated() {
        TransformResDTO response = responseWithValidation("ERROR");
        ValidationIssueDTO warning = issue("WARNING");
        warning.setMessage("warning detail");
        ValidationIssueDTO information = issue("INFORMATION");
        information.setMessage("informational detail");
        response.getValidationResponse().getResults().getFirst().setIssues(
                List.of(issue("ERROR"), warning, information));
        when(client.callConvertCdaInBundle(any())).thenReturn(response);

        ResourceDTO result = publish();

        assertEquals("FHIR - ERROR: sensitive clinical diagnostic", result.getErrorMessage());
        assertEquals("FHIR - WARNING: warning detail", result.getWarning());
        assertNull(result.getBundleJson());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ERROR", "FATAL"})
    void normalizationErrorsBlockPublication(String severity) {
        TransformResDTO response = new TransformResDTO("", new Document("resourceType", "Bundle"));
        response.setNormalizationIssues(List.of(issue(severity)));
        when(client.callConvertCdaInBundle(any())).thenReturn(response);

        ResourceDTO result = publish();

        assertEquals("NORMALIZATION - " + severity + ": sensitive clinical diagnostic", result.getErrorMessage());
        assertNull(result.getWarning());
        assertNull(result.getBundleJson());
    }

    @ParameterizedTest
    @ValueSource(strings = {"INFORMATION", "information", "warning"})
    void nonBlockingSeveritiesAllowPublication(String severity) {
        when(client.callConvertCdaInBundle(any())).thenReturn(responseWithValidation(severity));

        ResourceDTO result = publish();

        assertNull(result.getErrorMessage());
        assertNotNull(result.getBundleJson());
        if ("warning".equals(severity)) {
            assertNotNull(result.getWarning());
        } else {
            assertNull(result.getWarning());
        }
    }

    private ResourceDTO publish() {
        return service.createFhirResources("<ClinicalDocument/>", "", request, 1, "hash",
                "transform", "engine", "080", "institution", "sha1");
    }

    private TransformResDTO responseWithValidation(String severity) {
        ValidationResultDTO validation = new ValidationResultDTO();
        validation.setType("FHIR");
        validation.setIssues(List.of(issue(severity)));
        FhirValidationDTO validations = new FhirValidationDTO();
        validations.setResults(List.of(validation));
        return new TransformResDTO("", new Document("resourceType", "Bundle"), validations);
    }

    private ValidationIssueDTO issue(String severity) {
        ValidationIssueDTO issue = new ValidationIssueDTO();
        issue.setSeverity(severity);
        issue.setMessage("sensitive clinical diagnostic");
        return issue;
    }
}
