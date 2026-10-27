package it.finanze.sanita.fse2.ms.gtw.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.IValidatorClient;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.BenchmarkCFG;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.controller.impl.AbstractCTL;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.ValidationInfoDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.ActivityEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.RawValidationEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.RestExecutionResultEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.SystemTypeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ValidationException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.facade.ICdaFacadeSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.IJwtSRV;

@ExtendWith(MockitoExtension.class)
class ValidationOutcomeTest {

    @Mock private IValidatorClient validatorClient;
    @Mock private ICdaFacadeSRV cdaFacadeSRV;
    @Mock private IJwtSRV jwtSRV;
    @Mock private BenchmarkCFG benchmarkCfg;

    private final TestController controller = new TestController();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "validatorClient", validatorClient);
        ReflectionTestUtils.setField(controller, "cdaFacadeSRV", cdaFacadeSRV);
        ReflectionTestUtils.setField(controller, "jwtSRV", jwtSRV);
        ReflectionTestUtils.setField(controller, "benchmarkCfg", benchmarkCfg);
        when(jwtSRV.getSystemByIssuer("issuer")).thenReturn(SystemTypeEnum.TS);
    }

    @ParameterizedTest
    @EnumSource(value = RawValidationEnum.class, names = {"OK", "SEMANTIC_WARNING", "PRUNING_WARNING"})
    void successfulOutcomesPersistValidationAndReturnWarnings(RawValidationEnum outcome) throws Exception {
        // Exercise the actual validator wire format, including the new outcome name.
        ValidationInfoDTO response = new ObjectMapper().readValue("""
                {"result":"%s","message":["WARNING: diagnostic"],"transformID":"map","engineID":"engine"}
                """.formatted(outcome.name()), ValidationInfoDTO.class);
        when(validatorClient.validate("<ClinicalDocument/>", "workflow", SystemTypeEnum.TS)).thenReturn(response);

        String warning = controller.runValidation();

        assertEquals("WARNING: diagnostic", warning);
        verify(cdaFacadeSRV).create(anyString(), eq("workflow"), eq("map"), eq("engine"));
    }

    @ParameterizedTest
    @EnumSource(value = RawValidationEnum.class, names = {"FHIR_VALIDATION_ERROR", "SEMANTIC_ERROR", "SYNTAX_ERROR"})
    void errorOutcomesRemainBlocking(RawValidationEnum outcome) {
        when(validatorClient.validate("<ClinicalDocument/>", "workflow", SystemTypeEnum.TS)).thenReturn(
                new ValidationInfoDTO(outcome, List.of("ERROR: diagnostic"), "map", "engine"));

        assertThrows(ValidationException.class, controller::runValidation);
        verify(cdaFacadeSRV, never()).create(anyString(), anyString(), anyString(), anyString());
        if (outcome == RawValidationEnum.FHIR_VALIDATION_ERROR) {
            assertEquals(RestExecutionResultEnum.FHIR_VALIDATOR_ERROR, RestExecutionResultEnum.fromRawResult(outcome));
        }
    }

    @Test
    void okOutcomeDoesNotExposeInformationalMessagesAsWarnings() {
        when(validatorClient.validate("<ClinicalDocument/>", "workflow", SystemTypeEnum.TS)).thenReturn(
                new ValidationInfoDTO(RawValidationEnum.OK,
                        List.of("INFORMATION: diagnostic", "warning: actual warning"), "map", "engine"));

        assertEquals("warning: actual warning", controller.runValidation());
    }

    private static class TestController extends AbstractCTL {
        String runValidation() {
            return validate("<ClinicalDocument/>", ActivityEnum.VALIDATION, "workflow", "issuer");
        }
    }
}
