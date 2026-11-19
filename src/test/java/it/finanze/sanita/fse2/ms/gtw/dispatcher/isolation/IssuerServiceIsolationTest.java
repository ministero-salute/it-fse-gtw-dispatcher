package it.finanze.sanita.fse2.ms.gtw.dispatcher.isolation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.entity.IssuerETY;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.repository.mongo.IIssuerRepo;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.impl.IssuerSRV;

@Tag("ISOLATION")
@ActiveProfiles("ISOLATION")
@SpringJUnitConfig(IssuerSRV.class)
class IssuerServiceIsolationTest {
    @Autowired private IssuerSRV service;
    @MockitoBean private IIssuerRepo repository;

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = {true, false})
    void interpretsNullableConfigurationConservatively(Boolean flag) {
        var entity = new IssuerETY();
        entity.setMockUar(flag);
        entity.setFhirBundleInResponse(flag);
        when(repository.getByName("issuer")).thenReturn(entity);
        assertEquals(Boolean.FALSE.equals(flag), service.isEdsEnabledForIssuer("issuer"));
        assertEquals(Boolean.TRUE.equals(flag), service.isFhirBundleEnabledForIssuer("issuer"));
        verify(repository, times(2)).getByName("issuer");
    }

    @Test
    void missingIssuerDisablesBothFeatures() {
        assertFalse(service.isEdsEnabledForIssuer("missing"));
        assertFalse(service.isFhirBundleEnabledForIssuer("missing"));
    }

    @Test
    void repositoryFailureDisablesBothFeatures() {
        when(repository.getByName("issuer")).thenThrow(new IllegalStateException("offline"));
        assertFalse(service.isEdsEnabledForIssuer("issuer"));
        assertFalse(service.isFhirBundleEnabledForIssuer("issuer"));
    }
}
