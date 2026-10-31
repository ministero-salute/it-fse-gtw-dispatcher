package it.finanze.sanita.fse2.ms.gtw.dispatcher.isolation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import com.mongodb.client.result.DeleteResult;
import org.bson.Document;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.entity.RoutingTableETY;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.mongo.impl.RoutingTableRepo;

@Tag("ISOLATION")
@ActiveProfiles("ISOLATION")
@SpringJUnitConfig(RoutingTableRepo.class)
class RoutingTableRepositoryIsolationTest {
    @Autowired private RoutingTableRepo repository;
    @MockitoBean private MongoTemplate mongo;

    private void assertIssuerQuery(Query query) {
        assertEquals(new Document(RoutingTableETY.FIELD_ISSUER, "issuer"), query.getQueryObject());
    }

    @Test
    void insertStampsDateAndReturnsStoredEntity() {
        var entity = RoutingTableServiceIsolationTest.entry();
        entity.setInsertionDate(null);
        when(mongo.insert(entity)).thenReturn(entity);
        long before = System.currentTimeMillis();
        assertSame(entity, repository.insert(entity));
        assertTrue(entity.getInsertionDate().getTime() >= before);
        assertTrue(entity.getInsertionDate().getTime() <= System.currentTimeMillis());
        verify(mongo).insert(entity);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void findsIssuerOrReturnsNull(boolean found) {
        var entity = found ? RoutingTableServiceIsolationTest.entry() : null;
        when(mongo.findOne(any(Query.class), eq(RoutingTableETY.class))).thenReturn(entity);
        assertSame(entity, repository.findByIssuer("issuer"));
        var query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).findOne(query.capture(), eq(RoutingTableETY.class));
        assertIssuerQuery(query.getValue());
    }

    @Test
    void returnsAllEntries() {
        var entries = List.of(RoutingTableServiceIsolationTest.entry());
        when(mongo.findAll(RoutingTableETY.class)).thenReturn(entries);
        assertSame(entries, repository.findAll());
        verify(mongo).findAll(RoutingTableETY.class);
    }

    @ParameterizedTest
    @CsvSource({"true,true", "true,false", "false,true", "false,false"})
    void updateIncludesOnlyNonNullFieldsAndReadsUpdatedEntry(boolean endpoint, boolean description) {
        var change = new RoutingTableETY();
        change.setNotificationEndpoint(endpoint ? "https://example.test/new" : null);
        change.setDescription(description ? "new description" : null);
        var stored = RoutingTableServiceIsolationTest.entry();
        when(mongo.findOne(any(Query.class), eq(RoutingTableETY.class))).thenReturn(stored);
        assertSame(stored, repository.update("issuer", change));
        var query = ArgumentCaptor.forClass(Query.class);
        var update = ArgumentCaptor.forClass(Update.class);
        var order = inOrder(mongo);
        order.verify(mongo).updateFirst(query.capture(), update.capture(), eq(RoutingTableETY.class));
        order.verify(mongo).findOne(any(Query.class), eq(RoutingTableETY.class));
        assertIssuerQuery(query.getValue());
        Map<?, ?> fields = (Map<?, ?>) update.getValue().getUpdateObject().get("$set");
        if (!endpoint && !description) {
            assertTrue(update.getValue().getUpdateObject().isEmpty());
        } else {
            assertEquals((endpoint ? 1 : 0) + (description ? 1 : 0), fields.size());
            assertEquals(change.getNotificationEndpoint(), fields.get(RoutingTableETY.FIELD_NOTIFICATION_ENDPOINT));
            assertEquals(change.getDescription(), fields.get(RoutingTableETY.FIELD_DESCRIPTION));
        }
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 1, 2})
    void deletionDependsOnDeletedCount(long count) {
        when(mongo.remove(any(Query.class), eq(RoutingTableETY.class)))
                .thenReturn(DeleteResult.acknowledged(count));
        assertEquals(count > 0, repository.deleteByIssuer("issuer"));
        var query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).remove(query.capture(), eq(RoutingTableETY.class));
        assertIssuerQuery(query.getValue());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void existenceUsesIssuerQuery(boolean exists) {
        when(mongo.exists(any(Query.class), eq(RoutingTableETY.class))).thenReturn(exists);
        assertEquals(exists, repository.existsByIssuer("issuer"));
        var query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).exists(query.capture(), eq(RoutingTableETY.class));
        assertIssuerQuery(query.getValue());
    }

    @Test
    void insertFailurePreservesCause() {
        var entity = RoutingTableServiceIsolationTest.entry();
        var failure = new IllegalStateException("offline");
        when(mongo.insert(entity)).thenThrow(failure);
        assertSame(failure, assertThrows(BusinessException.class, () -> repository.insert(entity)).getCause());
    }

    @Test
    void lookupFailurePreservesCause() {
        var failure = new IllegalStateException("offline");
        when(mongo.findOne(any(Query.class), eq(RoutingTableETY.class))).thenThrow(failure);
        assertSame(failure, assertThrows(BusinessException.class,
                () -> repository.findByIssuer("issuer")).getCause());
    }

    @Test
    void listFailurePreservesCause() {
        var failure = new IllegalStateException("offline");
        when(mongo.findAll(RoutingTableETY.class)).thenThrow(failure);
        assertSame(failure, assertThrows(BusinessException.class, repository::findAll).getCause());
    }

    @Test
    void updateFailureDoesNotReadBack() {
        var failure = new IllegalStateException("offline");
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq(RoutingTableETY.class)))
                .thenThrow(failure);
        assertSame(failure, assertThrows(BusinessException.class,
                () -> repository.update("issuer", RoutingTableServiceIsolationTest.entry())).getCause());
        verify(mongo, never()).findOne(any(Query.class), eq(RoutingTableETY.class));
    }

    @Test
    void deletionFailurePreservesCause() {
        var failure = new IllegalStateException("offline");
        when(mongo.remove(any(Query.class), eq(RoutingTableETY.class))).thenThrow(failure);
        assertSame(failure, assertThrows(BusinessException.class,
                () -> repository.deleteByIssuer("issuer")).getCause());
    }

    @Test
    void existenceFailurePreservesCause() {
        var failure = new IllegalStateException("offline");
        when(mongo.exists(any(Query.class), eq(RoutingTableETY.class))).thenThrow(failure);
        assertSame(failure, assertThrows(BusinessException.class,
                () -> repository.existsByIssuer("issuer")).getCause());
    }
}
