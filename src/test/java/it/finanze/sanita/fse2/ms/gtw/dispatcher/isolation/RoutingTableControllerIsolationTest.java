package it.finanze.sanita.fse2.ms.gtw.dispatcher.isolation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.controller.impl.RoutingTableCTL;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.RoutingTableResDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.IRoutingTableSRV;

@Tag("ISOLATION")
@ActiveProfiles("ISOLATION")
@SpringJUnitConfig(RoutingTableCTL.class)
class RoutingTableControllerIsolationTest {
    @Autowired private RoutingTableCTL controller;
    @MockitoBean private IRoutingTableSRV service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private RoutingTableResDTO response() {
        return RoutingTableResDTO.builder().issuer("issuer").notificationEndpoint("https://example.test").build();
    }

    @Test
    void createsEntryWith201AndPayload() throws Exception {
        when(service.createRoutingEntry(any())).thenReturn(response());
        mvc.perform(post("/v1/routing-table").contentType(MediaType.APPLICATION_JSON)
                .content("{\"issuer\":\"issuer\",\"notificationEndpoint\":\"https://example.test\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.issuer").value("issuer"))
                .andExpect(jsonPath("$.notificationEndpoint").value("https://example.test"));
        verify(service).createRoutingEntry(argThat(r -> "issuer".equals(r.getIssuer())
                && "https://example.test".equals(r.getNotificationEndpoint())));
    }

    @Test
    void getsEntryByIssuer() throws Exception {
        when(service.getRoutingEntryByIssuer("issuer")).thenReturn(response());
        mvc.perform(get("/v1/routing-table/issuer/issuer"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.issuer").value("issuer"));
        verify(service).getRoutingEntryByIssuer("issuer");
    }

    @Test
    void listsEntries() throws Exception {
        when(service.getAllRoutingEntries()).thenReturn(RoutingTableResDTO.builder()
                .entries(List.of(response())).build());
        mvc.perform(get("/v1/routing-table")).andExpect(status().isOk())
                .andExpect(jsonPath("$.entries[0].issuer").value("issuer"));
        verify(service).getAllRoutingEntries();
    }

    @Test
    void updatesEntry() throws Exception {
        when(service.updateRoutingEntry(eq("issuer"), any())).thenReturn(response());
        mvc.perform(put("/v1/routing-table/issuer").contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\":\"new description\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.issuer").value("issuer"));
        verify(service).updateRoutingEntry(eq("issuer"), argThat(r ->
                "new description".equals(r.getDescription()) && r.getNotificationEndpoint() == null));
    }

    @Test
    void deletesEntry() throws Exception {
        when(service.deleteRoutingEntry("issuer")).thenReturn(response());
        mvc.perform(delete("/v1/routing-table/issuer")).andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer").value("issuer"));
        verify(service).deleteRoutingEntry("issuer");
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"issuer\":\"issuer\",\"notificationEndpoint\":\"ftp://invalid\"}", "{invalid"})
    void invalidCreationNeverCallsService(String json) throws Exception {
        mvc.perform(post("/v1/routing-table").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void invalidUpdateNeverCallsService() throws Exception {
        mvc.perform(put("/v1/routing-table/issuer").contentType(MediaType.APPLICATION_JSON)
                .content("{\"notificationEndpoint\":\"invalid\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void serviceFailureIsNotSilentlyConvertedToSuccess() {
        var failure = new IllegalStateException("unavailable");
        when(service.getRoutingEntryByIssuer("issuer")).thenThrow(failure);
        var exception = assertThrows(jakarta.servlet.ServletException.class,
                () -> mvc.perform(get("/v1/routing-table/issuer/issuer")));
        assertSame(failure, exception.getCause());
    }
}
