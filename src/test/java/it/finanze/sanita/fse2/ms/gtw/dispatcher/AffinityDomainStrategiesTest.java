package it.finanze.sanita.fse2.ms.gtw.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.JWTPayloadDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.UpdateMetadataReqDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.MetadataValidationException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.strategy.ad21.Ad21Strategy;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.strategy.ad24.Ad24Strategy;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.strategy.ad241.Ad241Strategy;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.strategy.ad25.Ad25Strategy;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.strategy.ad26.Ad26Strategy;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.strategy.ad261.Ad261Strategy;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.strategy.ad262.Ad262Strategy;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.strategy.ad263.Ad263Strategy;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.ad.strategy.ad264.Ad264Strategy;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.validation.dto.ValidationResultDTO;

@DisplayName("Affinity Domain Strategies test")
public class AffinityDomainStrategiesTest {

    /**
     * Builds a valid {@link JWTPayloadDTO} for AD 2.6.4.
     * {@code resource_hl7_type} uses the LOINC format expected by
     * {@code StringUtility.extractHl7TypeCode()}: {@code ('11502-2^^2.16.840.1.113883.6.1')}.
     * CODE_11502_2 (Referto di Laboratorio) maps to TipoDocAltoLivAd264Enum.REF.
     */
    private JWTPayloadDTO buildJWTPayloadDTO(Boolean validationException) {
        return JWTPayloadDTO.builder()
                .iss("080")
                .iat(1540890704L)
                .exp(1540918800L)
                .jti("1540918800")
                .aud("fse-gateway")
                .sub("RSSMRA22A01A399Z")
                .subject_organization_id("080")
                .subject_organization("Regione Emilia-Romagna")
                .locality("GTW_NAME^^^^^&2.16.840.1.113883.2.9.4.1.3&ISO^^^^GTW_STRUCTURE")
                .subject_role("APR")
                .person_id("RSSMRA22A01A399Z")
                .patient_consent(true)
                .purpose_of_use("TREATMENT")
                // validationException=true  → null   → CorrelationDocumentType264Validator throws MetadataValidationException
                // validationException=false → "11502-2" → CODE_11502_2 → REF ✓
                .resource_hl7_type(validationException ? null : "('11502-2^^2.16.840.1.113883.6.1')")
                .action_id("UPDATE")
                .attachment_hash("")
                .subject_application_id("ApplicationId")
                .subject_application_vendor("ApplicationVendor")
                .subject_application_version("ApplicationVersion")
                .use_subject_as_author(false)
                .build();
    }

    /**
     * Builds a valid {@link UpdateMetadataReqDTO} for AD 2.6.4.
     * {@code tipoDocumentoLivAlto = "REF"} pairs with {@code resource_hl7_type "11502-2"}
     * via CorrelationDocumentType264Validator.
     */
    private UpdateMetadataReqDTO buildUpdateMetadataReqDTO() {
        UpdateMetadataReqDTO dto = new UpdateMetadataReqDTO();
        dto.setTipologiaStruttura("Ospedale");
        dto.setAssettoOrganizzativo("AD_PSC001");
        dto.setTipoDocumentoLivAlto("REF");
        dto.setTipoAttivitaClinica("CON");
        dto.setAdministrativeRequest(Arrays.asList("SSN"));
        dto.setIdentificativoSottomissione("identificativoSottomissione");
        dto.setConservazioneANorma("Conservazione sostitutiva");
        dto.setDataInizioPrestazione("20240101000000");
        dto.setDataFinePrestazione("20240101235959");
        dto.setAttiCliniciRegoleAccesso(Arrays.asList("P99"));
        return dto;
    }

    @Test
    @DisplayName("Affinity Domain 2.6.4 Strategy Test")
    void affinityDomain264StrategyTest() {

        Ad264Strategy ad264 = new Ad264Strategy();

        // ── 1. null resource_hl7_type → CorrelationDocumentType264Validator receives
        //       null documentType → throws MetadataValidationException ──────────────
        UpdateMetadataReqDTO updateDTO = buildUpdateMetadataReqDTO();
        JWTPayloadDTO jwtNullType = buildJWTPayloadDTO(true);
        assertThrows(MetadataValidationException.class,
                () -> ad264.validateUpdateMetadataReqDTO(updateDTO, jwtNullType));

        // ── 2. null request → TipoDocAltoLivAd264Enum.valueOf(null) → NullPointerException
        //       (correlation check runs before the null-guard on request) ────────────
        JWTPayloadDTO jwtValid = buildJWTPayloadDTO(false);
        assertThrows(NullPointerException.class,
                () -> ad264.validateUpdateMetadataReqDTO(null, jwtValid));

        // ── 3. LOINC / tipoDocumentoLivAlto mismatch:
        //       "57833-6" → CODE_57833_6 → maps to PRS, not REF → throws ────────────
        JWTPayloadDTO jwtMismatch = JWTPayloadDTO.builder()
                .resource_hl7_type("('57833-6^^2.16.840.1.113883.6.1')")
                .build();
        assertThrows(MetadataValidationException.class,
                () -> ad264.validateUpdateMetadataReqDTO(buildUpdateMetadataReqDTO(), jwtMismatch));

        // ── 4. valid request + valid JWT → success ────────────────────────────────
        ValidationResultDTO result = ad264.validateUpdateMetadataReqDTO(buildUpdateMetadataReqDTO(), jwtValid);
        assertTrue(result.isValid());
        assertEquals("2.6.4", result.getAdVersion());
        assertTrue(result.getMissingFields().isEmpty());

        // ── 5. invalid tipologiaStruttura → value-set error ───────────────────────
        UpdateMetadataReqDTO dtoInvalidStruttura = buildUpdateMetadataReqDTO();
        dtoInvalidStruttura.setTipologiaStruttura("INVALID_STRUTTURA");
        ValidationResultDTO resultStruttura = ad264.validateUpdateMetadataReqDTO(dtoInvalidStruttura, jwtValid);
        assertFalse(resultStruttura.isValid());
        assertEquals("INVALID_VALUE_SET", resultStruttura.getErrorCode());

        // ── 6. invalid assettoOrganizzativo → value-set error ─────────────────────
        UpdateMetadataReqDTO dtoInvalidAssetto = buildUpdateMetadataReqDTO();
        dtoInvalidAssetto.setAssettoOrganizzativo("INVALID_ASSETTO");
        ValidationResultDTO resultAssetto = ad264.validateUpdateMetadataReqDTO(dtoInvalidAssetto, jwtValid);
        assertFalse(resultAssetto.isValid());
        assertEquals("INVALID_VALUE_SET", resultAssetto.getErrorCode());

        // ── 7. invalid tipoAttivitaClinica → value-set error ──────────────────────
        UpdateMetadataReqDTO dtoInvalidAttivita = buildUpdateMetadataReqDTO();
        dtoInvalidAttivita.setTipoAttivitaClinica("INVALID_ATTIVITA");
        ValidationResultDTO resultAttivita = ad264.validateUpdateMetadataReqDTO(dtoInvalidAttivita, jwtValid);
        assertFalse(resultAttivita.isValid());
        assertEquals("INVALID_VALUE_SET", resultAttivita.getErrorCode());

        // ── 8. invalid administrativeRequest → value-set error ────────────────────
        UpdateMetadataReqDTO dtoInvalidAdmin = buildUpdateMetadataReqDTO();
        dtoInvalidAdmin.setAdministrativeRequest(Arrays.asList("INVALID_CODE"));
        ValidationResultDTO resultAdmin = ad264.validateUpdateMetadataReqDTO(dtoInvalidAdmin, jwtValid);
        assertFalse(resultAdmin.isValid());
        assertEquals("INVALID_VALUE_SET", resultAdmin.getErrorCode());

        // ── 9. multiple invalid fields → all errors accumulate ────────────────────
        UpdateMetadataReqDTO dtoMultipleErrors = buildUpdateMetadataReqDTO();
        dtoMultipleErrors.setTipologiaStruttura("INVALID_STRUTTURA");
        dtoMultipleErrors.setAssettoOrganizzativo("INVALID_ASSETTO");
        dtoMultipleErrors.setTipoAttivitaClinica("INVALID_ATTIVITA");
        ValidationResultDTO resultMultiple = ad264.validateUpdateMetadataReqDTO(dtoMultipleErrors, jwtValid);
        assertFalse(resultMultiple.isValid());
        assertTrue(resultMultiple.getMissingFields().size() >= 3);
    }

    @Test
    @DisplayName("Affinity Domain 2.6.3 Strategy Test")
    void affinityDomain263StrategyTest() {

        Ad263Strategy ad263 = new Ad263Strategy();

        // ── 1. null resource_hl7_type → CorrelationDocumentType264Validator receives
        //       null documentType → throws MetadataValidationException ──────────────
        UpdateMetadataReqDTO updateDTO = buildUpdateMetadataReqDTO();
        JWTPayloadDTO jwtNullType = buildJWTPayloadDTO(true);
        assertThrows(MetadataValidationException.class,
                () -> ad263.validateUpdateMetadataReqDTO(updateDTO, jwtNullType));

        // ── 2. null request → TipoDocAltoLivAd264Enum.valueOf(null) → NullPointerException
        //       (correlation check runs before the null-guard on request) ────────────
        JWTPayloadDTO jwtValid = buildJWTPayloadDTO(false);
        assertThrows(NullPointerException.class,
                () -> ad263.validateUpdateMetadataReqDTO(null, jwtValid));

        // ── 3. LOINC / tipoDocumentoLivAlto mismatch:
        //       "57833-6" → CODE_57833_6 → maps to PRS, not REF → throws ────────────
        JWTPayloadDTO jwtMismatch = JWTPayloadDTO.builder()
                .resource_hl7_type("('57833-6^^2.16.840.1.113883.6.1')")
                .build();
        assertThrows(MetadataValidationException.class,
                () -> ad263.validateUpdateMetadataReqDTO(buildUpdateMetadataReqDTO(), jwtMismatch));

        // ── 4. valid request + valid JWT → success ────────────────────────────────
        ValidationResultDTO result = ad263.validateUpdateMetadataReqDTO(buildUpdateMetadataReqDTO(), jwtValid);
        assertTrue(result.isValid());
        assertEquals("2.6.3", result.getAdVersion());
        assertTrue(result.getMissingFields().isEmpty());

        // ── 5. invalid tipologiaStruttura → value-set error ───────────────────────
        UpdateMetadataReqDTO dtoInvalidStruttura = buildUpdateMetadataReqDTO();
        dtoInvalidStruttura.setTipologiaStruttura("INVALID_STRUTTURA");
        ValidationResultDTO resultStruttura = ad263.validateUpdateMetadataReqDTO(dtoInvalidStruttura, jwtValid);
        assertFalse(resultStruttura.isValid());
        assertEquals("INVALID_VALUE_SET", resultStruttura.getErrorCode());

        // ── 6. invalid assettoOrganizzativo → value-set error ─────────────────────
        UpdateMetadataReqDTO dtoInvalidAssetto = buildUpdateMetadataReqDTO();
        dtoInvalidAssetto.setAssettoOrganizzativo("INVALID_ASSETTO");
        ValidationResultDTO resultAssetto = ad263.validateUpdateMetadataReqDTO(dtoInvalidAssetto, jwtValid);
        assertFalse(resultAssetto.isValid());
        assertEquals("INVALID_VALUE_SET", resultAssetto.getErrorCode());

        // ── 7. invalid tipoAttivitaClinica → value-set error ──────────────────────
        UpdateMetadataReqDTO dtoInvalidAttivita = buildUpdateMetadataReqDTO();
        dtoInvalidAttivita.setTipoAttivitaClinica("INVALID_ATTIVITA");
        ValidationResultDTO resultAttivita = ad263.validateUpdateMetadataReqDTO(dtoInvalidAttivita, jwtValid);
        assertFalse(resultAttivita.isValid());
        assertEquals("INVALID_VALUE_SET", resultAttivita.getErrorCode());

        // ── 8. invalid administrativeRequest → value-set error ────────────────────
        UpdateMetadataReqDTO dtoInvalidAdmin = buildUpdateMetadataReqDTO();
        dtoInvalidAdmin.setAdministrativeRequest(Arrays.asList("INVALID_CODE"));
        ValidationResultDTO resultAdmin = ad263.validateUpdateMetadataReqDTO(dtoInvalidAdmin, jwtValid);
        assertFalse(resultAdmin.isValid());
        assertEquals("INVALID_VALUE_SET", resultAdmin.getErrorCode());

        // ── 9. multiple invalid fields → all errors accumulate ────────────────────
        UpdateMetadataReqDTO dtoMultipleErrors = buildUpdateMetadataReqDTO();
        dtoMultipleErrors.setTipologiaStruttura("INVALID_STRUTTURA");
        dtoMultipleErrors.setAssettoOrganizzativo("INVALID_ASSETTO");
        dtoMultipleErrors.setTipoAttivitaClinica("INVALID_ATTIVITA");
        ValidationResultDTO resultMultiple = ad263.validateUpdateMetadataReqDTO(dtoMultipleErrors, jwtValid);
        assertFalse(resultMultiple.isValid());
        assertTrue(resultMultiple.getMissingFields().size() >= 3);
    }

    @Test
    @DisplayName("Affinity Domain 2.6.2 Strategy Test")
    void affinityDomain262StrategyTest(){

        Ad262Strategy ad262 = new Ad262Strategy();

        // ── 1. valid request + valid JWT → success ────────────────────────────────
        JWTPayloadDTO jwtValid = buildJWTPayloadDTO(false);
        ValidationResultDTO result = ad262.validateUpdateMetadataReqDTO(buildUpdateMetadataReqDTO(), jwtValid);
        assertTrue(result.isValid());
        assertEquals("2.6.2", result.getAdVersion());
        assertTrue(result.getMissingFields().isEmpty());

        // ── 2. invalid tipologiaStruttura → value-set error ───────────────────────
        UpdateMetadataReqDTO dtoInvalidStruttura = buildUpdateMetadataReqDTO();
        dtoInvalidStruttura.setTipologiaStruttura("INVALID_STRUTTURA");
        ValidationResultDTO resultStruttura = ad262.validateUpdateMetadataReqDTO(dtoInvalidStruttura, jwtValid);
        assertFalse(resultStruttura.isValid());
        assertEquals("INVALID_VALUE_SET", resultStruttura.getErrorCode());

         // ── 3. invalid assettoOrganizzativo → value-set error ─────────────────────
        UpdateMetadataReqDTO dtoInvalidAssetto = buildUpdateMetadataReqDTO();
        dtoInvalidAssetto.setAssettoOrganizzativo("INVALID_ASSETTO");
        ValidationResultDTO resultAssetto = ad262.validateUpdateMetadataReqDTO(dtoInvalidAssetto, jwtValid);
        assertFalse(resultAssetto.isValid());
        assertEquals("INVALID_VALUE_SET", resultAssetto.getErrorCode());

        // ── 4. invalid tipoAttivitaClinica → value-set error ──────────────────────
        UpdateMetadataReqDTO dtoInvalidAttivita = buildUpdateMetadataReqDTO();
        dtoInvalidAttivita.setTipoAttivitaClinica("INVALID_ATTIVITA");
        ValidationResultDTO resultAttivita = ad262.validateUpdateMetadataReqDTO(dtoInvalidAttivita, jwtValid);
        assertFalse(resultAttivita.isValid());
        assertEquals("INVALID_VALUE_SET", resultAttivita.getErrorCode());

        // ── 5. invalid administrativeRequest → value-set error ────────────────────
        UpdateMetadataReqDTO dtoInvalidAdmin = buildUpdateMetadataReqDTO();
        dtoInvalidAdmin.setAdministrativeRequest(Arrays.asList("INVALID_CODE"));
        ValidationResultDTO resultAdmin = ad262.validateUpdateMetadataReqDTO(dtoInvalidAdmin, jwtValid);
        assertFalse(resultAdmin.isValid());
        assertEquals("INVALID_VALUE_SET", resultAdmin.getErrorCode());

        // ── 6. invalid tipoDocumentoLivAlto → value-set error ────────────────────
        UpdateMetadataReqDTO dtoInvalidTipoDoc = buildUpdateMetadataReqDTO();
        dtoInvalidTipoDoc.setTipoDocumentoLivAlto("INVALID_TIPO_DOC");
        ValidationResultDTO resultTipoDoc = ad262.validateUpdateMetadataReqDTO(dtoInvalidTipoDoc, jwtValid);
        assertFalse(resultTipoDoc.isValid());
        assertEquals("INVALID_VALUE_SET", resultTipoDoc.getErrorCode());

        // ── 7. multiple invalid fields → all errors accumulate ────────────────────
        UpdateMetadataReqDTO dtoMultipleErrors = buildUpdateMetadataReqDTO();
        dtoMultipleErrors.setTipologiaStruttura("INVALID_STRUTTURA");
        dtoMultipleErrors.setAssettoOrganizzativo("INVALID_ASSETTO");
        dtoMultipleErrors.setTipoAttivitaClinica("INVALID_ATTIVITA");
        ValidationResultDTO resultMultiple = ad262.validateUpdateMetadataReqDTO(dtoMultipleErrors, jwtValid);
        assertFalse(resultMultiple.isValid());
        assertTrue(resultMultiple.getMissingFields().size() >= 3);

        
    }

    @Test
    @DisplayName("Affinity Domain 2.6.1 Strategy Test")
    void affinityDomain261StrategyTest(){

        Ad261Strategy ad261 = new Ad261Strategy();

         // ── 1. valid request + valid JWT → success ────────────────────────────────
        JWTPayloadDTO jwtValid = buildJWTPayloadDTO(false);
        ValidationResultDTO result = ad261.validateUpdateMetadataReqDTO(buildUpdateMetadataReqDTO(), jwtValid);
        assertTrue(result.isValid());
        assertEquals("2.6.1", result.getAdVersion());
        assertTrue(result.getMissingFields().isEmpty());

         // ── 2. invalid tipologiaStruttura → value-set error ───────────────────────
        UpdateMetadataReqDTO dtoInvalidStruttura = buildUpdateMetadataReqDTO();
        dtoInvalidStruttura.setTipologiaStruttura("INVALID_STRUTTURA");
        ValidationResultDTO resultStruttura = ad261.validateUpdateMetadataReqDTO(dtoInvalidStruttura, jwtValid);
        assertFalse(resultStruttura.isValid());
        assertEquals("INVALID_VALUE_SET", resultStruttura.getErrorCode());

         // ── 3. invalid assettoOrganizzativo → value-set error ─────────────────────
        UpdateMetadataReqDTO dtoInvalidAssetto = buildUpdateMetadataReqDTO();
        dtoInvalidAssetto.setAssettoOrganizzativo("INVALID_ASSETTO");
        ValidationResultDTO resultAssetto = ad261.validateUpdateMetadataReqDTO(dtoInvalidAssetto, jwtValid);
        assertFalse(resultAssetto.isValid());
        assertEquals("INVALID_VALUE_SET", resultAssetto.getErrorCode());

        // ── 4. invalid tipoAttivitaClinica → value-set error ──────────────────────
        UpdateMetadataReqDTO dtoInvalidAttivita = buildUpdateMetadataReqDTO();
        dtoInvalidAttivita.setTipoAttivitaClinica("INVALID_ATTIVITA");
        ValidationResultDTO resultAttivita = ad261.validateUpdateMetadataReqDTO(dtoInvalidAttivita, jwtValid);
        assertFalse(resultAttivita.isValid());
        assertEquals("INVALID_VALUE_SET", resultAttivita.getErrorCode());

        // ── 5. invalid administrativeRequest → value-set error ────────────────────
        UpdateMetadataReqDTO dtoInvalidAdmin = buildUpdateMetadataReqDTO();
        dtoInvalidAdmin.setAdministrativeRequest(Arrays.asList("INVALID_CODE"));
        ValidationResultDTO resultAdmin = ad261.validateUpdateMetadataReqDTO(dtoInvalidAdmin, jwtValid);
        assertFalse(resultAdmin.isValid());
        assertEquals("INVALID_VALUE_SET", resultAdmin.getErrorCode());

        // ── 6. invalid tipoDocumentoLivAlto → value-set error ────────────────────
        UpdateMetadataReqDTO dtoInvalidTipoDoc = buildUpdateMetadataReqDTO();
        dtoInvalidTipoDoc.setTipoDocumentoLivAlto("INVALID_TIPO_DOC");
        ValidationResultDTO resultTipoDoc = ad261.validateUpdateMetadataReqDTO(dtoInvalidTipoDoc, jwtValid);
        assertFalse(resultTipoDoc.isValid());
        assertEquals("INVALID_VALUE_SET", resultTipoDoc.getErrorCode());

        // ── 7. multiple invalid fields → all errors accumulate ────────────────────
        UpdateMetadataReqDTO dtoMultipleErrors = buildUpdateMetadataReqDTO();
        dtoMultipleErrors.setTipologiaStruttura("INVALID_STRUTTURA");
        dtoMultipleErrors.setAssettoOrganizzativo("INVALID_ASSETTO");
        dtoMultipleErrors.setTipoAttivitaClinica("INVALID_ATTIVITA");
        ValidationResultDTO resultMultiple = ad261.validateUpdateMetadataReqDTO(dtoMultipleErrors, jwtValid);
        assertFalse(resultMultiple.isValid());
        assertTrue(resultMultiple.getMissingFields().size() >= 3);

    }

    @Test 
    @DisplayName("Affinity Domain 2.4.1 Strategy Test")
    void affinityDomain241StrategyTest(){

        Ad241Strategy ad241 = new Ad241Strategy();

        // ── 1. valid request + valid JWT → success ────────────────────────────────
        JWTPayloadDTO jwtValid = buildJWTPayloadDTO(false);
        ValidationResultDTO result = ad241.validateUpdateMetadataReqDTO(buildUpdateMetadataReqDTO(), jwtValid);
        assertTrue(result.isValid());
        assertEquals("2.4.1", result.getAdVersion());
        assertTrue(result.getMissingFields().isEmpty());

         // ── 2. invalid tipologiaStruttura → value-set error ───────────────────────
        UpdateMetadataReqDTO dtoInvalidStruttura = buildUpdateMetadataReqDTO();
        dtoInvalidStruttura.setTipologiaStruttura("INVALID_STRUTTURA");
        ValidationResultDTO resultStruttura = ad241.validateUpdateMetadataReqDTO(dtoInvalidStruttura, jwtValid);
        assertFalse(resultStruttura.isValid());
        assertEquals("INVALID_VALUE_SET", resultStruttura.getErrorCode());

         // ── 3. invalid assettoOrganizzativo → value-set error ─────────────────────
        UpdateMetadataReqDTO dtoInvalidAssetto = buildUpdateMetadataReqDTO();
        dtoInvalidAssetto.setAssettoOrganizzativo("INVALID_ASSETTO");
        ValidationResultDTO resultAssetto = ad241.validateUpdateMetadataReqDTO(dtoInvalidAssetto, jwtValid);
        assertFalse(resultAssetto.isValid());
        assertEquals("INVALID_VALUE_SET", resultAssetto.getErrorCode());

        // ── 4. invalid tipoAttivitaClinica → value-set error ──────────────────────
        UpdateMetadataReqDTO dtoInvalidAttivita = buildUpdateMetadataReqDTO();
        dtoInvalidAttivita.setTipoAttivitaClinica("INVALID_ATTIVITA");
        ValidationResultDTO resultAttivita = ad241.validateUpdateMetadataReqDTO(dtoInvalidAttivita, jwtValid);
        assertFalse(resultAttivita.isValid());
        assertEquals("INVALID_VALUE_SET", resultAttivita.getErrorCode());

        // ── 5. invalid administrativeRequest → value-set error ────────────────────
        UpdateMetadataReqDTO dtoInvalidAdmin = buildUpdateMetadataReqDTO();
        dtoInvalidAdmin.setAdministrativeRequest(Arrays.asList("INVALID_CODE"));
        ValidationResultDTO resultAdmin = ad241.validateUpdateMetadataReqDTO(dtoInvalidAdmin, jwtValid);
        assertFalse(resultAdmin.isValid());
        assertEquals("INVALID_VALUE_SET", resultAdmin.getErrorCode());

        // ── 6. invalid tipoDocumentoLivAlto → value-set error ────────────────────
        UpdateMetadataReqDTO dtoInvalidTipoDoc = buildUpdateMetadataReqDTO();
        dtoInvalidTipoDoc.setTipoDocumentoLivAlto("INVALID_TIPO_DOC");
        ValidationResultDTO resultTipoDoc = ad241.validateUpdateMetadataReqDTO(dtoInvalidTipoDoc, jwtValid);
        assertFalse(resultTipoDoc.isValid());
        assertEquals("INVALID_VALUE_SET", resultTipoDoc.getErrorCode());

        // ── 7. multiple invalid fields → all errors accumulate ────────────────────
        UpdateMetadataReqDTO dtoMultipleErrors = buildUpdateMetadataReqDTO();
        dtoMultipleErrors.setTipologiaStruttura("INVALID_STRUTTURA");
        dtoMultipleErrors.setAssettoOrganizzativo("INVALID_ASSETTO");
        dtoMultipleErrors.setTipoAttivitaClinica("INVALID_ATTIVITA");
        ValidationResultDTO resultMultiple = ad241.validateUpdateMetadataReqDTO(dtoMultipleErrors, jwtValid);
        assertFalse(resultMultiple.isValid());
        assertTrue(resultMultiple.getMissingFields().size() >= 3);
    }

    @Test 
    @DisplayName("Affinity Domain 2.6 Strategy Test")
    void affinityDomain26StrategyTest(){

        Ad26Strategy ad26 = new Ad26Strategy();

        // ── 1. valid request + valid JWT → success ────────────────────────────────
        JWTPayloadDTO jwtValid = buildJWTPayloadDTO(false);
        ValidationResultDTO result = ad26.validateUpdateMetadataReqDTO(buildUpdateMetadataReqDTO(), jwtValid);
        assertTrue(result.isValid());
        assertEquals("2.6", result.getAdVersion());
        assertTrue(result.getMissingFields().isEmpty());

         // ── 2. invalid tipologiaStruttura → value-set error ───────────────────────
        UpdateMetadataReqDTO dtoInvalidStruttura = buildUpdateMetadataReqDTO();
        dtoInvalidStruttura.setTipologiaStruttura("INVALID_STRUTTURA");
        ValidationResultDTO resultStruttura = ad26.validateUpdateMetadataReqDTO(dtoInvalidStruttura, jwtValid);
        assertFalse(resultStruttura.isValid());
        assertEquals("INVALID_VALUE_SET", resultStruttura.getErrorCode());

         // ── 3. invalid assettoOrganizzativo → value-set error ─────────────────────
        UpdateMetadataReqDTO dtoInvalidAssetto = buildUpdateMetadataReqDTO();
        dtoInvalidAssetto.setAssettoOrganizzativo("INVALID_ASSETTO");
        ValidationResultDTO resultAssetto = ad26.validateUpdateMetadataReqDTO(dtoInvalidAssetto, jwtValid);
        assertFalse(resultAssetto.isValid());
        assertEquals("INVALID_VALUE_SET", resultAssetto.getErrorCode());

        // ── 4. invalid tipoAttivitaClinica → value-set error ──────────────────────
        UpdateMetadataReqDTO dtoInvalidAttivita = buildUpdateMetadataReqDTO();
        dtoInvalidAttivita.setTipoAttivitaClinica("INVALID_ATTIVITA");
        ValidationResultDTO resultAttivita = ad26.validateUpdateMetadataReqDTO(dtoInvalidAttivita, jwtValid);
        assertFalse(resultAttivita.isValid());
        assertEquals("INVALID_VALUE_SET", resultAttivita.getErrorCode());

        // ── 5. invalid administrativeRequest → value-set error ────────────────────
        UpdateMetadataReqDTO dtoInvalidAdmin = buildUpdateMetadataReqDTO();
        dtoInvalidAdmin.setAdministrativeRequest(Arrays.asList("INVALID_CODE"));
        ValidationResultDTO resultAdmin = ad26.validateUpdateMetadataReqDTO(dtoInvalidAdmin, jwtValid);
        assertFalse(resultAdmin.isValid());
        assertEquals("INVALID_VALUE_SET", resultAdmin.getErrorCode());

        // ── 6. invalid tipoDocumentoLivAlto → value-set error ────────────────────
        UpdateMetadataReqDTO dtoInvalidTipoDoc = buildUpdateMetadataReqDTO();
        dtoInvalidTipoDoc.setTipoDocumentoLivAlto("INVALID_TIPO_DOC");
        ValidationResultDTO resultTipoDoc = ad26.validateUpdateMetadataReqDTO(dtoInvalidTipoDoc, jwtValid);
        assertFalse(resultTipoDoc.isValid());
        assertEquals("INVALID_VALUE_SET", resultTipoDoc.getErrorCode());

        // ── 7. multiple invalid fields → all errors accumulate ────────────────────
        UpdateMetadataReqDTO dtoMultipleErrors = buildUpdateMetadataReqDTO();
        dtoMultipleErrors.setTipologiaStruttura("INVALID_STRUTTURA");
        dtoMultipleErrors.setAssettoOrganizzativo("INVALID_ASSETTO");
        dtoMultipleErrors.setTipoAttivitaClinica("INVALID_ATTIVITA");
        ValidationResultDTO resultMultiple = ad26.validateUpdateMetadataReqDTO(dtoMultipleErrors, jwtValid);
        assertFalse(resultMultiple.isValid());
        assertTrue(resultMultiple.getMissingFields().size() >= 3);
    }

    @Test 
    @DisplayName("Affinity Domain 2.5 Strategy Test")
    void affinityDomain25StrategyTest(){

        Ad25Strategy ad25 = new Ad25Strategy();

        // ── 1. valid request + valid JWT → success ────────────────────────────────
        JWTPayloadDTO jwtValid = buildJWTPayloadDTO(false);
        ValidationResultDTO result = ad25.validateUpdateMetadataReqDTO(buildUpdateMetadataReqDTO(), jwtValid);
        assertTrue(result.isValid());
        assertEquals("2.5", result.getAdVersion());
        assertTrue(result.getMissingFields().isEmpty());

         // ── 2. invalid tipologiaStruttura → value-set error ───────────────────────
        UpdateMetadataReqDTO dtoInvalidStruttura = buildUpdateMetadataReqDTO();
        dtoInvalidStruttura.setTipologiaStruttura("INVALID_STRUTTURA");
        ValidationResultDTO resultStruttura = ad25.validateUpdateMetadataReqDTO(dtoInvalidStruttura, jwtValid);
        assertFalse(resultStruttura.isValid());
        assertEquals("INVALID_VALUE_SET", resultStruttura.getErrorCode());

         // ── 3. invalid assettoOrganizzativo → value-set error ─────────────────────
        UpdateMetadataReqDTO dtoInvalidAssetto = buildUpdateMetadataReqDTO();
        dtoInvalidAssetto.setAssettoOrganizzativo("INVALID_ASSETTO");
        ValidationResultDTO resultAssetto = ad25.validateUpdateMetadataReqDTO(dtoInvalidAssetto, jwtValid);
        assertFalse(resultAssetto.isValid());
        assertEquals("INVALID_VALUE_SET", resultAssetto.getErrorCode());

        // ── 4. invalid tipoAttivitaClinica → value-set error ──────────────────────
        UpdateMetadataReqDTO dtoInvalidAttivita = buildUpdateMetadataReqDTO();
        dtoInvalidAttivita.setTipoAttivitaClinica("INVALID_ATTIVITA");
        ValidationResultDTO resultAttivita = ad25.validateUpdateMetadataReqDTO(dtoInvalidAttivita, jwtValid);
        assertFalse(resultAttivita.isValid());
        assertEquals("INVALID_VALUE_SET", resultAttivita.getErrorCode());

        // ── 5. invalid administrativeRequest → value-set error ────────────────────
        UpdateMetadataReqDTO dtoInvalidAdmin = buildUpdateMetadataReqDTO();
        dtoInvalidAdmin.setAdministrativeRequest(Arrays.asList("INVALID_CODE"));
        ValidationResultDTO resultAdmin = ad25.validateUpdateMetadataReqDTO(dtoInvalidAdmin, jwtValid);
        assertFalse(resultAdmin.isValid());
        assertEquals("INVALID_VALUE_SET", resultAdmin.getErrorCode());

        // ── 6. invalid tipoDocumentoLivAlto → value-set error ────────────────────
        UpdateMetadataReqDTO dtoInvalidTipoDoc = buildUpdateMetadataReqDTO();
        dtoInvalidTipoDoc.setTipoDocumentoLivAlto("INVALID_TIPO_DOC");
        ValidationResultDTO resultTipoDoc = ad25.validateUpdateMetadataReqDTO(dtoInvalidTipoDoc, jwtValid);
        assertFalse(resultTipoDoc.isValid());
        assertEquals("INVALID_VALUE_SET", resultTipoDoc.getErrorCode());

        // ── 7. multiple invalid fields → all errors accumulate ────────────────────
        UpdateMetadataReqDTO dtoMultipleErrors = buildUpdateMetadataReqDTO();
        dtoMultipleErrors.setTipologiaStruttura("INVALID_STRUTTURA");
        dtoMultipleErrors.setAssettoOrganizzativo("INVALID_ASSETTO");
        dtoMultipleErrors.setTipoAttivitaClinica("INVALID_ATTIVITA");
        ValidationResultDTO resultMultiple = ad25.validateUpdateMetadataReqDTO(dtoMultipleErrors, jwtValid);
        assertFalse(resultMultiple.isValid());
        assertTrue(resultMultiple.getMissingFields().size() >= 3);
    }

    @Test 
    @DisplayName("Affinity Domain 2.4 Strategy Test")
    void affinityDomain24StrategyTest(){

        Ad24Strategy ad24 = new Ad24Strategy();

        // ── 1. valid request + valid JWT → success ────────────────────────────────
        JWTPayloadDTO jwtValid = buildJWTPayloadDTO(false);
        ValidationResultDTO result = ad24.validateUpdateMetadataReqDTO(buildUpdateMetadataReqDTO(), jwtValid);
        assertTrue(result.isValid());
        assertEquals("2.4", result.getAdVersion());
        assertTrue(result.getMissingFields().isEmpty());

         // ── 2. invalid tipologiaStruttura → value-set error ───────────────────────
        UpdateMetadataReqDTO dtoInvalidStruttura = buildUpdateMetadataReqDTO();
        dtoInvalidStruttura.setTipologiaStruttura("INVALID_STRUTTURA");
        ValidationResultDTO resultStruttura = ad24.validateUpdateMetadataReqDTO(dtoInvalidStruttura, jwtValid);
        assertFalse(resultStruttura.isValid());
        assertEquals("INVALID_VALUE_SET", resultStruttura.getErrorCode());

         // ── 3. invalid assettoOrganizzativo → value-set error ─────────────────────
        UpdateMetadataReqDTO dtoInvalidAssetto = buildUpdateMetadataReqDTO();
        dtoInvalidAssetto.setAssettoOrganizzativo("INVALID_ASSETTO");
        ValidationResultDTO resultAssetto = ad24.validateUpdateMetadataReqDTO(dtoInvalidAssetto, jwtValid);
        assertFalse(resultAssetto.isValid());
        assertEquals("INVALID_VALUE_SET", resultAssetto.getErrorCode());

        // ── 4. invalid tipoAttivitaClinica → value-set error ──────────────────────
        UpdateMetadataReqDTO dtoInvalidAttivita = buildUpdateMetadataReqDTO();
        dtoInvalidAttivita.setTipoAttivitaClinica("INVALID_ATTIVITA");
        ValidationResultDTO resultAttivita = ad24.validateUpdateMetadataReqDTO(dtoInvalidAttivita, jwtValid);
        assertFalse(resultAttivita.isValid());
        assertEquals("INVALID_VALUE_SET", resultAttivita.getErrorCode());

        // ── 5. invalid administrativeRequest → value-set error ────────────────────
        UpdateMetadataReqDTO dtoInvalidAdmin = buildUpdateMetadataReqDTO();
        dtoInvalidAdmin.setAdministrativeRequest(Arrays.asList("INVALID_CODE"));
        ValidationResultDTO resultAdmin = ad24.validateUpdateMetadataReqDTO(dtoInvalidAdmin, jwtValid);
        assertFalse(resultAdmin.isValid());
        assertEquals("INVALID_VALUE_SET", resultAdmin.getErrorCode());

        // ── 6. invalid tipoDocumentoLivAlto → value-set error ────────────────────
        UpdateMetadataReqDTO dtoInvalidTipoDoc = buildUpdateMetadataReqDTO();
        dtoInvalidTipoDoc.setTipoDocumentoLivAlto("INVALID_TIPO_DOC");
        ValidationResultDTO resultTipoDoc = ad24.validateUpdateMetadataReqDTO(dtoInvalidTipoDoc, jwtValid);
        assertFalse(resultTipoDoc.isValid());
        assertEquals("INVALID_VALUE_SET", resultTipoDoc.getErrorCode());

        // ── 7. multiple invalid fields → all errors accumulate ────────────────────
        UpdateMetadataReqDTO dtoMultipleErrors = buildUpdateMetadataReqDTO();
        dtoMultipleErrors.setTipologiaStruttura("INVALID_STRUTTURA");
        dtoMultipleErrors.setAssettoOrganizzativo("INVALID_ASSETTO");
        dtoMultipleErrors.setTipoAttivitaClinica("INVALID_ATTIVITA");
        ValidationResultDTO resultMultiple = ad24.validateUpdateMetadataReqDTO(dtoMultipleErrors, jwtValid);
        assertFalse(resultMultiple.isValid());
        assertTrue(resultMultiple.getMissingFields().size() >= 3);
    }

    @Test 
    @DisplayName("Affinity Domain 2.1 Strategy Test")
    void affinityDomain21StrategyTest(){

        Ad21Strategy ad21 = new Ad21Strategy();

        // ── 1. valid request + valid JWT → success ────────────────────────────────
        JWTPayloadDTO jwtValid = buildJWTPayloadDTO(false);
        UpdateMetadataReqDTO validBody = buildUpdateMetadataReqDTO();
        validBody.setAdministrativeRequest(null);
        ValidationResultDTO result = ad21.validateUpdateMetadataReqDTO(validBody, jwtValid);
        assertTrue(result.isValid());
        assertEquals("2.1", result.getAdVersion());
        assertTrue(result.getMissingFields().isEmpty());

         // ── 2. invalid tipologiaStruttura → value-set error ───────────────────────
        UpdateMetadataReqDTO dtoInvalidStruttura = buildUpdateMetadataReqDTO();
        dtoInvalidStruttura.setTipologiaStruttura("INVALID_STRUTTURA");
        dtoInvalidStruttura.setAdministrativeRequest(null);
        ValidationResultDTO resultStruttura = ad21.validateUpdateMetadataReqDTO(dtoInvalidStruttura, jwtValid);
        assertFalse(resultStruttura.isValid());
        assertEquals("INVALID_VALUE_SET", resultStruttura.getErrorCode());

         // ── 3. invalid assettoOrganizzativo → value-set error ─────────────────────
        UpdateMetadataReqDTO dtoInvalidAssetto = buildUpdateMetadataReqDTO();
        dtoInvalidAssetto.setAssettoOrganizzativo("INVALID_ASSETTO");
        dtoInvalidAssetto.setAdministrativeRequest(null);
        ValidationResultDTO resultAssetto = ad21.validateUpdateMetadataReqDTO(dtoInvalidAssetto, jwtValid);
        assertFalse(resultAssetto.isValid());
        assertEquals("INVALID_VALUE_SET", resultAssetto.getErrorCode());

        // ── 4. invalid tipoAttivitaClinica → value-set error ──────────────────────
        UpdateMetadataReqDTO dtoInvalidAttivita = buildUpdateMetadataReqDTO();
        dtoInvalidAttivita.setTipoAttivitaClinica("INVALID_ATTIVITA");
        dtoInvalidAttivita.setAdministrativeRequest(null);
        ValidationResultDTO resultAttivita = ad21.validateUpdateMetadataReqDTO(dtoInvalidAttivita, jwtValid);
        assertFalse(resultAttivita.isValid());
        assertEquals("INVALID_VALUE_SET", resultAttivita.getErrorCode());

        // ── 5. invalid tipoDocumentoLivAlto → value-set error ────────────────────
        UpdateMetadataReqDTO dtoInvalidTipoDoc = buildUpdateMetadataReqDTO();
        dtoInvalidTipoDoc.setTipoDocumentoLivAlto("INVALID_TIPO_DOC");
        dtoInvalidTipoDoc.setAdministrativeRequest(null);
        ValidationResultDTO resultTipoDoc = ad21.validateUpdateMetadataReqDTO(dtoInvalidTipoDoc, jwtValid);
        assertFalse(resultTipoDoc.isValid());
        assertEquals("INVALID_VALUE_SET", resultTipoDoc.getErrorCode());

        // ── 6. multiple invalid fields → all errors accumulate ────────────────────
        UpdateMetadataReqDTO dtoMultipleErrors = buildUpdateMetadataReqDTO();
        dtoMultipleErrors.setTipologiaStruttura("INVALID_STRUTTURA");
        dtoMultipleErrors.setAssettoOrganizzativo("INVALID_ASSETTO");
        dtoMultipleErrors.setTipoAttivitaClinica("INVALID_ATTIVITA");
        ValidationResultDTO resultMultiple = ad21.validateUpdateMetadataReqDTO(dtoMultipleErrors, jwtValid);
        assertFalse(resultMultiple.isValid());
        assertTrue(resultMultiple.getMissingFields().size() >= 3);
    }

}
