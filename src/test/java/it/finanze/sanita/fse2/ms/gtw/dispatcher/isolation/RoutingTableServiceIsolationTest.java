package it.finanze.sanita.fse2.ms.gtw.dispatcher.isolation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.RoutingTableCreateReqDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.RoutingTableUpdateReqDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.NoRecordFoundException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.entity.RoutingTableETY;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.mongo.IRoutingTableRepo;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.impl.RoutingTableSRV;

@Tag("ISOLATION")
@ActiveProfiles("ISOLATION")
@SpringJUnitConfig(RoutingTableSRV.class)
class RoutingTableServiceIsolationTest {
    @Autowired private RoutingTableSRV service;
    @MockitoBean private IRoutingTableRepo repository;

    static RoutingTableETY entry() {
        RoutingTableETY entity = new RoutingTableETY();
        entity.setId("id-1");
        entity.setIssuer("issuer");
        entity.setNotificationEndpoint("https://example.test/notify");
        entity.setDescription("description");
        entity.setInsertionDate(new Date(1234));
        return entity;
    }

    @Test
    void createsAndMapsPersistedEntry() {
        when(repository.insert(any())).thenReturn(entry());
        var result = service.createRoutingEntry(new RoutingTableCreateReqDTO(
                "issuer", "https://example.test/notify", "description"));
        assertEquals("id-1", result.getId());
        assertEquals("issuer", result.getIssuer());
        assertEquals("https://example.test/notify", result.getNotificationEndpoint());
        assertEquals("description", result.getDescription());
        assertEquals(new Date(1234), result.getInsertionDate());
        assertTrue(result.getMessage().contains("created"));
        verify(repository).insert(argThat(e -> "issuer".equals(e.getIssuer())
                && "description".equals(e.getDescription())
                && "https://example.test/notify".equals(e.getNotificationEndpoint())));
    }

    @Test
    void duplicateDoesNotInsert() {
        when(repository.existsByIssuer("issuer")).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.createRoutingEntry(
                new RoutingTableCreateReqDTO("issuer", "https://example.test", null)));
        verify(repository, never()).insert(any());
    }

    @Test
    void retrievesExistingEntry() {
        when(repository.findByIssuer("issuer")).thenReturn(entry());
        assertEquals("id-1", service.getRoutingEntryByIssuer("issuer").getId());
        verify(repository).findByIssuer("issuer");
    }

    @Test
    void missingLookupFails() {
        assertThrows(NoRecordFoundException.class, () -> service.getRoutingEntryByIssuer("missing"));
    }

    @ParameterizedTest
    @CsvSource({"0", "1"})
    void listsAndCountsEntries(int count) {
        when(repository.findAll()).thenReturn(count == 0 ? List.of() : List.of(entry()));
        var result = service.getAllRoutingEntries();
        assertEquals(count, result.getEntries().size());
        assertEquals("Retrieved " + count + " routing table entries", result.getMessage());
        if (count > 0) assertEquals("issuer", result.getEntries().getFirst().getIssuer());
    }

    @ParameterizedTest
    @CsvSource({"https://example.test/new,", ",new description", "https://example.test/new,new description"})
    void updatesOnlyRequestedFields(String endpoint, String description) {
        when(repository.existsByIssuer("issuer")).thenReturn(true);
        when(repository.update(eq("issuer"), any())).thenReturn(entry());
        assertEquals("id-1", service.updateRoutingEntry("issuer",
                new RoutingTableUpdateReqDTO(endpoint, description)).getId());
        verify(repository).update(eq("issuer"), argThat(e ->
                java.util.Objects.equals(endpoint, e.getNotificationEndpoint())
                && java.util.Objects.equals(description, e.getDescription())));
    }

    @Test
    void rejectsMissingUpdate() {
        assertThrows(NoRecordFoundException.class, () -> service.updateRoutingEntry("missing",
                new RoutingTableUpdateReqDTO(null, "new")));
        verify(repository, never()).update(anyString(), any());
    }

    @Test
    void rejectsEmptyUpdate() {
        when(repository.existsByIssuer("issuer")).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.updateRoutingEntry("issuer",
                new RoutingTableUpdateReqDTO()));
        verify(repository, never()).update(anyString(), any());
    }

    @Test
    void deletesExistingEntry() {
        when(repository.deleteByIssuer("issuer")).thenReturn(true);
        var result = service.deleteRoutingEntry("issuer");
        assertEquals("issuer", result.getIssuer());
        assertTrue(result.getMessage().contains("deleted"));
    }

    @Test
    void rejectsMissingDeletion() {
        assertThrows(NoRecordFoundException.class, () -> service.deleteRoutingEntry("missing"));
    }

    @Test
    void preservesRepositoryFailure() {
        var failure = new BusinessException("database unavailable");
        when(repository.findAll()).thenThrow(failure);
        assertSame(failure, assertThrows(BusinessException.class, service::getAllRoutingEntries));
    }
}
