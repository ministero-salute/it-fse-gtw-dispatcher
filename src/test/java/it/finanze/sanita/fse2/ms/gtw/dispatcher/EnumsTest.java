/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * 
 * Copyright (C) 2023 Ministero della Salute
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package it.finanze.sanita.fse2.ms.gtw.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.AccreditamentoPrefixEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.ActionEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.ActivityEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.AdministrativeReqEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.AttivitaClinicaEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.CloudProviderEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.ConfidentialityCodeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.ConfigItemTypeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.DescriptionEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.DestinationTypeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.DirectFhirSourceEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.DocumentMetadataKeyEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.DocumentTypeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.EdsStrategyEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.ErrorInstanceEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.ErrorLogEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.EventCodeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.EventStatusEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.EventTypeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.HealthDataFormatEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.HealthcareFacilityEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.IniActivityEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.InjectionModeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.IssueSeverityEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.LowLevelDocEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.OperationLogEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.PracticeSettingCodeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.PriorityTypeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.ProcessorOperationEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.PurposeOfUseEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.RawValidationEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.RestExecutionResultEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.ResultLogEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.RoleEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.SignVerificationModeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.SubjectOrganizationEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.SystemTypeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.TipoDocAltoLivEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.UpdateFlowTypeEnum;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(Constants.Profile.TEST)
class EnumsTest {

    @Test
    @DisplayName("AssettoOrgEnum test")
    void testAssettoOrgEnums() {
        String code = "AD_PSC001";
        String description = "Allergologia";
        assertEquals(code, PracticeSettingCodeEnum.AD_PSC001.getCode());
        assertEquals(description, PracticeSettingCodeEnum.AD_PSC001.getDescription());
    }

    @Test
    @DisplayName("IniActivityEnums test")
    void testIniActivityEnums() {
        String code = "READ";
        String description = "Lettura";
        assertEquals(code, IniActivityEnum.READ.getCode());
        assertEquals(description, IniActivityEnum.READ.getDescription());
    }

    @Test
    @DisplayName("RegionCodeEnum test")
    void testRegionCodeEnums() {
        String code = "160";
        String description = "Regione Puglia";
        assertEquals(code, SubjectOrganizationEnum.REGIONE_PUGLIA.getCode());
        assertEquals(description, SubjectOrganizationEnum.REGIONE_PUGLIA.getDisplay());
    }

    @Test
    @DisplayName("RoleEnum test")
    void testRoleEnums() {
        String code = "DSA";
        String description = "Direttore sanitario";
        assertEquals(code, RoleEnum.DSA.getCode());
        assertEquals(description, RoleEnum.DSA.getDescription());
    }

    @Test
    @DisplayName("SignVerificationModeEnum test")
    void testSignVerificationModeEnums() {
        String code = "T";
        assertEquals(code, SignVerificationModeEnum.TODAY.getCode());
        assertEquals(SignVerificationModeEnum.TODAY, SignVerificationModeEnum.get(code));
    }

    @Test
    @DisplayName("RawValidationEnum test")
    void testRawValidationEnums() {
        String code = "00";
        String description = "OK";
        assertEquals(code, RawValidationEnum.OK.getCode());
        assertEquals(description, RawValidationEnum.OK.getDescription());
    }

    @Test
    @DisplayName("EventStatusEnum test")
    void testEventStatusEnums() {
        String name = "SUCCESS";
        assertEquals(name, EventStatusEnum.SUCCESS.getName());
    }

    @Test
    @DisplayName("InjectionModeEnum test")
    void testInjectionModeEnums() {
        String code = "A";
        assertEquals(code, InjectionModeEnum.ATTACHMENT.getCode());
    }

    @Test
    @DisplayName("HealthDataFormatEnum test")
    void testHealthDataFormatEnums() {
        String code = "V";
        assertEquals(code, HealthDataFormatEnum.CDA.getCode());
    }

    @Test
    @DisplayName("ResultLogEnum test")
    void testResultLogEnums() {
        String code = "OK";
        String description = "Operazione eseguita con successo";
        assertEquals(code, ResultLogEnum.OK.getCode());
        assertEquals(description, ResultLogEnum.OK.getDescription());
    }

    @Test
    @DisplayName("ActivityEnum test")
    void testActivityEnums() {
        String code = "V";
        assertEquals(code, ActivityEnum.VERIFICA.getCode());
    }

    @Test
    @DisplayName("EventTypeEnum test")
    void testEventTypeEnums() {
        String name = "VALIDATION";
        assertEquals(name, EventTypeEnum.VALIDATION.getName());
    }

    @Test
    @DisplayName("OperationLogEnum test")
    void testOperationLogEnums() {
        String code = "PUB-CDA2";
        String description = "Pubblicazione CDA2";
        assertEquals(description, OperationLogEnum.PUB_CDA2.getDescription());
        assertEquals(code, OperationLogEnum.PUB_CDA2.getCode());
    }

    @Test
    @DisplayName("EventCodeEnum test")
    void testEventCodeEnum() {
        String code = "P99";
        String description = "Oscuramento del documento";
        assertEquals(description, EventCodeEnum.P99.getDescription());
        assertEquals(code, EventCodeEnum.P99.getCode());
    } 

    @Test
    @DisplayName("RestExecutionResultEnum test")
    void testRestExecutionResultEnum() {
        String type = "00";
        String title = "Pubblicazione effettuata correttamente.";
        assertNull(RestExecutionResultEnum.OK.getErrorCategory());
        assertEquals(title, RestExecutionResultEnum.OK.getTitle());
        assertEquals(type, RestExecutionResultEnum.OK.getType());

        RawValidationEnum rawResult = RawValidationEnum.OK;
        assertEquals(rawResult.getCode(), RestExecutionResultEnum.fromRawResult(rawResult).getType());
        rawResult = RawValidationEnum.SEMANTIC_ERROR;
        assertEquals(rawResult.getDescription() + ".", RestExecutionResultEnum.fromRawResult(rawResult).getTitle());
        rawResult = RawValidationEnum.SYNTAX_ERROR;
        assertEquals(rawResult.getDescription() + ".", RestExecutionResultEnum.fromRawResult(rawResult).getTitle());
    }
    
    @Test
    @DisplayName("AccreditamentoPrefixEnum test")
    void testAccreditamentoPrefixEnum() {
        String prefix = "CRASH_TIMEOUT";
        assertEquals(prefix, AccreditamentoPrefixEnum.CRASH_TIMEOUT.getPrefix());
        assertEquals(AccreditamentoPrefixEnum.CRASH_TIMEOUT, AccreditamentoPrefixEnum.get(prefix));
        assertEquals(AccreditamentoPrefixEnum.CRASH_TIMEOUT, AccreditamentoPrefixEnum.getStartWith(prefix));
    }
    
    @Test
    @DisplayName("DescriptionEnum test")
    void testDescriptionEnum() {
        String oid = "2.16.840.1.113883.2.9.6.1.5";
        assertEquals(oid, DescriptionEnum.EROG_PRESC_FARMACEUTICA_AIC.getOid());
    }
    
    @Test
    @DisplayName("ErrorInstanceEnum test")
    void testErrorInstanceEnum() {
        String instance = "";
        assertEquals(instance, ErrorInstanceEnum.NO_INFO.getInstance());
        assertEquals(ErrorInstanceEnum.NO_INFO, ErrorInstanceEnum.get(instance));
    }
    
    @Test
    @DisplayName("SystemTypeEnum test")
    void testSystemTypeEnum() {
        String name = "TS";
        assertEquals(name, SystemTypeEnum.TS.value());
    }

    @Test
    @DisplayName("ActionEnum test")
    void testActionEnum() {
        assertEquals(ActionEnum.CREATE, ActionEnum.get("CREATE"));
        assertEquals(ActionEnum.DELETE, ActionEnum.get("DELETE"));
        assertNull(ActionEnum.get("UNKNOWN"));
    }

    @Test
    @DisplayName("AdministrativeReqEnum test")
    void testAdministrativeReqEnum() {
        assertEquals("SSN", AdministrativeReqEnum.SSN.getCode());
        assertEquals("Regime SSN", AdministrativeReqEnum.SSN.getDescription());
        assertEquals("NOSSN", AdministrativeReqEnum.NOSSN.getCode());
        assertEquals("Regime privato", AdministrativeReqEnum.NOSSN.getDescription());
    }

    @Test
    @DisplayName("AttivitaClinicaEnum test")
    void testAttivitaClinicaEnum() {
        assertEquals("PHR", AttivitaClinicaEnum.PHR.getCode());
        assertEquals("Personal Health Record Update", AttivitaClinicaEnum.PHR.getDescription());
        assertEquals("CON", AttivitaClinicaEnum.CON.getCode());
        assertEquals("Consulto", AttivitaClinicaEnum.CON.getDescription());
    }

    @Test
    @DisplayName("CloudProviderEnum test")
    void testCloudProviderEnum() {
        assertEquals(CloudProviderEnum.AWS, CloudProviderEnum.valueOf("AWS"));
        assertEquals(CloudProviderEnum.AZURE, CloudProviderEnum.valueOf("AZURE"));
    }

    @Test
    @DisplayName("ConfidentialityCodeEnum test")
    void testConfidentialityCodeEnum() {
        assertEquals("N", ConfidentialityCodeEnum.NORMAL.getCode());
        assertEquals("Normal", ConfidentialityCodeEnum.NORMAL.getDisplay());
        assertEquals("Normal", ConfidentialityCodeEnum.getDisplayByCode("N"));
        assertEquals("", ConfidentialityCodeEnum.getDisplayByCode("UNKNOWN"));
        assertEquals(ConfidentialityCodeEnum.RESTRICTED, ConfidentialityCodeEnum.fromCode("R"));
        assertNull(ConfidentialityCodeEnum.fromCode(null));
        assertNull(ConfidentialityCodeEnum.fromCode("UNKNOWN"));
    }

    @Test
    @DisplayName("ConfigItemTypeEnum test")
    void testConfigItemTypeEnum() {
        List<ConfigItemTypeEnum> priority = ConfigItemTypeEnum.priority();
        assertNotNull(priority);
        assertEquals(ConfigItemTypeEnum.GENERIC, priority.get(0));
        assertEquals(2, priority.size());
    }

    @Test
    @DisplayName("DestinationTypeEnum test")
    void testDestinationTypeEnum() {
        assertEquals(DestinationTypeEnum.INDEXER, DestinationTypeEnum.valueOf("INDEXER"));
        assertEquals(DestinationTypeEnum.PUBLISHER, DestinationTypeEnum.valueOf("PUBLISHER"));
    }

    @Test
    @DisplayName("DirectFhirSourceEnum test")
    void testDirectFhirSourceEnum() {
        assertEquals("JSON", DirectFhirSourceEnum.JSON.getSource());
        assertEquals("PDF", DirectFhirSourceEnum.PDF.getSource());
    }

    @Test
    @DisplayName("DocumentMetadataKeyEnum test")
    void testDocumentMetadataKeyEnum() {
        assertEquals(DocumentMetadataKeyEnum.RAW_CDA, DocumentMetadataKeyEnum.valueOf("RAW_CDA"));
        assertEquals(DocumentMetadataKeyEnum.PATIENT_ID, DocumentMetadataKeyEnum.valueOf("PATIENT_ID"));
        assertEquals(DocumentMetadataKeyEnum.HASH, DocumentMetadataKeyEnum.valueOf("HASH"));
    }

    @Test
    @DisplayName("DocumentTypeEnum test")
    void testDocumentTypeEnum() {
        assertEquals("57833-6", DocumentTypeEnum.CODE_57833_6.getCode());
        assertEquals("Prescrizione farmaceutica", DocumentTypeEnum.CODE_57833_6.getDocumentType());
        assertEquals(DocumentTypeEnum.CODE_11502_2, DocumentTypeEnum.getByCode("11502-2"));
        assertNull(DocumentTypeEnum.getByCode("UNKNOWN"));
    }

    @Test
    @DisplayName("EdsStrategyEnum test")
    void testEdsStrategyEnum() {
        assertEquals(EdsStrategyEnum.NO_EDS, EdsStrategyEnum.valueOf("NO_EDS"));
        assertEquals(EdsStrategyEnum.NO_EDS_WITH_LOG, EdsStrategyEnum.valueOf("NO_EDS_WITH_LOG"));
    }

    @Test
    @DisplayName("ErrorLogEnum test")
    void testErrorLogEnum() {
        assertEquals("KO-GENERIC", ErrorLogEnum.KO_GENERIC.getCode());
        assertEquals("Errore generico", ErrorLogEnum.KO_GENERIC.getDescription());
        assertEquals("KO-VAL", ErrorLogEnum.KO_VAL.getCode());
        assertEquals("Errore nella validazione del CDA", ErrorLogEnum.KO_VAL.getDescription());
    }

    @Test
    @DisplayName("HealthcareFacilityEnum test")
    void testHealthcareFacilityEnum() {
        assertEquals("Ospedale", HealthcareFacilityEnum.Ospedale.getCode());
        assertEquals("Cittadino", HealthcareFacilityEnum.Cittadino.getCode());
    }

    @Test
    @DisplayName("IssueSeverityEnum test")
    void testIssueSeverityEnum() {
        assertEquals(IssueSeverityEnum.FATAL, IssueSeverityEnum.valueOf("FATAL"));
        assertEquals(IssueSeverityEnum.ERROR, IssueSeverityEnum.valueOf("ERROR"));
        assertEquals(IssueSeverityEnum.SUCCESS, IssueSeverityEnum.valueOf("SUCCESS"));
    }

    @Test
    @DisplayName("LowLevelDocEnum test")
    void testLowLevelDocEnum() {
        assertEquals("PDF", LowLevelDocEnum.PDF.getCode());
        assertEquals("PDF", LowLevelDocEnum.PDF.getDescription());
        assertEquals(LowLevelDocEnum.TXT, LowLevelDocEnum.fromCode("TXT"));
        assertNull(LowLevelDocEnum.fromCode(null));
        assertNull(LowLevelDocEnum.fromCode("UNKNOWN"));
    }

    @Test
    @DisplayName("PriorityTypeEnum test")
    void testPriorityTypeEnum() {
        assertEquals(PriorityTypeEnum.LOW, PriorityTypeEnum.valueOf("LOW"));
        assertEquals(PriorityTypeEnum.HIGH, PriorityTypeEnum.valueOf("HIGH"));
        assertEquals(PriorityTypeEnum.NULL, PriorityTypeEnum.valueOf("NULL"));
    }

    @Test
    @DisplayName("ProcessorOperationEnum test")
    void testProcessorOperationEnum() {
        assertEquals("PUBLISH", ProcessorOperationEnum.PUBLISH.getName());
        assertEquals("DELETE", ProcessorOperationEnum.DELETE.getName());
        assertEquals("REPLACE", ProcessorOperationEnum.REPLACE.getName());
    }

    @Test
    @DisplayName("PurposeOfUseEnum test")
    void testPurposeOfUseEnum() {
        assertEquals("TREATMENT", PurposeOfUseEnum.TREATMENT.getDisplay());
        assertEquals("Trattamento di cura ordinario", PurposeOfUseEnum.TREATMENT.getDescription());
        assertEquals(PurposeOfUseEnum.TREATMENT, PurposeOfUseEnum.get("TREATMENT"));
        assertEquals(PurposeOfUseEnum.EMERGENCY, PurposeOfUseEnum.get("emergency"));
        assertNull(PurposeOfUseEnum.get("UNKNOWN"));
    }

    @Test
    @DisplayName("TipoDocAltoLivEnum test")
    void testTipoDocAltoLivEnum() {
        assertEquals("WOR", TipoDocAltoLivEnum.WOR.getCode());
        assertEquals("Documento di workflow", TipoDocAltoLivEnum.WOR.getDescription());
        assertEquals("PRE", TipoDocAltoLivEnum.PRE.getCode());
        assertEquals("Prestazioni", TipoDocAltoLivEnum.PRE.getDescription());
    }

    @Test
    @DisplayName("UpdateFlowTypeEnum test")
    void testUpdateFlowTypeEnum() {
        assertTrue(UpdateFlowTypeEnum.UPDATE_ITI57.shouldValidateAffinityDomain());
        assertTrue(UpdateFlowTypeEnum.UPDATE_ITI57.shouldUseAsyncRetry());
        assertFalse(UpdateFlowTypeEnum.UPDATE_OSCURAMENTO.shouldValidateAffinityDomain());
        assertFalse(UpdateFlowTypeEnum.UPDATE_OSCURAMENTO.shouldUseAsyncRetry());
    }

}
