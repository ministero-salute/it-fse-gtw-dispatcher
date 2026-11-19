package it.finanze.sanita.fse2.ms.gtw.dispatcher.isolation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.*;
import org.springframework.test.util.ReflectionTestUtils;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.audit.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.audit.filters.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.entity.AuditETY;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.mongo.IAuditRepo;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.IConfigSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.adapter.CustomResponseBodyAdviceAdapter;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;
import org.springframework.http.MediaType;
import org.springframework.http.server.*;

@Tag("ISOLATION")
class AuditPipelineIsolationTest {
    @ParameterizedTest @ValueSource(strings = {"request", "update", "delete"})
    void filtersCaptureRequestAndResponseButSkipBenchmarkTraffic(String kind) {
        AuditFilter filter = switch (kind) {
            case "request" -> new RequestBodyFilter();
            case "update" -> new UpdateReqFilter();
            default -> new DeleteFilter();
        };
        var benchmark = mock(BenchmarkCFG.class);
        ReflectionTestUtils.setField(filter, "benchmarkCFG", benchmark);
        var config = mock(IConfigSRV.class);
        if (filter instanceof RequestBodyFilter) ReflectionTestUtils.setField(filter, "config", config);
        var req = new MockHttpServletRequest("POST", "/documents");
        assertFalse(filter.match(req));
        req.addParameter("requestBody", "{\"id\":1}");
        req.setAttribute("UPDATE_REQ", Map.of("id", 1));
        req.setMethod(kind.equals("delete") ? "DELETE" : "POST");
        assertTrue(filter.match(req));
        Object response = Map.of("ok", true);
        AuditETY audit = filter.apply("/documents", req, response);
        assertEquals("/documents", audit.getServizio());
        assertEquals(req.getMethod(), audit.getHttpMethod());
        assertSame(response, audit.getResponse());
        assertNotNull(audit.getEnd_time());
        req.setAttribute("JWT_ISSUER", "issuer#private-fiscal-code");
        when(config.isCfOnIssuerNotAllowed()).thenReturn(true);
        audit = filter.apply("/documents", req, response);
        if (filter instanceof RequestBodyFilter) {
            assertEquals("issuer", audit.getJwt_issuer());
            assertNull(req.getAttribute("JWT_ISSUER"));
            req.setAttribute("JWT_ISSUER", "issuer");
            assertEquals("issuer", filter.apply("/documents", req, response).getJwt_issuer());
        }
        when(benchmark.isBenchmarkEnable()).thenReturn(true);
        req.setAttribute("JWT_ISSUER", "ordinary-issuer");
        assertNotNull(filter.apply("/documents", req, response));
        req.setAttribute("JWT_ISSUER", Constants.App.BENCHMARK_ISSUER);
        assertNull(filter.apply("/documents", req, response));
    }

    @Test void managerHonorsExclusionsAndOnlyPersistsNonNullMatchingAudits() {
        var manager = new AuditManager();
        var repo = mock(IAuditRepo.class);
        var exclusion = mock(AuditExclusion.class);
        var filter = mock(AuditFilter.class);
        ReflectionTestUtils.setField(manager, "repository", repo);
        ReflectionTestUtils.setField(manager, "exclusions", List.of(exclusion));
        ReflectionTestUtils.setField(manager, "filters", List.of(filter));
        var req = new MockHttpServletRequest("POST", "/documents/a%20b");
        var body = new Object();
        when(exclusion.verify("/documents/a b", req)).thenReturn(true);
        manager.process(req, body);
        verifyNoInteractions(filter, repo);
        when(exclusion.verify("/documents/a b", req)).thenReturn(false);
        manager.process(req, body);
        verifyNoInteractions(repo);
        when(filter.match(req)).thenReturn(true);
        manager.process(req, body);
        verifyNoInteractions(repo);
        var entity = new AuditETY();
        when(filter.apply("/documents/a b", req, body)).thenReturn(entity);
        manager.process(req, body);
        verify(repo).save(entity);
        var noUri = mock(jakarta.servlet.http.HttpServletRequest.class);
        manager.process(noUri, body);
        verify(exclusion).verify(null, noUri);
    }

    @Test void responseAdvicePreservesBodyAndPropagatesAuditFailures() {
        var advice = new CustomResponseBodyAdviceAdapter();
        var config = mock(IConfigSRV.class);
        var manager = mock(AuditManager.class);
        ReflectionTestUtils.setField(advice, "config", config);
        ReflectionTestUtils.setField(advice, "manager", manager);
        Object body = new Object();
        var request = new ServletServerHttpRequest(new MockHttpServletRequest());
        var response = new ServletServerHttpResponse(new MockHttpServletResponse());
        assertTrue(advice.supports(null, null));
        assertSame(body, advice.beforeBodyWrite(body, null, MediaType.APPLICATION_JSON, null, request, response));
        verifyNoInteractions(manager);
        when(config.isAuditEnable()).thenReturn(true);
        assertSame(body, advice.beforeBodyWrite(body, null, MediaType.APPLICATION_JSON, null, request, response));
        verify(manager).process(request.getServletRequest(), body);
        doThrow(new IllegalStateException("audit unavailable")).when(manager).process(any(), any());
        assertThrows(BusinessException.class, () -> advice.beforeBodyWrite(body, null, MediaType.APPLICATION_JSON, null, request, response));
        assertSame(body, advice.beforeBodyWrite(body, null, null, null, mock(ServerHttpRequest.class), response));
        assertSame(body, advice.beforeBodyWrite(body, null, null, null, request, mock(ServerHttpResponse.class)));
    }
}
