package it.finanze.sanita.fse2.ms.gtw.dispatcher.isolation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.entity.engine.EngineETY;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.entity.engine.sub.EngineMap;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.mongo.IEngineRepo;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.IConfigSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.impl.*;

@Tag("ISOLATION")
class InspectionAndEngineBranchesIsolationTest {
    @ParameterizedTest @ValueSource(strings = {"disabled", "benchmark", "null", "empty", "merge", "noEvents"})
    void inspectionMergesIniAuditsOnlyWhenEnabledAndSortsChronologically(String mode) {
        var service = new TransactionInspectSRV();
        var status = mock(IStatusCheckClient.class);
        var ini = mock(IIniClient.class);
        var config = mock(IConfigSRV.class);
        ReflectionTestUtils.setField(service, "statusCheckClient", status);
        ReflectionTestUtils.setField(service, "iniClient", ini);
        ReflectionTestUtils.setField(service, "configSRV", config);
        when(config.isAuditIniEnable()).thenReturn(!mode.equals("disabled"));
        var latest = new StatusCheckDTO();
        latest.setEventDate("2026-10-31");
        latest.setIssuer(mode.equals("benchmark") ? Constants.App.BENCHMARK_ISSUER : "ordinary");
        var response = new TransactionInspectResDTO();
        response.setTransactionData(new ArrayList<>(mode.equals("noEvents") ? List.of() : List.of(latest)));
        when(status.callSearchEventByWorkflowInstanceId("workflow")).thenReturn(response);
        var earlier = new StatusCheckDTO();
        earlier.setEventDate("2026-10-30");
        var audit = new IniAuditsDto();
        audit.setAudit(mode.equals("merge") ? List.of(earlier) : List.of());
        when(ini.callSearchEventByWorkflowInstanceId("workflow")).thenReturn(mode.equals("null") ? null : audit);
        assertSame(response, service.callSearchEventByWorkflowInstanceId("workflow"));
        if (mode.equals("merge")) assertEquals(List.of(earlier, latest), response.getTransactionData());
        else assertEquals(mode.equals("noEvents") ? List.of() : List.of(latest), response.getTransactionData());
        if (mode.equals("disabled") || mode.equals("benchmark")) verifyNoInteractions(ini);
        else verify(ini).callSearchEventByWorkflowInstanceId("workflow");
    }

    @Test void inspectionDelegatesTraceDocumentAndIngestionLookups() {
        var service = new TransactionInspectSRV();
        var status = mock(IStatusCheckClient.class);
        var eds = mock(IEdsClient.class);
        ReflectionTestUtils.setField(service, "statusCheckClient", status);
        ReflectionTestUtils.setField(service, "edsClient", eds);
        var transaction = new TransactionInspectResDTO();
        when(status.callSearchEventByTraceId("id")).thenReturn(transaction);
        assertSame(transaction, service.callSearchEventByTraceId("id"));
        // Documents currently use the trace lookup: preserve this existing contract.
        assertSame(transaction, service.callSearchEventByIdDocumento("id"));
        var ingestion = mock(GetIngestionStatusResponseDTO.class);
        when(eds.getEdsStatus("workflow")).thenReturn(ingestion);
        assertSame(ingestion, service.getEdsStatusByWorkflowInstanceId("workflow"));
    }

    @Test void engineLookupReturnsMatchingMapAndWrapsMissingOrUnavailableEngines() {
        var service = new EngineSRV();
        var repo = mock(IEngineRepo.class);
        ReflectionTestUtils.setField(service, "engines", repo);
        assertThrows(BusinessException.class, () -> service.getStructureObjectID("template"));
        var engine = new EngineETY();
        engine.setId("engine");
        engine.setRoots(List.of(new EngineMap("other-map", List.of("other"), "uri", "1"),
            new EngineMap("map", List.of("template"), "uri", "1")));
        when(repo.getLatestEngine()).thenReturn(engine);
        assertEquals("engine", service.getStructureObjectID("template").getKey());
        assertEquals("map", service.getStructureObjectID("template").getValue());
        assertThrows(BusinessException.class, () -> service.getStructureObjectID("missing"));
        when(repo.getLatestEngine()).thenThrow(new IllegalStateException("database offline"));
        assertInstanceOf(IllegalStateException.class, assertThrows(BusinessException.class,
            () -> service.getStructureObjectID("template")).getCause());
    }
}
