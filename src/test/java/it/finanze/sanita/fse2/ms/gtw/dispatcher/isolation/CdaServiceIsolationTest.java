package it.finanze.sanita.fse2.ms.gtw.dispatcher.isolation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.ValidationDataDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.mongo.IValidatedDocumentsRepo;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.impl.CdaSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;

@Tag("ISOLATION")
@ExtendWith(MockitoExtension.class)
class CdaServiceIsolationTest {
    @Mock IValidatedDocumentsRepo repository;
    @InjectMocks CdaSRV service;

    @Test
    void createsValidationRecordsAndBenchmarkRecords() {
        service.create("hash", "workflow", "map", "engine");
        service.createBenchMark("benchmark", "workflow", "map", "engine");
        verify(repository).create(argThat(e -> "hash".equals(e.getHashCda())
                && "workflow".equals(e.getWorkflowInstanceId()) && "map".equals(e.getPrimaryKeyTransform())
                && "engine".equals(e.getPrimaryKeyEngine())));
        verify(repository).createBenchmark(argThat(e -> "benchmark".equals(e.getHashCda())));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"workflow", "different"})
    void onlyRejectsDifferentNonEmptyWorkflow(String workflow) {
        var data = new ValidationDataDTO();
        data.setWorkflowInstanceId("workflow");
        data.setCdaValidated(true);
        when(repository.findItemByHash("hash")).thenReturn(data);
        assertEquals(!"different".equals(workflow), service.retrieveValidationInfo("hash", workflow).isCdaValidated());
        assertEquals("workflow", service.get("hash"));
        when(repository.findItemByWorkflowInstanceId("workflow")).thenReturn(data);
        assertSame(data, service.getByWorkflowInstanceId("workflow"));
    }

    @Test
    void missingValidationRemainsUnvalidated() {
        var data = new ValidationDataDTO();
        when(repository.findItemByHash("hash")).thenReturn(data);
        assertFalse(service.retrieveValidationInfo("hash", "workflow").isCdaValidated());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void emptyHashNeverDeletes(String hash) {
        assertFalse(service.consumeHash(hash));
        assertFalse(service.consumeHashBenchmark(hash));
        verifyNoInteractions(repository);
    }

    @Test
    void deletesNormalAndBenchmarkHashes() {
        when(repository.deleteItem("hash")).thenReturn(true);
        when(repository.deleteItemBenchmark("hash")).thenReturn(true);
        assertTrue(service.consumeHash("hash"));
        assertTrue(service.consumeHashBenchmark("hash"));
    }

    @Test
    void wrapsFailuresFromAllRepositoryOperations() {
        var failure = new IllegalStateException("offline");
        doThrow(failure).when(repository).create(any());
        doThrow(failure).when(repository).createBenchmark(any());
        when(repository.findItemByHash(anyString())).thenThrow(failure);
        when(repository.findItemByWorkflowInstanceId(anyString())).thenThrow(failure);
        when(repository.deleteItem(anyString())).thenThrow(failure);
        when(repository.deleteItemBenchmark(anyString())).thenThrow(failure);
        Stream.<Runnable>of(() -> service.create("h", "w", "m", "e"), () -> service.createBenchMark("h", "w", "m", "e"),
                () -> service.get("h"), () -> service.getByWorkflowInstanceId("w"),
                () -> service.retrieveValidationInfo("h", "w"), () -> service.consumeHash("h"),
                () -> service.consumeHashBenchmark("h"))
                .forEach(call -> assertSame(failure, assertThrows(BusinessException.class, call::run).getCause()));
    }
}
