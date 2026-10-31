package it.finanze.sanita.fse2.ms.gtw.dispatcher.isolation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.test.util.ReflectionTestUtils;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.IStatusCheckClient;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.CDACFG;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.impl.AccreditamentoSimulationSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.*;

@Tag("ISOLATION")
class AccreditationSimulationBranchesIsolationTest {
    private AccreditamentoSimulationSRV service;
    private ICdaSRV cda;
    private IStatusCheckClient status;

    @BeforeEach void setup() {
        service = new AccreditamentoSimulationSRV();
        cda = mock(ICdaSRV.class);
        status = mock(IStatusCheckClient.class);
        ReflectionTestUtils.setField(service, "cdaSRV", cda);
        ReflectionTestUtils.setField(service, "statusCheckClient", status);
    }

    @ParameterizedTest @EnumSource(AccreditamentoPrefixEnum.class)
    void everyReservedPrefixRequiresARandomSuffix(AccreditamentoPrefixEnum prefix) {
        assertEquals(prefix, AccreditamentoPrefixEnum.get(prefix.getPrefix().toLowerCase()));
        assertThrows(ValidationException.class, () -> service.runSimulation(prefix.getPrefix(), new byte[0], EventTypeEnum.PUBLICATION));
    }

    @Test void timeoutAndOrdinaryReplacementUseDistinctPaths() {
        assertThrows(ConnectionRefusedException.class, () -> service.runSimulation("CRASH_TIMEOUT_random", new byte[0], EventTypeEnum.PUBLICATION));
        assertNull(service.runSimulation("ordinary", new byte[0], EventTypeEnum.REPLACE));
        verify(status).callSearchEventByIdDocumento("ordinary");
        assertNull(service.runSimulation("TRIAL_ID_SKIP_ACCREDITATIONCHECK", new byte[0], EventTypeEnum.REPLACE));
        verifyNoMoreInteractions(status);
        assertNull(AccreditamentoPrefixEnum.get("unknown"));
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void skippedValidationPersistsEngineMapAndNormalizedCdaHash(boolean authenticator) {
        var cfg = mock(CDACFG.class);
        when(cfg.getCdaAttachmentName()).thenReturn("cda.xml");
        var engines = mock(IEngineSRV.class);
        when(engines.getStructureObjectID("template")).thenReturn(Pair.of("engine", "transform"));
        ReflectionTestUtils.setField(service, "cdaCFG", cfg);
        ReflectionTestUtils.setField(service, "engines", engines);
        String xml = "<ClinicalDocument><templateId root='template'/>" +
            (authenticator ? "<LegalAuthenticator><time value='secret'/><name>private</name><empty/></LegalAuthenticator>" : "") + "</ClinicalDocument>";
        byte[] pdf = {1, 2};
        try (var pdfUtility = mockStatic(PDFUtility.class); var cdaUtility = mockStatic(CdaUtility.class)) {
            pdfUtility.when(() -> PDFUtility.extractContentFromAttachments(pdf, "cda.xml")).thenReturn(xml);
            cdaUtility.when(() -> CdaUtility.getWorkflowInstanceId(any(org.jsoup.nodes.Document.class))).thenReturn("workflow");
            for (String prefix : new String[]{"SKIP_VALIDATION", "CRASH_WF_EDS", "CRASH_INI", "CRASH_EDS"}) {
                assertEquals("workflow", service.runSimulation(prefix + "_random", pdf, EventTypeEnum.PUBLICATION).getWorkflowInstanceId());
            }
            String normalized = org.jsoup.Jsoup.parse(xml, "", org.jsoup.parser.Parser.xmlParser()).toString();
            if (authenticator) normalized = normalized.replace("secret", "PLACEHOLDER").replace("private", "PLACEHOLDER");
            verify(cda, times(4)).create(StringUtility.encodeSHA256B64(normalized), "workflow", "transform", "engine");
        }
    }
}
