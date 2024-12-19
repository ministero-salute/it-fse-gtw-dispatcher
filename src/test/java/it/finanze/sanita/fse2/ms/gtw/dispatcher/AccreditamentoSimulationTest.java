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

import static it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants.Profile.TEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.IStatusCheckClient;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.CDACFG;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.AccreditamentoSimulationDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.AccreditamentoPrefixEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.EventTypeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ConnectionRefusedException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ValidationException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.IAccreditamentoSimulationSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.ICdaSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.IEngineSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.CdaUtility;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.FileUtility;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.PDFUtility;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.StringUtility;
import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(TEST)
public class AccreditamentoSimulationTest {

    @Autowired
    private IAccreditamentoSimulationSRV service;

    @MockBean
    private IEngineSRV engines;

    @MockBean
    private ICdaSRV cdaSRV;

    @MockBean
    private CDACFG cdaCFG;

    @MockBean
    private IStatusCheckClient statusCheckClient;

    private byte[] pdfAttachment;

    public AccreditamentoSimulationTest() {
    }

    @BeforeEach
    void setUp() {
        pdfAttachment = FileUtility.getFileFromInternalResources("Files/accreditamento/SIGNED_LDO1.pdf");
        when(cdaCFG.getCdaAttachmentName()).thenReturn("cda.xml");
    }

    @Test
    void simulationCrashTimeoutTest() {
        assertThrows(ConnectionRefusedException.class, () ->
                service.runSimulation(AccreditamentoPrefixEnum.CRASH_TIMEOUT.getPrefix() + "id", pdfAttachment, EventTypeEnum.GENERIC_ERROR)
        );
    }

    @Test
    void simulationReplaceEventCallsStatusCheckTest() {
        service.runSimulation("SOMEPREFIX_123", pdfAttachment, EventTypeEnum.REPLACE);
        verify(statusCheckClient).callSearchEventByIdDocumento("SOMEPREFIX_123");
    }

    @Test
    void simulationSkipValidationTest() {

        String cdaContent = "<ClinicalDocument><templateid root=\"someRoot\"/><LegalAuthenticator/></ClinicalDocument>";
        try (MockedStatic<PDFUtility> pdfMock = org.mockito.Mockito.mockStatic(PDFUtility.class);
             MockedStatic<CdaUtility> cdaUtilityMock = org.mockito.Mockito.mockStatic(CdaUtility.class);
             MockedStatic<StringUtility> stringUtilityMock = org.mockito.Mockito.mockStatic(StringUtility.class)) {

            pdfMock.when(() -> PDFUtility.extractCDAFromAttachments(any(byte[].class), any(String.class)))
                    .thenReturn(cdaContent);
            cdaUtilityMock.when(() -> CdaUtility.getWorkflowInstanceId(any(Document.class)))
                    .thenReturn("WII-12345");
            stringUtilityMock.when(() -> StringUtility.encodeSHA256B64(any(String.class)))
                    .thenReturn("hashedValue");
            when(engines.getStructureObjectID("someRoot")).thenReturn(Pair.of("engineId","transformId"));

            AccreditamentoSimulationDTO result = service.runSimulation("SKIP_VALIDATION_randomStr", pdfAttachment, EventTypeEnum.GENERIC_ERROR);
            assertEquals("WII-12345", result.getWorkflowInstanceId());
            verify(cdaSRV).create("hashedValue", "WII-12345", "transformId", "engineId");
        }
    }

    @Test
    void simulationCrashWfEdsTest() {
        String cdaContent = "<ClinicalDocument><templateid root=\"someRoot\"/><LegalAuthenticator/></ClinicalDocument>";
        try (MockedStatic<PDFUtility> pdfMock = org.mockito.Mockito.mockStatic(PDFUtility.class);
             MockedStatic<CdaUtility> cdaUtilityMock = org.mockito.Mockito.mockStatic(CdaUtility.class);
             MockedStatic<StringUtility> stringUtilityMock = org.mockito.Mockito.mockStatic(StringUtility.class)) {

            pdfMock.when(() -> PDFUtility.extractCDAFromAttachments(any(byte[].class), any(String.class)))
                    .thenReturn(cdaContent);
            cdaUtilityMock.when(() -> CdaUtility.getWorkflowInstanceId(any(Document.class)))
                    .thenReturn("WII-9999");
            stringUtilityMock.when(() -> StringUtility.encodeSHA256B64(any(String.class)))
                    .thenReturn("hashedValue");
            when(engines.getStructureObjectID("someRoot")).thenReturn(Pair.of("engineId","transformId"));

            AccreditamentoSimulationDTO result = service.runSimulation("CRASH_WF_EDS_random", pdfAttachment, EventTypeEnum.GENERIC_ERROR);
            assertEquals("WII-9999", result.getWorkflowInstanceId());
        }
    }

    @Test
    void simulationCrashIniTest() {
        String cdaContent = "<ClinicalDocument><templateid root=\"someRoot\"/><LegalAuthenticator/></ClinicalDocument>";
        try (MockedStatic<PDFUtility> pdfMock = org.mockito.Mockito.mockStatic(PDFUtility.class);
             MockedStatic<CdaUtility> cdaUtilityMock = org.mockito.Mockito.mockStatic(CdaUtility.class);
             MockedStatic<StringUtility> stringUtilityMock = org.mockito.Mockito.mockStatic(StringUtility.class)) {

            pdfMock.when(() -> PDFUtility.extractCDAFromAttachments(any(byte[].class), any(String.class)))
                    .thenReturn(cdaContent);
            cdaUtilityMock.when(() -> CdaUtility.getWorkflowInstanceId(any(Document.class)))
                    .thenReturn("WII-INI");
            stringUtilityMock.when(() -> StringUtility.encodeSHA256B64(any(String.class)))
                    .thenReturn("hashedValue");
            when(engines.getStructureObjectID("someRoot")).thenReturn(Pair.of("engineId","transformId"));

            AccreditamentoSimulationDTO result = service.runSimulation("CRASH_INI_anything", pdfAttachment, EventTypeEnum.GENERIC_ERROR);
            assertEquals("WII-INI", result.getWorkflowInstanceId());
        }
    }

    @Test
    void simulationCrashEdsTest() {
        String cdaContent = "<ClinicalDocument><templateid root=\"someRoot\"/><LegalAuthenticator/></ClinicalDocument>";
        try (MockedStatic<PDFUtility> pdfMock = org.mockito.Mockito.mockStatic(PDFUtility.class);
             MockedStatic<CdaUtility> cdaUtilityMock = org.mockito.Mockito.mockStatic(CdaUtility.class);
             MockedStatic<StringUtility> stringUtilityMock = org.mockito.Mockito.mockStatic(StringUtility.class)) {

            pdfMock.when(() -> PDFUtility.extractCDAFromAttachments(any(byte[].class), any(String.class)))
                    .thenReturn(cdaContent);
            cdaUtilityMock.when(() -> CdaUtility.getWorkflowInstanceId(any(Document.class)))
                    .thenReturn("WII-EDS");
            stringUtilityMock.when(() -> StringUtility.encodeSHA256B64(any(String.class)))
                    .thenReturn("hashedValue");
            when(engines.getStructureObjectID("someRoot")).thenReturn(Pair.of("engineId","transformId"));

            AccreditamentoSimulationDTO result = service.runSimulation("CRASH_EDS_something", pdfAttachment, EventTypeEnum.GENERIC_ERROR);
            assertEquals("WII-EDS", result.getWorkflowInstanceId());
        }
    }

    @Test
    void simulationValidationExceptionNoRandomStringTest() {
        assertThrows(ValidationException.class, () ->
                service.runSimulation("SKIP_VALIDATION", pdfAttachment, EventTypeEnum.GENERIC_ERROR)
        );
    }

    @Test
    void simulationNoPrefixReturnsNullTest() {
        AccreditamentoSimulationDTO result = service.runSimulation("NO_PREFIX_123", pdfAttachment, EventTypeEnum.GENERIC_ERROR);
        assertNull(result);
    }

    @Test
    void simulationTrialSkipAccreditationCheckWithReplaceEventDoesNotCallStatusCheck() {
        service.runSimulation("TRIAL_ID_SKIP_ACCREDITATIONCHECK", pdfAttachment, EventTypeEnum.REPLACE);

        verify(statusCheckClient, org.mockito.Mockito.never()).callSearchEventByIdDocumento("TRIAL_ID_SKIP_ACCREDITATIONCHECK");
    }

    @Test
    void simulationGenericNoPrefixNoErrorTest() {
        AccreditamentoSimulationDTO result = service.runSimulation("SOME_OTHER_ID_123", pdfAttachment, EventTypeEnum.GENERIC_ERROR);
        assertNull(result);
    }
}
