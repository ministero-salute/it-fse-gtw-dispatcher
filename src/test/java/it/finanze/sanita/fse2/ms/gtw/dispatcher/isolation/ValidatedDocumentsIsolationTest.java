package it.finanze.sanita.fse2.ms.gtw.dispatcher.isolation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import java.util.Date;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import com.mongodb.MongoException;
import com.mongodb.client.result.DeleteResult;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.entity.ValidatedDocumentsETY;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.mongo.impl.ValidatedDocumentsRepo;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;

@Tag("ISOLATION")
@ExtendWith(MockitoExtension.class)
class ValidatedDocumentsIsolationTest {
    @Mock MongoTemplate mongo;
    @InjectMocks ValidatedDocumentsRepo repository;

    private ValidatedDocumentsETY entity() {
        var entity = ValidatedDocumentsETY.setContent("hash", "workflow", "map", "engine");
        entity.setId("id");
        entity.setInsertionDate(new Date(172800000));
        return entity;
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void savesNewOrRefreshesExistingValidation(boolean exists) {
        var old = exists ? entity() : null;
        when(mongo.findOne(any(Query.class), eq(ValidatedDocumentsETY.class))).thenReturn(old);
        var input = ValidatedDocumentsETY.setContent("hash", "new-workflow", "new-map", "engine");
        repository.create(input);
        var query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).findOne(query.capture(), eq(ValidatedDocumentsETY.class));
        assertEquals("hash", query.getValue().getQueryObject().getString("hash_cda"));
        var saved = ArgumentCaptor.forClass(ValidatedDocumentsETY.class);
        verify(mongo).save(saved.capture());
        assertSame(exists ? old : input, saved.getValue());
        assertEquals("new-workflow", saved.getValue().getWorkflowInstanceId());
        assertEquals("new-map", saved.getValue().getPrimaryKeyTransform());
        assertNotNull(saved.getValue().getInsertionDate());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void mapsFoundAndMissingValidation(boolean exists) {
        var entity = exists ? entity() : null;
        when(mongo.findOne(any(Query.class), eq(ValidatedDocumentsETY.class))).thenReturn(entity);
        var hash = repository.findItemByHash("hash");
        var workflow = repository.findItemByWorkflowInstanceId("workflow");
        assertEquals(exists, hash.isCdaValidated());
        assertEquals(exists, workflow.isCdaValidated());
        if (exists) {
            assertEquals("workflow", hash.getWorkflowInstanceId());
            assertEquals("hash", hash.getHash());
            assertEquals("map", workflow.getTransformID());
            assertEquals("engine", workflow.getEngineID());
            assertEquals(entity.getInsertionDate(), workflow.getInsertionDate());
        }
        var queries = ArgumentCaptor.forClass(Query.class);
        verify(mongo, times(2)).findOne(queries.capture(), eq(ValidatedDocumentsETY.class));
        assertEquals("hash", queries.getAllValues().get(0).getQueryObject().getString("hash_cda"));
        assertEquals("workflow", queries.getAllValues().get(1).getQueryObject().getString("w_id"));
    }

    @Test
    void findsByIdAndChecksExistence() {
        var entity = entity();
        when(mongo.findOne(any(Query.class), eq(ValidatedDocumentsETY.class))).thenReturn(entity);
        when(mongo.exists(any(Query.class), eq(ValidatedDocumentsETY.class))).thenReturn(true);
        assertSame(entity, repository.findItemById("id"));
        assertTrue(repository.isItemInserted("hash"));
        var query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).findOne(query.capture(), eq(ValidatedDocumentsETY.class));
        assertEquals("id", query.getValue().getQueryObject().getString("_id"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void deletionReturnsAcknowledgementNotCount(boolean acknowledged) {
        when(mongo.remove(any(Query.class), eq(ValidatedDocumentsETY.class)))
                .thenReturn(acknowledged ? DeleteResult.acknowledged(0) : DeleteResult.unacknowledged());
        assertEquals(acknowledged, repository.deleteItem("hash"));
        var entity = entity();
        when(mongo.findOne(any(Query.class), eq(ValidatedDocumentsETY.class))).thenReturn(entity);
        when(mongo.remove(entity)).thenReturn(acknowledged ? DeleteResult.acknowledged(1) : DeleteResult.unacknowledged());
        assertEquals(acknowledged, repository.deleteItemBenchmark("hash"));
    }

    @Test
    void shiftsInsertionDateAndDoesNotSaveMissingItem() {
        var entity = entity();
        when(mongo.findOne(any(Query.class), eq(ValidatedDocumentsETY.class))).thenReturn(entity, null);
        when(mongo.save(entity)).thenReturn(entity);
        assertEquals("id", repository.updateInsertionDate("workflow", 1));
        assertEquals(86400000L, entity.getInsertionDate().getTime());
        assertEquals("", repository.updateInsertionDate("missing", 1));
        verify(mongo).save(entity);
        var benchmark = entity();
        repository.createBenchmark(benchmark);
        assertTrue(benchmark.getInsertionDate().getTime() > 172800000);
        verify(mongo).save(benchmark);
    }

    @Test
    void wrapsMongoLookupFailuresForEveryLookupOperation() {
        var failure = new MongoException("offline");
        when(mongo.findOne(any(Query.class), eq(ValidatedDocumentsETY.class))).thenThrow(failure);
        Stream.<Runnable>of(() -> repository.findItemById("id"), () -> repository.findItemByHash("hash"),
                () -> repository.findItemByWorkflowInstanceId("workflow"), () -> repository.create(entity()),
                () -> repository.updateInsertionDate("workflow", 1), () -> repository.deleteItemBenchmark("hash"))
                .forEach(call -> assertSame(failure, assertThrows(BusinessException.class, call::run).getCause()));
    }

    @Test
    void wrapsMongoExistenceAndDeleteFailures() {
        var failure = new MongoException("offline");
        when(mongo.exists(any(Query.class), eq(ValidatedDocumentsETY.class))).thenThrow(failure);
        when(mongo.remove(any(Query.class), eq(ValidatedDocumentsETY.class))).thenThrow(failure);
        assertSame(failure, assertThrows(BusinessException.class, () -> repository.isItemInserted("hash")).getCause());
        assertSame(failure, assertThrows(BusinessException.class, () -> repository.deleteItem("hash")).getCause());
    }
}
