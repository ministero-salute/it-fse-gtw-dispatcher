package it.finanze.sanita.fse2.ms.gtw.dispatcher.isolation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static it.finanze.sanita.fse2.ms.gtw.dispatcher.client.routes.base.ClientRoutes.Config.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.IConfigClient;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.ConfigItemDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.ConfigItemTypeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.impl.ConfigSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.ProfileUtility;

@Tag("ISOLATION")
class ConfigServiceIsolationTest {
    private ConfigSRV service;
    private IConfigClient client;
    private ProfileUtility profiles;
    private static final List<String> KEYS = List.of(PROPS_NAME_REMOVE_EDS_ENABLE, PROPS_NAME_AUDIT_ENABLED,
            PROPS_NAME_CONTROL_LOG_ENABLED, PROPS_NAME_SUBJECT, PROPS_NAME_ISSUER_CF, PROPS_NAME_AUDIT_INI_ENABLED);

    @BeforeEach void setup() {
        service = new ConfigSRV();
        client = mock(IConfigClient.class);
        profiles = mock(ProfileUtility.class);
        ReflectionTestUtils.setField(service, "client", client);
        ReflectionTestUtils.setField(service, "profiles", profiles);
        ReflectionTestUtils.setField(service, "refreshRate", Long.MAX_VALUE);
    }

    private ConfigItemDTO items(Map<String, String> values) {
        var item = new ConfigItemDTO.ConfigDataItemDTO();
        item.setItems(values);
        var response = new ConfigItemDTO();
        response.setConfigurationItems(List.of(item));
        return response;
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void initializesAllFlagsAndRefreshesOnlyWhenExpired(boolean refresh) {
        when(client.getConfigurationItems(any())).thenReturn(items(KEYS.stream()
                .collect(java.util.stream.Collectors.toMap(k -> k, k -> "false"))));
        service.postConstruct();
        assertEquals(List.of(false, false, false, false, false, false), flags());
        verify(client, never()).getProps(any(), any(), any());
        if (refresh) {
            ReflectionTestUtils.setField(service, "refreshRate", 0L);
            when(client.getProps(anyString(), eq("false"), eq(ConfigItemTypeEnum.DISPATCHER))).thenReturn("true");
            assertEquals(List.of(true, true, true, true, true, true), flags());
            for (String key : KEYS) verify(client).getProps(key, "false", ConfigItemTypeEnum.DISPATCHER);
        }
    }

    private List<Boolean> flags() {
        return List.of(service.isRemoveEds(), service.isAuditEnable(), service.isControlLogPersistenceEnable(),
                service.isSubjectNotAllowed(), service.isCfOnIssuerNotAllowed(), service.isAuditIniEnable());
    }

    @Test void testProfileNeverContactsConfigurationService() {
        when(profiles.isTestProfile()).thenReturn(true);
        service.postConstruct();
        verifyNoInteractions(client);
    }

    @Test void missingRequiredConfigurationFailsInitialization() {
        var empty = new ConfigItemDTO();
        empty.setConfigurationItems(List.of());
        when(client.getConfigurationItems(any())).thenReturn(empty);
        assertTrue(assertThrows(IllegalStateException.class, service::postConstruct).getMessage().contains("Missing props"));
    }

    @Test void initializationFailureIsNotSilentlyIgnored() {
        var failure = new IllegalStateException("configuration offline");
        when(client.getConfigurationItems(any())).thenThrow(failure);
        assertSame(failure, assertThrows(IllegalStateException.class, service::postConstruct));
    }
}
