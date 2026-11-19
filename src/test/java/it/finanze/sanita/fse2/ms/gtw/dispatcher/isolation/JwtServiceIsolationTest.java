package it.finanze.sanita.fse2.ms.gtw.dispatcher.isolation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.JwtCFG;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.JWTPayloadDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.JWTPayloadDTO.JWTPayloadDTOBuilder;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.SystemTypeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ValidationException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.impl.JwtSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.impl.UtilitySRV;

@Tag("ISOLATION")
@ExtendWith(MockitoExtension.class)
class JwtServiceIsolationTest {
    @Mock private UtilitySRV utility;
    @Mock private JwtCFG config;
    @InjectMocks private JwtSRV service;

    static JWTPayloadDTOBuilder payload() {
        return JWTPayloadDTO.builder().iss("issuer").sub("RSSMRA22A01A399Z")
                .subject_organization_id("080").subject_organization("Regione Emilia-Romagna")
                .locality("GTW^^^^^&2.16.840.1.113883.2.9.4.1.3&ISO^^^^STR").subject_role("APR")
                .person_id("RSSMRA22A01A399Z").patient_consent(true).purpose_of_use("TREATMENT")
                .resource_hl7_type("('11502-2^^2.16.840.1.113883.6.1')").action_id("CREATE")
                .attachment_hash("hash").subject_application_id("app").subject_application_vendor("vendor")
                .subject_application_version("1");
    }

    @Test
    void validatesOperationSpecificActionAndPurpose() {
        when(utility.isValidCf("RSSMRA22A01A399Z")).thenReturn(true);
        assertDoesNotThrow(() -> service.validatePayloadForCreate(payload().build()));
        assertDoesNotThrow(() -> service.validatePayloadForValidation(payload().build()));
        assertDoesNotThrow(() -> service.validatePayloadForReplace(payload().action_id("UPDATE").build()));
        assertDoesNotThrow(() -> service.validatePayloadForUpdate(payload().action_id("UPDATE").purpose_of_use("UPDATE").build()));
        assertDoesNotThrow(() -> service.validatePayloadForDelete(payload().action_id("DELETE").purpose_of_use("UPDATE").build()));
        assertTrue(assertThrows(ValidationException.class,
                () -> service.validatePayloadForCreate(payload().action_id("DELETE").build())).getError().getDetail().contains("action_id"));
        assertTrue(assertThrows(ValidationException.class,
                () -> service.validatePayloadForCreate(payload().purpose_of_use("UPDATE").build())).getError().getDetail().contains("purpose_of_use"));
        verify(utility, atLeast(5)).isValidCf("RSSMRA22A01A399Z");
    }

    static Stream<Consumer<JWTPayloadDTOBuilder>> missingFields() {
        return Stream.of(p -> p.patient_consent(null), p -> p.action_id(null),
                p -> p.locality(null), p -> p.person_id(null), p -> p.purpose_of_use(null),
                p -> p.subject_organization(null), p -> p.subject_organization_id(null),
                p -> p.subject_role(null), p -> p.subject_application_id(null),
                p -> p.subject_application_vendor(null), p -> p.subject_application_version(null));
    }

    @ParameterizedTest
    @MethodSource("missingFields")
    void rejectsMissingRequiredFields(Consumer<JWTPayloadDTOBuilder> mutation) {
        var payload = payload();
        mutation.accept(payload);
        var error = assertThrows(ValidationException.class, () -> service.validateMandatoryFields(payload.build()));
        assertTrue(error.getError().getDetail().contains("non è valorizzato"));
        verifyNoInteractions(utility);
    }

    @Test
    void rejectsNullPayload() {
        assertTrue(assertThrows(ValidationException.class,
                () -> service.validateMandatoryFields(null)).getError().getDetail().contains("jwt payload"));
    }

    static Stream<Consumer<JWTPayloadDTOBuilder>> invalidFields() {
        return Stream.of(p -> p.action_id("invalid"), p -> p.purpose_of_use("invalid"),
                p -> p.subject_role("invalid"), p -> p.subject_organization_id("invalid"),
                p -> p.subject_organization("invalid"), p -> p.subject_organization("Regione Lazio"));
    }

    @ParameterizedTest
    @MethodSource("invalidFields")
    void rejectsUnknownOrIncoherentValueSets(Consumer<JWTPayloadDTOBuilder> mutation) {
        when(utility.isValidCf(anyString())).thenReturn(true);
        var payload = payload();
        mutation.accept(payload);
        assertNotNull(assertThrows(ValidationException.class,
                () -> service.validatePayloadForCreate(payload.build())).getError().getDetail());
    }

    @Test
    void validatesOscuramentoRoleAndLocality() {
        var payload = payload().action_id("UPDATE").purpose_of_use("SYSADMIN").subject_role("NOR").locality("------");
        assertDoesNotThrow(() -> service.validatePayloadForUpdateOscuramento(payload.build()));
        payload.locality("invalid");
        assertTrue(assertThrows(ValidationException.class,
                () -> service.validatePayloadForUpdateOscuramento(payload.build())).getError().getDetail().contains("locality"));
        payload.subject_role("APR");
        assertTrue(assertThrows(ValidationException.class,
                () -> service.validatePayloadForUpdateOscuramento(payload.build())).getError().getDetail().contains("subject_role"));
    }

    @Test
    void fiscalCodeUsesMefValidationButAcceptsOtherAuthorities() {
        when(utility.isValidCf("RSSMRA22A01A399Z")).thenReturn(true);
        service.checkFiscalCode("RSSMRA22A01A399Z", "sub");
        service.checkFiscalCode("RSSMRA22A01A399Z^^^&2.16.840.1.113883.2.9.4.3.2&ISO", "sub");
        service.checkFiscalCode("foreign^^^&other&ISO", "sub");
        verify(utility, times(2)).isValidCf("RSSMRA22A01A399Z");
        assertThrows(ValidationException.class, () -> service.checkFiscalCode(null, "sub"));
        assertThrows(ValidationException.class, () -> service.checkFiscalCode("invalid", "sub"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"unknown", " "})
    void unknownIssuerIsNotTs(String issuer) {
        assertEquals(SystemTypeEnum.NONE, service.getSystemByIssuer(issuer));
    }

    @Test
    void identifiesTsAndRejectsMalformedLocality() {
        when(config.getTsIssuer()).thenReturn("TS");
        assertEquals(SystemTypeEnum.TS, service.getSystemByIssuer("TS"));
        assertThrows(ValidationException.class, () -> service.isValidLocality("invalid"));
    }
}
