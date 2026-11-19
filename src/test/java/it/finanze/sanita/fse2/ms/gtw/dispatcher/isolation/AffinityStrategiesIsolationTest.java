package it.finanze.sanita.fse2.ms.gtw.dispatcher.isolation;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.JWTPayloadDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.UpdateMetadataReqDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.AffinityDomainStrategy;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.strategy.ad21.Ad21Strategy;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.strategy.ad24.Ad24Strategy;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.strategy.ad241.Ad241Strategy;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.strategy.ad25.Ad25Strategy;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.strategy.ad26.Ad26Strategy;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.strategy.ad261.Ad261Strategy;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.strategy.ad262.Ad262Strategy;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.strategy.ad263.Ad263Strategy;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.strategy.ad264.Ad264Strategy;

@Tag("ISOLATION")
class AffinityStrategiesIsolationTest {
    static Stream<AffinityDomainStrategy> strategies() {
        return Stream.of(new Ad21Strategy(), new Ad24Strategy(), new Ad241Strategy(),
                new Ad25Strategy(), new Ad26Strategy(), new Ad261Strategy(), new Ad262Strategy(),
                new Ad263Strategy(), new Ad264Strategy());
    }

    static JWTPayloadDTO jwt() {
        return new JWTPayloadDTO("issuer", 1L, 2L, "jti", "aud", "RSSMRA22A01A399Z", "080",
                "Regione Emilia-Romagna", "GTW^^^^^&2.16.840.1.113883.2.9.4.1.3&ISO^^^^STR",
                "APR", "RSSMRA22A01A399Z", true, "TREATMENT", "('11502-2^^2.16.840.1.113883.6.1')",
                "CREATE", "hash", "application", "vendor", "1", false);
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void rejectsUnknownMetadataValuesWithVersionAndFieldDiagnostics(AffinityDomainStrategy strategy) {
        var request = new UpdateMetadataReqDTO();
        request.setTipoDocumentoLivAlto("REF");
        request.setAssettoOrganizzativo("not-a-practice-setting");
        request.setTipologiaStruttura("not-a-facility");
        request.setTipoAttivitaClinica("not-an-activity");
        request.setAdministrativeRequest(List.of("not-an-administrative-request"));
        var result = strategy.validateUpdateMetadataReqDTO(request, jwt());
        assertFalse(result.isValid());
        assertEquals(strategy.versionId(), result.getAdVersion());
        assertEquals("INVALID_VALUE_SET", result.getErrorCode());
        assertTrue(result.getErrorMessage().contains("assettoOrganizzativo"));
        assertTrue(result.getErrorMessage().contains("tipologiaStruttura"));
        assertTrue(result.getErrorMessage().contains("tipoAttivitaClinica"));
        assertFalse(result.getMissingFields().isEmpty());
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void optionalMetadataMayBeAbsent(AffinityDomainStrategy strategy) {
        var request = new UpdateMetadataReqDTO();
        request.setTipoDocumentoLivAlto("REF");
        var result = strategy.validateUpdateMetadataReqDTO(request, jwt());
        assertTrue(result.isValid(), result.getErrorMessage());
        assertEquals(strategy.versionId(), result.getAdVersion());
        assertTrue(result.getMissingFields().isEmpty());
        assertNotNull(strategy.effectiveFrom());
    }
}
